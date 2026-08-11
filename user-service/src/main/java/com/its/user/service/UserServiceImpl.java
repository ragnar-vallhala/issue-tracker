package com.its.user.service;

import com.its.user.client.IssueClient;
import com.its.user.dto.request.LoginRequest;
import com.its.user.dto.request.SignUpRequest;
import com.its.user.dto.request.UpdateUserRequest;
import com.its.user.dto.response.IssueSummary;
import com.its.user.dto.response.LoginResponse;
import com.its.user.dto.response.SignUpResponse;
import com.its.user.dto.response.UserResponse;
import com.its.user.entity.Role;
import com.its.user.entity.User;
import com.its.user.exception.DuplicateResourceException;
import com.its.user.exception.InvalidCredentialsException;
import com.its.user.exception.ResourceNotFoundException;
import com.its.user.repository.UserRepository;
import com.its.user.security.JwtService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IssueClient issueClient;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           IssueClient issueClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.issueClient = issueClient;
    }

    @Override
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        String email = normaliseEmail(request.email());

        // Checked explicitly so the caller gets a 409 naming the field, rather than the
        // 500 that a raw unique-constraint violation would produce (FR-USR-02).
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("email", "Email is already registered");
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.profile(),
                request.role());

        User saved = userRepository.save(user);
        log.info("Created user {} with role {}", saved.getUserId(), saved.getRole());

        return SignUpResponse.from(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normaliseEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Note both failure paths above throw the identical exception with the identical
        // message. That symmetry is the point: any difference between "unknown email" and
        // "wrong password" is an account enumeration oracle (FR-USR-05).
        log.info("User {} logged in", user.getUserId());

        return new LoginResponse(
                jwtService.issue(user),
                user.getUserId(),
                user.getName(),
                user.getRole(),
                jwtService.getExpirySeconds());
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    public List<UserResponse> findByRole(Role role) {
        return userRepository.findByRole(role).stream().map(UserResponse::from).toList();
    }

    @Override
    public UserResponse findById(Integer userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Override
    @Transactional
    public UserResponse update(Integer userId, UpdateUserRequest request) {
        User user = requireUser(userId);

        // Null means "leave alone", so a caller can send just the field they changed.
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.profile() != null) {
            user.setProfile(request.profile());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Integer userId) {
        User user = requireUser(userId);
        userRepository.delete(user);
        log.info("Deleted user {}", userId);
    }

    @Override
    public List<IssueSummary> findIssuesByUserId(Integer userId) {
        // Confirm the user exists before delegating, so a bad id is a 404 from here
        // rather than an empty list from the Issue Service - "no such user" and "no
        // issues" are different answers and deserve different status codes.
        requireUser(userId);
        return issueClient.findByAssignee(userId);
    }

    @Override
    public List<IssueSummary> findIssuesByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.user(username));
        return issueClient.findByAssignee(user.getUserId());
    }

    private User requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));
    }

    /**
     * Emails are stored and compared lower-cased and trimmed.
     *
     * <p>Without this, {@code Emily@x.com} and {@code emily@x.com} are two accounts that
     * a user would reasonably expect to be one, and the uniqueness check in
     * {@link #signUp} would not catch the second.
     */
    private String normaliseEmail(String email) {
        return email.trim().toLowerCase();
    }
}
