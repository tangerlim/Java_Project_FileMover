package Java_Project.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// rename 패턴의 {토큰}을 실제 값으로 치환해서 새 파일명을 만듦
//
//   {name}   원래 파일명에서 확장자 뺀 부분     lecture_03.pdf -> lecture_03
//   {ext}    확장자 (점 없이)                  -> pdf
//   {0}      정규식에 매칭된 전체 문자열
//   {1},{2}  정규식 괄호 그룹
//   그 외    날짜 형식으로 처리 ({yyyy-MM}, {yyyyMMdd} 등) — 파일 수정 시각 기준
//
// 예) "데이터통신_{1}주차.{ext}" + lecture_03.pdf  ->  데이터통신_03주차.pdf
public class NameResolver {

    // { 와 } 사이에 } 가 아닌 문자 1개 이상 → 그룹 1이 토큰 이름
    private static final Pattern TOKEN = Pattern.compile("\\{([^}]+)}");
    // Windows 파일명에 쓸 수 없는 문자
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    public String resolve(String pattern, String originalName, List<String> groups, LocalDateTime time) {
        int dot = originalName.lastIndexOf('.');
        String baseName = (dot == -1) ? originalName : originalName.substring(0, dot);
        String ext = (dot == -1) ? "" : originalName.substring(dot + 1);

        Matcher m = TOKEN.matcher(pattern);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group(1);
            String value = switch (token) {
                case "name" -> baseName;
                case "ext" -> ext;
                default -> token.matches("\\d+")
                        ? groupValue(groups, Integer.parseInt(token))
                        : formatDate(token, time);
            };
            // 값 안의 $나 \ 가 특수문자로 해석되지 않도록 quoteReplacement
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);

        return sanitize(sb.toString());
    }

    private String groupValue(List<String> groups, int index) {
        // namePattern이 없는 규칙이면 groups가 빈 리스트 → "괄호가 -1개" 대신 원인을 바로 안내
        if (groups.isEmpty()) {
            throw new IllegalArgumentException(
                    "namePattern이 없는 규칙에서는 {" + index + "}을 쓸 수 없습니다 (rename에는 {name}, {ext}, 날짜만 가능)");
        }
        if (index >= groups.size()) {
            throw new IllegalArgumentException(
                    "{" + index + "}에 해당하는 정규식 그룹이 없습니다 (namePattern에 괄호가 " + (groups.size() - 1) + "개)");
        }
        return groups.get(index);
    }

    private String formatDate(String token, LocalDateTime time) {
        try {
            return time.format(DateTimeFormatter.ofPattern(token));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 토큰: {" + token + "}");
        }
    }

    // 금지 문자는 _ 로, 끝의 점/공백은 제거 (Windows에서 허용 안 됨)
    private String sanitize(String name) {
        String cleaned = ILLEGAL_CHARS.matcher(name).replaceAll("_").strip();
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("변환 결과 파일명이 비어 있습니다");
        }
        return cleaned;
    }
}
