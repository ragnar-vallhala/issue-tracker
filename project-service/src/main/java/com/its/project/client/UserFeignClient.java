package com.its.project.client;

import com.its.project.dto.response.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative client for the User Service.
 *
 * <p>The name is the Eureka registration, not a host: Spring Cloud LoadBalancer resolves
 * {@code user-service} to a live instance at call time, so nothing here breaks when the
 * service moves or is scaled out.
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/api/users/{userId}")
    UserSummary findById(@PathVariable("userId") Integer userId);
}
