package com.example.demo.teamform;

import com.example.demo.memberform.vo.AvailabilityVo;
import com.example.demo.teamform.dao.TeamFormDao;
import com.example.demo.teamform.dto.TeamFormSaveRequest;
import com.example.demo.teamform.dto.TeamFormSaveResponse;
import com.example.demo.teamform.vo.TeamPositionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeamFormService {

    private static final Set<String> ALLOWED_POSITIONS = Set.of("V", "D", "B", "EG1", "EG2", "AG", "K");
    private static final Set<String> ALLOWED_PROFICIENCIES = Set.of("상", "중", "하");

    private final TeamFormDao teamFormDao;

    public TeamFormService(TeamFormDao teamFormDao) {
        this.teamFormDao = teamFormDao;
    }

    @Transactional
    public TeamFormSaveResponse save(long userId, TeamFormSaveRequest request) {
        if (!teamFormDao.existsUser(userId)) {
            throw new IllegalArgumentException("존재하지 않는 부원입니다. userId=" + userId);
        }

        List<TeamPositionVo> positions = toPositionVos(request.positions());
        List<AvailabilityVo> availabilities = toAvailabilityVos(request.availabilities());
        String preferredTeammates = request.preferredTeammates() == null
                ? ""
                : request.preferredTeammates().trim();

        teamFormDao.deleteAllByUserId(userId);
        teamFormDao.insertHeader(userId, preferredTeammates);
        int savedPositionCount = teamFormDao.insertPositions(userId, positions);
        int savedAvailabilityCount = teamFormDao.insertAvailabilities(userId, availabilities);

        return new TeamFormSaveResponse(savedPositionCount, savedAvailabilityCount, "팀제 신청이 저장되었습니다.");
    }

    private List<TeamPositionVo> toPositionVos(List<TeamFormSaveRequest.PositionRequest> requests) {
        Set<String> seen = new HashSet<>();
        return requests.stream()
                .map(item -> {
                    String position = item.position() == null ? "" : item.position().trim();
                    String proficiency = item.proficiency() == null ? "" : item.proficiency().trim();

                    if (!ALLOWED_POSITIONS.contains(position)) {
                        throw new IllegalArgumentException("허용되지 않은 포지션입니다: " + position);
                    }
                    if (!ALLOWED_PROFICIENCIES.contains(proficiency)) {
                        throw new IllegalArgumentException("숙련도는 상, 중, 하 중 하나여야 합니다.");
                    }
                    if (!seen.add(position)) {
                        throw new IllegalArgumentException("포지션이 중복되었습니다: " + position);
                    }
                    return new TeamPositionVo(position, proficiency);
                })
                .toList();
    }

    private List<AvailabilityVo> toAvailabilityVos(List<TeamFormSaveRequest.AvailabilityRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(item -> {
                    LocalDateTime from = parseDateTime(item.availableFrom());
                    LocalDateTime to = parseDateTime(item.availableTo());
                    if (!from.isBefore(to)) {
                        throw new IllegalArgumentException("availableFrom은 availableTo보다 빨라야 합니다.");
                    }
                    return new AvailabilityVo(from, to);
                })
                .toList();
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("날짜 형식이 잘못되었습니다: " + value);
        }
    }
}
