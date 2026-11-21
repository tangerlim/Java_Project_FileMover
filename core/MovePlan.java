package Java_Project.core;

import java.nio.file.Path;

public class MovePlan {

    private final Path source;     // 옮길 파일
    private final Path targetDir;  // 갈 폴더

    // 생성자: new MovePlan(파일, 폴더)로 만들 때 실행됨
    public MovePlan(Path source, Path targetDir) {
        this.source = source;
        this.targetDir = targetDir;
    }

    public Path getSource() {
        return source;
    }

    public Path getTargetDir() {
        return targetDir;
    }
}