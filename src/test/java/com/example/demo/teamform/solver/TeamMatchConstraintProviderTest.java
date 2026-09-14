package com.example.demo.teamform.solver;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import com.example.demo.teamform.solver.domain.MatchMember;
import com.example.demo.teamform.solver.domain.MatchTeam;
import com.example.demo.teamform.solver.domain.MatchTimeSlot;
import com.example.demo.teamform.solver.domain.TeamMatchPlan;
import com.example.demo.teamform.solver.domain.TeamSchedule;
import com.example.demo.teamform.solver.domain.TeamSeat;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class TeamMatchConstraintProviderTest {

    private static final MatchTeam TEAM_A = new MatchTeam(0, "A팀");
    private static final MatchTeam TEAM_B = new MatchTeam(1, "B팀");
    private static final MatchTimeSlot MON_1900 = new MatchTimeSlot("월", "19:00");
    private static final MatchTimeSlot TUE_1900 = new MatchTimeSlot("화", "19:00");

    private final ConstraintVerifier<TeamMatchConstraintProvider, TeamMatchPlan> verifier =
            ConstraintVerifier.build(
                    new TeamMatchConstraintProvider(),
                    TeamMatchPlan.class,
                    TeamSeat.class,
                    TeamSchedule.class
            );

    @Test
    void h1_rejectsMemberWithoutThatSession() {
        MatchMember drummer = member(1, "드럼", Map.of("D", 1), Set.of(MON_1900));
        TeamSeat vocalSeat = seat("0-V", TEAM_A, "V", drummer);

        verifier.verifyThat(TeamMatchConstraintProvider::sessionMustBeInMemberPreference)
                .given(vocalSeat)
                .penalizesBy(1);
    }

    @Test
    void h2_allowsVocalPlusGuitarWhenVocalRanksFirst() {
        MatchMember member = member(1, "보컬기타", Map.of("V", 1, "EG1", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", member);
        TeamSeat guitar = seat("0-EG1", TEAM_A, "EG1", member);

        verifier.verifyThat(TeamMatchConstraintProvider::atMostTwoSlotsInSameTeam)
                .given(vocal, guitar)
                .penalizesBy(0);
        verifier.verifyThat(TeamMatchConstraintProvider::doubleUpMustIncludeVocal)
                .given(vocal, guitar)
                .penalizesBy(0);
        verifier.verifyThat(TeamMatchConstraintProvider::vocalMustOutrankInstrumentToDoubleUp)
                .given(vocal, guitar)
                .penalizesBy(0);
    }

    @Test
    void h2_allowsVocalPlusKeyboardWhenVocalRanksFirst() {
        MatchMember member = member(1, "보컬키보드", Map.of("V", 1, "K", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", member);
        TeamSeat keyboard = seat("0-K", TEAM_A, "K", member);

        verifier.verifyThat(TeamMatchConstraintProvider::doubleUpMustIncludeVocal)
                .given(vocal, keyboard)
                .penalizesBy(0);
        verifier.verifyThat(TeamMatchConstraintProvider::vocalMustOutrankInstrumentToDoubleUp)
                .given(vocal, keyboard)
                .penalizesBy(0);
    }

    @Test
    void h2_rejectsVocalPlusGuitarWhenGuitarRanksFirst() {
        MatchMember member = member(1, "기타주", Map.of("EG1", 1, "V", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", member);
        TeamSeat guitar = seat("0-EG1", TEAM_A, "EG1", member);

        verifier.verifyThat(TeamMatchConstraintProvider::vocalMustOutrankInstrumentToDoubleUp)
                .given(vocal, guitar)
                .penalizesBy(1);
    }

    @Test
    void h2_allowsVocalPlusDrumEvenIfRanksAreNotConsecutive() {
        MatchMember member = member(1, "보컬드럼", Map.of("V", 1, "EG1", 2, "D", 3), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", member);
        TeamSeat drum = seat("0-D", TEAM_A, "D", member);

        verifier.verifyThat(TeamMatchConstraintProvider::vocalMustOutrankInstrumentToDoubleUp)
                .given(vocal, drum)
                .penalizesBy(0);
    }

    @Test
    void h2_rejectsTwoGuitarSeatsWithoutVocal() {
        MatchMember member = member(1, "기타만", Map.of("EG1", 1, "EG2", 2), Set.of(MON_1900));
        TeamSeat eg1 = seat("0-EG1", TEAM_A, "EG1", member);
        TeamSeat eg2 = seat("0-EG2", TEAM_A, "EG2", member);

        verifier.verifyThat(TeamMatchConstraintProvider::doubleUpMustIncludeVocal)
                .given(eg1, eg2)
                .penalizesBy(1);
    }

    @Test
    void h2_rejectsThreeSeatsInSameTeam() {
        MatchMember member = member(1, "3자리", Map.of("V", 1, "EG1", 2, "K", 3), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", member);
        TeamSeat guitar = seat("0-EG1", TEAM_A, "EG1", member);
        TeamSeat keyboard = seat("0-K", TEAM_A, "K", member);

        verifier.verifyThat(TeamMatchConstraintProvider::atMostTwoSlotsInSameTeam)
                .given(vocal, guitar, keyboard)
                .penalizesBy(1);
    }

    @Test
    void h3_rejectsMoreTeamsThanMaxTeamCount() {
        MatchMember member = member(1, "1팀만", 1, Map.of("V", 1), Set.of(MON_1900, TUE_1900));
        TeamSeat teamA = seat("0-V", TEAM_A, "V", member);
        TeamSeat teamB = seat("1-V", TEAM_B, "V", member);

        verifier.verifyThat(TeamMatchConstraintProvider::maxTeamCountExceeded)
                .given(teamA, teamB)
                .penalizesBy(1);
    }

    @Test
    void h4_rejectsUnavailableRehearsalSlot() {
        MatchMember member = member(1, "화요일만", Map.of("V", 1), Set.of(TUE_1900));
        TeamSeat seat = seat("0-V", TEAM_A, "V", member);
        TeamSchedule schedule = schedule(TEAM_A, MON_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::teamTimeSlotUnavailableForMember)
                .given(seat, schedule)
                .penalizesBy(1);
    }

    @Test
    void h5_rejectsSameTimeSlotAcrossTwoTeams() {
        MatchMember member = member(1, "두팀", 2, Map.of("V", 1), Set.of(MON_1900));
        TeamSeat teamA = seat("0-V", TEAM_A, "V", member);
        TeamSeat teamB = seat("1-V", TEAM_B, "V", member);
        TeamSchedule scheduleA = schedule(TEAM_A, MON_1900);
        TeamSchedule scheduleB = schedule(TEAM_B, MON_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::memberDoubleBookedAcrossTeams)
                .given(teamA, teamB, scheduleA, scheduleB)
                .penalizesBy(1);
    }

    @Test
    void h5_allowsSameMemberOnTwoTeamsAtDifferentTimes() {
        MatchMember member = member(1, "두팀", 2, Map.of("V", 1), Set.of(MON_1900, TUE_1900));
        TeamSeat teamA = seat("0-V", TEAM_A, "V", member);
        TeamSeat teamB = seat("1-V", TEAM_B, "V", member);
        TeamSchedule scheduleA = schedule(TEAM_A, MON_1900);
        TeamSchedule scheduleB = schedule(TEAM_B, TUE_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::memberDoubleBookedAcrossTeams)
                .given(teamA, teamB, scheduleA, scheduleB)
                .penalizesBy(0);
    }

    @Test
    void h5_allowsSameTeamVocalInstrumentDoubleUpAtOneTime() {
        MatchMember member = member(1, "겸임", Map.of("V", 1, "EG1", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", member);
        TeamSeat guitar = seat("0-EG1", TEAM_A, "EG1", member);
        TeamSchedule schedule = schedule(TEAM_A, MON_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::memberDoubleBookedAcrossTeams)
                .given(vocal, guitar, schedule)
                .penalizesBy(0);
    }

    @Test
    void s1_penalizesUnfilledSeat() {
        TeamSeat empty = new TeamSeat("0-V", TEAM_A, "V");

        verifier.verifyThat(TeamMatchConstraintProvider::unfilledSlotPenalty)
                .given(empty)
                .penalizesBy(1);
    }

    private static MatchMember member(long id, String name, Map<String, Integer> ranks, Set<MatchTimeSlot> slots) {
        return member(id, name, 2, ranks, slots);
    }

    private static MatchMember member(
            long id,
            String name,
            int maxTeams,
            Map<String, Integer> ranks,
            Set<MatchTimeSlot> slots
    ) {
        Map<String, String> levels = new LinkedHashMap<>();
        Map<String, Integer> priorities = new LinkedHashMap<>();
        ranks.forEach((position, rank) -> {
            levels.put(position, "중");
            priorities.put(position, rank);
        });
        return new MatchMember(id, name, maxTeams, levels, priorities, slots);
    }

    private static TeamSeat seat(String id, MatchTeam team, String position, MatchMember member) {
        TeamSeat seat = new TeamSeat(id, team, position);
        seat.setMember(member);
        return seat;
    }

    private static TeamSchedule schedule(MatchTeam team, MatchTimeSlot timeSlot) {
        TeamSchedule schedule = new TeamSchedule("schedule-" + team.getTeamIndex(), team);
        schedule.setTimeSlot(timeSlot);
        return schedule;
    }
}
