package com.its.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.its.user.client.IssueClient;
import com.its.user.dto.request.LoginRequest;
import com.its.user.dto.request.SignUpRequest;
import com.its.user.dto.response.LoginResponse;
import com.its.user.dto.response.SignUpResponse;
import com.its.user.entity.Role;
import com.its.user.entity.User;
import com.its.user.exception.DuplicateResourceException;
import com.its.user.exception.InvalidCredentialsException;
import com.its.user.exception.ResourceNotFoundException;
import com.its.user.exception.ServiceUnavailableException;
import com.its.user.repository.UserRepository;
import com.its.user.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private IssueClient issueClient;

    // A real encoder, not a mock: the point of several of these tests is that hashing
    // actually happens, which a stub would happily fake.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserServiceImpl service;

    private UserServiceImpl service() {
        if (service == null) {
            service = new UserServiceImpl(userRepository, passwordEncoder, jwtService, issueClient);
        }
        return service;
    }

    private User existingUser(String rawPassword) {
        User user = new User("Emily Sinha", "emily.sinha@example.com",
                passwordEncoder.encode(rawPassword), "Project owner", Role.PROJECT_OWNER);
        ReflectionTestUtils.setField(user, "userId", 101);
        return user;
    }

    @Test
    @DisplayName("Sign-up stores a BCrypt hash, never the raw password")
    void signUpHashesPassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SignUpRequest request = new SignUpRequest(
                "Carlos Singh", "carlos.singh@example.com", "CarlosStrong$2025",
                "Front-end developer", Role.ASSIGNEE);

        service().signUp(request);

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(saved -> {
            assertThat(saved.getPassword()).isNotEqualTo("CarlosStrong$2025");
            assertThat(saved.getPassword()).startsWith("$2");
            assertThat(passwordEncoder.matches("CarlosStrong$2025", saved.getPassword())).isTrue();
            return true;
        }));
    }

    @Test
    @DisplayName("Sign-up response carries the confirmation the case study specifies")
    void signUpReturnsConfirmationMessage() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SignUpResponse response = service().signUp(new SignUpRequest(
                "Carlos Singh", "carlos.singh@example.com", "CarlosStrong$2025",
                null, Role.ASSIGNEE));

        assertThat(response.message()).isEqualTo("Your account is created successfully");
        assertThat(response.user().role()).isEqualTo(Role.ASSIGNEE);
    }

    @Test
    @DisplayName("Sign-up normalises the email so casing cannot create a second account")
    void signUpNormalisesEmail() {
        when(userRepository.existsByEmail("emily.sinha@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service().signUp(new SignUpRequest("Emily Sinha", "  Emily.Sinha@Example.COM  ",
                "EmilySecure!2025", null, Role.PROJECT_OWNER));

        verify(userRepository).existsByEmail("emily.sinha@example.com");
    }

    @Test
    @DisplayName("A duplicate email is a 409 naming the field, not a constraint violation")
    void signUpRejectsDuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service().signUp(new SignUpRequest(
                "Emily Sinha", "emily.sinha@example.com", "EmilySecure!2025",
                null, Role.PROJECT_OWNER)))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login returns a token, the user's id and their role for dashboard routing")
    void loginSucceeds() {
        User user = existingUser("EmilySecure!2025");
        when(userRepository.findByEmail("emily.sinha@example.com")).thenReturn(Optional.of(user));
        when(jwtService.issue(user)).thenReturn("signed.jwt.value");
        when(jwtService.getExpirySeconds()).thenReturn(3600L);

        LoginResponse response = service().login(
                new LoginRequest("emily.sinha@example.com", "EmilySecure!2025"));

        assertThat(response.token()).isEqualTo("signed.jwt.value");
        assertThat(response.userId()).isEqualTo(101);
        assertThat(response.role()).isEqualTo(Role.PROJECT_OWNER);
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("An unknown email and a wrong password are indistinguishable to the caller")
    void loginFailuresAreIndistinguishable() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        Throwable unknownEmail = org.assertj.core.api.Assertions.catchThrowable(
                () -> service().login(new LoginRequest("ghost@example.com", "whatever12")));

        when(userRepository.findByEmail("emily.sinha@example.com"))
                .thenReturn(Optional.of(existingUser("EmilySecure!2025")));

        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> service().login(new LoginRequest("emily.sinha@example.com", "wrong-one")));

        // Same type, same message. Any divergence here is an account enumeration oracle.
        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    @Test
    @DisplayName("Issues by username resolve through the derived local part")
    void findsIssuesByUsername() {
        User user = existingUser("EmilySecure!2025");
        when(userRepository.findByUsername("emily.sinha")).thenReturn(Optional.of(user));
        when(issueClient.findByAssignee(101)).thenReturn(java.util.List.of());

        service().findIssuesByUsername("emily.sinha");

        verify(issueClient).findByAssignee(101);
    }

    @Test
    @DisplayName("An unknown user is a 404 before the Issue Service is ever called")
    void unknownUserDoesNotReachIssueService() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findIssuesByUserId(999))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(issueClient, never()).findByAssignee(anyInt());
    }

    @Test
    @DisplayName("A downstream outage propagates as 503, never as an empty issue list")
    void issueServiceOutageIsNotSwallowed() {
        User user = existingUser("EmilySecure!2025");
        when(userRepository.findById(101)).thenReturn(Optional.of(user));
        when(issueClient.findByAssignee(101))
                .thenThrow(new ServiceUnavailableException("Issue Service", new RuntimeException()));

        // The alternative - returning List.of() - would tell the caller this user has no
        // work assigned, which is a different claim entirely (SRS A-08).
        assertThatThrownBy(() -> service().findIssuesByUserId(101))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
