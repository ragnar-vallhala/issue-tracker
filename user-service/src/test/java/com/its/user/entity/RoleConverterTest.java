package com.its.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The role encoding is the single highest-consequence constant in this codebase: getting
 * it backwards inverts every permission check while leaving the system apparently
 * working. These tests pin it to the reference workbook (SRS A-04).
 */
class RoleConverterTest {

    private final RoleConverter converter = new RoleConverter();

    @Test
    @DisplayName("Project Owner is stored as 0, per the reference workbook")
    void projectOwnerIsZero() {
        assertThat(converter.convertToDatabaseColumn(Role.PROJECT_OWNER)).isZero();
        assertThat(converter.convertToEntityAttribute(0)).isEqualTo(Role.PROJECT_OWNER);
    }

    @Test
    @DisplayName("Assignee is stored as 1, per the reference workbook")
    void assigneeIsOne() {
        assertThat(converter.convertToDatabaseColumn(Role.ASSIGNEE)).isEqualTo(1);
        assertThat(converter.convertToEntityAttribute(1)).isEqualTo(Role.ASSIGNEE);
    }

    @Test
    @DisplayName("Round-trips every role")
    void roundTrips() {
        for (Role role : Role.values()) {
            Integer code = converter.convertToDatabaseColumn(role);
            assertThat(converter.convertToEntityAttribute(code)).isEqualTo(role);
        }
    }

    @Test
    @DisplayName("Nulls pass through rather than defaulting to a role")
    void handlesNulls() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("An unknown code fails loudly instead of silently picking a role")
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute(7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown role code: 7");
    }
}
