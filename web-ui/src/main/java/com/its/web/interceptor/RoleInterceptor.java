package com.its.web.interceptor;

import com.its.web.session.SessionAccessor;
import com.its.web.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Keeps each role inside its own area (FR-UI-03).
 *
 * <p>A mismatch is <em>forwarded</em> to the 403 page, not redirected to the other
 * dashboard. Redirecting is the tempting choice and it produces an infinite bounce the
 * first time a user's role and their bookmark disagree: /owner/** sends them to
 * /assignee/dashboard, which sends them back, forever.
 *
 * <p>This is a usability control, not a security one. The real enforcement is in the
 * services, which check the role on the token for every state change (FR-ISS-07). All
 * this does is keep someone from reaching a page that would fail anyway.
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final SessionAccessor sessionAccessor;

    public RoleInterceptor(SessionAccessor sessionAccessor) {
        this.sessionAccessor = sessionAccessor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        Optional<SessionUser> current = sessionAccessor.current(request);
        if (current.isEmpty()) {
            // AuthInterceptor runs first and will already have redirected; this is only
            // reached if the ordering is ever changed.
            response.sendRedirect(request.getContextPath() + "/login?reason=required");
            return false;
        }

        SessionUser user = current.get();
        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean ownerArea = path.startsWith("/owner");
        boolean assigneeArea = path.startsWith("/assignee");

        if ((ownerArea && !user.isProjectOwner()) || (assigneeArea && !user.isAssignee())) {
            // The status must be set explicitly: a forward renders a different view but
            // does not change the response code, so without this the 403 page would be
            // served as 200 OK - correct to a human reading it, a lie to anything else.
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.getRequestDispatcher("/error/403").forward(request, response);
            return false;
        }

        return true;
    }
}
