package io.github.likeelysia.formalagent.extract;

import io.github.likeelysia.formalagent.exception.AgentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** 把知识点清单渲染成给人看的 Markdown,并写到文件。 */
@Component
public class KnowledgeReport {

    /** 渲染成 Markdown 文本。 */
    public String render(String title, List<KnowledgePoint> points) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 知识点清单:").append(title).append('\n').append('\n');

        if (points.isEmpty()) {
            sb.append("> 这份材料里没有提取到知识点。\n");
            return sb.toString();
        }

        sb.append("共 ").append(points.size()).append(" 个知识点。\n\n");
        for (int i = 0; i < points.size(); i++) {
            KnowledgePoint p = points.get(i);
            sb.append(i + 1).append(". **").append(p.name()).append("** —— ")
                    .append(p.detail()).append('\n');
        }
        return sb.toString();
    }

    /** 渲染 → 写文件(UTF-8;父目录不存在会自动建)。返回写出的路径。 */
    public Path save(Path out, String title, List<KnowledgePoint> points) {
        try {
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(out, render(title, points), StandardCharsets.UTF_8);
            return out;
        } catch (IOException e) {
            throw new AgentException("写知识点清单失败:" + out, e);
        }
    }
}