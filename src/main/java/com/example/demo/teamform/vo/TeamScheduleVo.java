package com.example.demo.teamform.vo;

import java.time.LocalTime;

public record TeamScheduleVo(
        String dayOfWeek,
        LocalTime startTime
) {
}
