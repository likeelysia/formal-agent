package io.github.likeelysia.formalagent.doc;

import java.nio.file.Path;
import java.util.List;

/** 文档读取器:给我一个文件路径,还我一串"带出处的文本段"。 */
public interface DocumentReader {
    List<TextSegment> read(Path file);
}