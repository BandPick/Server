package com.example.demo.team;

import com.example.demo.scheduler.SchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;
    private final SchedulerService schedulerService;

    public TeamController(TeamService teamService, SchedulerService schedulerService) {
        this.teamService = teamService;
        this.schedulerService = schedulerService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<Team>> generateTeams() {
        return ResponseEntity.ok(schedulerService.generateTeams());
    }

    @GetMapping
    public ResponseEntity<List<Team>> getTeams() {
        return ResponseEntity.ok(teamService.getTeams());
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<Team> getTeamById(@PathVariable String teamId) {
        Team team = teamService.getTeamById(teamId);

        if (team == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(team);
    }

    @PatchMapping("/{teamId}/members")
    public ResponseEntity<Team> updateTeamMembers(@PathVariable String teamId,
                                                  @RequestBody TeamMemberUpdateRequest request) {
        Team team = teamService.updateTeamMembers(teamId, request.getMemberIds());

        if (team == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(team);
    }
}