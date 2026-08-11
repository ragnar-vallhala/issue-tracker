package com.its.user.controller;

import com.its.user.dto.request.LoginRequest;
import com.its.user.dto.request.SignUpRequest;
import com.its.user.dto.request.UpdateUserRequest;
import com.its.user.dto.response.IssueSummary;
import com.its.user.dto.response.LoginResponse;
import com.its.user.dto.response.SignUpResponse;
import com.its.user.dto.response.UserResponse;
import com.its.user.entity.Role;
import com.its.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User Service endpoints (SRS section 9.1).
 *
 * <p>Every method returns {@code ResponseEntity} (Milestone 6). The controller does no
 * work beyond binding, delegating and choosing a status code - all rules live in the
 * service layer.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Identity, credentials and roles")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a new user",
            description = "Sign-up. The 'profile' field is a short text description of the "
                    + "person, not an image URL (SRS A-16).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Email already registered")})
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse created = userService.signUp(request);

        return ResponseEntity
                .created(URI.create("/api/users/" + created.user().userId()))
                .body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and log them in",
            description = "Returns a signed JWT plus the caller's role, which determines "
                    + "which dashboard they are sent to (FR-USR-06).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")})
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping
    @Operation(summary = "Retrieve a list of all users",
            description = "Optionally filtered by role, which the UI uses to populate "
                    + "assignee pickers with only ASSIGNEE users.")
    public ResponseEntity<List<UserResponse>> findAll(
            @RequestParam(required = false) Role role) {

        return ResponseEntity.ok(
                role == null ? userService.findAll() : userService.findByRole(role));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Retrieve details of a specific user by user ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such user")})
    public ResponseEntity<UserResponse> findById(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.findById(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update a user's name or profile description")
    public ResponseEntity<UserResponse> update(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(userService.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> delete(@PathVariable Integer userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/issues")
    @Operation(summary = "Retrieve issues assigned to a specific user by user ID",
            description = "INTER-SERVICE COMMUNICATION - delegates to the Issue Service, "
                    + "which is the sole owner of issue queries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issues assigned to the user"),
            @ApiResponse(responseCode = "404", description = "No such user"),
            @ApiResponse(responseCode = "503", description = "Issue Service unreachable")})
    public ResponseEntity<List<IssueSummary>> issuesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.findIssuesByUserId(userId));
    }

    @GetMapping("/username/{username}/issues")
    @Operation(summary = "Retrieve issues assigned to a specific user by username",
            description = "INTER-SERVICE COMMUNICATION. Username is the local part of the "
                    + "user's email address (SRS A-18).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issues assigned to the user"),
            @ApiResponse(responseCode = "404", description = "No such username"),
            @ApiResponse(responseCode = "503", description = "Issue Service unreachable")})
    public ResponseEntity<List<IssueSummary>> issuesByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findIssuesByUsername(username));
    }
}
