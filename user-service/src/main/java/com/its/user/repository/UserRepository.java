package com.its.user.repository;

import com.its.user.entity.Role;
import com.its.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    /**
     * Resolves a username to a user (SRS A-18).
     *
     * <p>Username is the email's local part, so this matches on the portion before the
     * '@'. Anchoring the pattern with the '@' matters: a bare {@code LIKE 'sam.lee%'}
     * would also match {@code sam.leeson@...}.
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE CONCAT(:username, '@%')")
    Optional<User> findByUsername(@Param("username") String username);
}
