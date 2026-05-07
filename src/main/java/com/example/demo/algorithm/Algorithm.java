package com.example.demo.algorithm;

import com.example.demo.common.type.*;

import java.time.LocalDate;
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
    final int $SETTING_min_common_day_until_deadline    = 4;

    // ($SETTING_min_common_time_per_1play) * 2 = MIN_COMMON_TIMES_BITCOUNT
    // 최소 공통 가능시간(비트) => 비트 2개 = 1시간
    final int MIN_COMMON_TIMES_BITCOUNT   = 2; // 1시간

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

        // --- Phase 0 ---
        // 드럼 지원자 우선순위-가능시간 순으로 확정
        System.out.println("[Phase 0] 드럼 우선 배정");
        assignDrumFirst(state, songList, requiredSessions, memberList);

        // --- Phase 1 ---
        System.out.println("[Phase 1] 지원자 1명인 곳 확정, 불가능한 후보 지우기");
        propagate(state, songList, requiredSessions);

        // --- Phase 2 ---
        // 미확정 슬롯 후보 수 오름차순 수집
        System.out.println("[Phase 2] Bottleneck 그리디 배정");
        List<int[]> unresolved = new ArrayList<>();
        for (int index = 0; index<songList.size(); index++){
            String song = songList.get(index);
            for (Position sess : requiredSessions.getOrDefault(song, List.of())){
                if(!state.confirmed.get(song).containsKey(sess)){
                    unresolved.add(new int[]{index, sess.ordinal()});
                }
            }
        }
        // 모든 슬롯 해결될 때까지 반복
        while (!unresolved.isEmpty()){
            // 후보자 가장 적은 슬롯 찾기
            int hurryIdx = -1;
            int minSize = Integer.MAX_VALUE;
            for (int i = 0; i<unresolved.size(); i++){
                int[] u = unresolved.get(i);
                int size = state.candidates.get(songList.get(u[0]))
                        .getOrDefault(Position.values()[u[1]], List.of()).size();
                if (size < minSize){
                    minSize = size;
                    hurryIdx = i;
                }
            }
            if (hurryIdx == -1) break;
            int[] hurrySlot = unresolved.remove(hurryIdx);

            String song = songList.get(hurrySlot[0]);
            Position sess = Position.values()[hurrySlot[1]];

            // 이미 배정되었다면 스킵
            if (state.confirmed.get(song).containsKey(sess)) continue;

            List<Member_AL> cands = state.candidates.get(song).getOrDefault(sess, new ArrayList<>());
            // 후보가 있다면 적합한 사람 선택
            if (!cands.isEmpty()){
                Member_AL chosen = pickBestCandidate(cands, song, state);
                if (chosen != null) {
                    confirm(state, song, sess, chosen, songList, requiredSessions);
                    propagate(state, songList, requiredSessions);
                }
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

        // --- phase 4 ---
        // 제외 후보 아닌 곡 빈 슬롯 -> 가능시간 많은 멤버로 채우기
        System.out.println("[Phase 4] 미배정 세션 채우기");
        for (String song : songList){
            if (state.excluded.contains(song)) continue;

            for(Position sess : requiredSessions.getOrDefault(song, List.of())){
                if (state.confirmed.get(song).containsKey(sess)) continue;

                memberList.values().stream()
                        .filter(m -> m.session.contains(sess))
                        .filter(m -> {
                            List<String> alreadyIn = state.assignedSongList.get(m.$USER_code);
                            return alreadyIn.stream().noneMatch(assignedSong ->
                                    hasTimeConflict(assignedSong, song, m, state, memberList));
                        })
                        .max(Comparator.comparingInt(Member_AL::totalAvailableBits))
                        .ifPresent(best ->
                                confirm(state, song, sess, best, songList, requiredSessions));
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

    // 후보 중 공통 가능일 최대인 멤버 선택
    public Member_AL pickBestCandidate(List<Member_AL> candidate, String song, AssignmentState state){
        List<Member_AL> current = new ArrayList<>(state.confirmed.get(song).values());
        Member_AL bestCandidate = null;
        int bestScore = -1;

        for (Member_AL cand : candidate){
            // 이미 배정된 곡들과 시간갈등 False이면 통과
            boolean hasConflict = state.assignedSongList.get(cand.$USER_code).stream()
                    .anyMatch(assignedSong -> hasTimeConflict(assignedSong, song, cand, state, null));
            if (hasConflict) continue;

            // 희망 순위 확인
            int rank = getChoiceRank(cand, song);

            // 공통 가능일
            List<Member_AL> temp = new ArrayList<>(current);
            temp.add(cand);
            int commonDays = calcCommonDaysCount(temp);

            // 점수 = 순위 가중치 + 공통 가능일
            int score = (rank > 0? (MAX_CHOICE - rank + 1)*1000 : 0) + commonDays;

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

        // 합주 가능한 날 수 (비교군 곡 시점)
        int possibleDaysCount = 0;

        for (LocalDate date : compare_timeB.keySet()){
            long bitsB = compare_timeB.getOrDefault(date, 0L);
            long bitsA = assinged_timeA.getOrDefault(date, 0L);
            long commonBits = bitsA & bitsB;

            if (commonBits == 0L){
                // A곡과 시간겹침X -> B곡 단독 최소시간 충족시 count+1
                if (Long.bitCount(bitsB) >= MIN_COMMON_TIMES_BITCOUNT){
                    possibleDaysCount++;
                }
            }   else{
                // 겹치는 날 -> 합집합 최소시간*2 이상이면 나눠쓸 수 있음
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

        // 합주 가능한 날 수 (비교군 곡 시점)
        int possibleDaysCount = 0;

        for (LocalDate date : compare_timeB.keySet()){
            long bitsB = compare_timeB.getOrDefault(date, 0L);
            long bitsA = assinged_timeA.getOrDefault(date, 0L);
            long commonBits = bitsA & bitsB;

            if (commonBits == 0L){
                // A곡과 시간겹침X -> B곡 단독 최소시간 충족시 count+1
                if (Long.bitCount(bitsB) >= MIN_COMMON_TIMES_BITCOUNT){
                    possibleDaysCount++;
                }
            }   else{
                // 겹치는 날 -> 합집합 최소시간*2 이상이면 나눠쓸 수 있음
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


        // 배정된 곡과 시간이 겹치는 후보 곡 제거
        for (String otherSong : songList){
            if (otherSong.equals(song)) continue;
            if (state.confirmed.get(otherSong).containsKey(sess)) continue;

            for (Position otherSess : state.candidates.get(otherSong).keySet()){
                List<Member_AL> otherJoin = state.candidates.get(otherSong)
                        .getOrDefault(otherSess, new ArrayList<>());
                if (!otherJoin.contains(memberAL)) continue;
                // 두 곡의 합주 시간이 겹칠 때만 후보에서 제거
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
            // 1. 드럼이 필요한 곡인지, 이미 배정되었는지 확인
            List<Position> needed = requiredSessions.getOrDefault(song, List.of());
            if (!needed.contains(Position.DRUM)) continue;
            if (state.confirmed.get(song).containsKey(Position.DRUM)) continue;
            // 2. 곡의 전체 드럼 후보 리스트 가져오기
            List<Member_AL> drumCands = state.candidates.get(song)
                    .getOrDefault(Position.DRUM, List.of());
            if (drumCands.isEmpty()) continue;

            Member_AL chosen = null;
            // 3. 1순위부터 MAX_CHOICE 순위까지 차례대로 후보 있는지 확인
            for (int rank=1; rank<= MAX_CHOICE; rank++){
                final int currentRank = rank;
                // 해당 순위로 곡을 희망한 드럼 후보들 필터링, 정렬
                List<Member_AL> rankCands = drumCands.stream()
                        .filter(m -> getChoiceRank(m, song)==currentRank)
                        .sorted(Comparator.comparingInt(Member_AL::totalAvailableBits).reversed())
                        .collect(Collectors.toList());
                if (!rankCands.isEmpty()){
                    chosen = rankCands.get(0);
                    break;
                }
            }
            // 4. 지원자 없을 시
            if (chosen == null){
                chosen = memberList.values().stream()
                        .filter(m -> m.session.contains(Position.DRUM))
                        .max(Comparator.comparingInt(Member_AL::totalAvailableBits))
                        .orElse(null);
            }
            // 5. 확정
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

/*
    class SlotAssignment{
        String song;
        $POSITION_Session session;
        Member assignedMember;

        SlotAssignment(String song, $POSITION_Session session){
            this.song = song;
            this.session = session;
        }
    }
*/

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
    }
}

/*
[Phase 1] 제약 전파 - 선택지 줄이기
  각 (곡, 세션) 슬롯에 가능한 멤버 목록 계산
  → 선택지 1명인 슬롯부터 확정 (강제 배정)
  → 확정되면 그 멤버의 다른 슬롯 선택지에서 제거
  → 반복 (연쇄적으로 줄어듦)

[Phase 2] Bottleneck 우선 그리디
  남은 미확정 슬롯을 선택지 수 오름차순 정렬
  → 선택지 가장 적은 슬롯부터
  → 현재 배정 멤버들과 공통 가능일 가장 많은 후보 선택
  → 확정 후 다시 제약 전파

[Phase 3] 공통 가능일 threshold 검사
  곡별 배정 완료 후 공통 가능일 < 4 → 제외 후보 표시

[Phase 4] 미지원 세션 채우기
  제외 후보 아닌 곡의 빈 슬롯
  → 해당 세션 가능 & 가능일 많은 멤버 순으로 채움

[Phase 5] 기획자 수동 조정 후 확정
 */
