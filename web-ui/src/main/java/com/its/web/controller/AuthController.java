package com.its.web.controller;

import com.its.web.client.ApiException;
import com.its.web.client.ItsApiClient;
import com.its.web.form.LoginForm;
import com.its.web.form.SignUpForm;
import com.its.web.session.SessionAccessor;
import com.its.web.session.SessionUser;
import com.its.web.view.Views.LoginResult;
import com.its.web.view.Views.SignUpResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Sign-up, login and logout - the only pages reachable without a session (FR-UI-01). */
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final ItsApiClient api;
    private final SessionAccessor sessionAccessor;

    public AuthController(ItsApiClient api, SessionAccessor sessionAccessor) {
        this.api = api;
        this.sessionAccessor = sessionAccessor;
    }

    @GetMapping("/")
    public String root(HttpServletRequest request) {
        return sessionAccessor.current(request)
                .map(user -> "redirect:" + user.dashboardPath())
                .orElse("redirect:/login");
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String reason,
                            HttpServletRequest request,
                            Model model) {

        // Already logged in - send them on rather than showing a form they do not need.
        var existing = sessionAccessor.current(request);
        if (existing.isPresent()) {
            return "redirect:" + existing.get().dashboardPath();
        }

        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("notice", noticeFor(reason));

        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult binding,
                        HttpServletRequest request,
                        Model model) {

        if (binding.hasErrors()) {
            return "auth/login";
        }

        try {
            LoginResult result = api.login(form.getEmail(), form.getPassword());

            SessionUser user = new SessionUser(
                    result.userId(), result.name(), result.role(), result.token());
            sessionAccessor.store(request, user);

            log.info("User {} signed in as {}", result.userId(), result.role());

            // Role decides the destination (FR-UI-02, FR-USR-06).
            return "redirect:" + user.dashboardPath();

        } catch (ApiException ex) {
            if (ex.isUnauthorized()) {
                // One message for both "no such account" and "wrong password" - the API
                // does not distinguish them, and neither should this page.
                model.addAttribute("error", "Invalid email or password.");
                return "auth/login";
            }
            throw ex;
        }
    }

    @GetMapping("/signup")
    public String signUpPage(Model model) {
        model.addAttribute("signUpForm", new SignUpForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signUp(@Valid @ModelAttribute("signUpForm") SignUpForm form,
                         BindingResult binding,
                         Model model) {

        if (binding.hasErrors()) {
            return "auth/signup";
        }

        try {
            SignUpResult result = api.signUp(form.getName(), form.getEmail(),
                    form.getPassword(), form.getProfile(), form.getRole());

            // The case study specifies this confirmation and a hyperlink to login.
            model.addAttribute("created", result);
            return "auth/signup-success";

        } catch (ApiException ex) {
            if (ex.getStatus().value() == 409) {
                binding.rejectValue("email", "duplicate",
                        "That email address is already registered.");
                return "auth/signup";
            }

            // A 400 from the service means this tier's constraints and the service's have
            // drifted apart. Show them against their fields rather than a generic page.
            if (ex.getStatus().value() == 400 && !ex.fieldErrors().isEmpty()) {
                ex.fieldErrors().forEach(fieldError ->
                        binding.rejectValue(fieldError.field(), "invalid", fieldError.reason()));
                return "auth/signup";
            }

            throw ex;
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        sessionAccessor.clear(request);
        return "redirect:/login?reason=logged-out";
    }

    private String noticeFor(String reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason) {
            case "expired" -> "Your session has expired. Please sign in again.";
            case "required" -> "Please sign in to continue.";
            case "logged-out" -> "You have been signed out.";
            default -> null;
        };
    }
}
