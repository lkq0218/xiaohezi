package com.xiaohezi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.security.mode=oidc", "gemini.enabled=false", "app.rocketmq.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/not-called"
})
@AutoConfigureMockMvc
class OidcAuthorizationIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void userCannotAccessAdminLearningEndpoint() throws Exception {
        mvc.perform(post("/api/learning/concurrency/fanout")
                .with(jwt().authorities(createAuthorityList("ROLE_USER")))
                .contentType("application/json").content("{\"urls\":[\"http://localhost\"]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminPassesAuthorizationBoundary() throws Exception {
        mvc.perform(post("/api/learning/concurrency/fanout")
                .with(jwt().authorities(createAuthorityList("ROLE_ADMIN")))
                .contentType("application/json").content("{\"urls\":[]}"))
            .andExpect(status().isBadRequest());
    }
}
