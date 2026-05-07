package com.example.demo.preference;

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
    public ResponseEntity<?> createPreference(@RequestBody Preference preference) {
        Preference createdPreference = preferenceService.createPreference(preference);

        if (createdPreference == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPreference);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<Preference>> getPreferencesByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok(preferenceService.getPreferencesByMemberId(memberId));
    }

    @PutMapping("/{preferenceId}")
    public ResponseEntity<?> updatePreference(@PathVariable String preferenceId,
                                              @RequestBody Preference updatedPreference) {
        Preference preference = preferenceService.updatePreference(preferenceId, updatedPreference);

        if (preference == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 preferenceId 또는 memberId입니다.");
        }

        return ResponseEntity.ok(preference);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<PreferenceSummary>> getPreferenceSummary() {
        return ResponseEntity.ok(preferenceService.getPreferenceSummary());
    }
}