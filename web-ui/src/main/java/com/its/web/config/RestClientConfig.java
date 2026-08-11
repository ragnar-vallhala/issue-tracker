package com.its.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.its.web.client.ApiException;
import com.its.web.session.SessionAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * The HTTP client this tier uses to reach the gateway.
 *
 * <p>Two pieces of behaviour are wired in here so that no calling code has to remember
 * them: the bearer token is attached automatically, and every non-2xx response becomes an
 * {@link ApiException} carrying the parsed error body.
 */
@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Bean
    public RestTemplate gatewayRestTemplate(RestTemplateBuilder builder,
                                            SessionAccessor sessionAccessor,
                                            ObjectMapper objectMapper) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalInterceptors((request, body, execution) -> {
                    // The token relay (DESIGN 8.4). Reading it from the session here is
                    // what keeps it out of every method signature - and out of the browser.
                    sessionAccessor.current().ifPresent(user ->
                            request.getHeaders().setBearerAuth(user.token()));

                    return execution.execute(request, body);
                })
                .errorHandler(new ApiErrorHandler(objectMapper))
                .build();
    }

    /**
     * Turns an error response into an {@link ApiException} with the body parsed.
     *
     * <p>Without this the default handler throws {@code HttpClientErrorException} with the
     * body as an unparsed string, and every controller wanting to show "email already
     * registered" against the right field would have to deserialise it by hand.
     */
    private static final class ApiErrorHandler extends DefaultResponseErrorHandler {

        private final ObjectMapper objectMapper;

        private ApiErrorHandler(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            HttpStatusCode status = response.getStatusCode();
            ApiException.ApiError error = null;

            try (InputStream stream = response.getBody()) {
                String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                if (!body.isBlank()) {
                    error = objectMapper.readValue(body, ApiException.ApiError.class);
                }
            } catch (Exception ex) {
                // A body that is not our error shape - an infrastructure 502, say.
                // The status alone is still enough to render a sensible page.
                log.debug("Could not parse error body from API: {}", ex.getMessage());
            }

            throw new ApiException(status, error);
        }

        @Override
        protected boolean hasError(HttpStatusCode statusCode) {
            return statusCode.isError();
        }
    }

    /** Convenience for building gateway URLs without repeating the base in every call. */
    @Bean
    public HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }
}
