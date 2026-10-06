package io.github.likeelysia.formalagent.cli;

import io.github.likeelysia.formalagent.config.SpringConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.extract.IngestService;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import java.nio.file.Path;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** 命令行入口:把一份教材导入知识库(当前支持 txt/md;PDF 见 1a-2)。 */
public class IngestMain {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法:IngestMain <教材文件>");
            return;
        }
        Path file = Path.of(args[0]);

        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(SpringConfig.class)) {

            IngestService ingest = ctx.getBean(IngestService.class);
            KnowledgeStore store = ctx.getBean(KnowledgeStore.class);

            List<KnowledgeItem> added = ingest.ingest(file);
            System.out.println("已导入 " + added.size() + " 个知识点(本次)→ 知识库现有 "
                    + store.all().size() + " 条");
        } catch (AgentException e) {
            System.out.println("[出错] " + e.getMessage());
        }
    }
}