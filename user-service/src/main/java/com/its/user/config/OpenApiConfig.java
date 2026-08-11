package com.its.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI documentation for this service (FR-SYS-04, Milestone 6). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ITS - User Service")
                .version("1.0.0")
                .description("""
                        Identity, credentials and roles for the Issue Tracking System.

                        Role encoding is 0 = PROJECT_OWNER, 1 = ASSIGNEE (SRS A-04); the API
                        always uses the enum name. Two endpoints are inter-service calls that
                        delegate to the Issue Service and will answer 503 if it is unreachable.
                        """)
                );
    }
}
