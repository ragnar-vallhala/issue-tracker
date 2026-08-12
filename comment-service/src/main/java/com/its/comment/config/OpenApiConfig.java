package com.its.comment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI documentation for this service (FR-SYS-04, Milestone 6). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI commentServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ITS - Comment Service")
                .version("1.0.0")
                .description("""
                        Comment threads on issues.

                        Not specified by either source document - the case study lists a
                        Comment Service in its microservices breakdown and refers to a
                        section 7.4 that does not exist. This contract is the project's own
                        (SRS A-01).

                        Paste the token from POST /api/users/login into Authorize before calling
                        anything here.
                        """))
                .components(new Components().addSecuritySchemes("bearerAuth", BEARER_AUTH))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * Declared so Swagger UI's Authorize button attaches the JWT to every Try it out call,
     * which is what makes the UI usable as the system's API client. The gateway is what
     * actually enforces this; a service reached on its own port does not.
     */
    static final SecurityScheme BEARER_AUTH = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT issued by POST /api/users/login. Paste the raw token, without a Bearer prefix.");
}
