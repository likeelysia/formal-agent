package io.github.likeelysia.formalagent.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型调用审计日志:每次调用模型后,记一行到 {@code logs/api-YYYY-MM-DD.log}。
 *
 * <p><b>2026-10-06:从"手写文件追加"改为走 SLF4J/Logback。</b>
 * 现在它只是发一条 logger 名为 {@code "api"} 的日志 —— 落哪个文件、怎么按天滚动、
 * 保留多久,全部由 {@code logback-spring.xml} 决定(见那里的 {@code <logger name="api">})。
 * 这就是"日志框架"替我们干掉的活:以前那 40 行手写代码,现在只剩一行 {@code API.info(...)}。
 */
public final class ApiLogger {

    /** 专用 logger 名:"api" —— logback 配置里给它单独一个文件 appender */
    private static final Logger API = LoggerFactory.getLogger("api");

    /** 工具类:不允许 new */
    private ApiLogger() {
    }

    /**
     * 记录一次调用。
     *
     * @param model        模型名
     * @param statusCode   HTTP 状态码
     * @param costMs       耗时(毫秒)
     * @param requestChars 请求内容字符数
     * @param answerChars  回答字符数
     */
    public static void record(String model, int statusCode, long costMs, int requestChars, int answerChars) {
        String line = String.join(" | ",
                String.valueOf(statusCode),
                costMs + "ms",
                model,
                "请求 " + requestChars + " 字",
                "回复 " + answerChars + " 字");
        API.info(line);
    }
}
