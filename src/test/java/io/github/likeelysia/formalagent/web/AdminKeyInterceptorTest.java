package io.github.likeelysia.formalagent.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 管理接口鉴权测试:没配=放行;配了=带对放行、带错/不带 401。 */
class AdminKeyInterceptorTest {

    @Test
    @DisplayName("没配密钥 → 直接放行(开发模式)")
    void openWhenNotConfigured() throws Exception {
        AdminKeyInterceptor interceptor = new AdminKeyInterceptor("");

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(),
                new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("配了密钥:带对 → 放行")
    void allowsCorrectKey() throws Exception {
        AdminKeyInterceptor interceptor = new AdminKeyInterceptor("s3cret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Admin-Key", "s3cret");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("配了密钥:带错 / 不带 → 401 + JSON 错误")
    void rejectsMissingOrWrongKey() throws Exception {
        AdminKeyInterceptor interceptor = new AdminKeyInterceptor("s3cret");

        MockHttpServletRequest wrong = new MockHttpServletRequest();
        wrong.addHeader("X-Admin-Key", "nope");
        MockHttpServletResponse wrongResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(wrong, wrongResponse, new Object()));
        assertEquals(401, wrongResponse.getStatus());
        assertTrue(wrongResponse.getContentAsString().contains("X-Admin-Key"));

        MockHttpServletResponse noneResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), noneResponse, new Object()));
        assertEquals(401, noneResponse.getStatus());
    }
}
