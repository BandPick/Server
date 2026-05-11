package com.example.demo.schedule;

import com.example.demo.schedule.dto.ScheduleRequest;
import com.example.demo.schedule.dto.ScheduleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestBody ScheduleRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduleResponse.from(scheduleService.createSchedule(request.toEntity())));
    }

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules() {

        return ResponseEntity.ok(
                scheduleService.getSchedules().stream()
                        .map(ScheduleResponse::from)
                        .toList()
        );
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<ScheduleResponse>>
    getSchedulesByTeamId(@PathVariable Integer teamId) {

        return ResponseEntity.ok(
                scheduleService.getSchedulesByTeamId(teamId).stream()
                        .map(ScheduleResponse::from)
                        .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable Integer id,
            @RequestBody ScheduleRequest request) {

        Schedule schedule =
                scheduleService.updateSchedule(id, request.toEntity());

        if (schedule == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("수정 실패");
        }

        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchedule(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                scheduleService.deleteSchedule(id)
        );
    }
}
