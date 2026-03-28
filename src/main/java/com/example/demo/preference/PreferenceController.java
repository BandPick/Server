package com.example.demo.preference;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import com.example.demo.song.Song;
import com.example.demo.song.SongService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;
    private final MemberService memberService;
    private final SongService songService;

    public PreferenceController(PreferenceService preferenceService,
                                MemberService memberService,
                                SongService songService) {
        this.preferenceService = preferenceService;
        this.memberService = memberService;
        this.songService = songService;
    }

    @PostMapping
    public ResponseEntity<?> createPreference(@RequestBody Preference preference) {
        Member member = memberService.getMemberById(preference.getParticipantNumber());
        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        Song song = songService.getSongById(preference.getSongId());
        if (song == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 songId입니다.");
        }

        Preference createdPreference = preferenceService.createPreference(preference);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPreference);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<Preference>> getPreferencesByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok(preferenceService.getPreferencesByMemberId(memberId));
    }

    @PutMapping("/{preferenceId}")
    public ResponseEntity<?> updatePreference(@PathVariable String preferenceId,
                                              @RequestBody Preference updatedPreference) {
        Member member = memberService.getMemberById(updatedPreference.getParticipantNumber());
        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 memberId입니다.");
        }

        Song song = songService.getSongById(updatedPreference.getSongId());
        if (song == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 songId입니다.");
        }

        Preference preference = preferenceService.updatePreference(preferenceId, updatedPreference);

        if (preference == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(preference);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<PreferenceSummary>> getPreferenceSummary() {
        return ResponseEntity.ok(preferenceService.getPreferenceSummary());
    }
}