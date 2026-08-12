package com.its.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

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
}
