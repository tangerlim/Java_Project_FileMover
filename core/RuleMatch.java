package Java_Project.core;

import Java_Project.config.Rule;

import java.util.List;

// RuleMatcher의 결과: "어떤 규칙에 걸렸는지" + "정규식 그룹으로 잡힌 값들"
// groups.get(0) = 매칭된 전체 문자열, groups.get(1) = 첫 번째 ( ) ...
// namePattern이 없는 규칙이면 groups는 빈 리스트
public class RuleMatch {

    private final Rule rule;
    private final List<String> groups;

    public RuleMatch(Rule rule, List<String> groups) {
        this.rule = rule;
        this.groups = groups;
    }

    public Rule getRule() {
        return rule;
    }

    public List<String> getGroups() {
        return groups;
    }
}
