package com.xiaohezi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "gemini.enabled=false",
    "app.rocketmq.enabled=false"
})
@AutoConfigureMockMvc
class AppControllerIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublicAndTraceable() throws Exception {
        mvc.perform(get("/api/health").header("X-Request-Id", "test-trace"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-Id", "test-trace"))
            .andExpect(jsonPath("$.traceId").value("test-trace"))
            .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mvc.perform(post("/api/conversations").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
    }

    @Test
    void loginRejectsMalformedInputWithoutLeakingInternals() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
    }

    @Test
    void validLoginReturnsConfiguredToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"demo\",\"password\":\"demo123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("xhz-demo-token"));
    }

    @Test
    void metricsRequireAdminRole() throws Exception {
        mvc.perform(get("/actuator/metrics").with(jwt()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk());
    }
}
