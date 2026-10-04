package io.github.likeelysia.formalagent.llm.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import java.util.List;
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
                List.of(new Message("user", "你好")),
                8192);

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"max_tokens\":8192"), json);
        assertTrue(json.contains("\"model\":\"deepseek-chat\""), json);
        assertTrue(json.contains("\"role\":\"user\""), json);
        assertTrue(json.contains("\"content\":\"你好\""), json);
    }
}
