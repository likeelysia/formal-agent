package io.github.likeelysia.formalagent.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.likeelysia.formalagent.exception.AgentException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** KnowledgeReport 的单元测试:纯字符串拼接 + 写文件,零网络。 */
class KnowledgeReportTest {

    private static final List<KnowledgePoint> POINTS = List.of(
            new KnowledgePoint("IoC", "控制反转,把创建对象的权力交给容器"),
            new KnowledgePoint("DI", "依赖注入,容器把依赖塞进对象"));

    @TempDir Path tmp;

    @Test
    @DisplayName("正常渲染:标题 + 统计 + 带序号的加粗条目")
    void rendersPoints() {
        String md = new KnowledgeReport().render("lesson.md", POINTS);

        assertTrue(md.startsWith("# 知识点清单:lesson.md\n\n"), "第一行应该是标题");
        assertTrue(md.contains("共 2 个知识点。"));
        assertTrue(md.contains("1. **IoC** —— 控制反转,把创建对象的权力交给容器"));
        assertTrue(md.contains("2. **DI** —— 依赖注入,容器把依赖塞进对象"));
        assertTrue(md.indexOf("**IoC**") < md.indexOf("**DI**"), "顺序应该和入参一致");
    }

    @Test
    @DisplayName("空清单:给一句提示,不写统计行")
    void rendersEmptyHint() {
        String md = new KnowledgeReport().render("empty.md", List.of());

        assertTrue(md.contains("> 这份材料里没有提取到知识点。"));
        assertFalse(md.contains("共"), "没有知识点就不该有'共 N 个'");
    }

    @Test
    @DisplayName("写文件:父目录自动创建,内容与 render 一致(UTF-8)")
    void savesFileWithParentDirs() throws Exception {
        Path out = tmp.resolve("a").resolve("b").resolve("out.md");

        Path saved = new KnowledgeReport().save(out, "lesson.md", POINTS);

        assertEquals(out, saved);
        assertTrue(Files.exists(saved), "文件应该真的写出来了");
        assertEquals(new KnowledgeReport().render("lesson.md", POINTS),
                Files.readString(saved, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("写失败(父路径被文件占着)→ 包成 AgentException")
    void saveFailureWrapsException() throws Exception {
        Path occupied = tmp.resolve("occupied");
        Files.writeString(occupied, "占位");

        assertThrows(AgentException.class,
                () -> new KnowledgeReport().save(occupied.resolve("x.md"), "t", POINTS));
    }
}