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
    private static final MatchTimeSlot MON_1000 = new MatchTimeSlot("월", "10:00");
    private static final MatchTimeSlot MON_1230 = new MatchTimeSlot("월", "12:30");
    private static final MatchTimeSlot TUE_1900 = new MatchTimeSlot("화", "19:00");
    private static final MatchTimeSlot WED_1900 = new MatchTimeSlot("수", "19:00");

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
    void h5_rejectsOverlappingSlotWhenEachTeamHasTwoRehearsals() {
        MatchMember member = member(1, "두팀", 2, Map.of("V", 1), Set.of(MON_1900, TUE_1900, WED_1900));
        TeamSeat teamA = seat("0-V", TEAM_A, "V", member);
        TeamSeat teamB = seat("1-V", TEAM_B, "V", member);
        TeamSchedule scheduleA1 = schedule(TEAM_A, 1, MON_1900);
        TeamSchedule scheduleA2 = schedule(TEAM_A, 2, WED_1900);
        TeamSchedule scheduleB1 = schedule(TEAM_B, 1, TUE_1900);
        TeamSchedule scheduleB2 = schedule(TEAM_B, 2, MON_1900); // overlaps A1

        verifier.verifyThat(TeamMatchConstraintProvider::memberDoubleBookedAcrossTeams)
                .given(teamA, teamB, scheduleA1, scheduleA2, scheduleB1, scheduleB2)
                .penalizesBy(1);
    }

    @Test
    void h5_allowsSameDayDifferentStartsInsideSharedWindow() {
        MatchTimeSlot mon1700 = new MatchTimeSlot("월", "17:00");
        MatchTimeSlot mon1900 = new MatchTimeSlot("월", "19:00");
        MatchMember member = member(
                1,
                "두팀",
                2,
                Map.of("V", 1),
                Set.of(mon1700, mon1900, TUE_1900, WED_1900)
        );
        TeamSeat teamA = seat("0-V1", TEAM_A, "V1", member);
        TeamSeat teamB = seat("1-V1", TEAM_B, "V1", member);
        TeamSchedule scheduleA = schedule(TEAM_A, mon1700);
        TeamSchedule scheduleB = schedule(TEAM_B, mon1900);

        verifier.verifyThat(TeamMatchConstraintProvider::memberDoubleBookedAcrossTeams)
                .given(teamA, teamB, scheduleA, scheduleB)
                .penalizesBy(0);
    }

    @Test
    void s8_rewardsSameDayStaggerForMultiTeamMember() {
        MatchTimeSlot mon1700 = new MatchTimeSlot("월", "17:00");
        MatchTimeSlot mon1900 = new MatchTimeSlot("월", "19:00");
        MatchMember member = member(1, "두팀", 2, Map.of("V", 1), Set.of(mon1700, mon1900));
        TeamSeat teamA = seat("0-V1", TEAM_A, "V1", member);
        TeamSeat teamB = seat("1-V1", TEAM_B, "V1", member);
        TeamSchedule scheduleA = schedule(TEAM_A, mon1700);
        TeamSchedule scheduleB = schedule(TEAM_B, mon1900);

        // 2 hours apart → 4 half-hours × 4 soft
        verifier.verifyThat(TeamMatchConstraintProvider::preferStaggeredScheduleForMultiTeamMembers)
                .given(teamA, teamB, scheduleA, scheduleB)
                .rewardsWith(4 * TeamMatchConstraintProvider.MULTI_TEAM_STAGGER_REWARD_PER_HALF_HOUR);
    }

    @Test
    void h6_rejectsSameWeekdayForTwoRehearsals() {
        TeamSchedule first = schedule(TEAM_A, 1, MON_1900);
        TeamSchedule second = schedule(TEAM_A, 2, new MatchTimeSlot("월", "20:00"));

        verifier.verifyThat(TeamMatchConstraintProvider::teamRehearsalDaysMustDiffer)
                .given(first, second)
                .penalizesBy(1);
    }

    @Test
    void h6_allowsTwoRehearsalsOnDifferentDays() {
        TeamSchedule first = schedule(TEAM_A, 1, MON_1900);
        TeamSchedule second = schedule(TEAM_A, 2, TUE_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::teamRehearsalDaysMustDiffer)
                .given(first, second)
                .penalizesBy(0);
    }

    @Test
    void h2_rejectsTwoVocalSeatsForSameMember() {
        MatchMember member = member(1, "보컬둘", Map.of("V", 1), Set.of(MON_1900));
        TeamSeat v1 = seat("0-V1", TEAM_A, "V1", member);
        TeamSeat v2 = seat("0-V2", TEAM_A, "V2", member);

        verifier.verifyThat(TeamMatchConstraintProvider::cannotOccupyTwoVocalSeats)
                .given(v1, v2)
                .penalizesBy(1);
    }

    @Test
    void h2_allowsVocalPlusGuitarOnV1Seat() {
        MatchMember member = member(1, "보컬기타", Map.of("V", 1, "EG1", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V1", TEAM_A, "V1", member);
        TeamSeat guitar = seat("0-EG1", TEAM_A, "EG1", member);

        verifier.verifyThat(TeamMatchConstraintProvider::doubleUpMustIncludeVocal)
                .given(vocal, guitar)
                .penalizesBy(0);
        verifier.verifyThat(TeamMatchConstraintProvider::vocalMustOutrankInstrumentToDoubleUp)
                .given(vocal, guitar)
                .penalizesBy(0);
        verifier.verifyThat(TeamMatchConstraintProvider::cannotOccupyTwoVocalSeats)
                .given(vocal, guitar)
                .penalizesBy(0);
    }

    @Test
    void s1c_penalizesAssignedMemberMissingFirstChoice() {
        MatchMember member = member(1, "드럼1순위", Map.of("D", 1, "V", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V1", TEAM_A, "V1", member);

        verifier.verifyThat(TeamMatchConstraintProvider::assignedWithoutFirstChoicePenalty)
                .given(member, vocal)
                .penalizesBy(1);
    }

    @Test
    void s1c_allowsAssignedMemberOnFirstChoice() {
        MatchMember member = member(1, "보컬1순위", Map.of("V", 1, "D", 2), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V1", TEAM_A, "V1", member);

        verifier.verifyThat(TeamMatchConstraintProvider::assignedWithoutFirstChoicePenalty)
                .given(member, vocal)
                .penalizesBy(0);
    }

    @Test
    void h7_rejectsEmptyDrumSeat() {
        TeamSeat emptyDrum = new TeamSeat("0-D", TEAM_A, "D");

        verifier.verifyThat(TeamMatchConstraintProvider::drumSeatMustBeFilled)
                .given(emptyDrum)
                .penalizesBy(1);
    }

    @Test
    void h7_allowsFilledDrumSeat() {
        MatchMember drummer = member(1, "드럼", Map.of("D", 1), Set.of(MON_1900));
        TeamSeat drum = seat("0-D", TEAM_A, "D", drummer);

        verifier.verifyThat(TeamMatchConstraintProvider::drumSeatMustBeFilled)
                .given(drum)
                .penalizesBy(0);
    }

    @Test
    void s1_penalizesUnfilledSeat() {
        TeamSeat empty = new TeamSeat("0-D", TEAM_A, "D");

        verifier.verifyThat(TeamMatchConstraintProvider::unfilledSlotPenalty)
                .given(empty)
                .penalizesBy(TeamMatchConstraintProvider.UNFILLED_SLOT_PENALTY);
    }

    @Test
    void s1_penalizesEmptyV1MoreThanV2() {
        TeamSeat emptyV1 = new TeamSeat("0-V1", TEAM_A, "V1");
        TeamSeat emptyV2 = new TeamSeat("0-V2", TEAM_A, "V2");

        verifier.verifyThat(TeamMatchConstraintProvider::unfilledSlotPenalty)
                .given(emptyV1)
                .penalizesBy(TeamMatchConstraintProvider.UNFILLED_V1_PENALTY);
        verifier.verifyThat(TeamMatchConstraintProvider::unfilledSlotPenalty)
                .given(emptyV2)
                .penalizesBy(TeamMatchConstraintProvider.UNFILLED_V2_PENALTY);
    }

    @Test
    void h2_rejectsV2WithoutV1() {
        MatchMember vocal = member(1, "보컬", Map.of("V", 1), Set.of(MON_1900));
        TeamSeat v2 = seat("0-V2", TEAM_A, "V2", vocal);

        verifier.verifyThat(TeamMatchConstraintProvider::vocalV2RequiresV1Filled)
                .given(v2)
                .penalizesBy(1);
    }

    @Test
    void h2_allowsV2WhenV1Filled() {
        MatchMember first = member(1, "보컬1", Map.of("V", 1), Set.of(MON_1900));
        MatchMember second = member(2, "보컬2", Map.of("V", 1), Set.of(MON_1900));
        TeamSeat v1 = seat("0-V1", TEAM_A, "V1", first);
        TeamSeat v2 = seat("0-V2", TEAM_A, "V2", second);

        verifier.verifyThat(TeamMatchConstraintProvider::vocalV2RequiresV1Filled)
                .given(v1, v2)
                .penalizesBy(0);
    }

    @Test
    void s1d_penalizesV2WhileEmptyV1Remains() {
        MatchMember first = member(1, "보컬1", Map.of("V", 1), Set.of(MON_1900));
        MatchMember second = member(2, "보컬2", Map.of("V", 1), Set.of(MON_1900));
        TeamSeat teamAv1 = seat("0-V1", TEAM_A, "V1", first);
        TeamSeat teamAv2 = seat("0-V2", TEAM_A, "V2", second);
        TeamSeat emptyV1 = new TeamSeat("1-V1", TEAM_B, "V1");

        verifier.verifyThat(TeamMatchConstraintProvider::discourageV2WhileEmptyV1Remains)
                .given(teamAv1, teamAv2, emptyV1)
                .penalizesBy(1);
    }

    @Test
    void s7_rewardsV2ScheduleOverlapWithTeammates() {
        MatchMember vocal = member(1, "보컬", Map.of("V", 1), Set.of(MON_1900, TUE_1900));
        MatchMember drummer = member(2, "드럼", Map.of("D", 1), Set.of(MON_1900, WED_1900));
        TeamSeat v2 = seat("0-V2", TEAM_A, "V2", vocal);
        TeamSeat drum = seat("0-D", TEAM_A, "D", drummer);

        verifier.verifyThat(TeamMatchConstraintProvider::preferV2WithBestScheduleFit)
                .given(v2, drum)
                .rewardsWith(1);
    }

    @Test
    void s1b_penalizesMemberWithNoTeamSeat() {
        MatchMember orphan = member(1, "미배정", Map.of("V", 1), Set.of(MON_1900));
        MatchMember placed = member(2, "배정됨", Map.of("D", 1), Set.of(MON_1900));
        TeamSeat drum = seat("0-D", TEAM_A, "D", placed);

        verifier.verifyThat(TeamMatchConstraintProvider::unassignedMemberPenalty)
                .given(orphan, placed, drum)
                .penalizesBy(1);
    }

    @Test
    void s1b_allowsMemberOnceAssigned() {
        MatchMember placed = member(1, "배정됨", Map.of("V", 1), Set.of(MON_1900));
        TeamSeat vocal = seat("0-V", TEAM_A, "V", placed);

        verifier.verifyThat(TeamMatchConstraintProvider::unassignedMemberPenalty)
                .given(placed, vocal)
                .penalizesBy(0);
    }

    @Test
    void s5_rewardsMorningSlot() {
        TeamSchedule morning = schedule(TEAM_A, MON_1000);

        verifier.verifyThat(TeamMatchConstraintProvider::preferMorningAndLunchSlots)
                .given(morning)
                .rewardsWith(TeamMatchConstraintProvider.MORNING_SLOT_REWARD);
    }

    @Test
    void s5_rewardsLunchSlot() {
        TeamSchedule lunch = schedule(TEAM_A, MON_1230);

        verifier.verifyThat(TeamMatchConstraintProvider::preferMorningAndLunchSlots)
                .given(lunch)
                .rewardsWith(TeamMatchConstraintProvider.LUNCH_SLOT_REWARD);
    }

    @Test
    void s5_doesNotRewardEveningSlot() {
        TeamSchedule evening = schedule(TEAM_A, MON_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::preferMorningAndLunchSlots)
                .given(evening)
                .rewardsWith(0);
    }

    @Test
    void s6_penalizesTwoTeamsOnSameSlot() {
        TeamSchedule teamA = schedule(TEAM_A, MON_1900);
        TeamSchedule teamB = schedule(TEAM_B, MON_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::discourageSameSlotOvercrowd)
                .given(teamA, teamB)
                .penalizesBy(1);
    }

    @Test
    void s6_allowsDifferentSlots() {
        TeamSchedule teamA = schedule(TEAM_A, MON_1900);
        TeamSchedule teamB = schedule(TEAM_B, TUE_1900);

        verifier.verifyThat(TeamMatchConstraintProvider::discourageSameSlotOvercrowd)
                .given(teamA, teamB)
                .penalizesBy(0);
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
        return schedule(team, 1, timeSlot);
    }

    private static TeamSchedule schedule(MatchTeam team, int rehearsalIndex, MatchTimeSlot timeSlot) {
        TeamSchedule schedule = new TeamSchedule(
                "schedule-" + team.getTeamIndex() + "-" + rehearsalIndex,
                team,
                rehearsalIndex
        );
        schedule.setTimeSlot(timeSlot);
        return schedule;
    }
}
