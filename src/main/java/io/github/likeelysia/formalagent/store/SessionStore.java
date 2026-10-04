package io.github.likeelysia.formalagent.store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.likeelysia.formalagent.chat.ChatSession;
import org.springframework.stereotype.Component;

/**
 * 会话存档/读档。
 *
 * <p>.bin = Java 原生序列化(二进制,学机制用);.json = 给人看的可读版。
 *
 * <p><b>2026-10-04 重构:由「静态工具类」改为「Spring 管理的 bean」。</b>
 * 原因:它依赖文件系统与 ObjectMapper,属于"有依赖、可能被替换"的<b>服务</b>,
 * 而不是无状态的纯函数工具。静态方法无法被容器注入、无法被 AOP 代理、
 * 也无法多态替换(将来从"本地文件"换成"数据库/云存储"时会被卡住)。
 *
 * <p>原来的调用方式(静态)已变为<b>实例方法</b>:调用方从容器取 bean 即可,
 * 依赖由容器统一装配。
 */
@Component
public class SessionStore {

    /** 默认存放目录:sessions/(程序正常运行时用这个) */
    private static final Path DEFAULT_DIR = Paths.get("sessions");

    private final ObjectMapper mapper;               // ← 由容器注入的统一实例

    public SessionStore(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // ==================== 存档 ====================

    /** 【存档】写进默认目录 sessions/ */
    public void save(ChatSession session) {
        save(session, DEFAULT_DIR);
    }

    /** 【存档】写进指定目录(测试会传 @TempDir 的临时目录) */
    public void save(ChatSession session, Path dir) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(session.getName() + ".bin");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(session);        // ★ 一次把整个对象写进去
            }
            System.out.println("[SessionStore] 已存档:" + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[SessionStore] 存档失败:" + e.getMessage());
        }
    }

    // ==================== 读档 ====================

    /** 【读档】从默认目录 sessions/ 读 */
    public ChatSession load(String name) {
        return load(name, DEFAULT_DIR);
    }

    /** 【读档】从指定目录读;文件不存在返回 null(第一次运行的正常情况) */
    public ChatSession load(String name, Path dir) {
        Path file = dir.resolve(name + ".bin");
        if (!Files.exists(file)) {
            return null;                          // 没档可读
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (ChatSession) in.readObject(); // ★ 反序列化,要强转成 ChatSession
        } catch (IOException | ClassNotFoundException e) {
            // ClassNotFoundException 是 readObject 会抛的受检异常,必须一起处理
            System.err.println("[SessionStore] 读档失败:" + e.getMessage());
            return null;
        }
    }

    // ==================== 导出可读 JSON ====================

    /** 【导出】写进默认目录 sessions/ */
    public void exportJson(ChatSession session) {
        exportJson(session, DEFAULT_DIR);
    }

    /** 【导出】写进指定目录 */
    public void exportJson(ChatSession session, Path dir) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(session.getName() + ".json");
            mapper.writerWithDefaultPrettyPrinter()          // 缩进美化,方便人看
                    .writeValue(file.toFile(), session.getMessages());
            System.out.println("[SessionStore] 已导出可读版:" + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[SessionStore] 导出 JSON 失败:" + e.getMessage());
        }
    }
}
