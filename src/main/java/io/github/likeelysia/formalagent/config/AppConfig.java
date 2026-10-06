package io.github.likeelysia.formalagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 应用配置:原来是我自己读 properties 的静态工具类,
 * 现在改成"Spring 管的 bean" —— 值由 @Value 从 config.properties 注入。
 */
@Component                                        // ← 归容器管
@PropertySource("classpath:config.properties")    // ← 读 classpath 根下这个文件
public class AppConfig {

    @Value("${deepseek.api.url}")
    private String apiUrl;                        // 没有默认值 → 缺了就启动报错(fail-fast)

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.maxTokens:200}")           // ${key:默认值} 写法
    private int maxTokens;

    @Value("${deepseek.timeoutSeconds:30}")
    private int timeoutSeconds;

    @Value("${app.debug:false}")
    private boolean debug;

    @Value("${llm.requestTimeoutSeconds:60}")
    private int requestTimeoutSeconds;

    @Value("${llm.maxAttempts:3}")
    private int maxAttempts;

    @Value("${llm.retryBaseMs:500}")
    private long retryBaseMs;

    @Value("${llm.retryMaxMs:8000}")
    private long retryMaxMs;

    @Value("${vision.api.url}")
    private String visionApiUrl;

    @Value("${vision.model}")
    private String visionModel;

    @Value("${vision.maxTokens:4096}")
    private int visionMaxTokens;

    @Value("${embedding.url:http://127.0.0.1:8090}")
    private String embeddingUrl;

    @Value("${embedding.model:bge-small-zh-v1.5}")
    private String embeddingModel;

    // 方法名保持不变 → 调用点改动最小
    public String apiUrl() { return apiUrl; }
    public String model() { return model; }
    public int maxTokens() { return maxTokens; }
    public int timeoutSeconds() { return timeoutSeconds; }
    public boolean debug() { return debug; }
    public int requestTimeoutSeconds() { return requestTimeoutSeconds; }
    public int maxAttempts()           { return maxAttempts; }
    public long retryBaseMs()          { return retryBaseMs; }
    public long retryMaxMs()           { return retryMaxMs; }
    public String visionApiUrl()  { return visionApiUrl; }
    public String visionModel()   { return visionModel; }
    public int visionMaxTokens()  { return visionMaxTokens; }
    public String embeddingUrl()   { return embeddingUrl; }
    public String embeddingModel() { return embeddingModel; }
}