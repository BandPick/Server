package com.example.demo.setlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SetlistService {

    private final SetlistRepository setlistRepository;

    public SetlistService(SetlistRepository setlistRepository) {
        this.setlistRepository = setlistRepository;
    }

    @Transactional(readOnly = true)
    public List<SetlistResponse> listAll() {
        return setlistRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 요청 본문으로 셋리스트를 저장합니다.
     * 1) 요청 id로 기존 행 갱신
     * 2) id가 없으면 제목+가수로 기존 행 매칭 후 갱신 (세션 변경 시에도 동일 곡으로 인식)
     * 3) 매칭되지 않으면 새 행 생성
     * 다른 테이블에서 참조 중인 setlist 삭제로 인한 FK 오류를 막기 위해
     * 요청에 없는 기존 행은 삭제하지 않습니다.
     */
    @Transactional
    public List<SetlistResponse> replaceAll(SetlistSaveRequest request) {
        List<Setlist> existingRows = setlistRepository.findAll();

        if (request.items() == null || request.items().isEmpty()) {
            return setlistRepository.findAllByOrderByIdAsc().stream()
                    .map(this::toResponse)
                    .toList();
        }

        Map<Long, Setlist> existingById = new HashMap<>();
        Map<String, Deque<Setlist>> existingBySong = new HashMap<>();
        for (Setlist row : existingRows) {
            existingById.put(row.getId(), row);
            String key = songKey(row.getTitle(), row.getArtist());
            existingBySong.computeIfAbsent(key, k -> new LinkedList<>()).add(row);
        }

        Set<Long> keptIds = new HashSet<>();

        for (SetlistWriteItem it : request.items()) {
            if (it == null) {
                continue;
            }
            String t = it.title() != null ? it.title().trim() : "";
            String a = it.artist() != null ? it.artist().trim() : "";
            if (t.isEmpty() || a.isEmpty()) {
                continue;
            }
            List<SessionValue> normalizedSessions = normalizeSessions(it.sessions());

            Setlist row = null;
            if (it.id() != null) {
                row = existingById.get(it.id());
            }
            if (row == null) {
                Deque<Setlist> candidates = existingBySong.get(songKey(t, a));
                if (candidates != null) {
                    while (!candidates.isEmpty()) {
                        Setlist candidate = candidates.pollFirst();
                        if (!keptIds.contains(candidate.getId())) {
                            row = candidate;
                            break;
                        }
                    }
                }
            }
            if (row == null) {
                row = new Setlist();
            }
            row.setTitle(t);
            row.setArtist(a);
            row.setSessions(toSessionEntities(row, normalizedSessions));
            Setlist saved = setlistRepository.save(row);
            keptIds.add(saved.getId());
        }

        return setlistRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private List<SessionValue> normalizeSessions(List<String> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return new ArrayList<>();
        }
        List<SessionValue> out = new ArrayList<>();
        for (String s : sessions) {
            if (s == null) {
                continue;
            }
            String v = s.trim();
            if (v.isEmpty()) {
                continue;
            }
            if (v.startsWith("기타(") && v.endsWith(")")) {
                String extra = v.substring(3, v.length() - 1).trim();
                out.add(new SessionValue("기타", extra));
            } else {
                out.add(new SessionValue(v, ""));
            }
        }
        return out;
    }

    private SetlistResponse toResponse(Setlist row) {
        return new SetlistResponse(
                row.getId(),
                row.getTitle(),
                row.getArtist(),
                toLabels(row.getSessions()));
    }

    private String songKey(String title, String artist) {
        String t = title == null ? "" : title.trim();
        String a = artist == null ? "" : artist.trim();
        return t + "\u0001" + a;
    }

    private List<Session> toSessionEntities(Setlist row, List<SessionValue> values) {
        return values.stream()
                .map(value -> new Session(row, value.position(), value.extra()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> toLabels(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }
        return sessions.stream()
                .map(session -> {
                    String position = session.getPosition().name();
                    String extra = session.getExtra() == null ? "" : session.getExtra().trim();
                    if ("기타".equals(position) && !extra.isEmpty()) {
                        return "기타(" + extra + ")";
                    }
                    return position;
                })
                .toList();
    }

    private record SessionValue(String position, String extra) {
    }
}
