package io.github.likeelysia.formalagent.doc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** 文档管道:按扩展名选读取器 → 读成"带出处的文本段" → 切成"带出处的块"。 */
@Component
public class DocPipeline {

    private final ReaderRegistry registry;
    private final TextSplitter splitter;

    public DocPipeline(ReaderRegistry registry, TextSplitter splitter) {
        this.registry = registry;
        this.splitter = splitter;
    }

    /** 读文件 → 切块 → 返回带出处的块列表。 */
    public List<TextChunk> load(Path file) {
        List<TextChunk> chunks = new ArrayList<>();
        for (TextSegment segment : registry.forFile(file).read(file)) {
            for (String piece : splitter.split(segment.text())) {
                chunks.add(new TextChunk(piece, segment.location()));
            }
        }
        return chunks;
    }
}