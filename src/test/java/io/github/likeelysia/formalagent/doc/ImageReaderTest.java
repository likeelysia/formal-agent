package io.github.likeelysia.formalagent.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.VisionClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageReaderTest {

    @TempDir Path tmp;

    @Test
    @DisplayName("图片 → 视觉客户端拿文字,包成一个文本段")
    void extractsText() throws IOException {
        Path img = tmp.resolve("shot.png");
        Files.write(img, new byte[]{1, 2, 3});       // 内容无所谓,假客户端不真读

        VisionClient fake = (image, prompt) -> "Hello OCR";
        List<TextSegment> segments = new ImageReader(fake).read(img);

        assertEquals(1, segments.size());
        assertEquals("Hello OCR", segments.get(0).text());
    }

    @Test
    @DisplayName("没识别到文字 → 抛 AgentException")
    void blankOcrThrows() throws IOException {
        Path img = tmp.resolve("blank.jpg");
        Files.write(img, new byte[]{9});

        VisionClient fake = (image, prompt) -> "   ";
        assertThrows(AgentException.class, () -> new ImageReader(fake).read(img));
    }
}