package io.github.likeelysia.formalagent.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextSplitterTest {

    private final TextSplitter splitter = new TextSplitter(20, 5);   // 测试里用小块,好造数据

    @Test
    @DisplayName("空白文本 → 返回空列表")
    void blankText() {
        assertEquals(List.of(), splitter.split("   \n \n "));
        assertEquals(List.of(), splitter.split(""));
        assertEquals(List.of(), splitter.split(null));
    }

    @Test
    @DisplayName("短文本 → 只有一块")
    void shortText() {
        List<String> chunks = splitter.split("只有一句话。");
        assertEquals(1, chunks.size());
        assertEquals("只有一句话。", chunks.get(0));
    }

    @Test
    @DisplayName("按段落合并且每块都不超上限")
    void mergeParagraphs() {
        List<String> chunks = splitter.split("aaaaaaaaaaaa\n\nbbbbbbbbbbbb\n\ncccccccccccc");

        assertEquals(3, chunks.size());
        assertEquals("aaaaaaaaaaaa", chunks.get(0));
        assertEquals("aaaaa\nbbbbbbbbbbbb", chunks.get(1));     // 开头那 5 个 a 就是重叠进来的
        assertEquals("bbbbb\ncccccccccccc", chunks.get(2));
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 20), "没有一块能超过 chunkSize");
    }

    @Test
    @DisplayName("相邻块之间留有重叠(下一块以上一块结尾的 N 个字开头)")
    void overlapBetweenChunks() {
        List<String> chunks = splitter.split("aaaaaaaaaaaa\n\nbbbbbbbbbbbb\n\ncccccccccccc");

        String tailOfFirst = chunks.get(0).substring(chunks.get(0).length() - 5);
        assertTrue(chunks.get(1).startsWith(tailOfFirst));
        String tailOfSecond = chunks.get(1).substring(chunks.get(1).length() - 5);
        assertTrue(chunks.get(2).startsWith(tailOfSecond));
    }

    @Test
    @DisplayName("单段超长(没有空行)→ 硬切成多块")
    void hardSplitWhenParagraphTooLong() {
        List<String> chunks = splitter.split("x".repeat(50));

        assertEquals(3, chunks.size());                        // 步长 15:0~20 / 15~35 / 30~50
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 20));
        assertEquals("x".repeat(20), chunks.get(0));
        assertEquals("x".repeat(20), chunks.get(2));
    }

    @Test
    @DisplayName("构造参数非法 → 立刻抛 IllegalArgumentException")
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TextSplitter(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TextSplitter(10, -1));
        assertThrows(IllegalArgumentException.class, () -> new TextSplitter(10, 10));   // 重叠 ≥ 块大小 = 原地打转
    }
}