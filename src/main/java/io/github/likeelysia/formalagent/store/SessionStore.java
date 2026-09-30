package io.github.likeelysia.formalagent.store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.likeelysia.formalagent.chat.ChatSession;

/**
 * 会话存档/读档。
 *
 * <p>.bin = Java 原生序列化(二进制,学机制用);.json = 给人看的可读版。
 *
 * <p>2026-09-30 小改:每个方法都加了「可指定目录」的重载。
 * 目的是让<b>单元测试</b>传入一个临时目录,不会把测试数据写进真实的 sessions/。
 * 原来的调用方式(save(s)/load(name)/exportJson(s))完全没变,行为一致。
 */
public final class SessionStore {

    /** 默认存放目录:sessions/(程序正常运行时用这个) */
    private static final Path DEFAULT_DIR = Paths.get("sessions");

    private SessionStore() {
    }

    // ==================== 存档 ====================

    /** 【存档】写进默认目录 sessions/ */
    public static void save(ChatSession session) {
        save(session, DEFAULT_DIR);
    }

    /** 【存档】写进指定目录(测试会传 @TempDir 的临时目录) */
    public static void save(ChatSession session, Path dir) {
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
    public static ChatSession load(String name) {
        return load(name, DEFAULT_DIR);
    }

    /** 【读档】从指定目录读;文件不存在返回 null(第一次运行的正常情况) */
    public static ChatSession load(String name, Path dir) {
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
    public static void exportJson(ChatSession session) {
        exportJson(session, DEFAULT_DIR);
    }

    /** 【导出】写进指定目录 */
    public static void exportJson(ChatSession session, Path dir) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(session.getName() + ".json");
            new ObjectMapper()
                    .writerWithDefaultPrettyPrinter()          // 缩进美化,方便人看
                    .writeValue(file.toFile(), session.getMessages());
            System.out.println("[SessionStore] 已导出可读版:" + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[SessionStore] 导出 JSON 失败:" + e.getMessage());
        }
    }
}
