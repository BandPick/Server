package com.example.demo.teamform;

import com.example.demo.auth.dao.UserDao;
import com.example.demo.auth.entity.UserEntity;
import com.example.demo.team.Team;
import com.example.demo.team.TeamRepository;
import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamFormPositionResponse;
import com.example.demo.teamform.dto.TeamSystemAssignmentSaveRequest;
import com.example.demo.teamform.dto.TeamSystemAssignmentSaveResponse;
import com.example.demo.teamform.dto.TeamSystemAssignmentSlotRequest;
import com.example.demo.teamform.dto.TeamSystemAssignmentTeamRequest;
import com.example.demo.teamform.dto.TeamSystemMatchResponse;
import com.example.demo.teamform.dto.TeamSystemTeamMemberResponse;
import com.example.demo.teamform.dto.TeamSystemTeamResponse;
import com.example.demo.teamform.dto.TeamSystemUnmatchedResponse;
import com.example.demo.teammember.TeamMember;
import com.example.demo.teammember.TeamMemberRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TeamSystemAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TeamSystemAssignmentService.class);
    private static final Set<String> ALLOWED_POSITIONS =
            Set.of("V", "D", "B", "EG1", "EG2", "AG", "K");

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamFormService teamFormService;
    private final UserDao userDao;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeamSystemAssignmentService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamFormService teamFormService,
            UserDao userDao,
            JdbcTemplate jdbcTemplate
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamFormService = teamFormService;
        this.userDao = userDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTeamMetaColumns() {
        try {
            jdbcTemplate.execute(
                    """
                    ALTER TABLE team
                    ADD COLUMN IF NOT EXISTS confirmed boolean NOT NULL DEFAULT false
                    """
            );
        } catch (Exception ex) {
            log.warn("team.confirmed 컬럼을 준비하지 못했습니다: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute(
                    """
                    ALTER TABLE team
                    ADD COLUMN IF NOT EXISTS needed_positions text
                    """
            );
        } catch (Exception ex) {
            log.warn("team.needed_positions 컬럼을 준비하지 못했습니다: {}", ex.getMessage());
        }
    }

    @Transactional
    public TeamSystemAssignmentSaveResponse save(TeamSystemAssignmentSaveRequest request) {
        if (request == null || request.teams() == null) {
            throw new IllegalArgumentException("저장할 팀 배정 데이터가 없습니다.");
        }

        teamRepository.deleteByTeamType(Team.TYPE_TEAM_SYSTEM);

        int memberCount = 0;
        for (TeamSystemAssignmentTeamRequest teamRequest : request.teams()) {
            String teamName = normalizeTeamName(teamRequest == null ? null : teamRequest.name());
            List<UserPosition> assignments = collectAssignments(teamName, teamRequest);
            Map<String, Boolean> neededByPosition = collectNeededByPosition(teamRequest);

            Team team = new Team();
            team.setTeamType(Team.TYPE_TEAM_SYSTEM);
            team.setName(teamName);
            team.setSetlistId(null);
            team.setConfirmed(teamRequest != null && Boolean.TRUE.equals(teamRequest.confirmed()));
            team.setNeededPositions(writeNeededPositions(neededByPosition));
            Team savedTeam = teamRepository.save(team);

            for (UserPosition assignment : assignments) {
                Long userId = assignment.userId();
                if (!userDao.findById(userId).isPresent()) {
                    throw new IllegalArgumentException("존재하지 않는 부원입니다. userId=" + userId);
                }

                TeamMember member = new TeamMember();
                member.setTeamId(savedTeam.getId());
                member.setUserId(userId);
                member.setSessionPosition(toDbPosition(assignment.position()));
                teamMemberRepository.save(member);
                memberCount += 1;
            }
        }

        return new TeamSystemAssignmentSaveResponse(
                request.teams().size(),
                memberCount,
                "팀제 배정을 저장했습니다."
        );
    }

    @Transactional(readOnly = true)
    public TeamSystemMatchResponse load() {
        List<Team> teams = teamRepository.findByTeamTypeOrderByNameAscIdAsc(Team.TYPE_TEAM_SYSTEM);
        Map<Long, TeamFormMemberResponse> formsByUserId = new HashMap<>();
        for (TeamFormMemberResponse form : teamFormService.listAll()) {
            formsByUserId.put(form.userId(), form);
        }

        Map<Long, String> userNames = new HashMap<>();
        for (UserEntity user : userDao.findAll()) {
            userNames.put(user.getId(), user.getName());
        }

        Set<Long> assignedUserIds = new HashSet<>();
        List<TeamSystemTeamResponse> teamResponses = new ArrayList<>();

        for (Team team : teams) {
            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            List<TeamSystemTeamMemberResponse> memberResponses = new ArrayList<>();

            for (TeamMember member : members) {
                Long userId = member.getUserId();
                if (userId == null) {
                    continue;
                }
                assignedUserIds.add(userId);

                String uiPosition = toUiPosition(member.getSessionPosition());
                TeamFormMemberResponse form = formsByUserId.get(userId);
                String name = form != null && form.name() != null && !form.name().isBlank()
                        ? form.name()
                        : userNames.getOrDefault(userId, "알 수 없음");
                String level = resolveLevel(form, uiPosition);

                memberResponses.add(new TeamSystemTeamMemberResponse(
                        userId,
                        uiPosition,
                        name,
                        level
                ));
            }

            String status = memberResponses.isEmpty() ? "대기" : "배정됨";
            teamResponses.add(new TeamSystemTeamResponse(
                    team.getName() == null ? "" : team.getName(),
                    status,
                    "",
                    memberResponses,
                    team.isConfirmed(),
                    readNeededPositions(team.getNeededPositions())
            ));
        }

        List<TeamSystemUnmatchedResponse> unmatched = formsByUserId.values().stream()
                .filter(form -> !assignedUserIds.contains(form.userId()))
                .map(form -> new TeamSystemUnmatchedResponse(
                        form.userId(),
                        form.name(),
                        "미배정"
                ))
                .toList();

        return new TeamSystemMatchResponse(teamResponses, unmatched);
    }

    private Map<String, Boolean> collectNeededByPosition(TeamSystemAssignmentTeamRequest teamRequest) {
        Map<String, Boolean> needed = new LinkedHashMap<>();
        if (teamRequest == null || teamRequest.slots() == null) {
            return needed;
        }
        for (TeamSystemAssignmentSlotRequest slot : teamRequest.slots()) {
            if (slot == null || slot.position() == null || slot.position().isBlank()) {
                continue;
            }
            String position = normalizePosition(slot.position());
            needed.put(position, slot.needed() == null || slot.needed());
        }
        return needed;
    }

    private String writeNeededPositions(Map<String, Boolean> neededByPosition) {
        if (neededByPosition == null || neededByPosition.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(neededByPosition);
        } catch (Exception ex) {
            throw new IllegalStateException("필요 세션 정보를 저장하지 못했습니다.", ex);
        }
    }

    private Map<String, Boolean> readNeededPositions(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Boolean> parsed = objectMapper.readValue(
                    raw,
                    new TypeReference<Map<String, Boolean>>() {
                    }
            );
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            log.warn("needed_positions 파싱 실패: {}", ex.getMessage());
            return Map.of();
        }
    }

    private List<UserPosition> collectAssignments(
            String teamName,
            TeamSystemAssignmentTeamRequest teamRequest
    ) {
        Map<Long, LinkedHashMap<String, Boolean>> positionsByUser = new LinkedHashMap<>();
        if (teamRequest == null || teamRequest.slots() == null) {
            return List.of();
        }

        for (TeamSystemAssignmentSlotRequest slot : teamRequest.slots()) {
            if (slot == null || slot.userId() == null) {
                continue;
            }

            String position = normalizePosition(slot.position());
            Long userId = slot.userId();
            positionsByUser
                    .computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                    .put(position, Boolean.TRUE);
        }

        List<UserPosition> assignments = new ArrayList<>();
        for (Map.Entry<Long, LinkedHashMap<String, Boolean>> entry : positionsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<String> positions = new ArrayList<>(entry.getValue().keySet());
            if (positions.size() > 1 && !positions.contains("V")) {
                throw new IllegalArgumentException(
                        teamName + ": 세션 겸임은 보컬(V)과 다른 세션 조합만 가능합니다. (userId=" + userId + ")"
                );
            }
            for (String position : positions) {
                assignments.add(new UserPosition(userId, position));
            }
        }

        return assignments;
    }

    private record UserPosition(Long userId, String position) {
    }

    private String normalizeTeamName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("팀 이름이 비어 있습니다.");
        }
        return name.trim();
    }

    private String normalizePosition(String position) {
        if (position == null || position.isBlank()) {
            throw new IllegalArgumentException("세션 포지션이 비어 있습니다.");
        }
        String normalized = position.trim().toUpperCase();
        if ("K1".equals(normalized) || "K2".equals(normalized)) {
            normalized = "K";
        }
        if (!ALLOWED_POSITIONS.contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 세션 포지션입니다: " + position);
        }
        return normalized;
    }

    private String toDbPosition(String uiPosition) {
        if ("K".equals(uiPosition)) {
            return "K1";
        }
        return uiPosition;
    }

    private String toUiPosition(String dbPosition) {
        if (dbPosition == null || dbPosition.isBlank()) {
            return "";
        }
        if ("K1".equals(dbPosition) || "K2".equals(dbPosition)) {
            return "K";
        }
        return dbPosition;
    }

    private String resolveLevel(TeamFormMemberResponse form, String position) {
        if (form == null || form.positions() == null || position == null || position.isBlank()) {
            return "";
        }
        for (TeamFormPositionResponse item : form.positions()) {
            if (item != null && Objects.equals(item.position(), position)) {
                return item.level() == null ? "" : item.level();
            }
        }
        if (!form.positions().isEmpty() && form.positions().get(0) != null) {
            String level = form.positions().get(0).level();
            return level == null ? "" : level;
        }
        return "";
    }
}
