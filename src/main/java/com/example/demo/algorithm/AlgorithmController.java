package com.example.demo.algorithm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/algorithm")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @PostMapping("/run")
    public ResponseEntity<Algorithm.AssignmentState> run() {
        Algorithm.AssignmentState result = algorithmService.run();
        return ResponseEntity.ok(result);
    }
}