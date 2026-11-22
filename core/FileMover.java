package Java_Project.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class FileMover {
    // 원래 이름 그대로 이동
    public Path move(Path source, Path targetDir) throws IOException {
        return move(source, targetDir, source.getFileName().toString());
    }

    // 새 이름으로 이동 (오버로딩)
    public Path move(Path source, Path targetDir, String newName) throws IOException {
        Files.createDirectories(targetDir);
        Path target = resolveUniqueTarget(targetDir, newName, Set.of());

        return Files.move(source, target);
    }

    // 미리보기용: 실제로 옮기지 않고 "옮긴다면 붙을 최종 경로"만 계산
    // reserved = 이번 미리보기에서 앞 파일들이 이미 차지한 경로 (실제 파일은 아직 없으니 따로 관리)
    public Path previewTarget(Path targetDir, String newName, Set<Path> reserved) {
        return resolveUniqueTarget(targetDir, newName, reserved);
    }

    private Path resolveUniqueTarget(Path targetDir, String fileName, Set<Path> reserved) {
        Path candidate = targetDir.resolve(fileName);

        // 1. 겹치지 않으면 원래 이름 그대로
        if (!isTaken(candidate, reserved)) {
            return candidate;
        }

        // 2. 이름과 확장자 분리 ("3주차 강의자료.pdf" → "3주차 강의자료", ".pdf")
        String baseName;
        String extension;
        int dot = fileName.lastIndexOf('.');
        if (dot == -1) {
            baseName = fileName;
            extension = "";
        } else {
            baseName = fileName.substring(0, dot);
            extension = fileName.substring(dot);   // 점 포함
        }

        // 3. 빈 이름이 나올 때까지 번호 증가
        int count = 1;
        while (isTaken(candidate, reserved)) {
            candidate = targetDir.resolve(baseName + " (" + count + ")" + extension);
            count++;
        }
        return candidate;
    }

    private boolean isTaken(Path candidate, Set<Path> reserved) {
        return Files.exists(candidate) || reserved.contains(candidate);
    }
}
