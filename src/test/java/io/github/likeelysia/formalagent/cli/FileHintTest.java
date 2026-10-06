package io.github.likeelysia.formalagent.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FileHint 的单元测试:纯字符串/路径判断,零 IO(除了写临时文件)。 */
class FileHintTest {

    @TempDir Path tmp;

    private Path write(String name) throws Exception {
        Path p = tmp.resolve(name);
        Files.writeString(p, "hi", StandardCharsets.UTF_8);
        return p;
    }

    @Test
    @DisplayName("认识 .txt / .md(大小写都行)")
    void picksRealTextFiles() throws Exception {
        Path md = write("lesson.md");
        Path txt = write("a.txt");
        Path upper = write("LESSON.MD");

        assertEquals(md, FileHint.asReadableFile(md.toString()));
        assertEquals(txt, FileHint.asReadableFile(txt.toString()));
        assertEquals(upper, FileHint.asReadableFile(upper.toString()));
    }

    @Test
    @DisplayName("认识 .pdf / .png / .jpg(大小写都行)")
    void picksPdfAndImages() throws Exception {
        Path pdf = write("book.pdf");
        Path png = write("a.png");
        Path jpg = write("a.JPG");

        assertEquals(pdf, FileHint.asReadableFile(pdf.toString()));
        assertEquals(png, FileHint.asReadableFile(png.toString()));
        assertEquals(jpg, FileHint.asReadableFile(jpg.toString()));
    }

    @Test
    @DisplayName("Windows 右键“复制文件地址”带的引号 / 前后空格,都能剥掉")
    void unwrapsQuotesAndSpaces() throws Exception {
        Path md = write("lesson.md");

        assertEquals(md, FileHint.asReadableFile("\"" + md + "\""));
        assertEquals(md, FileHint.asReadableFile("  \n" + md + " \n"));
    }

    @Test
    @DisplayName("还没支持的后缀(.docx / 无后缀)→ 不当文件")
    void ignoresUnsupportedSuffix() throws Exception {
        assertNull(FileHint.asReadableFile(write("a.docx").toString()));
        assertNull(FileHint.asReadableFile(write("readme").toString()));
    }

    @Test
    @DisplayName("不存在的路径 / 目录 → 不当文件")
    void ignoresMissingOrDirectory() throws Exception {
        assertNull(FileHint.asReadableFile("F:\\no\\such\\file.md"));
        assertNull(FileHint.asReadableFile(tmp.toString()));
    }

    @Test
    @DisplayName("普通聊天(问句 / 路径加问话 / null)→ 都不当文件")
    void treatsPlainChatAsChat() throws Exception {
        Path md = write("lesson.md");

        assertNull(FileHint.asReadableFile("什么是多态?"));
        assertNull(FileHint.asReadableFile(md + " 讲了什么"));
        assertNull(FileHint.asReadableFile("\"多态\"是什么"));
        assertNull(FileHint.asReadableFile(null));
    }
    @Test
    @DisplayName("isImage:认 .png/.jpg/.jpeg,不认 .pdf/.txt")
    void recognizesImages() {
        assertTrue(FileHint.isImage(Path.of("a.png")));
        assertTrue(FileHint.isImage(Path.of("a.JPG")));
        assertTrue(FileHint.isImage(Path.of("a.jpeg")));
        assertFalse(FileHint.isImage(Path.of("a.pdf")));
        assertFalse(FileHint.isImage(Path.of("a.txt")));
    }
}