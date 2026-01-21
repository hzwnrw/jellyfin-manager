package com.hzwnrw.jellyfin.dto;

public record JwtResponse(
        String token,
        String type,
        String username
)   
    {
    public JwtResponse(String token, String type) {
        this(token, type, null);
    }
}