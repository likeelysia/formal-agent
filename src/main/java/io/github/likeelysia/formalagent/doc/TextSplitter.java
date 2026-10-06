package io.github.likeelysia.formalagent.doc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文本切块器:把一整篇文本切成"有重叠"的小块。
 * 策略:先按空行分段 → 逐段累加成块 → 单段超长则硬切。
 */
@Component
public class TextSplitter {

    private final int chunkSize;         // 每块最多多少字符
    private final int overlap;           // 相邻块重叠多少字符(防止关键句被切断)

    public TextSplitter(@Value("${fa.extract.chunk-size:800}")int chunkSize, @Value("${fa.extract.overlap:100}")int overlap) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize 必须大于 0,当前是 " + chunkSize);
        if (overlap < 0) throw new IllegalArgumentException("overlap 不能是负数,当前是 " + overlap);
        if (overlap >= chunkSize) throw new IllegalArgumentException(
                "overlap 必须小于 chunkSize,否则每次都在原地重叠、切不完(当前 " + overlap + " >= " + chunkSize + ")");
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    /** 把整篇文本切成一堆块;空白文本返回空列表。 */
    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');   // 统一换行符
        List<String> paragraphs = new ArrayList<>();
        for (String p : normalized.split("\n\\s*\n")) {                       // 空行 = 分段
            String s = p.strip();
            if (!s.isEmpty()) paragraphs.add(s);
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String p : paragraphs) {
            if (p.length() > chunkSize) {                 // ① 单段就超长:当前块收尾,这段硬切
                if (current.length() > 0) { chunks.add(current.toString()); current.setLength(0); }
                chunks.addAll(hardSplit(p));
                continue;
            }
            if (current.length() == 0) {                  // ② 空块:直接放进去
                current.append(p);
                continue;
            }
            if (current.length() + 1 + p.length() <= chunkSize) {   // ③ 塞得下:合并(中间补个换行)
                current.append('\n').append(p);
                continue;
            }
            chunks.add(current.toString());               // ④ 塞不下:收尾
            String tail = current.substring(Math.max(0, current.length() - overlap));  // 留出重叠尾巴
            current.setLength(0);
            if (!tail.isEmpty() && tail.length() + 1 + p.length() <= chunkSize) {
                current.append(tail).append('\n').append(p);
            } else {
                current.append(p);                        // 尾巴 + 新段实在放不下 → 这一段单独成块
            }
        }
        if (current.length() > 0) chunks.add(current.toString());

        return List.copyOf(chunks);                       // 不可变,防止外面乱改
    }

    /** 单段超长时的兜底:按 (chunkSize - overlap) 的步长滑动着切。 */
    private List<String> hardSplit(String paragraph) {
        List<String> out = new ArrayList<>();
        int step = chunkSize - overlap;
        for (int start = 0; ; start += step) {
            int end = Math.min(start + chunkSize, paragraph.length());
            out.add(paragraph.substring(start, end));
            if (end == paragraph.length()) break;
        }
        return out;
    }
}