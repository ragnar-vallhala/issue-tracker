package com.its.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry for the Issue Tracking System (Milestone 4).
 *
 * <p>Every microservice registers here at start-up, and the API Gateway resolves
 * {@code lb://service-name} URIs against this registry to load-balance client-side.
 * The dashboard at http://localhost:8761 is the acceptance check for FR-SYS-01.
 *
 * <p>This application registers with nothing and fetches nothing - it is the registry
 * itself, so the corresponding client flags are disabled in application.yml.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
