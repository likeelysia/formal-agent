package io.github.likeelysia.formalagent.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.chat.Message;

/**
 * SessionStore 的单元测试(存档 / 读档 / 导出 JSON)。
 *
 * <p>关键角色 <b>@TempDir</b>:JUnit 会给每个测试方法注入一个"临时目录",
 * 测试跑完**自动删除**。所以这些测试只写临时目录(通过带 dir 的重载),
 * <b>绝不会碰到你真实的 sessions/ 目录</b>。
 */
class SessionStoreTest {

    /** JUnit 自动创建并注入的临时目录(每个测试方法各自独立) */
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("存档 → 读档:名字、条数、内容都要一模一样(往返测试)")
    void saveThenLoadShouldRoundTrip() {
        // ① 准备一个会话并塞入历史
        ChatSession original = new ChatSession("unit-test");
        original.add(new Message("system", "你是猫咪饲养员"));
        original.add(new Message("user", "你好"));
        original.add(new Message("assistant", "喵~"));

        // ② 存档 + 读档
        SessionStore.save(original, tempDir);
        ChatSession loaded = SessionStore.load("unit-test", tempDir);

        // ③ 断言
        assertNotNull(loaded, "存过档就应该能读回来");
        assertEquals("unit-test", loaded.getName(), "名字应当一致");
        assertEquals(3, loaded.getMessages().size(), "历史条数应当一致");
        assertEquals("喵~", loaded.getMessages().get(2).getContent(), "最后一条内容应当一致");
    }

    @Test
    @DisplayName("读一个不存在的档:应当返回 null,而不是报错")
    void loadMissingShouldReturnNull() {
        assertNull(SessionStore.load("这个会话不存在", tempDir));
    }

    @Test
    @DisplayName("存档应当在目录里生成 <名字>.bin 文件")
    void saveShouldCreateBinFile() {
        ChatSession s = new ChatSession("bin-test");
        s.add(new Message("user", "x"));

        SessionStore.save(s, tempDir);

        assertTrue(Files.exists(tempDir.resolve("bin-test.bin")), "应当生成 bin-test.bin");
    }

    @Test
    @DisplayName("导出 JSON:文件生成、内容是可解析的 JSON、字段对")
    void exportJsonShouldWriteParsableJson() throws IOException {
        ChatSession s = new ChatSession("json-test");
        s.add(new Message("user", "你好"));

        SessionStore.exportJson(s, tempDir);

        Path json = tempDir.resolve("json-test.json");
        assertTrue(Files.exists(json), "应当生成 json-test.json");

        // 能解析 = 是合法 JSON(这也是"导出没坏"最直接的证据)
        JsonNode root = new ObjectMapper().readTree(json.toFile());
        assertTrue(root.isArray(), "顶层应当是一个 JSON 数组");
        assertEquals(1, root.size(), "一条消息 → 数组里一个元素");
        assertEquals("user", root.get(0).path("role").asText());
        assertEquals("你好", root.get(0).path("content").asText());
    }

    @Test
    @DisplayName("默认目录的重载:读不存在的档也返回 null(且不写任何文件)")
    void defaultDirOverloadShouldStillWork() {
        // 只做"读"操作 → 不会污染真实的 sessions/
        assertNull(SessionStore.load("__肯定不存在的会话名__"));
    }
}
