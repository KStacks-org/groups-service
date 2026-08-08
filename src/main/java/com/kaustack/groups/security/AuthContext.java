package com.kaustack.groups.security;

import java.util.UUID;

public record AuthContext(
        UUID userId,
        String userName,
        String userEmail,
        String userGender  // "MALE", "FEMALE", or "UNKNOWN"
) {}
