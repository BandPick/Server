package com.example.demo.team;

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
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(team));
    }

    @GetMapping
    public ResponseEntity<List<Team>> getTeams() {
        return ResponseEntity.ok(teamService.getTeams());
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeamById(@PathVariable String teamId) {

        Team team = teamService.getTeamById(teamId);

        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 팀을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(team);
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<?> updateTeam(@PathVariable String teamId,
                                        @RequestBody Team updatedTeam) {

        Team team = teamService.updateTeam(teamId, updatedTeam);

        if (team == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("해당 팀을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(team);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<String> deleteTeam(@PathVariable String teamId) {

        return ResponseEntity.ok(
                teamService.deleteTeam(teamId)
        );
    }
}