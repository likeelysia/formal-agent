package io.github.likeelysia.formalagent.prompt;

import io.github.likeelysia.formalagent.exception.AgentException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;

/**
 * 提示词加载器:把散落在各处的提示词搬到 {@code resources/prompts/*.txt}。
 *
 * <p><b>为什么要外置</b>:提示词是"要反复调的文案",不该跟 Java 代码搅在一起 ——
 * 改一句话不用重新读代码、不用重新编译理解上下文;以后还能按环境放不同的版本。
 *
 * <p>读取后<b>缓存</b>(同一份只读一次);文件缺失会 fail-fast 报错,而不是悄悄用个空提示词。
 */
public final class Prompts {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private Prompts() {
    }

    /** 按名字取提示词(对应 {@code resources/prompts/<name>.txt})。 */
    public static String get(String name) {
        return CACHE.computeIfAbsent(name, Prompts::read);
    }

    private static String read(String name) {
        String path = "prompts/" + name + ".txt";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new AgentException("提示词文件读不到:" + path, e);
        }
    }
}
