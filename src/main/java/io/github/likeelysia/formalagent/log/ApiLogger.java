package io.github.likeelysia.formalagent.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 极简「调用日志」:每次调用模型后,追加一行记录到 logs/api-YYYY-MM-DD.log。
 *
 * <p>为什么自己写一遍?—— 为了看清"日志文件到底是怎么产生的"。
 * 等到阶段⑥ 用 Spring Boot 时,这些会被 Logback 这类日志框架自动完成。
 */
public final class ApiLogger {

    /** 日志目录(以后可以挪进配置文件) */
    private static final Path LOG_DIR = Paths.get("logs");

    /** 时间格式:2026-09-28 14:35:12 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        // 1) 文件名按天分:api-2026-09-28.log(LocalDate.toString() 就是 yyyy-MM-dd)
        String fileName = "api-" + LocalDateTime.now().toLocalDate() + ".log";

        // 2) 拼成一行,用 | 分隔(以后方便用工具/脚本切分统计)
        String line = String.join(" | ",
                LocalDateTime.now().format(TIME_FMT),
                String.valueOf(statusCode),
                costMs + "ms",
                model,
                "请求 " + requestChars + " 字",
                "回复 " + answerChars + " 字");

        // 3) 写文件:UTF-8 + 追加(不覆盖以前的记录)
        try {
            Files.createDirectories(LOG_DIR);              // 目录不存在就创建(相当于 mkdir -p)
            Path file = LOG_DIR.resolve(fileName);         // 拼出 logs/api-xxx.log
            try (BufferedWriter writer = Files.newBufferedWriter(file,
                    StandardCharsets.UTF_8,                // 指定编码,避免中文乱码
                    StandardOpenOption.CREATE,             // 文件不存在就创建
                    StandardOpenOption.APPEND)) {          // 已存在就【追加】← 关键
                writer.write(line);
                writer.newLine();                          // 写一个换行
            }
        } catch (IOException e) {
            // 日志是"辅助功能":写失败也不能让主流程崩 → 只提示,不往外抛
            System.err.println("[ApiLogger] 写日志失败:" + e.getMessage());
        }
    }
}