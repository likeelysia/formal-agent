package io.github.likeelysia.formalagent.cli;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

/** 判断"用户粘进来的这段文字"是不是一个可以读的文档路径。 */
public final class FileHint {

    private FileHint() { }   // 工具类:全是静态方法,不让 new

    /**
     * 是能读的 .txt/.md 文件 → 返回它的 Path;否则返回 null(当普通聊天处理)。
     * ⚠️ 先 strip、再去掉可能的成对引号(Windows 右键"复制文件地址"会带引号)。
     */
    public static Path asReadableFile(String raw) {
        String text = unwrap(raw);
        try {
            Path path = Path.of(text);
            if (!Files.isRegularFile(path)) return null;      // 不存在 / 是个目录 → 不当文件
            if (!isReadableSuffix(text)) return null;         // 后缀不在白名单 → 不当文件
            return path;
        } catch (InvalidPathException e) {                    // 有些字符(换行等)根本不是路径
            return null;
        }
    }

    private static String unwrap(String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1).strip();
        }
        return text;
    }

    private static boolean isReadableSuffix(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md");
    }
}