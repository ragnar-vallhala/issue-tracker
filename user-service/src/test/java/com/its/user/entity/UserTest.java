package com.its.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("Username is the local part of the email (SRS A-18)")
    void derivesUsernameFromEmail() {
        User user = new User("Sam Lee", "sam.lee@example.com", "hash", null, Role.ASSIGNEE);
        assertThat(user.getUsername()).isEqualTo("sam.lee");
    }

    @Test
    @DisplayName("An address with no @ degrades to the whole string rather than throwing")
    void handlesMalformedEmail() {
        User user = new User("Odd", "not-an-email", "hash", null, Role.ASSIGNEE);
        assertThat(user.getUsername()).isEqualTo("not-an-email");
    }

    @Test
    @DisplayName("Only the first @ splits the address")
    void splitsOnFirstAt() {
        User user = new User("Quoted", "a@b@example.com", "hash", null, Role.ASSIGNEE);
        assertThat(user.getUsername()).isEqualTo("a");
    }
}
