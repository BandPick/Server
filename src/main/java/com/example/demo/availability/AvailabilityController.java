package com.example.demo.availability;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final MemberService memberService;

    public AvailabilityController(AvailabilityService availabilityService, MemberService memberService) {
        this.availabilityService = availabilityService;
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<?> createAvailability(@RequestBody Availability availability) {

        Availability createdAvailability = availabilityService.createAvailability(availability);

        if (createdAvailability == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdAvailability);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<Availability>> getAvailabilitiesByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok(availabilityService.getAvailabilitiesByMemberId(memberId));
    }

    @PutMapping("/{availabilityId}")
    public ResponseEntity<?> updateAvailability(@PathVariable String availabilityId,
                                                @RequestBody Availability updatedAvailability) {

        Availability availability = availabilityService.updateAvailability(availabilityId, updatedAvailability);

        if (availability == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 availabilityId 또는 memberId입니다.");
        }

        return ResponseEntity.ok(availability);
    }
}