package com.example.demo.teamform;

import com.example.demo.teamform.dao.TeamFormDao;
import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamFormPositionResponse;
import com.example.demo.teamform.dto.TeamFormSaveRequest;
import com.example.demo.teamform.dto.TeamFormSaveResponse;
import com.example.demo.teamform.dto.TeamFormScheduleResponse;
import com.example.demo.teamform.vo.TeamFormHeaderRow;
import com.example.demo.teamform.vo.TeamFormPositionRow;
import com.example.demo.teamform.vo.TeamFormScheduleRow;
import com.example.demo.teamform.vo.TeamPositionVo;
import com.example.demo.teamform.vo.TeamScheduleVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TeamFormService {

    private static final Set<String> ALLOWED_POSITIONS = Set.of("V", "D", "B", "EG1", "EG2", "AG", "K");
    private static final Set<String> ALLOWED_LEVELS = Set.of("상", "중", "하");
    private static final Set<String> ALLOWED_DAYS = Set.of("월", "화", "수", "목", "금");
    private static final LocalTime MIN_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime MAX_START_TIME = LocalTime.of(21, 30);

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> POSITION_ORDER = List.of("V", "D", "B", "EG1", "EG2", "AG", "K");
    private static final List<String> DAY_ORDER = List.of("월", "화", "수", "목", "금");

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
        List<TeamScheduleVo> schedules = toScheduleVos(request.schedules());
        String teammates = request.teammates() == null ? "" : request.teammates().trim();
        int maxTeams = normalizeMaxTeams(request.maxTeams());

        teamFormDao.deleteByUserId(userId);
        int teamFormId = teamFormDao.insertForm(userId, teammates, maxTeams);
        int savedPositionCount = teamFormDao.insertPositions(teamFormId, positions);
        int savedScheduleCount = teamFormDao.insertSchedules(teamFormId, schedules);

        return new TeamFormSaveResponse(savedPositionCount, savedScheduleCount, "제출이 완료되었습니다.");
    }

    public List<TeamFormMemberResponse> listAll() {
        List<TeamFormHeaderRow> headers = teamFormDao.findAllHeaders();
        Map<Integer, List<TeamFormPositionResponse>> positionsByForm = new HashMap<>();
        for (TeamFormPositionRow row : teamFormDao.findAllPositions()) {
            positionsByForm
                    .computeIfAbsent(row.teamFormId(), ignored -> new ArrayList<>())
                    .add(new TeamFormPositionResponse(row.position(), row.level()));
        }
        Map<Integer, List<TeamFormScheduleResponse>> schedulesByForm = new HashMap<>();
        for (TeamFormScheduleRow row : teamFormDao.findAllSchedules()) {
            String startTime = row.startTime() == null ? "" : TIME_FORMAT.format(row.startTime());
            schedulesByForm
                    .computeIfAbsent(row.teamFormId(), ignored -> new ArrayList<>())
                    .add(new TeamFormScheduleResponse(row.dayOfWeek(), startTime));
        }

        List<TeamFormMemberResponse> result = new ArrayList<>();
        for (TeamFormHeaderRow header : headers) {
            List<TeamFormPositionResponse> positions = new ArrayList<>(
                    positionsByForm.getOrDefault(header.id(), List.of())
            );
            positions.sort(Comparator.comparingInt(item -> positionRank(item.position())));

            List<TeamFormScheduleResponse> schedules = new ArrayList<>(
                    schedulesByForm.getOrDefault(header.id(), List.of())
            );
            schedules.sort(Comparator
                    .comparingInt((TeamFormScheduleResponse item) -> dayRank(item.dayOfWeek()))
                    .thenComparing(TeamFormScheduleResponse::startTime));

            result.add(new TeamFormMemberResponse(
                    header.userId(),
                    header.userName(),
                    header.userCode() == null ? "" : header.userCode(),
                    header.teammates() == null ? "" : header.teammates(),
                    header.maxTeams() <= 0 ? 1 : header.maxTeams(),
                    header.createdAt() == null ? "" : DATE_TIME_FORMAT.format(header.createdAt()),
                    positions,
                    schedules
            ));
        }
        return result;
    }

    private int positionRank(String position) {
        int index = POSITION_ORDER.indexOf(position);
        return index < 0 ? POSITION_ORDER.size() : index;
    }

    private int dayRank(String dayOfWeek) {
        int index = DAY_ORDER.indexOf(dayOfWeek);
        return index < 0 ? DAY_ORDER.size() : index;
    }

    private int normalizeMaxTeams(Integer maxTeams) {
        if (maxTeams == null || maxTeams < 1 || maxTeams > 3) {
            throw new IllegalArgumentException("참여 가능 팀 수는 1팀부터 3팀까지 선택할 수 있습니다.");
        }
        return maxTeams;
    }

    private List<TeamPositionVo> toPositionVos(List<TeamFormSaveRequest.PositionRequest> requests) {
        Set<String> seen = new HashSet<>();
        return requests.stream()
                .map(item -> {
                    String position = normalize(item.position());
                    String level = normalize(item.level());

                    if (!ALLOWED_POSITIONS.contains(position)) {
                        throw new IllegalArgumentException("허용되지 않은 포지션입니다: " + position);
                    }
                    if (!ALLOWED_LEVELS.contains(level)) {
                        throw new IllegalArgumentException("숙련도는 상, 중, 하 중 하나여야 합니다.");
                    }
                    if (!seen.add(position)) {
                        throw new IllegalArgumentException("포지션이 중복되었습니다: " + position);
                    }
                    return new TeamPositionVo(position, level);
                })
                .toList();
    }

    private List<TeamScheduleVo> toScheduleVos(List<TeamFormSaveRequest.ScheduleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new HashSet<>();
        return requests.stream()
                .map(item -> {
                    String dayOfWeek = normalize(item.dayOfWeek());
                    LocalTime startTime = parseStartTime(item.startTime());

                    if (!ALLOWED_DAYS.contains(dayOfWeek)) {
                        throw new IllegalArgumentException("허용되지 않은 요일입니다: " + dayOfWeek);
                    }
                    if (startTime.isBefore(MIN_START_TIME) || startTime.isAfter(MAX_START_TIME)) {
                        throw new IllegalArgumentException("가능한 시간은 09:00부터 21:30까지입니다.");
                    }
                    if (startTime.getMinute() != 0 && startTime.getMinute() != 30) {
                        throw new IllegalArgumentException("시간은 30분 단위여야 합니다.");
                    }
                    if (startTime.getSecond() != 0 || startTime.getNano() != 0) {
                        throw new IllegalArgumentException("시간 초 단위는 0이어야 합니다.");
                    }

                    String uniqueKey = dayOfWeek + "-" + startTime;
                    if (!seen.add(uniqueKey)) {
                        throw new IllegalArgumentException("같은 시간대가 중복되었습니다: " + uniqueKey);
                    }
                    return new TeamScheduleVo(dayOfWeek, startTime);
                })
                .toList();
    }

    private LocalTime parseStartTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("시작 시간이 비어 있습니다.");
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("시간 형식이 잘못되었습니다: " + value);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
