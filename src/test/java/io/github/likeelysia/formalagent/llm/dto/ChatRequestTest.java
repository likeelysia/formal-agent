package io.github.likeelysia.formalagent.llm.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** ChatRequest 的序列化测试:确认产出的 JSON 字段名/形状符合 API 约定。 */
class ChatRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("序列化:max_tokens 必须是下划线命名(靠 @JsonProperty 映射)")
    void shouldSerializeMaxTokensAsSnakeCase() throws Exception {
        ChatRequest request = new ChatRequest(
                "deepseek-chat",
                List.of(new Message("user", "你好")),
                8192,
                null);

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"max_tokens\":8192"), json);
        assertTrue(json.contains("\"model\":\"deepseek-chat\""), json);
        assertTrue(json.contains("\"role\":\"user\""), json);
        assertTrue(json.contains("\"content\":\"你好\""), json);
    }

    @Test
    @DisplayName("普通对话:responseFormat=null 时不应出现 response_format 字段")
    void omitsResponseFormatWhenNull() throws Exception {
        ChatRequest request = new ChatRequest(
                "deepseek-chat", List.of(new Message("user", "hi")), 8192, null);

        String json = mapper.writeValueAsString(request);

        assertFalse(json.contains("response_format"), json);
    }

    @Test
    @DisplayName("结构化输出:responseFormat=JSON 时应带上 response_format")
    void includesJsonResponseFormat() throws Exception {
        ChatRequest request = new ChatRequest(
                "deepseek-chat", List.of(new Message("user", "hi")), 8192,
                ChatRequest.ResponseFormat.JSON);

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"response_format\":{\"type\":\"json_object\"}"), json);
    }
}
