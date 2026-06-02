package com.example.demo.availability;

import com.example.demo.availability.dto.AvailabilityRequest;
import com.example.demo.availability.dto.AvailabilityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public ResponseEntity<?> createAvailability(@RequestBody AvailabilityRequest request) {

        Availability createdAvailability = availabilityService.createAvailability(request.toEntity());

        if (createdAvailability == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(AvailabilityResponse.from(createdAvailability));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<AvailabilityResponse>> getAvailabilitiesByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok(
                availabilityService.getAvailabilitiesByMemberId(memberId).stream()
                        .map(AvailabilityResponse::from)
                        .toList()
        );
    }

    @PutMapping("/{availabilityId}")
    public ResponseEntity<?> updateAvailability(@PathVariable String availabilityId,
                                                @RequestBody AvailabilityRequest request) {

        Availability availability = availabilityService.updateAvailability(availabilityId, request.toEntity());

        if (availability == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 availabilityId 또는 memberId입니다.");
        }

        return ResponseEntity.ok(AvailabilityResponse.from(availability));
    }
}
