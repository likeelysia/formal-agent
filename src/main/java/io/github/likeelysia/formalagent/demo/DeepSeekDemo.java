package io.github.likeelysia.formalagent.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.config.AppConfig;
import io.github.likeelysia.formalagent.log.ApiLogger;
import io.github.likeelysia.formalagent.store.SessionStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

/**
 * 阶段 8 · 模块 1:v4 —— 只保留「和 DeepSeek 对话」这一条线。
 *
 * <p>相比 v1~v3:删掉 httpbin 镜子站与 404 演示;URL / 模型 / max_tokens / 超时 全部来自 config.properties;
 * 输出改为简洁框;原始 JSON 仅在 app.debug=true 时打印。
 */
public class DeepSeekDemo {

    /** 本次会话(启动时尝试从文件恢复,没有就新建) */
    private static ChatSession SESSION = new ChatSession("default");

    /** 系统提示词:定义 AI 的角色(以后想改风格,改这里或挪进配置) */
    private static final String SYSTEM_PROMPT = "你是一个猫咪饲养员,回答不超过两句话。";

    /** 复用的 HTTP 客户端(重对象,全类共用一份);连接超时从配置读 */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(AppConfig.timeoutSeconds()))
            .build();

    public static void main(String[] args)  {
        ChatSession restored = SessionStore.load(SESSION.getName());
        if (restored != null) {
            SESSION = restored;                       // ★ 恢复上次会话(含全部历史)
            System.out.println("已恢复历史,共 " + SESSION.getMessages().size() + " 条消息");
        } else {
            SESSION.add(new Message("system", SYSTEM_PROMPT));   // 第一次运行:定人设
        }  // 开局先定人设
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

        // 退出前存档(二进制)+ 导出可读版
        SessionStore.save(SESSION);
        SessionStore.exportJson(SESSION);
        System.out.println("再见 👋");
        scanner.close();
    }

    /**
     * 问一句、拿回回答,并按"简洁框"打印。
     *
     * @param userMessage 用户说的话(以后做多轮会话时,这里传用户输入)
     * @return AI 的回答原文
     */
    private static String askDeepSeek(String userMessage) throws Exception {

        // 1) 钥匙
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("没读到 DEEPSEEK_API_KEY —— 先配置环境变量再重启 IDEA");
        }

        // 2) 把"用户这句"先记进历史
        SESSION.add(new Message("user", userMessage));

        // 3) ⭐ 用 Jackson 把"整段历史"生成 JSON 数组
        //    为什么不手拼字符串?—— 用户输入里万一带 " 或换行,手拼会直接破坏 JSON;
        //    Jackson 会自动转义,这才是正确做法。
        List<Map<String, String>> history = new ArrayList<>();
        for (Message m : SESSION.getMessages()) {
            history.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        ObjectMapper mapper = new ObjectMapper();
        String messagesJson = mapper.writeValueAsString(history);   // 变成 [{"role":..,"content":..}, ...]

        // 4) 请求体:模型 / 历史 / max_tokens
        String json = """
                  {
                    "model": "%s",
                    "messages": %s,
                    "max_tokens": %d
                  }
                  """.formatted(AppConfig.model(), messagesJson, AppConfig.maxTokens());

        // 5) 造请求(和之前一样)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.apiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // 6) 发出去 + 计时
        long start = System.currentTimeMillis();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        long cost = System.currentTimeMillis() - start;

        // 7) 解析回答
        JsonNode root = mapper.readTree(response.body());
        String answer = root.path("choices").path(0).path("message").path("content").asText().strip();

        // 8) ⭐ 把"AI 的回答"也记进历史(这样下一轮它才记得自己说过什么)
        SESSION.add(new Message("assistant", answer));

        // 9) 简洁输出(同之前)
        String line = "─".repeat(48);
        System.out.println(line);
        System.out.println(" DeepSeek · " + AppConfig.model());
        System.out.println(" 状态 " + response.statusCode() + " | 耗时 " + cost + "ms | 回复 " + answer.length() + " 字"
                + " | 历史 " + SESSION.getMessages().size() + " 条");
        System.out.println(line);
        System.out.println(" " + answer);
        System.out.println(line);

        // 10) 日志 + 调试
        ApiLogger.record(AppConfig.model(), response.statusCode(), cost, json.length(), answer.length());
        if (AppConfig.debug()) {
            System.out.println("[debug] 请求体:" + json);
        }

        return answer;
    }
}