package com.its.web.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads and writes the {@link SessionUser}.
 *
 * <p>Also readable outside a controller, via {@link RequestContextHolder} - which is what
 * lets the outbound HTTP interceptor attach the caller's token without every client method
 * having to take one as a parameter.
 */
@Component
public class SessionAccessor {

    public Optional<SessionUser> current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((SessionUser) session.getAttribute(SessionUser.SESSION_KEY));
    }

    /**
     * The current user, resolved from the ambient request.
     *
     * @return empty outside a request, or when nobody is logged in
     */
    public Optional<SessionUser> current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return Optional.empty();
        }
        return current(attrs.getRequest());
    }

    public void store(HttpServletRequest request, SessionUser user) {
        // A fresh session on login. Reusing the pre-login one leaves the system open to
        // session fixation: an attacker who plants a known JSESSIONID before login would
        // otherwise hold a session that is authenticated afterwards.
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }

        request.getSession(true).setAttribute(SessionUser.SESSION_KEY, user);
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
