package Java_Project.history;

import java.nio.file.Path;
import java.nio.file.Paths;

// history.log 한 줄 = 이동 한 건
// 형식(탭 구분): 실행ID \t 시각 \t 원래 위치 \t 새 위치
// Windows 경로에는 탭이 들어갈 수 없어서 구분자로 안전함
public class HistoryEntry {

    private final String runId;
    private final String time;
    private final Path source;
    private final Path target;

    public HistoryEntry(String runId, String time, Path source, Path target) {
        this.runId = runId;
        this.time = time;
        this.source = source;
        this.target = target;
    }

    public String toLine() {
        return String.join("\t", runId, time, source.toString(), target.toString());
    }

    // 형식이 깨진 줄이면 null
    public static HistoryEntry parse(String line) {
        String[] parts = line.split("\t");
        if (parts.length != 4) {
            return null;
        }
        return new HistoryEntry(parts[0], parts[1], Paths.get(parts[2]), Paths.get(parts[3]));
    }

    public String getRunId() {
        return runId;
    }

    public String getTime() {
        return time;
    }

    public Path getSource() {
        return source;
    }

    public Path getTarget() {
        return target;
    }
}
