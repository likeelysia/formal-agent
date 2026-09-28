package io.github.likeelysia.formalagent.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 阶段 8 · 模块 1「API 调用基础」第一步 —— v1:让程序第一次"跟外面说上话"。
 *
 * <p>这一版故意调 httpbin.org(一面"镜子"),而不是 DeepSeek,原因有三个:
 * <ol>
 *   <li>不需要 API Key(也就不会泄露你的钥匙)</li>
 *   <li>不花一分钱(不消耗你的 API 余额)</li>
 *   <li>但走的是和调 DeepSeek 完全一样的 HTTP 流程</li>
 * </ol>
 *
 * <p>等这版跑通,下一版(v2)只需要:把 URL 换成 DeepSeek 的地址 + 加一行 Authorization 请求头。
 *
 * <p>运行方式(二选一):
 * <pre>
 *   ① IDEA 里点 main 方法左边那个绿色三角直接跑(推荐)
 *   ② 命令行:mvn -B compile
 *            java -cp target/classes io.github.likeelysia.formalagent.demo.HttpBasicDemo
 * </pre>
 */
public class HttpBasicDemo {

    /**
     * HttpClient = "打电话的人"。整个程序只造一个就够(它底层会帮你复用连接)。
     * connectTimeout:连不上最多等 10 秒,免得程序无限干等。
     */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        callMirrorSite();    // (1) 发一个成功的 GET
        checkStatusCode();   // (2) 亲眼看状态码是怎么变的
        askDeepSeek();

        System.out.println();
        System.out.println("恭喜 —— 这就是你的程序第一次通过网络和别人说话。");
    }

    /**
     * (1) 调 httpbin:这个站点会把"它收到的请求"原样发回来,
     * 所以正适合照镜子 —— 你能亲眼验证自己发出去的东西对方收到了。
     */
    private static void callMirrorSite() throws Exception {

        // ---- 第 1 步:造请求(相当于填一张点菜单) ----
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/get?name=peng&course=java"))
                .header("User-Agent", "formal-agent-learning/1.0")
                .header("X-Note", "hello-from-formal-agent")
                .build();
        // 注意:GET 请求没有请求体(没有"盒子里的货"),因为它的目的只是"取"。

        // ---- 第 2 步:发出去,并声明"回信我要字符串形式" ----
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // ---- 第 3 步:看回执 ----
        System.out.println("========== (1) 一次成功的 GET ==========");
        System.out.println("状态码 = " + response.statusCode());
        System.out.println("---- 服务器回给我的内容(响应体)开始 ----");
        System.out.println(response.body());
        System.out.println("---- 响应体结束 ----");
        System.out.println("请你在上面的响应体里找两样东西:");
        System.out.println("  1. args 里是不是有 name=peng、course=java ?  -> 说明 URL 上的参数对方收到了");
        System.out.println("  2. headers 里是不是有 X-Note ?              -> 说明你自己贴的请求头对方也收到了");
        System.out.println();
    }

    /**
     * (2) 状态码演示:代码几乎一模一样,只是把 URL 换成一个"故意要 404"的地址。
     * 体会一下:页面/响应体是空的,但状态码不是 200。
     */
    private static void checkStatusCode() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/status/404"))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("========== (2) 一个 404 ==========");
        System.out.println("状态码     = " + response.statusCode() + "   <- 响应体是空的,但回执上写着 404");
        System.out.println("响应体长度 = " + response.body().length() + " 个字符(确实是空的)");
        System.out.println("结论:状态码和响应体是两回事 —— 状态码在'回执小票'上,响应体是'盒子里的货'。");
    }


    /**
     * (3) v2:真正调一次 DeepSeek。
     */
    private static void askDeepSeek() throws Exception {

        // 1) 从环境变量掏钥匙。名字必须和 setx 写的一模一样。
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        // == null 问"变量根本不存在吗";isBlank() 问"是空的或全是空格吗"(比 isEmpty() 更严)
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("没读到 DEEPSEEK_API_KEY —— 先去配环境变量 + 重启 IDEA");
            return;
        }

        // 2) 请求体:要送过去的"货"
        String json = """
                {
                  "model": "deepseek-chat",
                  "messages": [
                    {"role": "system", "content": "你是一个猫咪饲养员,回答不超过两句话。"},
                    {"role": "user",   "content": "喵。"}
                  ],
                  "max_tokens": 100
                }
                """;

        // 3) 造请求:POST + 两个头 + 把货装进去
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // 4) 寄出去
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 5) 看回执  ← 你那三行 println 的正确位置就是这里
        System.out.println("========== (3) 调 DeepSeek ==========");
        System.out.println("状态码 = " + response.statusCode());
        System.out.println("响应体 = " + response.body());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        String answer = root.path("choices").path(0).path("message").path("content").asText();
        System.out.println("AI 的回答 = " + answer);
    }
}
