package com.its.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User Service - identity, credentials and roles (Milestone 1).
 *
 * <p>Owns {@code user_db} exclusively. No other service reads its tables; the user data
 * that Project and Issue need is fetched from this service over HTTP (SRS C-02).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
