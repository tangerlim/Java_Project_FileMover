package Java_Project.config;

// 규칙 하나 = 이름 + 조건 + 옮길 폴더 (+ 바꿀 이름)
// Gson이 기본 생성자로 객체를 만든 뒤 필드에 값을 채워 넣음
public class Rule {

    private String name;            // 로그 출력용 이름
    private MatchCondition match;   // 어떤 파일이 해당하는지
    private String target;          // 옮길 폴더 (baseDir 기준 상대경로 or 절대경로)
    private String rename;          // 새 파일명 패턴 (선택). 없으면 원래 이름 유지
                                    // 예: "데이터통신_{1}주차.{ext}"

    public String getName() {
        return name;
    }

    public MatchCondition getMatch() {
        return match;
    }

    public String getTarget() {
        return target;
    }

    public String getRename() {
        return rename;
    }

    @Override
    public String toString() {
        return "Rule{" + name + " -> " + target + (rename != null ? " / " + rename : "") + "}";
    }
}
