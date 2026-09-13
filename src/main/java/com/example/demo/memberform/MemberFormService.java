package com.example.demo.memberform;

import com.example.demo.memberform.dao.MemberFormDao;
import com.example.demo.memberform.dto.MemberFormAvailabilityResponse;
import com.example.demo.memberform.dto.MemberFormDetailResponse;
import com.example.demo.memberform.dto.MemberFormMatrixSaveRequest;
import com.example.demo.memberform.dto.MemberFormMatrixSaveResponse;
import com.example.demo.memberform.dto.MemberFormMemberResponse;
import com.example.demo.memberform.dto.MemberFormPickResponse;
import com.example.demo.memberform.dto.MemberFormSaveRequest;
import com.example.demo.memberform.dto.MemberFormSaveResponse;
import com.example.demo.memberform.vo.AvailabilityVo;
import com.example.demo.memberform.vo.FormPickRow;
import com.example.demo.memberform.vo.PickVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MemberFormService {

    private static final Set<String> ALLOWED_POSITIONS = Set.of("V", "D", "B", "EG1", "EG2", "AG", "K1", "K2", "기타");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MemberFormDao memberFormDao;

    public MemberFormService(MemberFormDao memberFormDao) {
        this.memberFormDao = memberFormDao;
    }

    public List<MemberFormMemberResponse> listAll() {
        Map<Long, List<FormPickRow>> byUser = new LinkedHashMap<>();
        for (FormPickRow row : memberFormDao.findAllPicks()) {
            byUser.computeIfAbsent(row.userId(), ignored -> new ArrayList<>()).add(row);
        }

        return byUser.values().stream()
                .map(rows -> {
                    rows.sort(Comparator
                            .comparingInt(FormPickRow::priority)
                            .thenComparing(FormPickRow::songTitle)
                            .thenComparing(FormPickRow::desiredPosition));
                    List<MemberFormPickResponse> picks = rows.stream()
                            .map(this::toPickResponse)
                            .toList();
                    return new MemberFormMemberResponse(rows.getFirst().userId(), rows.getFirst().userName(), picks);
                })
                .sorted(Comparator.comparing(MemberFormMemberResponse::name))
                .toList();
    }

    public MemberFormDetailResponse findByUserId(long userId) {
        List<MemberFormPickResponse> picks = memberFormDao.findPicksByUserId(userId).stream()
                .map(this::toPickResponse)
                .toList();
        List<MemberFormAvailabilityResponse> availabilities = memberFormDao.findAvailabilitiesByUserId(userId).stream()
                .map(row -> new MemberFormAvailabilityResponse(
                        DATE_TIME_FORMAT.format(row.availableFrom()),
                        DATE_TIME_FORMAT.format(row.availableTo())
                ))
                .toList();
        return new MemberFormDetailResponse(userId, picks, availabilities);
    }

    @Transactional
    public MemberFormSaveResponse save(long userId, MemberFormSaveRequest request) {
        if (userId != request.userId()) {
            throw new IllegalArgumentException("로그인 사용자와 요청 userId가 일치하지 않습니다.");
        }

        List<PickVo> picks = toPickVos(request.picks());
        List<AvailabilityVo> availabilities = toAvailabilityVos(request.availabilities());

        memberFormDao.deleteAllByUserId(userId);
        int savedPickCount = memberFormDao.insertPicks(userId, picks);
        int savedAvailabilityCount = memberFormDao.insertAvailabilities(userId, availabilities);

        return new MemberFormSaveResponse(savedPickCount, savedAvailabilityCount, "저장이 완료되었습니다.");
    }

    @Transactional
    public MemberFormMatrixSaveResponse replaceAllPicks(MemberFormMatrixSaveRequest request) {
        List<MemberFormMatrixSaveRequest.MatrixPickRequest> pickRequests =
                request.picks() == null ? List.of() : request.picks();

        Map<Long, List<PickVo>> picksByUser = new LinkedHashMap<>();
        for (MemberFormMatrixSaveRequest.MatrixPickRequest pick : pickRequests) {
            if (!memberFormDao.existsUser(pick.userId())) {
                throw new IllegalArgumentException("존재하지 않는 부원입니다. userId=" + pick.userId());
            }
            if (!memberFormDao.existsSetlist(pick.setlistId())) {
                throw new IllegalArgumentException("존재하지 않는 셋리스트입니다. setlistId=" + pick.setlistId());
            }

            PickVo pickVo = toPickVo(pick.priority(), pick.setlistId(), pick.desiredPosition(), pick.desiredExtra());
            picksByUser.computeIfAbsent(pick.userId(), ignored -> new ArrayList<>()).add(pickVo);
        }

        memberFormDao.deleteAllPicks();

        int savedPickCount = 0;
        for (Map.Entry<Long, List<PickVo>> entry : picksByUser.entrySet()) {
            savedPickCount += memberFormDao.insertPicks(entry.getKey(), entry.getValue());
        }

        return new MemberFormMatrixSaveResponse(savedPickCount, "희망곡 데이터가 저장되었습니다.");
    }

    private List<PickVo> toPickVos(List<MemberFormSaveRequest.PickRequest> pickRequests) {
        return pickRequests.stream()
                .map(pick -> toPickVo(
                        pick.priority(),
                        pick.setlistId(),
                        pick.desiredPosition(),
                        pick.desiredExtra()
                ))
                .toList();
    }

    private PickVo toPickVo(int priority, long setlistId, String desiredPosition, String desiredExtra) {
        String position = normalizePosition(desiredPosition);
        String extra = desiredExtra == null ? "" : desiredExtra.trim();

        if (priority <= 0) {
            throw new IllegalArgumentException("priority는 1 이상의 값이어야 합니다.");
        }
        if (!ALLOWED_POSITIONS.contains(position)) {
            throw new IllegalArgumentException("허용되지 않은 세션 포지션입니다: " + position);
        }
        if (!"기타".equals(position) && !extra.isBlank()) {
            throw new IllegalArgumentException("desiredExtra는 '기타' 선택 시에만 입력 가능합니다.");
        }

        return new PickVo(
                priority,
                setlistId,
                position,
                "기타".equals(position) ? extra : ""
        );
    }

    private List<AvailabilityVo> toAvailabilityVos(List<MemberFormSaveRequest.AvailabilityRequest> requests) {
        return requests.stream()
                .map(a -> {
                    LocalDateTime from = parseDateTime(a.availableFrom());
                    LocalDateTime to = parseDateTime(a.availableTo());
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

    private String normalizePosition(String position) {
        if (position == null) {
            return "";
        }
        return position.trim();
    }

    private MemberFormPickResponse toPickResponse(FormPickRow row) {
        String position = row.desiredPosition() == null ? "" : row.desiredPosition().trim();
        String extra = row.desiredExtra() == null ? "" : row.desiredExtra().trim();
        String session = "기타".equals(position) && !extra.isEmpty()
                ? "기타(" + extra + ")"
                : position;
        return new MemberFormPickResponse(row.priority(), row.songTitle(), session, row.setlistId());
    }
}
