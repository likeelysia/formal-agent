package io.github.likeelysia.formalagent.llm.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ChatResponse 的反序列化测试:确认能从真实响应里取到 content。 */
class ChatResponseTest {

    /** 与容器里那份 ObjectMapper 保持一致的配置:忽略不认识的多余字段 */
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    @DisplayName("解析真实响应片段:取到 choices[0].message.content")
    void shouldParseContentFromRealisticPayload() throws Exception {
        String json = """
                {
                  "id": "abc",
                  "object": "chat.completion",
                  "choices": [
                    {
                      "index": 0,
                      "message": { "role": "assistant", "content": "喵~" },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": { "prompt_tokens": 10, "completion_tokens": 3 }
                }
                """;

        ChatResponse response = mapper.readValue(json, ChatResponse.class);

        assertEquals("喵~", response.choices().get(0).message().content());
        assertEquals("assistant", response.choices().get(0).message().role());
    }

    @Test
    @DisplayName("API 新增字段不该导致解析失败")
    void shouldIgnoreUnknownFields() throws Exception {
        String json = """
                {"choices": [{"message": {"role": "assistant", "content": "hi"}, "weird_new_field": 1}],
                 "extra_top_level": true}
                """;

        ChatResponse response = mapper.readValue(json, ChatResponse.class);

        assertEquals("hi", response.choices().get(0).message().content());
    }
}
