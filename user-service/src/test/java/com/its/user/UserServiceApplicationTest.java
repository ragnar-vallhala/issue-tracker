package com.its.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.its.user.entity.Role;
import com.its.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Boots the whole service against an in-memory database and exercises the endpoints
 * end to end - JPA mappings, the role converter, validation, the exception handler and
 * the status-code contract of SRS 9.5, all through real HTTP plumbing.
 *
 * <p>The one thing it cannot cover is MySQL-specific behaviour, which is why the
 * acceptance criteria still call for the Postman collection against a real instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserServiceApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("The application context loads with every bean wired")
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    @DisplayName("Sign-up returns 201, the confirmation message, and no password field")
    void signUpReturns201() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Carlos Singh","email":"carlos.singh@example.com",
                                 "password":"CarlosStrong$2025",
                                 "profile":"Front-end developer","role":"ASSIGNEE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your account is created successfully"))
                .andExpect(jsonPath("$.user.userId").exists())
                .andExpect(jsonPath("$.user.role").value("ASSIGNEE"))
                .andExpect(jsonPath("$.user.username").value("carlos.singh"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("The role reaches the database as its integer code and comes back as the enum")
    void rolePersistsAsInteger() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Emily Sinha","email":"emily.role@example.com",
                                 "password":"EmilySecure!2025",
                                 "profile":"Project owner","role":"PROJECT_OWNER"}
                                """))
                .andExpect(status().isCreated());

        assertThat(userRepository.findByEmail("emily.role@example.com"))
                .get()
                .satisfies(user -> {
                    assertThat(user.getRole()).isEqualTo(Role.PROJECT_OWNER);
                    assertThat(user.getRole().getCode()).isZero();
                });
    }

    @Test
    @DisplayName("Invalid input is 400 with per-field detail")
    void validationReturns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","email":"not-an-email","password":"short",
                                 "role":"ASSIGNEE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("An unmappable role value is a 400 that says so, not a 500")
    void unknownRoleReturns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Odd One","email":"odd@example.com",
                                 "password":"password123","role":"ADMINISTRATOR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("PROJECT_OWNER or ASSIGNEE")));
    }

    @Test
    @DisplayName("A duplicate email is 409, reported against the email field")
    void duplicateEmailReturns409() throws Exception {
        String body = """
                {"name":"First User","email":"dupe@example.com",
                 "password":"password123","role":"ASSIGNEE"}
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    @DisplayName("Login issues a token and reports the role for dashboard routing")
    void loginIssuesToken() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Priya Jackson","email":"priya.jackson@example.com",
                                 "password":"PriyaSafe@2025","role":"PROJECT_OWNER"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"priya.jackson@example.com","password":"PriyaSafe@2025"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PROJECT_OWNER"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("A wrong password is 401 and reveals nothing about the account")
    void badLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("An unknown user id is 404 with the uniform error body")
    void unknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/users/424242"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/users/424242"));
    }
}
