package Java_Project.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// config.json을 읽어서 AppConfig(+ List<Rule>) 객체로 변환
public class ConfigLoader {

    private final Gson gson = new Gson();

    public AppConfig load(Path configFile) throws IOException {
        AppConfig config;
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, AppConfig.class);
        } catch (JsonParseException e) {
            // JSON 문법 오류를 IOException으로 감싸서 Main에서 한 번에 처리
            throw new IOException("config.json 형식 오류: " + e.getMessage(), e);
        }

        validate(config);
        return config;
    }

    // 필수 값이 빠졌으면 실행 전에 바로 알려주기
    private void validate(AppConfig config) throws IOException {
        if (config == null) {
            throw new IOException("config.json이 비어 있습니다");
        }
        if (config.getWatchDir() == null) {
            throw new IOException("watchDir가 없습니다");
        }
        if (config.getRules() == null || config.getRules().isEmpty()) {
            throw new IOException("rules가 비어 있습니다");
        }
        for (Rule rule : config.getRules()) {
            if (rule.getMatch() == null || rule.getTarget() == null) {
                throw new IOException("규칙 '" + rule.getName() + "'에 match 또는 target이 없습니다");
            }
        }
    }
}
