package io.github.likeelysia.formalagent.cli;

import io.github.likeelysia.formalagent.config.SpringConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.extract.ExtractService;
import io.github.likeelysia.formalagent.extract.KnowledgePoint;
import io.github.likeelysia.formalagent.extract.KnowledgeReport;
import io.github.likeelysia.formalagent.service.ChatService;
import io.github.likeelysia.formalagent.store.SessionStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    /** 终端里最多显示多少条知识点(剩下的只写进文件,免得刷屏)。 */
    private static final int MAX_SHOW = 15;

    public static void main(String[] args) {
        // ① 启动 Spring 容器
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(SpringConfig.class);

        // ② 一次拿齐三个"服务":聊天 + 提取 + 渲染
        ChatService service = ctx.getBean(ChatService.class);
        ExtractService extractService = ctx.getBean(ExtractService.class);
        KnowledgeReport report = ctx.getBean(KnowledgeReport.class);

        System.out.println("提示:直接说话=聊天;把 .md / .txt 文件路径粘进来=提取知识点。输入 exit 退出。");

        // ③ 循环
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("你 > ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().strip();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;

            Path file = FileHint.asReadableFile(line);     // ④ 先问一句:这是文件吗?
            if (file != null) {
                extractFile(file, extractService, report); // 是文件 → 走提取,不进聊天记录
                continue;
            }

            try {
                System.out.println("AI > " + service.send(line));
            } catch (AgentException e) {
                System.out.println("[出错] " + e.getMessage());
            }
        }

        // ⑤ 存档 + 关容器(SessionStore 现在是 bean,从容器取)
        SessionStore store = ctx.getBean(SessionStore.class);
        store.save(service.getSession());
        store.exportJson(service.getSession());
        scanner.close();
        ctx.close();
    }

    /** 文件 → 块 → 逐块提取 → 合并去重 → 写文件 + 终端显示前几条。 */
    private static void extractFile(Path file, ExtractService extractService, KnowledgeReport report) {
        System.out.println("(识别到文件,正在提取,请稍等…)");
        try {
            List<KnowledgePoint> points = extractService.extractFile(file);
            Path saved = report.save(
                    file.resolveSibling(file.getFileName().toString() + ".knowledge.md"),
                    file.getFileName().toString(),
                    points);

            System.out.println("AI > 提取完成:共 " + points.size() + " 个知识点 → " + saved.toAbsolutePath());

            int show = Math.min(points.size(), MAX_SHOW);
            for (int i = 0; i < show; i++) {
                System.out.println("     " + (i + 1) + ". " + points.get(i).name()
                        + " —— " + points.get(i).detail());
            }
            if (points.size() > show) {
                System.out.println("     …还有 " + (points.size() - show) + " 条,完整清单见上面的文件。");
            }
        } catch (AgentException e) {
            System.out.println("[出错] " + e.getMessage());
        }
    }
}