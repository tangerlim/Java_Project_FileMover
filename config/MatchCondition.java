package Java_Project.config;

import java.util.List;

// 규칙의 "조건" 부분. config.json의 "match": { ... } 와 1:1로 대응
// 적힌 조건은 모두 만족해야 매칭 (비워둔 조건은 검사하지 않음)
public class MatchCondition {

    private List<String> extensions; // 예: ["pdf", "pptx"]  (점 없이, 대소문자 무시)
    private String glob;             // 예: "*김우성*"  (파일명 와일드카드)
    private String namePattern;      // 예: "lecture_(\\d+)"  (정규식, 파일명 일부만 맞아도 OK)
                                     //     괄호 ( ) 로 잡은 부분은 rename에서 {1}, {2}로 사용

    public List<String> getExtensions() {
        return extensions;
    }

    public String getGlob() {
        return glob;
    }

    public String getNamePattern() {
        return namePattern;
    }
}
