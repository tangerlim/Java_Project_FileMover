package Java_Project.history;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// 처리 내역을 파일에 기록/읽기
// 이동할 때마다 바로 한 줄씩 추가(append) → 도중에 프로그램이 꺼져도 기록은 남음
//
// "실행(run)" = 되돌리기 한 번에 되돌아가는 단위
//   - 기존 파일 일괄 이동: 승인한 묶음 전체가 실행 1번
//   - 실시간 감시: 새 파일 하나하나가 각각 실행 1번
public class HistoryLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter RUN_ID = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Path logFile;
    private String runId;      // 지금 기록 중인 실행의 ID
    private String lastRunId;  // 같은 밀리초에 두 번 시작해도 ID가 겹치지 않게
    private int sameIdCount;

    public HistoryLog(Path logFile) {
        this.logFile = logFile;
    }

    public Path getLogFile() {
        return logFile;
    }

    // 새 실행 시작 → 이후 record()는 이 ID로 기록됨
    public synchronized void startRun() {
        String id = LocalDateTime.now().format(RUN_ID);
        if (id.equals(lastRunId)) {
            sameIdCount++;
            runId = id + "#" + sameIdCount;
        } else {
            sameIdCount = 0;
            runId = id;
        }
        lastRunId = id;
    }

    // 감시 스레드에서도 불리므로 synchronized (한 번에 한 스레드만 쓰기)
    public synchronized void record(Path source, Path target) throws IOException {
        if (runId == null) {
            startRun();
        }
        HistoryEntry entry = new HistoryEntry(runId, LocalDateTime.now().format(TIME), source, target);
        Files.writeString(logFile, entry.toLine() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // 전체 기록 읽기 (파일이 없으면 빈 리스트)
    public synchronized List<HistoryEntry> readAll() throws IOException {
        List<HistoryEntry> entries = new ArrayList<>();
        if (!Files.exists(logFile)) {
            return entries;
        }
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
            HistoryEntry e = HistoryEntry.parse(line);
            if (e != null) {
                entries.add(e);
            }
        }
        return entries;
    }

    // 기록 전체를 다시 쓰기 (되돌린 항목을 지울 때 사용)
    public synchronized void rewrite(List<HistoryEntry> entries) throws IOException {
        List<String> lines = entries.stream().map(HistoryEntry::toLine).toList();
        Files.write(logFile, lines, StandardCharsets.UTF_8);
    }
}
