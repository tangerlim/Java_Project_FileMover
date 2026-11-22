package Java_Project.util;

import java.nio.file.Path;
import java.util.List;

// 다운로드 중이거나 프로그램이 잠깐 만드는 임시 파일인지 판별
public class TempFileFilter {

    private static final List<String> TEMP_EXTENSIONS =
            List.of(".crdownload", ".part", ".tmp", ".download");

    private TempFileFilter() { } // 유틸 클래스라 객체 생성 막기

    public static boolean isTemp(Path file) {
        String name = file.getFileName().toString().toLowerCase();

        // 오피스 잠금 파일(~$보고서.docx), 숨김 파일(.DS_Store 등)
        if (name.startsWith("~$") || name.startsWith(".")) {
            return true;
        }
        for (String ext : TEMP_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
