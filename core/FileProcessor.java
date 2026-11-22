package Java_Project.core;

import Java_Project.config.Rule;
import Java_Project.history.HistoryLog;
import Java_Project.util.TempFileFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// "임시파일 거르기 → 규칙 매칭 → 이름 결정 → 이동 → 기록" 흐름을 묶음
//
// 모든 이동은 "미리보기 → 승인 → 이동" 순서
//   - 기존 파일: previewAll()로 보여주고 → 승인하면 executeAll()
//   - 새 파일:   ApprovalQueue가 plan()/printPlans()로 보여주고 → 승인하면 executeAll()
public class FileProcessor {

    private final RuleMatcher matcher;
    private final NameResolver nameResolver;
    private final FileMover mover;
    private final Path baseDir;
    private final HistoryLog history;

    public FileProcessor(RuleMatcher matcher, NameResolver nameResolver, FileMover mover,
                         Path baseDir, HistoryLog history) {
        this.matcher = matcher;
        this.nameResolver = nameResolver;
        this.mover = mover;
        this.baseDir = baseDir;
        this.history = history;
    }

    // 파일 하나를 어디로, 어떤 이름으로 보낼지 계획만 세움 (실제로 옮기지 않음)
    // 규칙에 안 맞거나 임시 파일이면 빈 Optional
    public Optional<MovePlan> plan(Path file) throws IOException {
        if (TempFileFilter.isTemp(file)) {
            return Optional.empty();
        }
        Optional<RuleMatch> found = matcher.findRule(file);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Rule rule = found.get().getRule();

        // target이 절대경로면 그대로, 상대경로면 baseDir 기준
        Path targetDir = baseDir.resolve(rule.getTarget());

        // rename이 없으면 원래 이름 유지
        String originalName = file.getFileName().toString();
        String newName = originalName;
        if (rule.getRename() != null) {
            newName = nameResolver.resolve(rule.getRename(), originalName,
                    found.get().getGroups(), modifiedTime(file));
        }

        return Optional.of(new MovePlan(file, targetDir, newName));
    }

    // 미리보기: 여러 파일의 계획을 세워 출력하고, 옮길 목록을 돌려줌 (이동/기록 없음)
    public List<MovePlan> previewAll(List<Path> files) {
        List<MovePlan> plans = new ArrayList<>();
        for (Path file : files) {
            try {
                plan(file).ifPresent(plans::add);
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("  (제외) " + file.getFileName() + " : " + e.getMessage());
            }
        }
        printPlans(plans);
        return plans;
    }

    // 계획 목록을 번호 붙여 출력. (1), (2)가 붙을 최종 이름까지 예측해서 보여줌
    public void printPlans(List<MovePlan> plans) {
        // 앞 파일들이 차지할 경로 (아직 안 옮겼으니 따로 기억해야 정확히 예측)
        Set<Path> reserved = new HashSet<>();
        int no = 1;
        for (MovePlan p : plans) {
            Path target = mover.previewTarget(p.getTargetDir(), p.getTargetName(), reserved);
            reserved.add(target);
            System.out.printf("  %2d. %s%n      -> %s%n", no++, p.getSource().getFileName(), target);
        }
    }

    // 승인된 목록을 실제로 이동. 묶음 전체가 되돌리기 1번 단위
    public void executeAll(List<MovePlan> plans) {
        history.startRun();
        for (MovePlan p : plans) {
            execute(p);
        }
    }

    // 실제 이동 + 기록. 실패해도 예외를 밖으로 던지지 않음 → 다음 파일은 계속 처리
    private void execute(MovePlan p) {
        Path fileName = p.getSource().getFileName();
        try {
            Path moved = mover.move(p.getSource(), p.getTargetDir(), p.getTargetName());
            System.out.println(fileName + " -> " + moved);
            recordSafely(p.getSource(), moved);
        } catch (IOException e) {
            System.out.println("이동 실패 : " + fileName + " (" + e.getMessage() + ")");
        }
    }

    // 이동은 이미 끝났으니, 기록 실패로 처리 전체를 실패로 보지는 않음 (경고만)
    private void recordSafely(Path source, Path target) {
        try {
            history.record(source, target);
        } catch (IOException e) {
            System.out.println("  (경고) 기록 실패 — 이 파일은 되돌리기 불가: " + e.getMessage());
        }
    }

    // {yyyy-MM} 같은 날짜 토큰 기준 = 파일 마지막 수정 시각 (보통 다운로드한 시각)
    private LocalDateTime modifiedTime(Path file) throws IOException {
        return LocalDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(), ZoneId.systemDefault());
    }
}
