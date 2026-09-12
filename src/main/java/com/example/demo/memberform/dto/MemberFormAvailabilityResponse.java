package com.example.demo.memberform.dto;

public record MemberFormAvailabilityResponse(
        String availableFrom,
        String availableTo
) {
}