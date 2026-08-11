package com.its.web.interceptor;

import com.its.web.session.SessionAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sends anyone without a session to the login page (FR-UI-01).
 *
 * <p>Applied to everything except the three public pages, so a page is protected by
 * default: forgetting to annotate a new controller leaves it locked rather than open.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionAccessor sessionAccessor;

    public AuthInterceptor(SessionAccessor sessionAccessor) {
        this.sessionAccessor = sessionAccessor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        if (sessionAccessor.current(request).isPresent()) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/login?reason=required");
        return false;
    }
}
