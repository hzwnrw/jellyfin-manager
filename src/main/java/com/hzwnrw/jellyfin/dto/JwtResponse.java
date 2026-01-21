package com.hzwnrw.jellyfin.dto;

/**
 * A production-grade immutable response object for JWT authentication.
 */
public record JwtResponse(
        String token,
        String type,
        String username // Optional: helpful for the frontend to know who logged in
) {
    // Standard constructor for the two fields you used
    public JwtResponse(String token, String type) {
        this(token, type, null);
    }
}