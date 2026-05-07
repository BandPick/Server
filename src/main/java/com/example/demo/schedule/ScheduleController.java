package com.example.demo.schedule;

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
    public ResponseEntity<Schedule> createSchedule(
            @RequestBody Schedule schedule) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createSchedule(schedule));
    }

    @GetMapping
    public ResponseEntity<List<Schedule>> getSchedules() {

        return ResponseEntity.ok(
                scheduleService.getSchedules()
        );
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Schedule>>
    getSchedulesByTeamId(@PathVariable Integer teamId) {

        return ResponseEntity.ok(
                scheduleService.getSchedulesByTeamId(teamId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable Integer id,
            @RequestBody Schedule updatedSchedule) {

        Schedule schedule =
                scheduleService.updateSchedule(id, updatedSchedule);

        if (schedule == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("수정 실패");
        }

        return ResponseEntity.ok(schedule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchedule(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                scheduleService.deleteSchedule(id)
        );
    }
}