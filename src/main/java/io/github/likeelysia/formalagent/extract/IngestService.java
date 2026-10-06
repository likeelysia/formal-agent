package io.github.likeelysia.formalagent.extract;

import io.github.likeelysia.formalagent.doc.DocPipeline;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import io.github.likeelysia.formalagent.doc.TextChunk;

/** 教材摄取:一个文件 → 知识点(带出处)→ 存进知识库。 */
@Component
public class IngestService {

    private final DocPipeline pipeline;
    private final KnowledgeExtractor extractor;
    private final KnowledgeStore store;

    public IngestService(DocPipeline pipeline, KnowledgeExtractor extractor, KnowledgeStore store) {
        this.pipeline = pipeline;
        this.extractor = extractor;
        this.store = store;
    }

    /** 提取并入库;返回本次导入的知识点(同名只留第一次)。 */
    public List<KnowledgeItem> ingest(Path file) {
        String source = file.getFileName().toString();
        String type = typeOf(source);

        Map<String, KnowledgeItem> items = new LinkedHashMap<>();    // name → item(同名去重)
        List<String> failedChunks = new ArrayList<>();
        for (TextChunk chunk : pipeline.load(file)) {
            List<KnowledgePoint> points;
            try {
                points = extractor.extract(chunk.text());
            } catch (AgentException e) {                            // ← 单块提炼失败不拖垮整本
                failedChunks.add(chunk.location());
                System.out.println("[提炼] " + chunk.location() + " 失败,已跳过:" + e.getMessage());
                continue;
            }
            for (KnowledgePoint p : points) {
                String id = source + "|" + p.name();
                items.putIfAbsent(p.name(),
                        new KnowledgeItem(id, p.name(), p.detail(), source, chunk.location(), type));
            }
        }
        if (!failedChunks.isEmpty()) {
            System.out.println("[提炼] ⚠ 有 " + failedChunks.size() + " 块没提炼成功:" + failedChunks);
        }

        List<KnowledgeItem> result = List.copyOf(items.values());
        store.addAll(result);
        return result;
    }

    private static String typeOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "pdf";
        return "text";
    }
}