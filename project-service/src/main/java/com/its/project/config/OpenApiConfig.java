package com.its.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI documentation for this service (FR-SYS-04, Milestone 6). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ITS - Project Service")
                .version("1.0.0")
                .description("""
                        Project lifecycle and ownership for the Issue Tracking System.

                        DELETE /api/projects/{id} cascades to the project's issues and their
                        comments. The cascade is ordered children-first and is not atomic; a
                        failed step leaves the project intact so the call can be retried.
                        """));
    }
}
