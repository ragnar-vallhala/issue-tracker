package com.its.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI is how every endpoint in SRS 9 is exercised by hand, so the generated
 * document has to carry the {@code bearerAuth} scheme: without it the UI renders the
 * endpoints but its Authorize button is absent, and nothing past the gateway's JWT filter
 * can be called at all. That failure is invisible to every other test here - the document
 * still serves, and the API still works - which is why it is asserted directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("The document declares a bearer scheme and applies it globally")
    void documentCarriesBearerAuth() throws Exception {
        mockMvc.perform(get("/api/users/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    @DisplayName("Login is documented, so a token can be obtained from the UI itself")
    void loginIsDocumented() throws Exception {
        mockMvc.perform(get("/api/users/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users/login'].post").exists());
    }
}
