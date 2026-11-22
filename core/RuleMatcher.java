package Java_Project.core;

import Java_Project.config.MatchCondition;
import Java_Project.config.Rule;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// 파일 하나를 받아서 맞는 Rule을 찾아줌 (위에서부터 처음 맞는 것)
public class RuleMatcher {

    private final List<Rule> rules;
    // 문자열 -> 컴파일된 객체. 파일마다 새로 만들지 않도록 미리 만들어 둠
    private final Map<String, PathMatcher> globCache = new HashMap<>();
    private final Map<String, Pattern> regexCache = new HashMap<>();

    // 정규식 문법이 틀렸으면 프로그램 시작 시점에 바로 알 수 있도록 생성자에서 컴파일
    public RuleMatcher(List<Rule> rules) {
        this.rules = rules;
        for (Rule rule : rules) {
            MatchCondition cond = rule.getMatch();
            if (cond.getGlob() != null) {
                globCache.put(cond.getGlob(),
                        FileSystems.getDefault().getPathMatcher("glob:" + cond.getGlob()));
            }
            if (cond.getNamePattern() != null) {
                try {
                    // 대소문자 무시: Lecture_03 / lecture_03 모두 매칭
                    regexCache.put(cond.getNamePattern(),
                            Pattern.compile(cond.getNamePattern(), Pattern.CASE_INSENSITIVE));
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException(
                            "규칙 '" + rule.getName() + "'의 namePattern 오류: " + e.getDescription(), e);
                }
            }
        }
    }

    public Optional<RuleMatch> findRule(Path file) {
        for (Rule rule : rules) {
            List<String> groups = matches(rule.getMatch(), file);
            if (groups != null) {
                return Optional.of(new RuleMatch(rule, groups));
            }
        }
        return Optional.empty();
    }

    // 매칭되면 정규식 그룹 리스트(없으면 빈 리스트), 안 되면 null
    private List<String> matches(MatchCondition cond, Path file) {
        String fileName = file.getFileName().toString();

        // 1. 확장자 조건
        List<String> extensions = cond.getExtensions();
        if (extensions != null && !extensions.isEmpty()) {
            String ext = extensionOf(fileName);
            boolean extOk = extensions.stream().anyMatch(e -> e.equalsIgnoreCase(ext));
            if (!extOk) {
                return null;
            }
        }

        // 2. 파일명 와일드카드 조건
        if (cond.getGlob() != null) {
            if (!globCache.get(cond.getGlob()).matches(file.getFileName())) {
                return null;
            }
        }

        // 3. 정규식 조건 — find(): 파일명 어딘가에 패턴이 있으면 OK
        //    (전체가 정확히 맞아야 하면 패턴을 ^...$ 로 감싸기)
        List<String> groups = new ArrayList<>();
        if (cond.getNamePattern() != null) {
            Matcher m = regexCache.get(cond.getNamePattern()).matcher(fileName);
            if (!m.find()) {
                return null;
            }
            for (int i = 0; i <= m.groupCount(); i++) {
                // 선택적 그룹 (..)? 이 비어 있으면 null이 나오므로 빈 문자열로
                groups.add(m.group(i) != null ? m.group(i) : "");
            }
        }

        return groups;
    }

    // "lecture_03.pdf" -> "pdf", 확장자 없으면 ""
    static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot == -1) ? "" : fileName.substring(dot + 1);
    }
}
