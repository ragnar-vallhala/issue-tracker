package com.its.issue.config;

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
    public OpenAPI issueServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ITS - Issue Service")
                .version("1.0.0")
                .description("""
                        Issue lifecycle, assignment and workflow for the Issue Tracking System.

                        Enum values: status TO_DO/IN_PROGRESS/IN_REVIEW/DONE, priority
                        LOW/MEDIUM/HIGH/CRITICAL, type BUG/TASK/STORY/EPIC. Only TO_DO, HIGH
                        and BUG are attested by the reference workbook (SRS A-11).

                        This service owns every issue query; the User and Project services'
                        issue endpoints delegate here.

                        Paste the token from POST /api/users/login into Authorize. Reached
                        through the gateway the token also decides who may change a status;
                        called directly on :8083 that check has no caller to apply.
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
