package com.example.demo.availability.dto;

import com.example.demo.availability.Availability;

import java.time.LocalDateTime;

public record AvailabilityRequest(
        Long userId,
        LocalDateTime availableFrom,
        LocalDateTime availableTo,
        String participantNumber
) {
    public Availability toEntity() {
        Availability availability = new Availability();
        availability.setUserId(userId);
        availability.setAvailableFrom(availableFrom);
        availability.setAvailableTo(availableTo);
        availability.setParticipantNumber(participantNumber);
        return availability;
    }
}
