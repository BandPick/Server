package com.example.demo.preference;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import com.example.demo.preference.dto.PreferenceRequest;
import com.example.demo.preference.dto.PreferenceResponse;
import com.example.demo.setlist.SetlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;
    private final MemberService memberService;
    private final SetlistRepository setlistRepository;

    public PreferenceController(PreferenceService preferenceService,
                                MemberService memberService,
                                SetlistRepository setlistRepository) {
        this.preferenceService = preferenceService;
        this.memberService = memberService;
        this.setlistRepository = setlistRepository;
    }

    @PostMapping
    public ResponseEntity<?> createPreference(@RequestBody PreferenceRequest request) {
        Preference preference = request.toEntity();
        Member member = memberService.getMemberById(preference.getParticipantNumber());
        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        if (!existsSetlist(preference.getSetlistId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 setlistId(songId)입니다.");
        }

        Preference createdPreference = preferenceService.createPreference(preference);
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
        Preference updatedPreference = request.toEntity();
        Member member = memberService.getMemberById(updatedPreference.getParticipantNumber());
        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        if (!existsSetlist(updatedPreference.getSetlistId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 setlistId(songId)입니다.");
        }

        Preference preference = preferenceService.updatePreference(preferenceId, updatedPreference);

        if (preference == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(PreferenceResponse.from(preference));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<PreferenceSummary>> getPreferenceSummary() {
        return ResponseEntity.ok(preferenceService.getPreferenceSummary());
    }

    private boolean existsSetlist(Integer songId) {
        if (songId == null) {
            return false;
        }
        return setlistRepository.existsById(songId.longValue());
    }
}
