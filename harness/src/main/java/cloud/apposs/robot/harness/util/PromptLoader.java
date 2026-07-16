package cloud.apposs.robot.harness.util;

import cloud.apposs.util.Param;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PromptLoader {
    private static final String PROMPT_PATH_PREFIX = "prompt/";

    // 缓存从磁盘中加载的提示词模板，避免重复读取文件
    private static final Map<String, String> promptCache = new ConcurrentHashMap<>();

    public static String readPrompt(String promptName) throws Exception {
        return readPrompt(promptName, null);
    }

    /**
     * 加载指定名称的 prompt 模板，并进行参数替换
     *
     * @param  promptName  prompt 模板名称（不带路径前缀和文件后缀）
     * @param  replacement 需要替换的参数键值对，键对应模板中的占位符（格式为 {key}），值为替换内容
     * @return 加载并替换后的 prompt 模板内容
     */
    public static String readPrompt(String promptName, Param replacement) throws Exception {
        String prompt = promptCache.computeIfAbsent(promptName, name -> {
            try {
                return handlePromptFileRead(name);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load prompt: " + name, e);
            }
        });
        if (replacement != null) {
            for (Map.Entry<String, Object> replace : replacement.entrySet()) {
                prompt = prompt.replace("{" + replace.getKey() + "}", String.valueOf(replace.getValue()));
            }
        }
        return prompt;
    }

    private static String handlePromptFileRead(String name) throws IOException {
        String fileName = PROMPT_PATH_PREFIX + name + ".md";
        try (InputStream inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("Prompt file not found: " + fileName);
            }
            return handleCopyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private static String handleCopyToString(InputStream in, Charset charset) throws IOException {
        if (in == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(in, charset);
        char[] buffer = new char[8192];

        int charsRead;
        while((charsRead = reader.read(buffer)) != -1) {
            out.append(buffer, 0, charsRead);
        }
        return out.toString();
    }
}
