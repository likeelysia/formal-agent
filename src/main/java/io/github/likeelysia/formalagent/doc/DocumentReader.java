package io.github.likeelysia.formalagent.doc;

import java.nio.file.Path;

/** 文档读取器:只回答一件事 —— "给我一个文件路径,还我一整段纯文本"。 */
public interface DocumentReader {
    String read(Path file);
}