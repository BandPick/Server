package com.example.demo.team;

import com.example.demo.team.dto.TeamRequest;
import com.example.demo.team.dto.TeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamResponse.from(teamService.createTeam(request.toEntity())));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getTeams() {
        return ResponseEntity.ok(
                teamService.getTeams().stream()
                        .map(TeamResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeamById(@PathVariable String teamId) {

        Team team = teamService.getTeamById(teamId);

        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 팀을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(TeamResponse.from(team));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<?> updateTeam(@PathVariable String teamId,
                                        @RequestBody TeamRequest request) {

        Team team = teamService.updateTeam(teamId, request.toEntity());

        if (team == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("해당 팀을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(TeamResponse.from(team));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<String> deleteTeam(@PathVariable String teamId) {

        return ResponseEntity.ok(
                teamService.deleteTeam(teamId)
        );
    }
}
