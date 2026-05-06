package com.example.demo.memberform.vo;

import java.time.LocalDateTime;

public record AvailabilityVo(
        LocalDateTime availableFrom,
        LocalDateTime availableTo
) {
}
