package Java_Project.config;

import java.util.List;

// config.json 파일 전체 구조
// {
//   "watchDir": "...",
//   "baseDir":  "...",
//   "rules":    [ { Rule }, { Rule }, ... ]
// }
public class AppConfig {

    private String watchDir;   // 감시(정리)할 폴더
    private String baseDir;    // 규칙의 target이 상대경로일 때 기준 폴더
    private List<Rule> rules;  // 위에서부터 순서대로 검사, 처음 맞는 규칙 사용
    private double stableSeconds = 2; // 감시 모드: 파일 크기가 이 시간 동안 그대로면 처리 (선택)
    private String historyFile;       // 처리 기록 파일 (선택). 없으면 config.json 옆의 history.log

    public String getHistoryFile() {
        return historyFile;
    }

    public double getStableSeconds() {
        return stableSeconds;
    }

    public String getWatchDir() {
        return watchDir;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public List<Rule> getRules() {
        return rules;
    }
}
