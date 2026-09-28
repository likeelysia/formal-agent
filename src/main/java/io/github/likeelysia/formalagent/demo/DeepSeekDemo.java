package io.github.likeelysia.formalagent.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import io.github.likeelysia.formalagent.log.ApiLogger;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.likeelysia.formalagent.config.AppConfig;

/**
 * 阶段 8 · 模块 1:v4 —— 只保留「和 DeepSeek 对话」这一条线。
 *
 * <p>相比 v1~v3:删掉 httpbin 镜子站与 404 演示;URL / 模型 / max_tokens / 超时 全部来自 config.properties;
 * 输出改为简洁框;原始 JSON 仅在 app.debug=true 时打印。
 */
public class DeepSeekDemo {

    /** 系统提示词:定义 AI 的角色(以后想改风格,改这里或挪进配置) */
    private static final String SYSTEM_PROMPT = "你是一个猫咪饲养员,回答不超过两句话。";

    /** 复用的 HTTP 客户端(重对象,全类共用一份);连接超时从配置读 */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(AppConfig.timeoutSeconds()))
            .build();

    public static void main(String[] args)  {
        Scanner scanner = new Scanner(System.in);          // Scanner 你在基础篇学过 ✔
        System.out.println("=== formal-agent 对话(输入 exit 退出)===");

        while (true) {
            System.out.print("你 > ");                     // print(不换行),光标停在后面
            if (!scanner.hasNextLine()) {                  // 输入流结束(如 Ctrl+Z)→ 退出
                break;
            }
            String line = scanner.nextLine().strip();      // strip():去首尾空白

            if (line.isEmpty()) {                          // 空行:跳过,别浪费一次 API 调用
                continue;
            }
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;                                     // 退出词(大小写不敏感)
            }

            try {
                askDeepSeek(line);                          // ← 用户输入直接喂进去
            } catch (Exception e) {
                // ⭐ 关键设计:一次失败只提示,不退出循环 —— 程序能继续用
                System.out.println("[出错] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        System.out.println("再见 👋");
        scanner.close();          // 现在整个 main 就这一句
    }

    /**
     * 问一句、拿回回答,并按"简洁框"打印。
     *
     * @param userMessage 用户说的话(以后做多轮会话时,这里传用户输入)
     * @return AI 的回答原文
     */
    private static String askDeepSeek(String userMessage) throws Exception {

        // 1) 钥匙只从环境变量读(绝不硬编码、绝不进配置文件)
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("没读到 DEEPSEEK_API_KEY —— 先配置环境变量再重启 IDEA");
        }

        // 2) 请求体:值全部来自配置/常量/参数(不再是写死的字符串)
        String json = """
                  {
                    "model": "%s",
                    "messages": [
                      {"role": "system", "content": "%s"},
                      {"role": "user",   "content": "%s"}
                    ],
                    "max_tokens": %d
                  }
                  """.formatted(AppConfig.model(), SYSTEM_PROMPT, userMessage, AppConfig.maxTokens());

        // 3) 造请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.apiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // 4) 发出去,并计时
        long start = System.currentTimeMillis();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        long cost = System.currentTimeMillis() - start;

        // 5) 解析出回答
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        String answer = root.path("choices").path(0).path("message").path("content").asText().strip();

        // 6) 简洁输出
        String line = "─".repeat(48);
        System.out.println(line);
        System.out.println(" DeepSeek · " + AppConfig.model());
        System.out.println(" 状态 " + response.statusCode() + " | 耗时 " + cost + "ms | 回复 " + answer.length() + " 字");
        System.out.println(line);
        System.out.println(" " + answer);
        System.out.println(line);

        // 7) 调试信息只在开关打开时打印
        if (AppConfig.debug()) {
            System.out.println("[debug] 原始响应:" + response.body());
        }
        // 8) 记一条调用日志
        ApiLogger.record(AppConfig.model(), response.statusCode(), cost, json.length(), answer.length());

        return answer;
    }
}