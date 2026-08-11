package com.its.issue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                        """));
    }
}
