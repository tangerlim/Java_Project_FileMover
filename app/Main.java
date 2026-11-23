package Java_Project.app;

import Java_Project.config.AppConfig;
import Java_Project.config.ConfigLoader;
import Java_Project.core.ApprovalQueue;
import Java_Project.core.FileMover;
import Java_Project.core.FileProcessor;
import Java_Project.core.FolderWatcher;
import Java_Project.core.MovePlan;
import Java_Project.core.NameResolver;
import Java_Project.core.RuleMatcher;
import Java_Project.history.HistoryLog;
import Java_Project.history.UndoManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

// 실행 흐름
//   1. 설정 읽기
//   2. 폴더에 이미 있는 파일 → 미리보기 출력 → y 입력하면 이동
//   3. 실시간 감시 시작 (q 입력할 때까지 계속)
//      새 파일 감지 → 미리보기 출력 → y = 이동 / n = 건너뛰기
//      그 외 명령: u = 마지막 작업 되돌리기, q = 종료
//
// 실행 인자로 config 파일 경로를 줄 수 있음 (없으면 기본 config.json)
public class Main {
    // IntelliJ 기본 작업 디렉터리 = 프로젝트 루트 기준
    private static final String DEFAULT_CONFIG = "src/Java_Project/config.json";
    // 되돌린 파일을 감시가 다시 정리하지 않도록 무시하는 시간
    private static final long UNDO_IGNORE_MILLIS = 5000;

    public static void main(String[] args) {
        Path configPath = Paths.get(args.length > 0 ? args[0] : DEFAULT_CONFIG);

        // 1. 설정 읽기
        AppConfig config;
        try {
            config = new ConfigLoader().load(configPath);
        } catch (IOException e) {
            System.out.println("설정 파일을 읽을 수 없습니다 (" + configPath.toAbsolutePath() + "): " + e.getMessage());
            return;
        }
        Path watchDir = Paths.get(config.getWatchDir());
        Path baseDir = Paths.get(config.getBaseDir() != null ? config.getBaseDir() : config.getWatchDir());
        Path historyPath = config.getHistoryFile() != null
                ? Paths.get(config.getHistoryFile())
                : configPath.toAbsolutePath().resolveSibling("history.log");
        HistoryLog history = new HistoryLog(historyPath);
        System.out.println("규칙 " + config.getRules().size() + "개 로드: " + config.getRules());

        // 부품 조립 (정규식 문법 오류는 여기서 걸러짐)
        FileProcessor processor;
        try {
            processor = new FileProcessor(new RuleMatcher(config.getRules()),
                    new NameResolver(), new FileMover(), baseDir, history);
        } catch (IllegalArgumentException e) {
            System.out.println("규칙 오류: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // 2. 기존 파일 정리 (미리보기 → 승인)
        Set<Path> seen = organizeExisting(processor, watchDir, scanner);

        // 3. 실시간 감시 + 명령 입력
        //    seen = 2단계에서 이미 물어본 파일 → 감시 시작 직후 재검사에서 다시 묻지 않음
        runWatch(processor, new UndoManager(history), watchDir, config.getStableSeconds(), scanner, seen);
    }

    // 폴더에 이미 있는 파일: 어디로 갈지 보여주고, y를 입력해야 실제로 이동
    // 반환값 = 이번에 본 파일 목록 (감시 시작 후 "그 사이 새로 생긴 파일"을 가려내는 데 씀)
    private static Set<Path> organizeExisting(FileProcessor processor, Path watchDir, Scanner scanner) {
        List<Path> files;
        try (Stream<Path> stream = Files.list(watchDir)) {
            files = stream.filter(Files::isRegularFile).toList(); // 폴더는 제외하고 일반 파일만
        } catch (IOException e) {
            System.out.println("폴더를 읽을 수 없습니다: " + e.getMessage());
            return Set.of();
        }
        Set<Path> seen = new HashSet<>(files);

        System.out.println();
        System.out.println("[기존 파일 미리보기] " + watchDir);
        List<MovePlan> plans = processor.previewAll(files);
        if (plans.isEmpty()) {
            System.out.println("  규칙에 맞는 기존 파일이 없습니다.");
            return seen;
        }

        System.out.print("위 " + plans.size() + "개 파일을 이동할까요? (y: 이동 / n: 건너뛰기) > ");
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (answer.equalsIgnoreCase("y")) {
            processor.executeAll(plans);
            System.out.println("[기존 파일 정리 완료]");
        } else {
            System.out.println("기존 파일은 그대로 둡니다.");
        }
        return seen;
    }

    // 감시는 별도 스레드에서, main 스레드는 명령 입력 대기
    // 새 파일은 바로 옮기지 않고 ApprovalQueue에 쌓아 두었다가 y 입력 시 이동
    private static void runWatch(FileProcessor processor, UndoManager undo, Path watchDir,
                                 double stableSeconds, Scanner scanner, Set<Path> seen) {
        ApprovalQueue queue = new ApprovalQueue(processor);
        FolderWatcher watcher = new FolderWatcher(watchDir, queue::offer, (long) (stableSeconds * 1000), seen);
        Thread watchThread = new Thread(watcher, "folder-watcher");
        watchThread.start();

        // Ctrl+C나 IntelliJ 정지 버튼으로 꺼도 감시를 깔끔하게 정리
        Runtime.getRuntime().addShutdownHook(new Thread(watcher::stop));

        System.out.println();
        printHelp();
        while (scanner.hasNextLine()) {
            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("q")) {
                break;
            }
            // 모든 명령은 감시 스레드에서 실행 → 새 파일 감지/이동과 겹치지 않음
            switch (cmd) {
                case "y" -> watcher.runOnWorker(queue::approveAll);
                case "n" -> watcher.runOnWorker(queue::rejectAll);
                case "u" -> watcher.runOnWorker(() -> undo.undoLastRun(
                        path -> watcher.ignoreFor(path, UNDO_IGNORE_MILLIS)));
                case "" -> { } // 그냥 Enter는 무시
                default -> printHelp();
            }
        }
        watcher.runOnWorker(() -> {
            if (queue.size() > 0) {
                System.out.println("승인 대기 중이던 " + queue.size() + "개 파일은 옮기지 않고 종료합니다.");
            }
        });
        watcher.stop();
        try {
            watchThread.join(); // 감시 스레드가 끝날 때까지 기다림
        } catch (InterruptedException ignored) {
        }
    }

    private static void printHelp() {
        System.out.println("[실시간 감시 중] 새 파일이 생기면 미리보기를 보여주고 승인을 받습니다.");
        System.out.println("  y + Enter : 승인 대기 중인 새 파일 이동");
        System.out.println("  n + Enter : 승인 대기 중인 새 파일 건너뛰기");
        System.out.println("  u + Enter : 마지막 작업 되돌리기 (누를 때마다 하나씩 이전으로)");
        System.out.println("  q + Enter : 종료");
    }
}
