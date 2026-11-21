package Java_Project.app;

import Java_Project.core.FileMover;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        Path watchDir = Paths.get("C:\\Users\\likgr\\Downloads");
        Path baseDir = Paths.get("C:\\Users\\likgr\\Desktop\\--");
        FileMover mover = new FileMover();

        List<Path> files; // try 블록 밖에서도 써야 하므로 바깥에 선언
        try (Stream<Path> stream = Files.list(watchDir)) {
            files = stream.filter(Files::isRegularFile).toList(); // 폴더는 제외하고 일반 파일만
        } catch (IOException e) {
            System.out.println("폴더를 읽을 수 없습니다: " + e.getMessage());
            return;
        }

        // for문 바깥에서 미리 만들어두기
        PathMatcher osMatcher =
                FileSystems.getDefault().getPathMatcher("glob:*김우성*");
        PathMatcher dataCommMatcher =
                FileSystems.getDefault().getPathMatcher("glob:ch[0-9]*");
        PathMatcher webMatcher =
                FileSystems.getDefault().getPathMatcher("glob:*주차*수업자료*");
        PathMatcher javaMatcher =
                FileSystems.getDefault().getPathMatcher("glob:*Java*");

        for (Path file : files) {
            Path fileName = file.getFileName();
            Path targetDir;

            if (osMatcher.matches(fileName)) {
                targetDir = baseDir.resolve("운영체제");
            } else if (dataCommMatcher.matches(fileName)) {
                targetDir = baseDir.resolve("데이터통신");
            } else if (webMatcher.matches(fileName)) {
                targetDir = baseDir.resolve("고웹프");
            } else if (javaMatcher.matches(fileName)) {
                targetDir = baseDir.resolve("Java");
            } else {
                continue;
            }
            try {
                Path moved = mover.move(file, targetDir);
                System.out.println(fileName + " -> " + moved);
            } catch (IOException e) {
                System.out.println("이동 실패 : " + fileName + " (" + e.getMessage() + ")");
            }
        }
    }
}
