package com.its.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway - the single entry point for all API traffic (Milestone 7).
 *
 * <p>Three jobs: route by path to the right service, resolve and load-balance instances
 * through Eureka, and verify the JWT once at the edge so that the services behind it can
 * trust an identity header instead of each re-implementing token handling.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
