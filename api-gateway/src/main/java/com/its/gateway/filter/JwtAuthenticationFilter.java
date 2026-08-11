package com.its.gateway.filter;

import com.its.gateway.security.JwtValidator;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Verifies the JWT once at the edge and tells the services behind it who is calling
 * (FR-SYS-06).
 *
 * <p>Two responsibilities, and the second is the one that is easy to overlook:
 *
 * <ol>
 *   <li><strong>Authenticate.</strong> Everything except sign-up, login and the API
 *       documentation needs a valid bearer token.
 *   <li><strong>Strip, then inject.</strong> {@code X-User-Id} and {@code X-User-Role} are
 *       removed from every inbound request before anything else happens, and re-added only
 *       from verified token claims.
 * </ol>
 *
 * <p>The stripping is not defensive tidiness. Downstream services trust those headers to
 * decide, for example, whether an Assignee may touch an issue - so if a client could set
 * them itself, anyone could send {@code X-User-Role: PROJECT_OWNER} and the entire
 * authorisation model would be decoration. The headers are stripped on public routes too,
 * where no token is checked at all, because that is exactly where an unauthenticated
 * caller would try to smuggle one in.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private static final String BEARER_PREFIX = "Bearer ";

    /** Paths reachable without a token, matched on method as well as path. */
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, "/api/users"),        // sign up
            new PublicRoute(HttpMethod.POST, "/api/users/login")); // log in

    /** Documentation and health, which are read-only and useful before you have a token. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/swagger-ui", "/v3/api-docs", "/webjars", "/actuator/health");

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest incoming = exchange.getRequest();

        // Always first: no request may carry its own identity headers inwards.
        ServerHttpRequest.Builder sanitised = incoming.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                });

        if (isPublic(incoming)) {
            return chain.filter(exchange.mutate().request(sanitised.build()).build());
        }

        Optional<String> token = bearerToken(incoming);
        if (token.isEmpty()) {
            return unauthorised(exchange, "Missing bearer token");
        }

        Optional<Claims> claims = jwtValidator.validate(token.get());
        if (claims.isEmpty()) {
            // Deliberately one message for expired, forged and malformed alike.
            return unauthorised(exchange, "Invalid or expired token");
        }

        Claims verified = claims.get();
        ServerHttpRequest authenticated = sanitised
                .header(USER_ID_HEADER, verified.getSubject())
                .header(USER_ROLE_HEADER, verified.get("role", String.class))
                .build();

        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        for (PublicRoute route : PUBLIC_ROUTES) {
            // Exact path match, not startsWith: POST /api/users creates an account and is
            // public, but that must not open up POST /api/users/anything-else.
            if (route.method().equals(request.getMethod()) && route.path().equals(path)) {
                return true;
            }
        }

        return false;
    }

    private Optional<String> bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    /**
     * Writes the same uniform error body the services use (FR-SYS-05), so a client parses
     * one error shape whether the failure happened at the edge or behind it.
     */
    private Mono<Void> unauthorised(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        log.debug("Rejected unauthenticated request to {}: {}", path, message);

        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized",\
                "message":"%s","path":"%s"}"""
                .formatted(Instant.now(), message, path);

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -50;
    }

    private record PublicRoute(HttpMethod method, String path) {
    }
}
