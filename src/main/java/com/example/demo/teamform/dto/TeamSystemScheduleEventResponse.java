package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemScheduleEventResponse(
        Integer id,
        Integer teamId,
        String title,
        String day,
        String startTime,
        String endTime,
        List<String> members,
        String note
) {
}
