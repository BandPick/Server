package com.example.demo.memberform.dto;

public record MemberFormSaveResponse(
        int savedPickCount,
        int savedAvailabilityCount,
        String message
) {
}
