package com.its.comment;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * Comment Service - comment threads on issues.
 *
 * <p>Owns {@code comment_db}. Calls nothing: it is the leaf of the dependency graph and
 * the deepest step of the delete cascade. There is no {@code @EnableFeignClients} here,
 * and that absence is the design (DESIGN 6.1).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }

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
                        """));
    }
}
