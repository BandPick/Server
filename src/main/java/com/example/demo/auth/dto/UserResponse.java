package com.example.demo.auth.dto;

public record UserResponse(
        Long id,
        String code,
        String name
) {
}
