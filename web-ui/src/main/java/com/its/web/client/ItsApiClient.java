package com.its.web.client;

import com.its.web.view.Views.CommentView;
import com.its.web.view.Views.IssueView;
import com.its.web.view.Views.LoginResult;
import com.its.web.view.Views.ProjectView;
import com.its.web.view.Views.SignUpResult;
import com.its.web.view.Views.UserView;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Every call this tier makes to the system, in one place.
 *
 * <p>All traffic goes through the gateway - this class never names a service, a port or
 * a registry (SRS C-07). The bearer token is attached by an interceptor, so no method
 * here takes one.
 *
 * <p>Transport failures are converted to a 503 {@link ApiException} by
 * {@link #execute(Supplier)}, which means callers and the error advice see exactly one
 * exception type whether the API answered with an error or could not be reached at all.
 */
@Component
public class ItsApiClient {

    private static final Logger log = LoggerFactory.getLogger(ItsApiClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ItsApiClient(RestTemplate gatewayRestTemplate,
                        @Value("${its.gateway.base-url}") String baseUrl) {
        this.restTemplate = gatewayRestTemplate;
        this.baseUrl = baseUrl;
    }

    // ---------------------------------------------------------------- users

    public SignUpResult signUp(String name, String email, String password,
                               String profile, String role) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("profile", profile);
        body.put("role", role);

        return execute(() ->
                restTemplate.postForObject(url("/api/users"), body, SignUpResult.class));
    }

    public LoginResult login(String email, String password) {
        Map<String, Object> body = Map.of("email", email, "password", password);

        return execute(() ->
                restTemplate.postForObject(url("/api/users/login"), body, LoginResult.class));
    }

    public List<UserView> listUsers() {
        return execute(() -> asList(
                restTemplate.getForObject(url("/api/users"), UserView[].class)));
    }

    /** Used to populate assignee pickers with only the users who can hold work. */
    public List<UserView> listAssignees() {
        return execute(() -> asList(restTemplate.getForObject(
                url("/api/users?role=ASSIGNEE"), UserView[].class)));
    }

    public UserView getUser(Integer userId) {
        return execute(() ->
                restTemplate.getForObject(url("/api/users/" + userId), UserView.class));
    }

    // ------------------------------------------------------------- projects

    public List<ProjectView> listProjects() {
        return execute(() -> asList(
                restTemplate.getForObject(url("/api/projects"), ProjectView[].class)));
    }

    public List<ProjectView> listProjectsByOwner(Integer ownerId) {
        return execute(() -> asList(restTemplate.getForObject(
                url("/api/projects/owner/" + ownerId), ProjectView[].class)));
    }

    public ProjectView getProject(Integer projectId) {
        return execute(() -> restTemplate.getForObject(
                url("/api/projects/" + projectId), ProjectView.class));
    }

    public ProjectView createProject(String name, Integer ownerId,
                                     String startDate, String endDate) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectName", name);
        body.put("projectOwnerId", ownerId);
        body.put("startDate", startDate);
        body.put("endDate", endDate);

        return execute(() ->
                restTemplate.postForObject(url("/api/projects"), body, ProjectView.class));
    }

    public void updateProject(Integer projectId, String name, Integer ownerId,
                              String startDate, String endDate) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectName", name);
        body.put("projectOwnerId", ownerId);
        body.put("startDate", startDate);
        body.put("endDate", endDate);

        execute(() -> {
            restTemplate.put(url("/api/projects/" + projectId), body);
            return null;
        });
    }

    /** Cascades to the project's issues and their comments (FR-PRJ-09). */
    public void deleteProject(Integer projectId) {
        execute(() -> {
            restTemplate.delete(url("/api/projects/" + projectId));
            return null;
        });
    }

    // --------------------------------------------------------------- issues

    public List<IssueView> issuesByProject(Integer projectId) {
        return execute(() -> asList(restTemplate.getForObject(
                url("/api/issues/project/" + projectId), IssueView[].class)));
    }

    public List<IssueView> issuesByAssignee(Integer assigneeId) {
        return execute(() -> asList(restTemplate.getForObject(
                url("/api/issues/assignee/" + assigneeId), IssueView[].class)));
    }

    public List<IssueView> issuesByCreator(Integer ownerId) {
        return execute(() -> asList(restTemplate.getForObject(
                url("/api/issues/owner/" + ownerId), IssueView[].class)));
    }

    public IssueView getIssue(Integer issueId) {
        return execute(() ->
                restTemplate.getForObject(url("/api/issues/" + issueId), IssueView.class));
    }

    public IssueView createIssue(Map<String, Object> body) {
        return execute(() ->
                restTemplate.postForObject(url("/api/issues"), body, IssueView.class));
    }

    public void updateIssue(Integer issueId, Map<String, Object> body) {
        execute(() -> {
            restTemplate.put(url("/api/issues/" + issueId), body);
            return null;
        });
    }

    public void deleteIssue(Integer issueId) {
        execute(() -> {
            restTemplate.delete(url("/api/issues/" + issueId));
            return null;
        });
    }

    // ------------------------------------------------------------- comments

    public List<CommentView> commentsByIssue(Integer issueId) {
        return execute(() -> asList(restTemplate.getForObject(
                url("/api/comments/issue/" + issueId), CommentView[].class)));
    }

    public void addComment(Integer issueId, Integer authorId, String body) {
        Map<String, Object> payload = Map.of(
                "issueId", issueId, "authorId", authorId, "body", body);

        execute(() -> restTemplate.postForObject(
                url("/api/comments"), payload, CommentView.class));
    }

    public void deleteComment(Integer commentId) {
        execute(() -> {
            restTemplate.delete(url("/api/comments/" + commentId));
            return null;
        });
    }

    // ---------------------------------------------------------------- misc

    private String url(String path) {
        return baseUrl + path;
    }

    private static <T> List<T> asList(T[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    /**
     * Runs a call, converting an unreachable gateway into a 503 {@link ApiException}.
     *
     * <p>{@link ApiException} itself passes through untouched - it has already been
     * built from a real response by the error handler.
     */
    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();

        } catch (ApiException ex) {
            throw ex;

        } catch (ResourceAccessException ex) {
            log.warn("Gateway unreachable: {}", ex.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, new ApiException.ApiError(
                    null, 503, "Service Unavailable",
                    "The application services are not reachable right now.", null, null));
        }
    }
}
