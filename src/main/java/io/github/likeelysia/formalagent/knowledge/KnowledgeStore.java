package io.github.likeelysia.formalagent.knowledge;

import java.util.List;

/** 知识库:存知识点 + 按查询检索。实现可换(JSON 文件 → 数据库 → 向量库)。 */
public interface KnowledgeStore {

    /** 批量加入(同 id 覆盖)。 */
    void addAll(List<KnowledgeItem> items);

    /** 取出全部知识点。 */
    List<KnowledgeItem> all();

    /** 检索最相关的 topK 条(向量检索;embedding 服务不可用时降级为关键词匹配)。 */
    List<KnowledgeItem> search(String query, int topK);
}