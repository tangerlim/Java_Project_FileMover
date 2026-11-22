package Java_Project.core;

import Java_Project.util.TempFileFilter;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

// 폴더에 새 파일이 생기면 감지해서 handler에 넘김 (Main에서는 ApprovalQueue::offer → 승인 대기)
//
// 흐름: 이벤트 수신 → (같은 파일이면 이전 예약 취소 = 중복 제거)
//       → stableMillis 뒤 크기 확인 → 그대로면 handler 호출 / 아직 변하면 다시 예약
//
// - WatchService는 하위 폴더까지 감시하지 않음 → 정리된 파일(하위 폴더로 이동)을 다시 감지할 일 없음
// - 실제 처리는 스레드 1개짜리 스케줄러에서 순서대로 → 같은 이름 충돌 처리가 꼬이지 않음
public class FolderWatcher implements Runnable {

    private static final int MAX_CHECKS = 150; // 너무 오래 안정 안 되면 포기 (기본 2초 × 150 = 5분)

    private final Path dir;
    private final Consumer<Path> handler; // 다운로드가 끝난(크기가 안정된) 파일을 받을 곳
    private final long stableMillis;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // 파일별 "처리 예약". 감시 스레드와 스케줄러 스레드가 같이 쓰므로 ConcurrentHashMap
    private final Map<Path, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    // 잠깐 무시할 파일 → 무시 끝나는 시각(ms). 되돌리기로 돌아온 파일을 다시 정리하지 않기 위해
    private final Map<Path, Long> ignoreUntil = new ConcurrentHashMap<>();
    private WatchService watchService;

    public FolderWatcher(Path dir, Consumer<Path> handler, long stableMillis) {
        this.dir = dir;
        this.handler = handler;
        this.stableMillis = stableMillis;
    }

    // 별도 스레드에서 실행됨: new Thread(watcher).start()
    @Override
    public void run() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
            System.out.println("[감시 시작] " + dir);

            while (true) {
                WatchKey key = watchService.take(); // 이벤트 올 때까지 대기 (blocking)

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) {
                        // 이벤트가 너무 많이 와서 일부 유실됨 → 폴더 전체 다시 훑기
                        System.out.println("[감시] 이벤트 유실, 폴더 전체 재검사");
                        rescanAll();
                        continue;
                    }
                    Path name = (Path) event.context();   // 파일 이름만 들어 있음
                    onEvent(dir.resolve(name));
                }

                if (!key.reset()) { // 감시 폴더가 삭제됐거나 접근 불가
                    System.out.println("[감시 종료] 폴더에 접근할 수 없습니다: " + dir);
                    break;
                }
            }
        } catch (ClosedWatchServiceException | InterruptedException e) {
            // stop()이 호출된 정상 종료
        } catch (IOException e) {
            System.out.println("[감시 실패] " + e.getMessage());
        } finally {
            scheduler.shutdownNow();
        }
        System.out.println("[감시 종료]");
    }

    // 감시 종료 (다른 스레드에서 호출) → take()가 예외를 던지며 루프 탈출
    public void stop() {
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {
        }
        scheduler.shutdownNow();
    }

    // 이 파일에 대한 이벤트를 millis 동안 무시
    public void ignoreFor(Path file, long millis) {
        ignoreUntil.put(file, System.currentTimeMillis() + millis);
    }

    // 작업을 파일 처리와 같은 스레드에서 실행하고 끝날 때까지 기다림
    // → 되돌리기와 자동 정리가 동시에 같은 파일을 건드리는 일 방지
    public void runOnWorker(Runnable task) {
        try {
            scheduler.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | RejectedExecutionException e) {
            System.out.println("[감시] 작업 실행 실패: " + e.getMessage());
        }
    }

    private boolean isIgnored(Path file) {
        Long until = ignoreUntil.get(file);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            ignoreUntil.remove(file); // 시간 지났으면 정리
            return false;
        }
        return true;
    }

    private void onEvent(Path file) {
        if (TempFileFilter.isTemp(file) || Files.isDirectory(file) || isIgnored(file)) {
            return; // 다운로드 중인 파일, 폴더, 되돌리기로 돌아온 파일은 무시
        }
        // 같은 파일에 이벤트가 연달아 오면 이전 예약을 취소하고 새로 예약 → 마지막 한 번만 처리
        ScheduledFuture<?> old = pending.remove(file);
        if (old != null) {
            old.cancel(false);
        }
        scheduleCheck(file, sizeOf(file), 1);
    }

    private void scheduleCheck(Path file, long lastSize, int attempt) {
        ScheduledFuture<?> future = scheduler.schedule(
                () -> checkAndProcess(file, lastSize, attempt), stableMillis, TimeUnit.MILLISECONDS);
        pending.put(file, future);
    }

    // stableMillis 동안 크기가 안 변했고, 다른 프로그램이 잠그고 있지 않으면 처리
    private void checkAndProcess(Path file, long lastSize, int attempt) {
        if (!Files.exists(file)) {
            pending.remove(file); // 그 사이 삭제/이름 변경됨
            return;
        }
        long size = sizeOf(file);
        boolean stable = (size == lastSize) && !isLocked(file);

        if (stable) {
            pending.remove(file);
            if (!isIgnored(file)) { // 기다리는 사이 되돌리기로 무시 대상이 됐을 수도 있음
                handler.accept(file);
            }
        } else if (attempt < MAX_CHECKS) {
            scheduleCheck(file, size, attempt + 1); // 아직 쓰는 중 → 다시 기다림
        } else {
            pending.remove(file);
            System.out.println("[감시] 파일이 계속 변경 중이라 건너뜀: " + file.getFileName());
        }
    }

    private void rescanAll() {
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(this::onEvent);
        } catch (IOException e) {
            System.out.println("[감시] 재검사 실패: " + e.getMessage());
        }
    }

    private long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return -1;
        }
    }

    // Windows에서는 다른 프로그램이 쓰는 중인 파일을 쓰기 모드로 열 수 없음 → 잠김 판단
    private boolean isLocked(Path file) {
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.WRITE)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
