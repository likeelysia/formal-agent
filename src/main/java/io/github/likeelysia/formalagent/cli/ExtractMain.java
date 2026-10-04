package io.github.likeelysia.formalagent.cli;

import io.github.likeelysia.formalagent.config.SpringConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.extract.ExtractService;
import io.github.likeelysia.formalagent.extract.KnowledgePoint;
import io.github.likeelysia.formalagent.extract.KnowledgeReport;
import java.nio.file.Path;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 命令行入口:给一个文件,吐出一份知识点清单的 Markdown。
 *
 * <p>用法(项目根目录):
 * java -cp "target\classes;target\dependency\*" io.github.likeelysia.formalagent.cli.ExtractMain &lt;输入文件&gt; [输出文件.md]
 */
public class ExtractMain {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法:ExtractMain <输入文件> [输出文件.md]");
            return;
        }

        Path input = Path.of(args[0]);
        Path output = args.length >= 2 ? Path.of(args[1]) : defaultOutput(input);

        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(SpringConfig.class)) {

            ExtractService service = ctx.getBean(ExtractService.class);
            KnowledgeReport report = ctx.getBean(KnowledgeReport.class);

            List<KnowledgePoint> points = service.extractFile(input);
            Path saved = report.save(output, input.getFileName().toString(), points);

            System.out.println("提取完成:共 " + points.size() + " 个知识点 → " + saved.toAbsolutePath());
        } catch (AgentException e) {
            System.out.println("[出错] " + e.getMessage());
        }
    }

    /** 没给输出文件时:和输入文件同目录,文件名后加 .knowledge.md。 */
    private static Path defaultOutput(Path input) {
        return input.resolveSibling(input.getFileName().toString() + ".knowledge.md");
    }
}