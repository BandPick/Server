package com.example.demo.teamform.vo;

import java.time.LocalTime;

public record TeamFormScheduleRow(
        int teamFormId,
        String dayOfWeek,
        LocalTime startTime
) {
}
