package io.github.likeelysia.formalagent.doc;

import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.VisionClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** 图片读取器:调视觉模型把图里的文字提取出来,作为一个文本段。 */
@Component
public class ImageReader implements DocumentReader {

    /** OCR 指令:只要文字,不要解释 */
    private static final String OCR_PROMPT =
            "请提取这张图片中的所有文字,原样输出;不要翻译、不要解释、不要添加任何说明。"
                    + "如果图中没有文字,只输出一个空字符串。";

    private final VisionClient visionClient;

    public ImageReader(VisionClient visionClient) {
        this.visionClient = visionClient;
    }

    @Override
    public List<TextSegment> read(Path file) {
        if (file == null) throw new AgentException("文件路径不能为空");
        if (!Files.isRegularFile(file)) throw new AgentException("这不是一个文件:" + file);

        String text = visionClient.ask(file, OCR_PROMPT);
        if (text == null || text.isBlank()) {
            throw new AgentException("这张图里没有识别到文字:" + file);
        }
        return List.of(new TextSegment(text, null));
    }
}