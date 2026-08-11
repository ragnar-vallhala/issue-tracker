package com.its.issue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Issue Service - issue lifecycle, assignment and workflow (Milestone 3).
 *
 * <p>Owns {@code issue_db} and is the sole owner of issue queries: the User and Project
 * services' issue endpoints are facades that delegate here. It calls out to User and
 * Project only to validate references on write, and to Comment for counts and cascade
 * deletes - so the read-direction call graph stays acyclic (DESIGN 6.1).
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class IssueServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IssueServiceApplication.class, args);
    }
}
