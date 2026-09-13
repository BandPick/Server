package com.example.demo.teamform;

import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamFormSaveRequest;
import com.example.demo.teamform.dto.TeamFormSaveResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class TeamFormController {

    private static final Logger log = LoggerFactory.getLogger(TeamFormController.class);

    private final TeamFormService teamFormService;

    public TeamFormController(TeamFormService teamFormService) {
        this.teamFormService = teamFormService;
    }

    @GetMapping("/{userId}/team-forms")
    public ResponseEntity<TeamFormMemberResponse> getTeamForm(@PathVariable long userId) {
        return teamFormService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
        } catch (DataIntegrityViolationException ex) {
            log.warn("팀제 신청 저장 제약 위반 userId={}: {}", userId, ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("팀제 신청을 저장하지 못했습니다. 입력 값과 테이블 제약을 확인해 주세요.");
        } catch (DataAccessException ex) {
            log.error("팀제 신청 저장 실패 userId={}: {}", userId, ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("팀제 신청을 저장하지 못했습니다. 서버 상태를 확인해 주세요.");
        }
    }
}
