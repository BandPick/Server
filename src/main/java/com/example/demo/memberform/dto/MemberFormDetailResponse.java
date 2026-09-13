package com.example.demo.memberform.dto;

import java.util.List;

public record MemberFormDetailResponse(
        long userId,
        List<MemberFormPickResponse> picks,
        List<MemberFormAvailabilityResponse> availabilities
) {
}