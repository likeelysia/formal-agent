package io.github.likeelysia.formalagent.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.likeelysia.formalagent.exception.AgentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlainTextReaderTest {

    @TempDir
    Path tmp;                                   // JUnit 每次测试送一个临时文件夹,测完自动清掉

    private final PlainTextReader reader = new PlainTextReader();

    @Test
    @DisplayName("能原样读出 UTF-8 文本(含中文和 emoji)")
    void readsUtf8Text() throws IOException {
        Path f = tmp.resolve("a.txt");
        Files.writeString(f, "第一行\n第二行:带中文和 emoji 🎯", StandardCharsets.UTF_8);

        assertEquals("第一行\n第二行:带中文和 emoji 🎯", reader.read(f));
    }

    @Test
    @DisplayName("文件不存在 → 抛 AgentException,提示里带“不存在”")
    void missingFile() {
        Path f = tmp.resolve("never-created.txt");

        AgentException e = assertThrows(AgentException.class, () -> reader.read(f));
        assertTrue(e.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("空白文件 → 抛 AgentException")
    void blankFile() throws IOException {
        Path f = tmp.resolve("blank.txt");
        Files.writeString(f, "   \n\n  ", StandardCharsets.UTF_8);

        assertThrows(AgentException.class, () -> reader.read(f));
    }
}