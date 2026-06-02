package com.example.demo.teammember;

import com.example.demo.teammember.dto.TeamMemberRequest;
import com.example.demo.teammember.dto.TeamMemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team-members")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService) {
        this.teamMemberService = teamMemberService;
    }

    @PostMapping
    public ResponseEntity<?> createTeamMember(
            @RequestBody TeamMemberRequest request) {

        TeamMember created =
                teamMemberService.createTeamMember(request.toEntity());

        if (created == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 member입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamMemberResponse.from(created));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<List<TeamMemberResponse>>
    getTeamMembersByTeamId(@PathVariable Integer teamId) {

        return ResponseEntity.ok(
                teamMemberService.getTeamMembersByTeamId(teamId).stream()
                        .map(TeamMemberResponse::from)
                        .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeamMember(
            @PathVariable Integer id,
            @RequestBody TeamMemberRequest request) {

        TeamMember updated =
                teamMemberService.updateTeamMember(
                        id,
                        request.toEntity()
                );

        if (updated == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("수정 실패");
        }

        return ResponseEntity.ok(TeamMemberResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTeamMember(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                teamMemberService.deleteTeamMember(id)
        );
    }
}
