package com.its.comment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CommentServiceApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private int createComment(int issueId, int authorId, String body) throws Exception {
        String response = mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new java.util.HashMap<>(java.util.Map.of(
                                        "issueId", issueId,
                                        "authorId", authorId,
                                        "body", body)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("commentId").asInt();
    }

    @Test
    @DisplayName("A comment can be created and read back on its issue")
    void createAndRead() throws Exception {
        createComment(500, 104, "Reproduced on Safari 17.");

        mockMvc.perform(get("/api/comments/issue/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Reproduced on Safari 17."))
                .andExpect(jsonPath("$[0].authorId").value(104));
    }

    @Test
    @DisplayName("The count endpoint reports the thread length")
    void countsComments() throws Exception {
        createComment(501, 104, "First.");
        createComment(501, 102, "Second.");

        mockMvc.perform(get("/api/comments/issue/501/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    @DisplayName("An empty thread counts zero rather than erroring")
    void countsEmptyThread() throws Exception {
        mockMvc.perform(get("/api/comments/issue/999999/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    @DisplayName("The author may edit their own comment")
    void authorMayEdit() throws Exception {
        int id = createComment(502, 104, "Origional text.");

        mockMvc.perform(put("/api/comments/" + id)
                        .header("X-User-Id", 104)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Original text.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Original text."));
    }

    @Test
    @DisplayName("Someone else may not edit it")
    void nonAuthorMayNotEdit() throws Exception {
        int id = createComment(503, 104, "Carlos wrote this.");

        mockMvc.perform(put("/api/comments/" + id)
                        .header("X-User-Id", 102)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Michael rewriting it.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Only the author")));
    }

    @Test
    @DisplayName("Someone else may not delete it")
    void nonAuthorMayNotDelete() throws Exception {
        int id = createComment(504, 104, "Carlos wrote this too.");

        mockMvc.perform(delete("/api/comments/" + id).header("X-User-Id", 102))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("The cascade removes a whole thread regardless of who wrote what")
    void cascadeIgnoresAuthorship() throws Exception {
        createComment(505, 104, "From Carlos.");
        createComment(505, 102, "From Michael.");
        createComment(505, 101, "From Emily.");

        // Applying the authorship rule here would make the cascade fail on any thread
        // with more than one participant - which is to say, on any real thread.
        mockMvc.perform(delete("/api/comments/issue/505"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/comments/issue/505/count"))
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    @DisplayName("Cascading an issue with no comments succeeds, so the caller can retry")
    void cascadeOnEmptyThreadIsIdempotent() throws Exception {
        mockMvc.perform(delete("/api/comments/issue/888888"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/comments/issue/888888"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("An empty comment body is rejected")
    void rejectsBlankBody() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueId\":506,\"authorId\":104,\"body\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"));
    }

    @Test
    @DisplayName("Editing a comment that does not exist is 404")
    void unknownCommentIs404() throws Exception {
        mockMvc.perform(put("/api/comments/424242")
                        .header("X-User-Id", 104)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Anything.\"}"))
                .andExpect(status().isNotFound());
    }
}
