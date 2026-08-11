package com.its.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.its.gateway.security.JwtValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * The gateway's authentication filter, with particular attention to header spoofing.
 *
 * <p>Every service behind the gateway trusts {@code X-User-Role} to make authorisation
 * decisions. If a client can set that header itself, every one of those decisions is
 * worthless - so the stripping behaviour is tested at least as carefully as the token
 * checking.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(new JwtValidator(SECRET));

    /** Captures the request as the filter chain would receive it. */
    private static final class CapturingChain
            implements org.springframework.cloud.gateway.filter.GatewayFilterChain {

        private final AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            captured.set(exchange);
            return Mono.empty();
        }

        HttpHeaders forwardedHeaders() {
            return captured.get().getRequest().getHeaders();
        }

        boolean wasCalled() {
            return captured.get() != null;
        }
    }

    private String tokenFor(String userId, String role, Instant expiry) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("A valid token is exchanged for identity headers")
    void injectsIdentityHeaders() {
        String token = tokenFor("101", "PROJECT_OWNER", Instant.now().plusSeconds(3600));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.forwardedHeaders().getFirst("X-User-Id")).isEqualTo("101");
        assertThat(chain.forwardedHeaders().getFirst("X-User-Role")).isEqualTo("PROJECT_OWNER");
    }

    @Test
    @DisplayName("A client-supplied X-User-Role is discarded, not forwarded")
    void stripsSpoofedRoleHeader() {
        String token = tokenFor("104", "ASSIGNEE", Instant.now().plusSeconds(3600));

        // An Assignee attempting to promote themselves by hand.
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Role", "PROJECT_OWNER")
                        .header("X-User-Id", "101"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        // The token wins; the injected values are the verified ones, not the claimed ones.
        assertThat(chain.forwardedHeaders().getFirst("X-User-Role")).isEqualTo("ASSIGNEE");
        assertThat(chain.forwardedHeaders().getFirst("X-User-Id")).isEqualTo("104");
        assertThat(chain.forwardedHeaders().get("X-User-Role")).hasSize(1);
    }

    @Test
    @DisplayName("Identity headers are stripped on public routes too")
    void stripsSpoofedHeadersOnPublicRoutes() {
        // The most tempting attack surface: a route with no token check at all.
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/users/login")
                        .header("X-User-Role", "PROJECT_OWNER")
                        .header("X-User-Id", "101"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.forwardedHeaders().getFirst("X-User-Role")).isNull();
        assertThat(chain.forwardedHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    @DisplayName("Sign-up and login pass without a token")
    void allowsPublicRoutes() {
        for (MockServerHttpRequest.BaseBuilder<?> builder : new MockServerHttpRequest.BaseBuilder<?>[]{
                MockServerHttpRequest.post("/api/users"),
                MockServerHttpRequest.post("/api/users/login")}) {

            CapturingChain chain = new CapturingChain();
            filter.filter(MockServerWebExchange.from(
                    (MockServerHttpRequest) builder.build()), chain).block();

            assertThat(chain.wasCalled()).isTrue();
        }
    }

    @Test
    @DisplayName("GET /api/users is not public, even though POST to the same path is")
    void listingUsersRequiresAToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("The public sign-up route does not open up paths beneath it")
    void publicRouteIsAnExactMatch() {
        // startsWith matching here would make POST /api/users/anything unauthenticated.
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/users/104/promote"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Each service's OpenAPI document is reachable without a token")
    void allowsRoutedApiDocs() {
        // These sit below a service's route prefix, so a startsWith check against
        // "/v3/api-docs" misses them entirely - which is what left Swagger 401ing.
        for (String path : new String[]{
                "/api/users/v3/api-docs",
                "/api/projects/v3/api-docs",
                "/api/issues/v3/api-docs",
                "/api/comments/v3/api-docs"}) {

            CapturingChain chain = new CapturingChain();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(MockServerHttpRequest.get(path));

            filter.filter(exchange, chain).block();

            assertThat(chain.wasCalled())
                    .withFailMessage("expected %s to be public", path)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("The gateway's own Swagger UI is reachable without a token")
    void allowsGatewaySwaggerUi() {
        for (String path : new String[]{"/swagger-ui.html", "/v3/api-docs", "/webjars/x.js"}) {
            CapturingChain chain = new CapturingChain();
            filter.filter(MockServerWebExchange.from(
                    MockServerHttpRequest.get(path)), chain).block();

            assertThat(chain.wasCalled()).isTrue();
        }
    }

    @Test
    @DisplayName("A traversal sequence cannot smuggle a private path past the docs exemption")
    void rejectsTraversalDisguisedAsDocs() {
        // Without the '..' guard, a "contains" match on /v3/api-docs would exempt this.
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/issues/v3/api-docs/../../users"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Ordinary API paths are still protected")
    void docsExemptionDoesNotLeak() {
        for (String path : new String[]{
                "/api/users", "/api/issues", "/api/projects/1011", "/api/comments/issue/1"}) {

            CapturingChain chain = new CapturingChain();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(MockServerHttpRequest.get(path));

            filter.filter(exchange, chain).block();

            assertThat(chain.wasCalled())
                    .withFailMessage("expected %s to require a token", path)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("A missing token is 401")
    void rejectsMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/issues"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.wasCalled()).isFalse();
    }

    @Test
    @DisplayName("A token signed with the wrong key is 401")
    void rejectsForgedToken() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-32-bytes".getBytes(StandardCharsets.UTF_8));

        String forged = Jwts.builder()
                .subject("101")
                .claim("role", "PROJECT_OWNER")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(wrongKey)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.wasCalled()).isFalse();
    }

    @Test
    @DisplayName("An expired token is 401")
    void rejectsExpiredToken() {
        String expired = tokenFor("101", "PROJECT_OWNER", Instant.now().minusSeconds(120));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("An Authorization header that is not a bearer token is 401")
    void rejectsNonBearerScheme() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"));

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
