package io.github.likeelysia.formalagent.doc;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * 文档管道:把"读文档"和"切块"串成一条流水线。
 * 调用方只需说一句:给我一个文件路径,还我一堆块。
 */
@Component
public class DocPipeline {

    private final DocumentReader reader;
    private final TextSplitter splitter;

    public DocPipeline(DocumentReader reader, TextSplitter splitter) {
        this.reader = reader;
        this.splitter = splitter;
    }

    /** 读文件 → 切块 → 返回块列表。 */
    public List<String> load(Path file) {
        String text = reader.read(file);
        return splitter.split(text);
    }
}