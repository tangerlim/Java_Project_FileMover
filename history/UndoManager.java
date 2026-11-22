package Java_Project.history;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

// 마지막 실행분을 역순으로 되돌리기 (새 위치 → 원래 위치)
//
// 안전 규칙
//   - 옮겨진 파일이 이미 없으면(사용자가 지움/옮김) 건너뜀
//   - 원래 자리에 같은 이름 파일이 새로 생겼으면 덮어쓰지 않고 건너뜀
//   - 성공한 항목만 기록에서 지움 → 실패한 항목은 남아서 다음에 다시 시도 가능
public class UndoManager {

    private final HistoryLog log;

    public UndoManager(HistoryLog log) {
        this.log = log;
    }

    public void undoLastRun() {
        undoLastRun(path -> { });
    }

    // beforeRestore: 파일을 원래 위치로 옮기기 직전에 호출됨
    //   → 감시 중이면 이 경로를 잠깐 무시하게 해서, 되돌린 파일을 다시 정리해버리는 일 방지
    public void undoLastRun(Consumer<Path> beforeRestore) {
        List<HistoryEntry> all;
        try {
            all = log.readAll();
        } catch (IOException e) {
            System.out.println("기록을 읽을 수 없습니다: " + e.getMessage());
            return;
        }
        if (all.isEmpty()) {
            System.out.println("되돌릴 기록이 없습니다 (" + log.getLogFile().toAbsolutePath() + ")");
            return;
        }

        // 마지막 줄의 실행ID = 가장 최근 실행
        String lastRun = all.get(all.size() - 1).getRunId();
        List<HistoryEntry> targets = all.stream().filter(e -> e.getRunId().equals(lastRun)).toList();
        System.out.println("[되돌리기] " + targets.get(0).getTime() + " 작업 " + targets.size() + "건");

        List<HistoryEntry> failed = new ArrayList<>();
        int done = 0;
        // 역순: 나중에 옮긴 것부터 되돌려야 (1), (2) 이름이 꼬이지 않음
        for (int i = targets.size() - 1; i >= 0; i--) {
            HistoryEntry e = targets.get(i);
            try {
                beforeRestore.accept(e.getSource());
                restore(e);
                System.out.println(e.getTarget().getFileName() + " -> " + e.getSource());
                done++;
            } catch (IOException ex) {
                System.out.println("되돌리기 실패 : " + e.getTarget().getFileName() + " (" + ex.getMessage() + ")");
                failed.add(e);
            }
        }

        // 이번에 되돌린 실행분은 지우고, 실패한 것만 남김
        List<HistoryEntry> remaining = new ArrayList<>(all.stream().filter(e -> !e.getRunId().equals(lastRun)).toList());
        Collections.reverse(failed); // 원래 기록 순서로 되돌려서 저장
        remaining.addAll(failed);
        try {
            log.rewrite(remaining);
        } catch (IOException e) {
            System.out.println("기록 갱신 실패: " + e.getMessage());
        }
        System.out.println("[되돌리기 완료] 성공 " + done + "건, 실패 " + failed.size() + "건");
    }

    private void restore(HistoryEntry e) throws IOException {
        if (!Files.exists(e.getTarget())) {
            throw new IOException("옮겨진 파일이 없습니다: " + e.getTarget());
        }
        if (Files.exists(e.getSource())) {
            throw new IOException("원래 위치에 같은 이름 파일이 이미 있습니다: " + e.getSource());
        }
        Files.createDirectories(e.getSource().getParent());
        Files.move(e.getTarget(), e.getSource());
    }
}
