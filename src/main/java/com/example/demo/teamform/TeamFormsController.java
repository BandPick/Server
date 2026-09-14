package com.example.demo.teamform;

import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamSystemAssignmentSaveRequest;
import com.example.demo.teamform.dto.TeamSystemAssignmentSaveResponse;
import com.example.demo.teamform.dto.TeamSystemMatchRequest;
import com.example.demo.teamform.dto.TeamSystemMatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/team-forms")
public class TeamFormsController {

    private static final Logger log = LoggerFactory.getLogger(TeamFormsController.class);

    private final TeamFormService teamFormService;
    private final TeamSystemMatchService teamSystemMatchService;
    private final TeamSystemAssignmentService teamSystemAssignmentService;

    public TeamFormsController(
            TeamFormService teamFormService,
            TeamSystemMatchService teamSystemMatchService,
            TeamSystemAssignmentService teamSystemAssignmentService
    ) {
        this.teamFormService = teamFormService;
        this.teamSystemMatchService = teamSystemMatchService;
        this.teamSystemAssignmentService = teamSystemAssignmentService;
    }

    @GetMapping
    public List<TeamFormMemberResponse> listAll() {
        return teamFormService.listAll();
    }

    @PostMapping("/match")
    public TeamSystemMatchResponse match(
            @RequestBody(required = false) TeamSystemMatchRequest request
    ) {
        return teamSystemMatchService.match(request);
    }

    @GetMapping("/assignments")
    public TeamSystemMatchResponse getAssignments() {
        return teamSystemAssignmentService.load();
    }

    @PostMapping("/assignments")
    public ResponseEntity<?> saveAssignments(@RequestBody TeamSystemAssignmentSaveRequest request) {
        try {
            TeamSystemAssignmentSaveResponse response = teamSystemAssignmentService.save(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            log.warn("팀제 배정 저장 제약 위반: {}", ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("팀제 배정을 저장하지 못했습니다. 포지션/유저 제약을 확인해 주세요.");
        } catch (DataAccessException ex) {
            log.error("팀제 배정 저장 실패: {}", ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("팀제 배정을 저장하지 못했습니다. 서버 상태를 확인해 주세요.");
        }
    }
}
