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
        AlgorithmService.RunResult runResult = algorithmService.run();
        return ResponseEntity.ok(runResult.state);
    }

    // 팀 배정(Step 1) + 합주 스케줄 생성(Step 2)
    @PostMapping("/run/full")
    public ResponseEntity<Map<String, Object>> runFull() {
        AlgorithmService.RunResult runResult = algorithmService.run();
        List<PracticeSchedule> schedules = algorithmService.runStep2(runResult.state);
        algorithmService.replaceGeneratedResults(runResult.state, schedules, runResult.songIdToName);

        Map<String, Object> result = new HashMap<>();
        result.put("assignment", runResult.state);
        result.put("schedules", schedules);

        return ResponseEntity.ok(result);
    }
}
