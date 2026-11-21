package Java_Project.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMover {
    public Path move(Path source, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path target = resolveUniqueTarget(targetDir, source.getFileName().toString());

        return Files.move(source, target);
    }

    private Path resolveUniqueTarget(Path targetDir, String fileName) {
        Path candidate = targetDir.resolve(fileName);

        // 1. 겹치지 않으면 원래 이름 그대로
        if (!Files.exists(candidate)) {
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
        while (Files.exists(candidate)) {
            candidate = targetDir.resolve(baseName + " (" + count + ")" + extension);
            count++;
        }
        return candidate;
    }
}