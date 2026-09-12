package com.example.demo.teamform;

import com.example.demo.teamform.dto.TeamFormSaveRequest;
import com.example.demo.teamform.dto.TeamFormSaveResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class TeamFormController {

    private final TeamFormService teamFormService;

    public TeamFormController(TeamFormService teamFormService) {
        this.teamFormService = teamFormService;
    }

    @PostMapping("/{userId}/team-forms")
    public ResponseEntity<?> saveTeamForm(
            @PathVariable long userId,
            @Valid @RequestBody TeamFormSaveRequest request
    ) {
        try {
            TeamFormSaveResponse response = teamFormService.save(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
