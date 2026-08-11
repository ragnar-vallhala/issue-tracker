package com.its.issue.client;

import com.its.issue.dto.response.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Declarative client for the User Service, used to validate assignees and creators. */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/api/users/{userId}")
    UserSummary findById(@PathVariable("userId") Integer userId);
}
