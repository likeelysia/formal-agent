package io.github.likeelysia.formalagent.doc;

import io.github.likeelysia.formalagent.exception.AgentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** 纯文本读取器:读 .txt / .md(UTF-8),整篇作为一个文本段。 */
@Component
public class PlainTextReader implements DocumentReader {

    @Override
    public List<TextSegment> read(Path file) {
        if (file == null) throw new AgentException("文件路径不能为空");
        if (!Files.exists(file)) throw new AgentException("文件不存在:" + file);
        if (!Files.isRegularFile(file)) throw new AgentException("这不是一个文件(可能是目录):" + file);

        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AgentException("读文件失败:" + file, e);
        }
        if (text.isBlank()) throw new AgentException("文件里没有内容(空白):" + file);

        return List.of(new TextSegment(text, null));   // 纯文本没有页概念
    }
}