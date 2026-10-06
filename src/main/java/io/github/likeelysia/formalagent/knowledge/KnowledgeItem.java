package io.github.likeelysia.formalagent.knowledge;

/**
 * 知识库里的一条知识点:内容 + 出处。
 * id 稳定标识(去重用);source=来源文件;location=位置(页码等,暂无则 null);type=来源类型。
 */
public record KnowledgeItem(
        String id,
        String name,
        String detail,
        String source,
        String location,
        String type) {
}