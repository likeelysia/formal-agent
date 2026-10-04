package io.github.likeelysia.formalagent.extract;

import io.github.likeelysia.formalagent.doc.DocPipeline;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 提取服务:一个文件 → 一份(去重后的)知识点清单。 */
@Component
public class ExtractService {

    private final DocPipeline pipeline;
    private final KnowledgeExtractor extractor;

    public ExtractService(DocPipeline pipeline, KnowledgeExtractor extractor) {
        this.pipeline = pipeline;
        this.extractor = extractor;
    }

    /**
     * 一个文件 → 整份知识点清单。
     * 按"出现顺序"排;同名知识点只保留第一次出现的那条(说明也用它)。
     */
    public List<KnowledgePoint> extractFile(Path file) {
        List<String> chunks = pipeline.load(file);

        Map<String, String> merged = new LinkedHashMap<>();      // key=知识点名 value=说明
        for (String chunk : chunks) {
            for (KnowledgePoint p : extractor.extract(chunk)) {
                merged.putIfAbsent(p.name(), p.detail());        // 已存在就不覆盖 → 保留第一次
            }
        }

        return merged.entrySet().stream()
                .map(entry -> new KnowledgePoint(entry.getKey(), entry.getValue()))
                .toList();
    }
}