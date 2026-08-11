package com.its.issue.service;

/**
 * Who is making the request, as told to us by the gateway.
 *
 * <p>The gateway verifies the JWT once at the edge and injects {@code X-User-Id} and
 * {@code X-User-Role}; downstream services trust those headers (DESIGN section 9). That
 * trust is only sound because the service ports are not publicly routable - if they were
 * ever exposed directly, this service would have to verify the token itself.
 *
 * <p>{@link #anonymous()} covers a call arriving without the headers, which in practice
 * means Postman hitting the service port directly during development. Such a caller is
 * treated as unrestricted, since there is no identity to apply a role rule to. That is a
 * development affordance, and it is the reason the header-stripping filter at the gateway
 * matters: a client must not be able to supply these headers itself.
 */
public record CallerIdentity(Integer userId, String role) {

    private static final CallerIdentity ANONYMOUS = new CallerIdentity(null, null);

    public static CallerIdentity anonymous() {
        return ANONYMOUS;
    }

    public static CallerIdentity of(Integer userId, String role) {
        if (userId == null && role == null) {
            return ANONYMOUS;
        }
        return new CallerIdentity(userId, role);
    }

    public boolean isAssignee() {
        return "ASSIGNEE".equals(role);
    }

    public boolean isKnown() {
        return role != null;
    }
}
