package com.example.demo.teamform;

import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamSystemMatchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/team-forms")
public class TeamFormsController {

    private final TeamFormService teamFormService;
    private final TeamSystemMatchService teamSystemMatchService;

    public TeamFormsController(
            TeamFormService teamFormService,
            TeamSystemMatchService teamSystemMatchService
    ) {
        this.teamFormService = teamFormService;
        this.teamSystemMatchService = teamSystemMatchService;
    }

    @GetMapping
    public List<TeamFormMemberResponse> listAll() {
        return teamFormService.listAll();
    }

    @PostMapping("/match")
    public TeamSystemMatchResponse match() {
        return teamSystemMatchService.match();
    }
}
