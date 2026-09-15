package com.example.demo.teamform.dto;

import java.time.LocalDate;
import java.util.List;

public record TeamSystemScheduleBoardSaveRequest(
        LocalDate weekStartDate,
        List<TeamSystemScheduleBoardSaveEventRequest> events
) {
}
