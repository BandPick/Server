package com.example.demo.algorithm;

import com.example.demo.common.type.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


// ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ



public class Algorithm{
    // 멤버 최대 희망곡 수
    final int MAX_CHOICE         = 4;
    // 배정 최소 공통 가능일
    final int $SETTING_min_common_day_until_deadline    = 1;

    // ($SETTING_min_common_time_per_1play) * 2 = MIN_COMMON_TIMES_BITCOUNT
    // 최소 공통 가능시간(비트) => 비트 2개 = 1시간
    final int MIN_COMMON_TIMES_BITCOUNT   = 2; // 1시간
    final int PRACTICE_SLOT_BITS = 2; // Step2 배정 단위: 1시간

    // ── Step2 설정: 팀당 목표 합주 횟수 ──
    // 이 값을 바꾸면 전체 합주 횟수 목표가 바뀜
    final int TARGET_PRACTICE_COUNT = 3;

    // ============================
    // Step 1: 합주 팀 편성
    // ============================

    public AssignmentState runStep1(
            List<String> songList,
            // 곡명->필요세션(list)
            Map<String, List<Position>> requiredSessions,
            // 곡명->할당된 멤버, 멤버's 세션
            Map<String, List<AssignedSession>> songMemberList,
            // 멤버코드->멤버객체
            Map<Integer, Member_AL> memberList) {

        AssignmentState state = new AssignmentState();

        // ㅡㅡㅡ 초기화 ㅡㅡㅡ
        for (String song : songList){
            state.candidates.put(song, new HashMap<>());
            state.confirmed.put(song, new HashMap<>());

            for (Position sess : requiredSessions.getOrDefault(song, List.of())){
                List<Member_AL> applicants = songMemberList
                        .getOrDefault(song, List.of()).stream()
                        .filter(as -> as.assignedSession == sess)
                        .map(as -> as.memberAL)
                        .collect(Collectors.toList());
                state.candidates.get(song).put(sess, new ArrayList<>(applicants));
            }
        }
        for (Member_AL m : memberList.values()){
            state.assignedSongList.put(m.$USER_code, new ArrayList<>());
        }
        for (List<AssignedSession> asList : songMemberList.values()){
            for (AssignedSession as : asList){
                state.assignedSongList.putIfAbsent(as.memberAL.$USER_code, new ArrayList<>());
            }
        }

        // --- Phase 1 ---
        System.out.println("[Phase 1] 드럼 필수 포함 전체 조합 평가 배정");
        assignBestCombinationBySong(state, songList, requiredSessions);

        // --- Phase 2 ---
        System.out.println("[Phase 2] 미배정 세션 채우기");
        for (String song : songList){
            for(Position sess : requiredSessions.getOrDefault(song, List.of())){
                if (state.confirmed.get(song).containsKey(sess)) continue;

                memberList.values().stream()
                        .filter(m -> m.session.contains(sess))
                        .filter(m -> {
                            List<String> alreadyIn = state.assignedSongList.get(m.$USER_code);
                            if (alreadyIn.contains(song)) return false;
                            return alreadyIn.stream().noneMatch(assignedSong ->
                                    hasTimeConflict(assignedSong, song, m, state, memberList));
                        })
                        .max(Comparator.comparingInt(Member_AL::totalAvailableBits))
                        .ifPresent(best ->
                                confirm(state, song, sess, best, songList, requiredSessions));
            }
        }

        // --- Phase 3 ---
        System.out.println("[Phase 3] 공통 가능일 검사");
        for (String song : songList){
            int score = calcCommonDaysCount_confirmed(song, state);
            state.songScore.put(song, score);
            if (score <= $SETTING_min_common_day_until_deadline) {
                state.excluded.add(song);
                System.out.printf(" [제외 후보] '%s' 공통 가능일  %d일 (기준: 최소 %d일)%n", song, score, $SETTING_min_common_day_until_deadline);
            }
        }
        return state;
    }

    // 입력된 멤버리스트 시간 상호 비교(&연산), 가능 <날짜,시간>만 남김
    public Map<LocalDate, Long> computeCommonTime(List<Member_AL> memberALS){
        if (memberALS.isEmpty()) return new HashMap<>();
        Map<LocalDate, Long> common = new HashMap<>(memberALS.get(0).availableSlots);
        for (int i = 1; i< memberALS.size(); i++){
            Map<LocalDate, Long> other = memberALS.get(i).availableSlots;
            common.replaceAll((date, time) -> time & other.getOrDefault(date, 0L));
            common.entrySet().removeIf(e -> e.getValue() == 0L);
        }
        return common;
    }

    // 입력된 멤버리스트의 공통 가능일 수(최소 합주시간보다 커야함) return
    public int calcCommonDaysCount(List<Member_AL> memberALS){
        if (memberALS == null || memberALS.isEmpty()) return 0;
        Map<LocalDate, Long> commonTimeMap = computeCommonTime(memberALS);
        return (int) commonTimeMap.values().stream()
                .filter(mask -> Long.bitCount(mask) >= MIN_COMMON_TIMES_BITCOUNT)
                .count();
    }

    // 현재 확정 인원 공통 가능일 수
    public int calcCommonDaysCount_confirmed(String song, AssignmentState state){
        List<Member_AL> confirmedMemberALS = new ArrayList<>(state.confirmed.get(song).values());
        return calcCommonDaysCount(confirmedMemberALS);
    }

    public int calcCommonBitsCount(List<Member_AL> memberALS) {
        return computeCommonTime(memberALS).values().stream()
                .mapToInt(Long::bitCount)
                .sum();
    }

    public void assignBestCombinationBySong(AssignmentState state, List<String> songList,
                                            Map<String, List<Position>> requiredSessions) {
        for (String song : songList) {
            List<Position> needed = requiredSessions.getOrDefault(song, List.of()).stream()
                    .distinct()
                    .collect(Collectors.toList());

            if (needed.isEmpty()) continue;

            TeamCombination best = findBestTeamCombination(state, song, needed);
            if (best == null) {
                System.out.printf(" [%s] 완성 가능한 전체 조합 없음%n", song);
                continue;
            }

            for (Map.Entry<Position, Member_AL> entry : best.membersByPosition.entrySet()) {
                Position position = entry.getKey();
                Member_AL memberAL = entry.getValue();
                state.confirmed.get(song).put(position, memberAL);
                state.assignedSongList
                        .computeIfAbsent(memberAL.$USER_code, ignored -> new ArrayList<>());
                List<String> assigned = state.assignedSongList.get(memberAL.$USER_code);
                if (!assigned.contains(song)) {
                    assigned.add(song);
                }
            }
        }
    }

    public TeamCombination findBestTeamCombination(AssignmentState state, String song, List<Position> needed) {
        List<Position> availablePositions = new ArrayList<>();
        for (Position position : needed) {
            boolean hasCandidates = !uniqueCandidates(
                    state.candidates.get(song).getOrDefault(position, List.of())).isEmpty();
            if (hasCandidates) {
                availablePositions.add(position);
            } else if (position == Position.DRUM) {
                return null;
            }
        }

        if (availablePositions.isEmpty()) {
            return null;
        }

        List<Position> orderedPositions = availablePositions.stream()
                .sorted(Comparator.comparingInt(position ->
                        uniqueCandidates(state.candidates.get(song).getOrDefault(position, List.of())).size()))
                .collect(Collectors.toList());

        TeamCombinationSearch search = new TeamCombinationSearch(song, orderedPositions);
        searchDfs(state, search, 0, new LinkedHashMap<>(), new HashSet<>());
        return search.best;
    }

    private void searchDfs(AssignmentState state, TeamCombinationSearch search, int index,
                           Map<Position, Member_AL> picked, Set<Integer> usedMemberCodes) {
        if (index == search.positions.size()) {
            TeamCombination candidate = evaluateCombination(search.song, picked);
            if (search.best == null || candidate.compareTo(search.best) > 0) {
                search.best = candidate;
            }
            return;
        }

        Position position = search.positions.get(index);
        List<Member_AL> candidates = uniqueCandidates(
                state.candidates.get(search.song).getOrDefault(position, List.of()));
        candidates.sort(Comparator
                .comparingInt((Member_AL memberAL) -> normalizedChoiceScore(memberAL, search.song))
                .reversed()
                .thenComparing(Comparator.comparingInt(Member_AL::totalAvailableBits).reversed()));

        for (Member_AL candidate : candidates) {
            if (usedMemberCodes.contains(candidate.$USER_code)) continue;

            picked.put(position, candidate);
            usedMemberCodes.add(candidate.$USER_code);
            searchDfs(state, search, index + 1, picked, usedMemberCodes);
            usedMemberCodes.remove(candidate.$USER_code);
            picked.remove(position);
        }
    }

    private TeamCombination evaluateCombination(String song, Map<Position, Member_AL> membersByPosition) {
        List<Member_AL> members = new ArrayList<>(membersByPosition.values());
        int commonDays = calcCommonDaysCount(members);
        int commonBits = calcCommonBitsCount(members);
        int preferenceScore = members.stream()
                .mapToInt(memberAL -> normalizedChoiceScore(memberAL, song))
                .sum();
        return new TeamCombination(new LinkedHashMap<>(membersByPosition), commonDays, commonBits, preferenceScore);
    }

    private List<Member_AL> uniqueCandidates(List<Member_AL> candidates) {
        Map<Integer, Member_AL> unique = new LinkedHashMap<>();
        for (Member_AL candidate : candidates) {
            unique.putIfAbsent(candidate.$USER_code, candidate);
        }
        return new ArrayList<>(unique.values());
    }

    private int normalizedChoiceScore(Member_AL memberAL, String song) {
        int rank = getChoiceRank(memberAL, song);
        if (rank <= 0) return 0;
        return Math.max(0, MAX_CHOICE - rank + 1);
    }

    // 후보 중 공통 가능일 최대인 멤버 선택
    public Member_AL pickBestCandidate(List<Member_AL> candidate, String song, AssignmentState state){
        List<Member_AL> current = new ArrayList<>(state.confirmed.get(song).values());
        Member_AL bestCandidate = null;
        int bestScore = -1;

        for (Member_AL cand : candidate){
            boolean hasConflict = state.assignedSongList.get(cand.$USER_code).stream()
                    .anyMatch(assignedSong -> hasTimeConflict(assignedSong, song, cand, state, null));
            if (hasConflict) continue;

            int rank = getChoiceRank(cand, song);

            List<Member_AL> temp = new ArrayList<>(current);
            temp.add(cand);
            int commonDays = calcCommonDaysCount(temp);

            int score = normalizedChoiceScore(cand, song) * 1000 + commonDays;

            if (score > bestScore){
                bestScore = score;
                bestCandidate = cand;
            }
        }
        return bestCandidate;
    }

    // A곡, B곡, 추가멤버 간 시간겹침 T/F 확인
    public boolean hasTimeConflictSimple(String assigned_songA, String compare_songB, Member_AL memberAL,
                                         AssignmentState state, Map<Integer, Member_AL> memberList){
        Map<LocalDate, Long> assinged_timeA = computeCommonTime(new ArrayList<>(state.confirmed.get(assigned_songA).values()));
        List<Member_AL> membersB = new ArrayList<>(state.confirmed.get(compare_songB).values());
        membersB.add(memberAL);
        Map<LocalDate, Long> compare_timeB = computeCommonTime(membersB);

        int possibleDaysCount = 0;

        for (LocalDate date : compare_timeB.keySet()){
            long bitsB = compare_timeB.getOrDefault(date, 0L);
            long bitsA = assinged_timeA.getOrDefault(date, 0L);
            long commonBits = bitsA & bitsB;

            if (commonBits == 0L){
                if (Long.bitCount(bitsB) >= MIN_COMMON_TIMES_BITCOUNT){
                    possibleDaysCount++;
                }
            } else {
                long unionBits = bitsA | bitsB;
                if (Long.bitCount(unionBits) >= MIN_COMMON_TIMES_BITCOUNT * 2){
                    possibleDaysCount++;
                }
            }
        }
        return possibleDaysCount < $SETTING_min_common_day_until_deadline;
    }

    // Phase 4용 : A곡, B곡 간 시간겹침 T/F 확인
    public boolean hasTimeConflict(String assigned_songA, String compare_songB, Member_AL memberAL,
                                   AssignmentState state, Map<Integer, Member_AL> memberList){
        Map<LocalDate, Long> assinged_timeA = computeCommonTime(new ArrayList<>(state.confirmed.get(assigned_songA).values()));
        Map<LocalDate, Long> compare_timeB = computeCommonTime(new ArrayList<>(state.confirmed.get(compare_songB).values()));

        int possibleDaysCount = 0;

        for (LocalDate date : compare_timeB.keySet()){
            long bitsB = compare_timeB.getOrDefault(date, 0L);
            long bitsA = assinged_timeA.getOrDefault(date, 0L);
            long commonBits = bitsA & bitsB;

            if (commonBits == 0L){
                if (Long.bitCount(bitsB) >= MIN_COMMON_TIMES_BITCOUNT){
                    possibleDaysCount++;
                }
            } else {
                long unionBits = bitsA | bitsB;
                if (Long.bitCount(unionBits) >= MIN_COMMON_TIMES_BITCOUNT * 2){
                    possibleDaysCount++;
                }
            }
        }
        return possibleDaysCount < $SETTING_min_common_day_until_deadline;
    }

    // 확정 처리
    public void confirm(AssignmentState state, String song, Position sess, Member_AL memberAL,
                        List<String> songList, Map<String, List<Position>> requiredSessions){
        state.confirmed.get(song).put(sess, memberAL);
        List<String> assigned = state.assignedSongList.get(memberAL.$USER_code);
        if (!assigned.contains(song)){
            assigned.add(song);
        }

        for (String otherSong : songList){
            if (otherSong.equals(song)) continue;
            if (state.confirmed.get(otherSong).containsKey(sess)) continue;

            for (Position otherSess : state.candidates.get(otherSong).keySet()){
                List<Member_AL> otherJoin = state.candidates.get(otherSong)
                        .getOrDefault(otherSess, new ArrayList<>());
                if (!otherJoin.contains(memberAL)) continue;
                boolean conflict = hasTimeConflictSimple(song, otherSong, memberAL, state, null);
                if (conflict) {
                    otherJoin.remove(memberAL);
                    System.out.printf("%s, 시간겹침으로 '%s' 후보에서 제거%n", memberAL.$USER_name, otherSong);
                }
            }
        }
    }

    //멤버의 해당 곡 희망 순위 반환 (없으면 -1)
    public int getChoiceRank(Member_AL memberAL, String song){
        for(Map.Entry<Integer, String> entry : memberAL.choice.entrySet()){
            if (entry.getValue().equals(song)) return entry.getKey();
        }
        return -1;
    }

    // 드럼 우선 배정
    public void assignDrumFirst(AssignmentState state, List<String> songList,
                                Map<String, List<Position>> requiredSessions,
                                Map<Integer, Member_AL> memberList){
        for (String song : songList){
            List<Position> needed = requiredSessions.getOrDefault(song, List.of());
            if (!needed.contains(Position.DRUM)) continue;
            if (state.confirmed.get(song).containsKey(Position.DRUM)) continue;
            List<Member_AL> drumCands = state.candidates.get(song)
                    .getOrDefault(Position.DRUM, List.of());
            if (drumCands.isEmpty()) continue;

            Member_AL chosen = null;
            for (int rank=1; rank<= MAX_CHOICE; rank++){
                final int currentRank = rank;
                List<Member_AL> rankCands = drumCands.stream()
                        .filter(m -> getChoiceRank(m, song)==currentRank)
                        .sorted(Comparator.comparingInt(Member_AL::totalAvailableBits).reversed())
                        .collect(Collectors.toList());
                if (!rankCands.isEmpty()){
                    chosen = rankCands.get(0);
                    break;
                }
            }
            if (chosen == null){
                chosen = memberList.values().stream()
                        .filter(m -> m.session.contains(Position.DRUM))
                        .max(Comparator.comparingInt(Member_AL::totalAvailableBits))
                        .orElse(null);
            }
            if (chosen != null) {
                confirm(state, song, Position.DRUM, chosen, songList, requiredSessions);
            }
        }
    }

    // --- Step1 단일 지원자 확정 -> 연쇄 제거 ---
    public void propagate(AssignmentState state, List<String> songList, Map<String, List<Position>> requiredSessions){
        boolean changed = true;
        while (changed){
            changed = false;
            for (String song : songList){
                for (Position sess : requiredSessions.getOrDefault(song, List.of())){
                    if(state.confirmed.get(song).containsKey(sess)) continue;
                    List<Member_AL> cands = state.candidates.get(song).getOrDefault(sess, List.of());
                    if (cands.size()==1){
                        confirm(state, song, sess, cands.get(0), songList, requiredSessions);
                        changed = true;
                    }
                }
            }
        }
    }

    // 결과 출력
    public void printTeamResult(AssignmentState state, List<String> songList){
        System.out.println("==== 배정 결과 ====");
        for (String song : songList){
            String status = state.excluded.contains(song)? "[제외후보]" : "";
            System.out.printf("%s%s (공통 가능일: %d일)%n",
                    song, status, state.songScore.getOrDefault(song,0));
            Map<Position, Member_AL> conf = state.confirmed.get(song);
            if (conf.isEmpty()){
                System.out.println("배정 없음");
            } else {
                conf.forEach((sess,m)->
                        System.out.printf("%-4s : %s%n", sess, m.$USER_name));
            }
            System.out.println();
        }
    }

    // ============================
    // Step 2: 합주 스케줄 생성
    // ============================


    // Algorithm.java 상단의 TARGET_PRACTICE_COUNT 상수로 횟수 설정
    public List<PracticeSchedule> generateSchedules(AssignmentState state) {
        return generateSchedules(state, TARGET_PRACTICE_COUNT);
    }

    public List<PracticeSchedule> generateSchedules(AssignmentState state, int targetCount) {
        List<PracticeSchedule> schedules = new ArrayList<>();
        // 날짜별 합주실 점유 비트마스크
        Map<LocalDate, Long> roomOccupied = new HashMap<>();
        // 배정 대상 곡 목록 (제외 곡 제외)
        List<String> activeSongs = state.confirmed.keySet().stream()
                .filter(song -> !state.excluded.contains(song))
                .collect(Collectors.toList());
        // 팀별 배정 횟수 추적
        Map<String, Integer> assignedCount = new HashMap<>();
        for (String song : activeSongs) {
            assignedCount.put(song, 0);
        }

        // == 라운드로빈 반복 ==
        // 2단계로 진행:
        //   1단계) 모든 팀이 최소 targetCount번 채울 때까지 미달 팀만 대상으로 배정
        //   2단계) 모든 팀이 목표 달성 후, 슬롯이 남아있는 팀은 계속 추가 배정
        // 슬롯 소진된 팀은 더 이상 시도하지 않음 (무한루프 방지)
        Set<String> exhausted = new HashSet<>();
        boolean anyAssigned = true;
        while (anyAssigned) {
            anyAssigned = false;

            // 미달 팀이 하나라도 있으면 미달 팀만, 모두 달성하면 전체 대상
            boolean hasUnderTarget = activeSongs.stream()
                    .anyMatch(song -> !exhausted.contains(song) && assignedCount.get(song) < targetCount);

            List<String> roundCandidates = activeSongs.stream()
                    .filter(song -> !exhausted.contains(song))
                    .filter(song -> !hasUnderTarget || assignedCount.get(song) < targetCount)
                    .collect(Collectors.toList());

            if (roundCandidates.isEmpty()) break;

            // 여유 없는 팀 우선: roomOccupied 반영 후 남은 가능 비트 수 오름차순 정렬
            roundCandidates.sort(Comparator.comparingInt(song -> {
                List<Member_AL> members = new ArrayList<>(state.confirmed.get(song).values());
                Map<LocalDate, Long> common = computeCommonTime(members);
                return common.entrySet().stream()
                        .mapToInt(e -> Long.bitCount(
                                e.getValue() & ~roomOccupied.getOrDefault(e.getKey(), 0L)))
                        .sum();
            }));

            for (String song : roundCandidates) {
                List<Member_AL> teamMembers = new ArrayList<>(state.confirmed.get(song).values());
                Map<LocalDate, Long> commonTimes = computeCommonTime(teamMembers);

                // 날짜를 합주실 점유 후 남은 비트 수 내림차순으로 정렬
                List<Map.Entry<LocalDate, Long>> sortedDates = new ArrayList<>(commonTimes.entrySet());
                sortedDates.sort((a, b) -> {
                    int bitsA = Long.bitCount(a.getValue() & ~roomOccupied.getOrDefault(a.getKey(), 0L));
                    int bitsB = Long.bitCount(b.getValue() & ~roomOccupied.getOrDefault(b.getKey(), 0L));
                    return Integer.compare(bitsB, bitsA);
                });

                // 1시간 슬롯을 확보할 수 있는 날짜 탐색
                PracticeSchedule bestSlot = null;

                for (Map.Entry<LocalDate, Long> entry : sortedDates) {
                    LocalDate date = entry.getKey();
                    long available = entry.getValue() & ~roomOccupied.getOrDefault(date, 0L);
                    if (available == 0L) continue;

                    PracticeSchedule slot = findBestTimeSlot(song, date, available);
                    if (slot != null) {
                        bestSlot = slot;
                        break;
                    }
                }

                // 슬롯 확정 → 합주실 점유 즉시 갱신
                if (bestSlot != null) {
                    schedules.add(bestSlot);
                    assignedCount.merge(song, 1, Integer::sum);
                    long slotMask = TimeUtils.createBitmask(bestSlot.getStartTime(), bestSlot.getEndTime());
                    roomOccupied.merge(bestSlot.getDate(), slotMask, (a, b) -> a | b);
                    anyAssigned = true;

                } else {
                    // 가능한 슬롯 없음 → exhausted 처리 (더 이상 시도 X)
                    exhausted.add(song);
                }
            }
        }

        return schedules;
    }

    // 비트마스크에서 가장 이른 1시간 연속 구간 추출
    public PracticeSchedule findBestTimeSlot(String song, LocalDate date, long mask) {
        for (int start = 0; start <= 48 - PRACTICE_SLOT_BITS; start++) {
            long slotMask = ((1L << PRACTICE_SLOT_BITS) - 1) << start;
            if ((mask & slotMask) == slotMask) {
                LocalTime startTime = TimeUtils.indexToTime(start);
                LocalTime endTime = TimeUtils.indexToTime(start + PRACTICE_SLOT_BITS);
                return new PracticeSchedule(song, date, startTime, endTime);
            }
        }

        return null;
    }

    public class AssignmentState {
        // (곡 -> (세션 -> 후보 멤버 list))
        Map<String, Map<Position, List<Member_AL>>> candidates = new HashMap<>();
        // (곡 -> (세션 -> 확정 멤버))
        Map<String, Map<Position, Member_AL>> confirmed = new HashMap<>();
        // 멤버ID(코드) -> 배정된 곡 목록
        Map<Integer, List<String>> assignedSongList = new HashMap<>();
        // 곡 -> 공통 가능일 수
        Map<String, Integer> songScore = new HashMap<>();
        // 제외 후보 곡 목록
        Set<String> excluded = new HashSet<>();

        public Map<String, Map<Position, List<Member_AL>>> getCandidates_AL() { return candidates; }
        public Map<String, Map<Position, Member_AL>> getConfirmed_AL() { return confirmed; }
        public Map<Integer, List<String>> getAssignedSongList_AL() { return assignedSongList; }
        public Map<String, Integer> getSongScore_AL() { return songScore; }
        public Set<String> getExcluded_AL() { return excluded; }
    }

    private static class TeamCombinationSearch {
        private final String song;
        private final List<Position> positions;
        private TeamCombination best;

        private TeamCombinationSearch(String song, List<Position> positions) {
            this.song = song;
            this.positions = positions;
        }
    }

    public static class TeamCombination implements Comparable<TeamCombination> {
        private final Map<Position, Member_AL> membersByPosition;
        private final int commonDays;
        private final int commonBits;
        private final int preferenceScore;

        private TeamCombination(Map<Position, Member_AL> membersByPosition,
                                int commonDays,
                                int commonBits,
                                int preferenceScore) {
            this.membersByPosition = membersByPosition;
            this.commonDays = commonDays;
            this.commonBits = commonBits;
            this.preferenceScore = preferenceScore;
        }

        @Override
        public int compareTo(TeamCombination other) {
            return Comparator
                    .comparingInt((TeamCombination combination) -> combination.commonDays)
                    .thenComparingInt(combination -> combination.commonBits)
                    .thenComparingInt(combination -> combination.preferenceScore)
                    .compare(this, other);
        }
    }
}

/*
[Step1] 합주 팀 편성
- [Phase 1] 제약 전파 - 선택지 줄이기
   각 (곡, 세션) 슬롯에 가능한 멤버 목록 계산
    -> 선택지 1명인 슬롯부터 확정 (강제 배정)
    -> 확정되면 그 멤버의 다른 슬롯 선택지에서 제거
    -> 반복 (연쇄적으로 줄어듦)
- [Phase 2] Bottleneck 우선 그리디
  남은 미확정 슬롯을 선택지 수 오름차순 정렬
    -> 선택지 가장 적은 슬롯부터
    -> 현재 배정 멤버들과 공통 가능일 가장 많은 후보 선택
    -> 확정 후 다시 제약 전파
- [Phase 3] 공통 가능일 threshold 검사
  곡별 배정 완료 후 공통 가능일 < 4 → 제외 후보 표시
- [Phase 4] 미지원 세션 채우기
  제외 후보 아닌 곡의 빈 슬롯
    -> 해당 세션 가능 & 가능일 많은 멤버 순으로 채움
- [Phase 5] 기획자 수동 조정 후 확정

[Step2] 합주 스케줄 생성
  - 라운드로빈 반복: 모든 팀 1회씩 배정 → 다음 라운드 (TARGET_PRACTICE_COUNT회 목표)
  - 각 라운드 내 배정 순서: 합주실 점유 반영 후 남은 슬롯이 적은 팀(여유 없는 팀) 우선
  - 합주실 충돌 방지: roomOccupied 비트마스크로 날짜별 점유 추적, 슬롯 확정 즉시 갱신
  - 슬롯 없으면 조기 종료 (해당 팀 카운트를 targetCount로 고정)
 */
