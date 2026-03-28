package com.example.demo.schedule;

import com.example.demo.scheduler.SchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final SchedulerService schedulerService;

    public ScheduleController(ScheduleService scheduleService, SchedulerService schedulerService) {
        this.scheduleService = scheduleService;
        this.schedulerService = schedulerService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<Schedule>> generateSchedules() {
        return ResponseEntity.ok(schedulerService.generateSchedules());
    }

    @GetMapping
    public ResponseEntity<List<Schedule>> getSchedules() {
        return ResponseEntity.ok(scheduleService.getSchedules());
    }

    @PatchMapping("/{scheduleId}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable String scheduleId,
                                                   @RequestBody ScheduleUpdateRequest request) {
        Schedule schedule = scheduleService.updateSchedule(scheduleId, request);

        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(schedule);
    }
}