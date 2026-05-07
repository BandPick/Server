package com.example.demo.teammember;

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
            @RequestBody TeamMember teamMember) {

        TeamMember created =
                teamMemberService.createTeamMember(teamMember);

        if (created == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("존재하지 않는 member입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<List<TeamMember>>
    getTeamMembersByTeamId(@PathVariable Integer teamId) {

        return ResponseEntity.ok(
                teamMemberService.getTeamMembersByTeamId(teamId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeamMember(
            @PathVariable Integer id,
            @RequestBody TeamMember updatedTeamMember) {

        TeamMember updated =
                teamMemberService.updateTeamMember(
                        id,
                        updatedTeamMember
                );

        if (updated == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("수정 실패");
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTeamMember(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                teamMemberService.deleteTeamMember(id)
        );
    }
}