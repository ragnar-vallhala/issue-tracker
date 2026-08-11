package com.its.project.client;

import com.its.project.dto.response.UserSummary;
import com.its.project.exception.ProjectExceptions.ServiceUnavailableException;
import feign.FeignException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wraps {@link UserFeignClient} to turn transport-level outcomes into domain ones.
 *
 * <p>The distinction this class exists to draw: a 404 from the User Service means the
 * user genuinely is not there, which makes the caller's request invalid (400). Anything
 * else - a connection refused, a timeout, a 500 - means we do not know, which is a 503.
 * Collapsing those two into "user not found" would report a confident falsehood during
 * an outage.
 */
@Component
public class UserGateway {

    private static final Logger log = LoggerFactory.getLogger(UserGateway.class);

    private final UserFeignClient userFeignClient;

    public UserGateway(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    /**
     * @return the user, or empty if the User Service is certain there is no such user
     * @throws ServiceUnavailableException if the User Service could not answer
     */
    public Optional<UserSummary> findById(Integer userId) {
        try {
            return Optional.ofNullable(userFeignClient.findById(userId));

        } catch (FeignException.NotFound ex) {
            log.debug("User Service reports no user {}", userId);
            return Optional.empty();

        } catch (FeignException ex) {
            log.warn("User Service call failed for user {}: {}", userId, ex.getMessage());
            throw new ServiceUnavailableException("User Service", ex);
        }
    }
}
