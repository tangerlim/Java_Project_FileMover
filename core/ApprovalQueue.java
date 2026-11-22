package Java_Project.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 실시간 감시로 들어온 새 파일을 바로 옮기지 않고 "승인 대기"에 모아 두는 곳
//
//   새 파일 감지 → offer() → 미리보기 출력 + 질문
//   사용자 y → approveAll() → 대기 중인 파일 전부 이동 (되돌리기 1번 단위)
//   사용자 n → rejectAll()  → 전부 그대로 둠
//
// 모든 메서드는 감시 스레드(FolderWatcher.runOnWorker)에서만 호출 → 동시 접근 걱정 없음
public class ApprovalQueue {

    private final FileProcessor processor;
    private final List<MovePlan> pending = new ArrayList<>();

    public ApprovalQueue(FileProcessor processor) {
        this.processor = processor;
    }

    // 안정된 새 파일이 들어옴. 규칙에 맞으면 대기 목록에 넣고 질문
    public void offer(Path file) {
        // 같은 파일이 이미 대기 중이면 다시 묻지 않음
        boolean already = pending.stream().anyMatch(p -> p.getSource().equals(file));
        if (already) {
            return;
        }
        Optional<MovePlan> plan;
        try {
            plan = processor.plan(file);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("[새 파일] 처리 불가 : " + file.getFileName() + " (" + e.getMessage() + ")");
            return;
        }
        if (plan.isEmpty()) {
            return; // 규칙에 안 맞는 파일은 묻지 않고 그대로 둠
        }
        pending.add(plan.get());

        System.out.println();
        System.out.println("[새 파일 미리보기] 승인 대기 " + pending.size() + "개");
        processor.printPlans(pending);
        System.out.println("이동할까요? (y: 이동 / n: 건너뛰기)");
    }

    public void approveAll() {
        if (pending.isEmpty()) {
            System.out.println("승인 대기 중인 파일이 없습니다.");
            return;
        }
        processor.executeAll(new ArrayList<>(pending));
        pending.clear();
    }

    public void rejectAll() {
        if (pending.isEmpty()) {
            System.out.println("승인 대기 중인 파일이 없습니다.");
            return;
        }
        System.out.println(pending.size() + "개 파일은 그대로 둡니다.");
        pending.clear();
    }

    public int size() {
        return pending.size();
    }
}
