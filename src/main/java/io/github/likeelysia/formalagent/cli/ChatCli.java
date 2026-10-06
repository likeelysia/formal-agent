package io.github.likeelysia.formalagent.cli;

import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.extract.ExtractService;
import io.github.likeelysia.formalagent.extract.KnowledgePoint;
import io.github.likeelysia.formalagent.extract.KnowledgeReport;
import io.github.likeelysia.formalagent.service.ChatService;
import io.github.likeelysia.formalagent.service.QaService;
import io.github.likeelysia.formalagent.store.SessionStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 交互式命令行:应用启动后由 Boot 调起(等价于原来的 {@code Main})。
 *
 * <p>它自己不再创建容器 —— 依赖全部由 Spring 注入,这就是"从手动 new 到 Boot"的区别。
 */
@Component
@ConditionalOnProperty(name = "fa.cli.enabled", havingValue = "true")   // 默认关;Web 模式下不抢 stdin
public class ChatCli implements CommandLineRunner {

    /** 终端里最多显示多少条知识点(剩下的只写进文件,免得刷屏)。 */
    private static final int MAX_SHOW = 15;

    private final ChatService service;
    private final ExtractService extractService;
    private final KnowledgeReport report;
    private final QaService qaService;
    private final SessionStore sessionStore;

    public ChatCli(ChatService service, ExtractService extractService, KnowledgeReport report,
                   QaService qaService, SessionStore sessionStore) {
        this.service = service;
        this.extractService = extractService;
        this.report = report;
        this.qaService = qaService;
        this.sessionStore = sessionStore;
    }

    @Override
    public void run(String... args) {
        System.out.println("提示:直接说话=聊天;粘 .md/.txt/.pdf=提取知识点;粘图片(.png/.jpg)=对照知识库回答。输入 exit 退出。");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("你 > ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().strip();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;

            Path file = FileHint.asReadableFile(line);     // ④ 先问一句:这是文件吗?
            if (file != null) {
                if (FileHint.isImage(file)) {
                    askAboutImage(file);                   // 图片 → 对照知识库回答
                } else {
                    extractFile(file);                     // 文档 → 提取知识点
                }
                continue;
            }

            try {
                System.out.println("AI > " + service.send(line));
            } catch (AgentException e) {
                System.out.println("[出错] " + e.getMessage());
            }
        }

        sessionStore.save(service.getSession());
        sessionStore.exportJson(service.getSession());
        scanner.close();
    }

    /** 文件 → 块 → 逐块提取 → 合并去重 → 写文件 + 终端显示前几条。 */
    private void extractFile(Path file) {
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

    /** 图片 → 对照知识库回答。 */
    private void askAboutImage(Path image) {
        System.out.println("(识别到图片,正在对照知识库回答,请稍等…)");
        try {
            System.out.println("AI > " + qaService.ask(image));
        } catch (AgentException e) {
            System.out.println("[出错] " + e.getMessage());
        }
    }
}
