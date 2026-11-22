package Java_Project.core;

import java.nio.file.Path;

public class MovePlan {

    private final Path source;       // 옮길 파일
    private final Path targetDir;    // 갈 폴더
    private final String targetName; // 갈 때 붙일 이름 (rename 없으면 원래 이름)

    // 생성자: new MovePlan(파일, 폴더, 이름)로 만들 때 실행됨
    public MovePlan(Path source, Path targetDir, String targetName) {
        this.source = source;
        this.targetDir = targetDir;
        this.targetName = targetName;
    }

    public Path getSource() {
        return source;
    }

    public Path getTargetDir() {
        return targetDir;
    }

    public String getTargetName() {
        return targetName;
    }
}
