package io.github.likeelysia.formalagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用配置:从 {@code application.yml} 的 {@code fa.*} 绑定过来(原 {@code @Value} 版的升级)。
 *
 * <p>好处:
 * <ol>
 *   <li><b>类型安全</b>:名字/类型写错,启动就报错,不用等运行期;</li>
 *   <li><b>分组</b>:deepseek / vision / embedding 各成一块,IDE 能补全、能跳转;</li>
 *   <li><b>松散绑定</b>:yml 里写 {@code max-tokens},Java 里叫 {@code maxTokens} 也能对上;</li>
 *   <li>支持 <b>profile</b> 与环境变量覆盖(见 application.yml 的 {@code ${FA_...:默认值}})。</li>
 * </ol>
 *
 * <p>注册方式见 {@link SpringConfig} 上的 {@code @EnableConfigurationProperties}
 * —— 这样普通 Spring 上下文(测试)也能用。
 */
@ConfigurationProperties(prefix = "fa")
public record AppConfig(
        boolean debug,
        Llm llm,
        Deepseek deepseek,
        Vision vision,
        Embedding embedding) {

    /** 调用 LLM 的通用参数(超时 / 重试)。 */
    public record Llm(int requestTimeoutSeconds, int connectTimeoutSeconds,
                      int maxAttempts, long retryBaseMs, long retryMaxMs) {
    }

    /** DeepSeek:文本对话 / 知识点提炼。 */
    public record Deepseek(String apiUrl, String model, int maxTokens) {
    }

    /** Kimi 视觉:扫描件 OCR / 图片问答。 */
    public record Vision(String apiUrl, String model, int maxTokens) {
    }

    /** 本地 embedding 服务。 */
    public record Embedding(String url, String model) {
    }
}
