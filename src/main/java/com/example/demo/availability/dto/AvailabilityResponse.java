package com.example.demo.availability.dto;

import com.example.demo.availability.Availability;

import java.time.LocalDateTime;

public record AvailabilityResponse(
        Integer id,
        Long userId,
        LocalDateTime availableFrom,
        LocalDateTime availableTo
) {
    public static AvailabilityResponse from(Availability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getUserId(),
                availability.getAvailableFrom(),
                availability.getAvailableTo()
        );
    }
}
