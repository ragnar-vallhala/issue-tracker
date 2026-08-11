package com.its.user.dto.response;

import com.its.user.entity.User;

/**
 * Sign-up confirmation (FR-USR-01).
 *
 * <p>The message text is fixed by the case study, which specifies the confirmation
 * "Your account is created successfully" with a hyperlink to login. The link belongs to
 * the web tier; the API supplies the message.
 */
public record SignUpResponse(UserResponse user, String message) {

    private static final String CONFIRMATION = "Your account is created successfully";

    public static SignUpResponse from(User user) {
        return new SignUpResponse(UserResponse.from(user), CONFIRMATION);
    }
}
