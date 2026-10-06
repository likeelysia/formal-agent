package io.github.likeelysia.formalagent.doc;

import io.github.likeelysia.formalagent.exception.AgentException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 读取器注册表:按文件扩展名分派到对应的 DocumentReader。
 * 以后加格式 = 加一个 DocumentReader 实现 + 在这里登记一行,调用方不用改。
 */
@Component
public class ReaderRegistry {

    private final Map<String, DocumentReader> byExtension;

    public ReaderRegistry(PlainTextReader plainTextReader, PdfReader pdfReader, ImageReader imageReader) {
        this.byExtension = Map.of(
                ".txt", plainTextReader,
                ".md", plainTextReader,
                ".pdf", pdfReader,
                ".png", imageReader,
                ".jpg", imageReader,
                ".jpeg", imageReader);
    }

    public DocumentReader forFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, DocumentReader> e : byExtension.entrySet()) {
            if (name.endsWith(e.getKey())) return e.getValue();
        }
        throw new AgentException("暂不支持的文件格式:" + name);
    }
}