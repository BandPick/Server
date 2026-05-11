package com.example.demo.preference;

import com.example.demo.preference.dto.PreferenceRequest;
import com.example.demo.preference.dto.PreferenceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @PostMapping
    public ResponseEntity<?> createPreference(@RequestBody PreferenceRequest request) {
        Preference createdPreference = preferenceService.createPreference(request.toEntity());

        if (createdPreference == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(PreferenceResponse.from(createdPreference));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<PreferenceResponse>> getPreferencesByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok(
                preferenceService.getPreferencesByMemberId(memberId).stream()
                        .map(PreferenceResponse::from)
                        .toList()
        );
    }

    @PutMapping("/{preferenceId}")
    public ResponseEntity<?> updatePreference(@PathVariable String preferenceId,
                                              @RequestBody PreferenceRequest request) {
        Preference preference = preferenceService.updatePreference(preferenceId, request.toEntity());

        if (preference == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 preferenceId 또는 memberId입니다.");
        }

        return ResponseEntity.ok(PreferenceResponse.from(preference));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<PreferenceSummary>> getPreferenceSummary() {
        return ResponseEntity.ok(preferenceService.getPreferenceSummary());
    }
}
