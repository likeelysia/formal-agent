package io.github.likeelysia.formalagent.llm.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ChatRequest 的序列化测试:确认产出的 JSON 字段名/形状符合 API 约定。 */
class ChatRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("序列化:max_tokens 必须是下划线命名(靠 @JsonProperty 映射)")
    void shouldSerializeMaxTokensAsSnakeCase() throws Exception {
        ChatRequest request = new ChatRequest(
                "deepseek-chat",
                List.of(AgentMessage.user("你好")),
                8192,
                null,
                null);

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"max_tokens\":8192"), json);
        assertTrue(json.contains("\"model\":\"deepseek-chat\""), json);
        assertTrue(json.contains("\"role\":\"user\""), json);
        assertTrue(json.contains("\"content\":\"你好\""), json);
    }

    @Test
    @DisplayName("普通对话:responseFormat / tools 为 null 时都不该出现")
    void omitsOptionalFieldsWhenNull() throws Exception {
        ChatRequest request = new ChatRequest(
                "deepseek-chat", List.of(AgentMessage.user("hi")), 8192, null, null);

        String json = mapper.writeValueAsString(request);

        assertFalse(json.contains("response_format"), json);
        assertFalse(json.contains("tools"), json);
    }

    @Test
    @DisplayName("结构化输出:responseFormat=JSON 时应带上 response_format")
    void includesJsonResponseFormat() throws Exception {
        ChatRequest request = new ChatRequest(
                "deepseek-chat", List.of(AgentMessage.user("hi")), 8192,
                ChatRequest.ResponseFormat.JSON, null);

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"response_format\":{\"type\":\"json_object\"}"), json);
    }

    @Test
    @DisplayName("工具调用:tools 按 OpenAI 格式序列化(function + JSON Schema)")
    void serializesTools() throws Exception {
        ToolDefinition tool = ToolDefinition.of("search_knowledge", "检索知识库",
                Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")),
                        "required", List.of("query")));

        ChatRequest request = new ChatRequest(
                "deepseek-chat", List.of(AgentMessage.user("hi")), 8192, null, List.of(tool));

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"type\":\"function\""), json);
        assertTrue(json.contains("\"name\":\"search_knowledge\""), json);
        assertTrue(json.contains("\"parameters\""), json);
    }

    @Test
    @DisplayName("工具结果消息:role=tool 且带 tool_call_id;assistant 消息带 tool_calls")
    void serializesToolProtocolMessages() throws Exception {
        AgentMessage assistant = AgentMessage.assistantWithTools(List.of(
                new ToolCall("call_1", "function", new ToolCall.Function("search_knowledge", "{\"query\":\"x\"}"))));
        AgentMessage toolResult = AgentMessage.tool("call_1", "结果");

        String json = mapper.writeValueAsString(new ChatRequest(
                "deepseek-chat", List.of(assistant, toolResult), 8192, null, null));

        assertTrue(json.contains("\"tool_calls\""), json);
        assertTrue(json.contains("\"tool_call_id\":\"call_1\""), json);
        assertTrue(json.contains("\"role\":\"tool\""), json);
    }
}
