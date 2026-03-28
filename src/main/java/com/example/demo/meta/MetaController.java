package com.example.demo.meta;

import com.example.demo.common.type.AvailabilityType;
import com.example.demo.common.type.DayOfWeekType;
import com.example.demo.common.type.Position;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/meta")
public class MetaController {

    @GetMapping("/positions")
    public ResponseEntity<List<Position>> getPositions() {
        return ResponseEntity.ok(Arrays.asList(Position.values()));
    }

    @GetMapping("/day-of-weeks")
    public ResponseEntity<List<DayOfWeekType>> getDayOfWeeks() {
        return ResponseEntity.ok(Arrays.asList(DayOfWeekType.values()));
    }

    @GetMapping("/availability-types")
    public ResponseEntity<List<AvailabilityType>> getAvailabilityTypes() {
        return ResponseEntity.ok(Arrays.asList(AvailabilityType.values()));
    }
}