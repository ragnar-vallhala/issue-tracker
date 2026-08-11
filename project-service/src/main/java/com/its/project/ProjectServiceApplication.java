package com.its.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Project Service - project lifecycle and ownership (Milestone 2).
 *
 * <p>Owns {@code project_db}. Calls the User Service to validate owners and the Issue
 * Service both to list a project's issues and to cascade deletes into them.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }
}
