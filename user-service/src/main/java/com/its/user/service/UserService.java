package com.its.user.service;

import com.its.user.dto.request.LoginRequest;
import com.its.user.dto.request.SignUpRequest;
import com.its.user.dto.request.UpdateUserRequest;
import com.its.user.dto.response.IssueSummary;
import com.its.user.dto.response.LoginResponse;
import com.its.user.dto.response.SignUpResponse;
import com.its.user.dto.response.UserResponse;
import com.its.user.entity.Role;
import java.util.List;

/** Business operations for user management (SRS section 3). */
public interface UserService {

    SignUpResponse signUp(SignUpRequest request);

    LoginResponse login(LoginRequest request);

    List<UserResponse> findAll();

    /** Filtered listing, used by the UI to populate assignee pickers (FR-UI-13). */
    List<UserResponse> findByRole(Role role);

    UserResponse findById(Integer userId);

    UserResponse update(Integer userId, UpdateUserRequest request);

    void delete(Integer userId);

    /** Inter-service: issues assigned to this user (FR-USR-09). */
    List<IssueSummary> findIssuesByUserId(Integer userId);

    /** Inter-service: issues assigned to this user, addressed by username (FR-USR-10). */
    List<IssueSummary> findIssuesByUsername(String username);
}
