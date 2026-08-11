package com.its.web.advice;

import com.its.web.client.ApiException;
import com.its.web.session.SessionAccessor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Turns an API failure into a page a person can read (FR-UI-05, FR-UI-20).
 *
 * <p>Because this exists, no controller carries a {@code try/catch} for HTTP problems.
 * The mapping is the one in DESIGN 8.5.
 */
@ControllerAdvice
public class GlobalWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalWebExceptionHandler.class);

    private final SessionAccessor sessionAccessor;

    public GlobalWebExceptionHandler(SessionAccessor sessionAccessor) {
        this.sessionAccessor = sessionAccessor;
    }

    @ExceptionHandler(ApiException.class)
    public Object handleApiException(ApiException ex, HttpServletRequest request, Model model) {
        int status = ex.getStatus().value();

        if (status == HttpStatus.UNAUTHORIZED.value()) {
            // The token has expired underneath the session. Clearing it and asking the
            // user to log in again is the only honest response - every subsequent call
            // would fail the same way, and an error page would leave them stuck.
            sessionAccessor.clear(request);
            return new RedirectView(request.getContextPath() + "/login?reason=expired");
        }

        model.addAttribute("apiMessage", ex.messageOrDefault("Something went wrong."));

        String view = switch (status) {
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 503 -> "error/503";
            default -> "error/500";
        };

        if (status >= 500) {
            log.error("API error {} on {}: {}", status, request.getRequestURI(), ex.getMessage());
        } else {
            log.debug("API error {} on {}: {}", status, request.getRequestURI(), ex.getMessage());
        }

        return new ModelAndView(view, model.asMap(), HttpStatus.valueOf(status));
    }

    /**
     * A URL that matches no handler and no static resource is a 404, not a 500.
     *
     * <p>Without this the catch-all below turns every mistyped path into "something went
     * wrong", which tells the user their request failed when in fact it was never a real
     * address - and buries genuine 500s in noise.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ModelAndView handleNotFound(Exception ex, HttpServletRequest request, Model model) {
        log.debug("No handler for {}", request.getRequestURI());

        model.addAttribute("apiMessage",
                "The page you asked for does not exist.");
        return new ModelAndView("error/404", model.asMap(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception rendering {}", request.getRequestURI(), ex);

        model.addAttribute("apiMessage", "An unexpected error occurred.");
        return new ModelAndView("error/500", model.asMap(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
