package com.example.demo.algorithm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/algorithm")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    // 팀 배정(Step 1)만 수행하여 반환
    @PostMapping("/run")
    public ResponseEntity<Algorithm.AssignmentState> run() {
        Algorithm.AssignmentState result = algorithmService.run();
        return ResponseEntity.ok(result);
    }

    // 팀 배정(Step 1) + 합주 스케줄 생성(Step 2)
    @PostMapping("/run/full")
    public ResponseEntity<Map<String, Object>> runFull() {
        Algorithm.AssignmentState state = algorithmService.run();
        List<PracticeSchedule> schedules = algorithmService.runStep2(state);

        Map<String, Object> result = new HashMap<>();
        result.put("assignment", state);
        result.put("schedules", schedules);

        return ResponseEntity.ok(result);
    }
}