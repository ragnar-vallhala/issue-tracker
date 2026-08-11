package com.its.user.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * BCrypt at strength 10 (FR-USR-03).
     *
     * <p>Strength is the work factor: each increment doubles hashing time. Ten is the
     * Spring default and a reasonable balance - high enough to make offline cracking
     * expensive, low enough that login stays inside the latency budget of NFR-01.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * The load-balanced RestTemplate used to reach the Issue Service.
     *
     * <p>RestTemplate specifically, rather than Feign: the case study's architecture
     * diagram labels this one edge "RestTemplate" and the Project/Issue edges "Feign
     * Client", and both mechanisms are worth having in the codebase. See DESIGN 6.1.
     *
     * <p>{@code @LoadBalanced} is what lets the URI be {@code http://issue-service/...} -
     * Spring Cloud LoadBalancer resolves that service name against Eureka and picks an
     * instance client-side.
     *
     * <p>The timeouts are not optional. A RestTemplate built without them waits forever
     * on a stalled downstream, and one hung dependency will exhaust this service's
     * request threads and take it down too (DESIGN 6.5).
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
