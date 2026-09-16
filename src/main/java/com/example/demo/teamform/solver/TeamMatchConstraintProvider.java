package com.example.demo.teamform.solver;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.example.demo.teamform.solver.domain.MatchMember;
import com.example.demo.teamform.solver.domain.MatchTimeSlot;
import com.example.demo.teamform.solver.domain.TeamSchedule;
import com.example.demo.teamform.solver.domain.TeamSeat;

import java.util.function.Function;

/**
 * Hard / soft constraints for team-system matching.
 *
 * <p>Two planning variables are linked here:
 * who sits in each {@link TeamSeat}, and when each {@link TeamSchedule} rehearses.
 * Each open team has two {@link TeamSchedule} entities (weekly rehearsals on different days).
 */
public class TeamMatchConstraintProvider implements ConstraintProvider {

    static final int UNFILLED_SLOT_PENALTY = 50;
    /** Fill primary vocal before considering V2. */
    static final int UNFILLED_V1_PENALTY = 70;
    /** Second vocal is rarely used; empty V2 is cheap, filled V2 is discouraged unless needed. */
    static final int UNFILLED_V2_PENALTY = 8;
    static final int FILLED_V2_PENALTY = 100;
    static final int DOUBLE_UP_PENALTY = 20;
    /** Higher than a single empty seat so coverage beats polishing full teams. */
    static final int UNASSIGNED_MEMBER_PENALTY = 120;
    /** Prefer giving assigned members their 1순위 session when possible. */
    static final int MISSING_FIRST_CHOICE_PENALTY = 70;
    /** Strongly avoid seating V2 while any open V1 is still empty. */
    static final int V2_BEFORE_V1_PENALTY = 90;
    static final int MORNING_SLOT_REWARD = 4;
    static final int LUNCH_SLOT_REWARD = 3;
    /** Soft push to spread teams off crowded evening slots into morning/lunch. */
    static final int SAME_SLOT_OVERCROWD_PENALTY = 8;
    /**
     * Spread rehearsals across Mon–Fri. Soft-cap is ~even for 8 teams × 2 sessions / 5 days.
     */
    static final int SAME_DAY_OVERCROWD_PENALTY = 12;
    static final int WEEKDAY_TEAM_SOFT_CAP = 3;
    /** Per overlapping availability slot with a teammate (capped). */
    static final int V2_SCHEDULE_FIT_REWARD_CAP = 15;
    /**
     * When one person is on multiple teams, prefer staggering their assigned
     * rehearsal starts (esp. within the same weekday window like 월 17–21).
     */
    static final int MULTI_TEAM_STAGGER_REWARD_PER_HALF_HOUR = 4;
    static final int MULTI_TEAM_DIFFERENT_DAY_REWARD = 2;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                sessionMustBeInMemberPreference(factory),
                atMostTwoSlotsInSameTeam(factory),
                doubleUpMustIncludeVocal(factory),
                vocalMustOutrankInstrumentToDoubleUp(factory),
                cannotOccupyTwoVocalSeats(factory),
                vocalV2RequiresV1Filled(factory),
                vocalMemberOnlyOneTeam(factory),
                maxTeamCountExceeded(factory),
                teamTimeSlotUnavailableForMember(factory),
                memberDoubleBookedAcrossTeams(factory),
                teamRehearsalDaysMustDiffer(factory),
                drumSeatMustBeFilled(factory),
                unfilledSlotPenalty(factory),
                unassignedMemberPenalty(factory),
                assignedWithoutFirstChoicePenalty(factory),
                discourageV2WhileEmptyV1Remains(factory),
                discourageFilledV2(factory),
                preferHigherRankSession(factory),
                preferHigherSkillLevel(factory),
                discourageDoubleUp(factory),
                preferMorningAndLunchSlots(factory),
                discourageSameSlotOvercrowd(factory),
                discourageWeekdayOvercrowd(factory),
                preferV2WithBestScheduleFit(factory),
                preferStaggeredScheduleForMultiTeamMembers(factory)
        };
    }

    /** H1: assigned member must have chosen this session. */
    Constraint sessionMustBeInMemberPreference(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .filter(seat -> !seat.getMember().canPlay(seat.getPosition()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Session must be in member preference");
    }

    /** H2-1: at most two seats per member in the same team. */
    Constraint atMostTwoSlotsInSameTeam(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(
                        TeamSeat::getTeamIndex,
                        TeamSeat::getMember,
                        ConstraintCollectors.count()
                )
                .filter((teamIndex, member, count) -> count > 2)
                .penalize(
                        HardSoftScore.ONE_HARD,
                        (teamIndex, member, count) -> (int) (count - 2)
                )
                .asConstraint("At most two slots in the same team");
    }

    /** H2-2: a double-up pair must include vocal; two instruments cannot play at once. */
    Constraint doubleUpMustIncludeVocal(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.equal(TeamSeat::getMember)
                )
                .filter((left, right) -> left.getMember() != null)
                .filter((left, right) -> left.isInstrument() && right.isInstrument())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Double-up must include vocal");
    }

    /** H2-3: vocal must outrank the instrument in the member's preference. */
    Constraint vocalMustOutrankInstrumentToDoubleUp(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.equal(TeamSeat::getMember)
                )
                .filter((left, right) -> left.getMember() != null)
                .filter((left, right) -> left.isVocal() || right.isVocal())
                .filter((left, right) -> {
                    String instrument = left.isVocal() ? right.getPosition() : left.getPosition();
                    return !left.getMember().canDoubleUpVocalWith(instrument);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Vocal must outrank instrument to double-up");
    }

    /** H2-4: a member cannot take both vocal seats (V1+V2) on the same team. */
    Constraint cannotOccupyTwoVocalSeats(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.equal(TeamSeat::getMember)
                )
                .filter((left, right) -> left.getMember() != null)
                .filter((left, right) -> left.isVocal() && right.isVocal())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Cannot occupy two vocal seats");
    }

    /** H2-5: V2 may be used only after that team's V1 is filled. */
    Constraint vocalV2RequiresV1Filled(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(seat -> "V2".equals(seat.getPosition()) && seat.isAssigned())
                .ifNotExists(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.filtering((v2, other) ->
                                "V1".equals(other.getPosition()) && other.isAssigned())
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Vocal V2 requires V1 filled");
    }

    /**
     * H2-6: anyone seated as vocal (V1/V2) may belong to only one team.
     * Same-team vocal+instrument double-up is still allowed.
     */
    Constraint vocalMemberOnlyOneTeam(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(seat -> seat.isAssigned() && seat.isVocal())
                .join(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getMember, TeamSeat::getMember)
                )
                .filter((vocalSeat, otherSeat) -> otherSeat.isAssigned()
                        && otherSeat.getTeamIndex() != vocalSeat.getTeamIndex())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Vocal member only one team");
    }

    /** H3: distinct teams per member cannot exceed maxTeams. */
    Constraint maxTeamCountExceeded(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(
                        TeamSeat::getMember,
                        ConstraintCollectors.countDistinct(TeamSeat::getTeamIndex)
                )
                .filter((member, teamCount) -> teamCount > member.getMaxTeams())
                .penalize(
                        HardSoftScore.ONE_HARD,
                        (member, teamCount) -> (int) (teamCount - member.getMaxTeams())
                )
                .asConstraint("Max team count exceeded");
    }

    /**
     * H4: every assigned member must be free at every rehearsal slot of the team
     * (both weekly rehearsals).
     */
    Constraint teamTimeSlotUnavailableForMember(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .join(
                        TeamSchedule.class,
                        Joiners.equal(TeamSeat::getTeamIndex, TeamSchedule::getTeamIndex)
                )
                .filter((seat, schedule) -> schedule.getTimeSlot() != null
                        && !seat.getMember().isAvailableAt(schedule.getTimeSlot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Team time slot unavailable for member");
    }

    /**
     * H5: a member on multiple teams cannot share an <em>exact assigned</em>
     * rehearsal start across those teams.
     *
     * <p>Overlapping availability windows are fine. Example: A팀 and B팀 both
     * have common availability 월 17:00–21:00 — the member may join both if
     * actual starts differ (A 월 17:00, B 월 19:00). Only identical starts
     * (both 월 17:00) are hard-forbidden.
     */
    Constraint memberDoubleBookedAcrossTeams(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .join(
                        TeamSchedule.class,
                        Joiners.equal(TeamSeat::getTeamIndex, TeamSchedule::getTeamIndex)
                )
                .filter((seat, schedule) -> schedule.getTimeSlot() != null)
                .join(
                        TeamSeat.class,
                        Joiners.equal((seat, schedule) -> seat.getMember(), TeamSeat::getMember)
                )
                .filter((seat, schedule, otherSeat) -> otherSeat.isAssigned()
                        && otherSeat.getTeamIndex() > seat.getTeamIndex())
                .join(
                        TeamSchedule.class,
                        Joiners.equal(
                                (seat, schedule, otherSeat) -> otherSeat.getTeamIndex(),
                                TeamSchedule::getTeamIndex
                        )
                )
                .filter((seat, schedule, otherSeat, otherSchedule) ->
                        otherSchedule.getTimeSlot() != null
                                && schedule.getTimeSlot().equals(otherSchedule.getTimeSlot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Member double-booked across teams");
    }

    /**
     * H6: a team's two weekly rehearsals must fall on different weekdays
     * so the team can rehearse at least twice a week.
     */
    Constraint teamRehearsalDaysMustDiffer(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        TeamSchedule.class,
                        Joiners.equal(TeamSchedule::getTeamIndex)
                )
                .filter((left, right) -> left.getTimeSlot() != null && right.getTimeSlot() != null)
                .filter((left, right) -> left.getTimeSlot().getDayOfWeek()
                        .equals(right.getTimeSlot().getDayOfWeek()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Team rehearsal days must differ");
    }

    /** H7: every open team must have a drummer — D seats cannot stay empty. */
    Constraint drumSeatMustBeFilled(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(TeamSeat.class)
                .filter(seat -> "D".equals(seat.getPosition()) && seat.isOpen() && !seat.isAssigned())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Drum seat must be filled");
    }

    /** S1: prefer filling seats rather than leaving them empty. */
    Constraint unfilledSlotPenalty(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(TeamSeat.class)
                .filter(seat -> !seat.isAssigned() && seat.isOpen())
                .penalize(HardSoftScore.ONE_SOFT, TeamMatchConstraintProvider::unfilledPenaltyWeight)
                .asConstraint("Unfilled session slot");
    }

    /**
     * S1b: every applicant should land on at least one team.
     * Weight sits above a single empty seat so coverage wins over polishing boards.
     */
    Constraint unassignedMemberPenalty(ConstraintFactory factory) {
        return factory.forEach(MatchMember.class)
                .ifNotExists(
                        TeamSeat.class,
                        Joiners.equal(Function.identity(), TeamSeat::getMember)
                )
                .penalize(HardSoftScore.ofSoft(UNASSIGNED_MEMBER_PENALTY))
                .asConstraint("Unassigned member");
    }

    /**
     * S1c: once assigned, prefer that at least one seat is the member's 1순위.
     * Below unassigned coverage, above polishing empty optional seats.
     */
    Constraint assignedWithoutFirstChoicePenalty(ConstraintFactory factory) {
        return factory.forEach(MatchMember.class)
                .filter(MatchMember::hasFirstChoiceSession)
                .ifExists(
                        TeamSeat.class,
                        Joiners.equal(Function.identity(), TeamSeat::getMember)
                )
                .ifNotExists(
                        TeamSeat.class,
                        Joiners.equal(Function.identity(), TeamSeat::getMember),
                        Joiners.filtering((member, seat) -> member.rankOf(seat.getPosition()) == 1)
                )
                .penalize(HardSoftScore.ofSoft(MISSING_FIRST_CHOICE_PENALTY))
                .asConstraint("Assigned without first-choice session");
    }

    /**
     * S1d: do not park leftover vocals on V2 while any open V1 seat is still empty.
     */
    Constraint discourageV2WhileEmptyV1Remains(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(seat -> "V2".equals(seat.getPosition()) && seat.isAssigned() && seat.isOpen())
                .ifExistsIncludingUnassigned(
                        TeamSeat.class,
                        Joiners.filtering((v2, v1) ->
                                "V1".equals(v1.getPosition()) && v1.isOpen() && !v1.isAssigned())
                )
                .penalize(HardSoftScore.ofSoft(V2_BEFORE_V1_PENALTY))
                .asConstraint("Discourage V2 while empty V1 remains");
    }

    /**
     * S1e: prefer one vocal per team. Filling V2 costs almost as much as leaving
     * someone unmatched, so it only happens when vocal seats are truly short.
     */
    Constraint discourageFilledV2(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(seat -> "V2".equals(seat.getPosition()) && seat.isAssigned() && seat.isOpen())
                .penalize(HardSoftScore.ofSoft(FILLED_V2_PENALTY))
                .asConstraint("Discourage filled V2");
    }

    /** S2: prefer a member's higher-ranked session. */
    Constraint preferHigherRankSession(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .reward(HardSoftScore.ONE_SOFT, TeamMatchConstraintProvider::rankScore)
                .asConstraint("Prefer higher rank session");
    }

    /** S3: slight preference for higher skill. */
    Constraint preferHigherSkillLevel(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .reward(HardSoftScore.ONE_SOFT, seat -> seat.getMember().levelScore(seat.getPosition()))
                .asConstraint("Prefer higher skill level");
    }

    /** S4: double-up is allowed but is a last resort (weight below S1). */
    Constraint discourageDoubleUp(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.equal(TeamSeat::getMember)
                )
                .filter((left, right) -> left.getMember() != null)
                .penalize(HardSoftScore.ofSoft(DOUBLE_UP_PENALTY))
                .asConstraint("Discourage double-up");
    }

    /** S5: when feasible, prefer morning / lunch rehearsal slots. */
    Constraint preferMorningAndLunchSlots(ConstraintFactory factory) {
        return factory.forEach(TeamSchedule.class)
                .filter(schedule -> schedule.getTimeSlot() != null)
                .reward(HardSoftScore.ONE_SOFT, TeamMatchConstraintProvider::timeBandReward)
                .asConstraint("Prefer morning and lunch slots");
    }

    /**
     * S6: discourage packing many teams onto the exact same slot
     * so leftover demand can spill into morning/lunch.
     */
    Constraint discourageSameSlotOvercrowd(ConstraintFactory factory) {
        return factory.forEach(TeamSchedule.class)
                .filter(schedule -> schedule.getTimeSlot() != null)
                .groupBy(
                        TeamSchedule::getTimeSlot,
                        ConstraintCollectors.countDistinct(TeamSchedule::getTeamIndex)
                )
                .filter((slot, teamCount) -> teamCount > 1)
                .penalize(
                        HardSoftScore.ofSoft(SAME_SLOT_OVERCROWD_PENALTY),
                        (slot, teamCount) -> (int) (teamCount - 1)
                )
                .asConstraint("Discourage same-slot overcrowding");
    }

    /**
     * S6b: keep weekly rehearsals spread across weekdays (avoid 화/목 pile-ups).
     * Counts distinct teams rehearsing that day; excess over the soft cap is squared.
     */
    Constraint discourageWeekdayOvercrowd(ConstraintFactory factory) {
        return factory.forEach(TeamSchedule.class)
                .filter(schedule -> schedule.getTimeSlot() != null
                        && schedule.getTimeSlot().getDayOfWeek() != null
                        && !schedule.getTimeSlot().getDayOfWeek().isBlank())
                .groupBy(
                        schedule -> schedule.getTimeSlot().getDayOfWeek(),
                        ConstraintCollectors.countDistinct(TeamSchedule::getTeamIndex)
                )
                .filter((day, teamCount) -> teamCount > WEEKDAY_TEAM_SOFT_CAP)
                .penalize(
                        HardSoftScore.ofSoft(SAME_DAY_OVERCROWD_PENALTY),
                        (day, teamCount) -> {
                            int excess = (int) (teamCount - WEEKDAY_TEAM_SOFT_CAP);
                            return excess * excess;
                        }
                )
                .asConstraint("Discourage weekday overcrowding");
    }

    /**
     * S7: leftover V2 vocals should join the team whose members share the most
     * available rehearsal slots with them.
     */
    Constraint preferV2WithBestScheduleFit(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(seat -> "V2".equals(seat.getPosition()) && seat.isAssigned())
                .join(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.filtering((v2, other) -> other.isAssigned()
                                && other.getMember() != null
                                && !other.getMember().equals(v2.getMember()))
                )
                .reward(
                        HardSoftScore.ONE_SOFT,
                        (v2, other) -> scheduleOverlapReward(v2.getMember(), other.getMember())
                )
                .asConstraint("Prefer V2 with best schedule fit");
    }

    /**
     * S8: multi-team members should get staggered assigned starts inside a shared
     * availability window (e.g. both teams free 월 17–21 → A 17:00, B 19:00).
     */
    Constraint preferStaggeredScheduleForMultiTeamMembers(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .join(
                        TeamSchedule.class,
                        Joiners.equal(TeamSeat::getTeamIndex, TeamSchedule::getTeamIndex)
                )
                .filter((seat, schedule) -> schedule.getTimeSlot() != null)
                .join(
                        TeamSeat.class,
                        Joiners.equal((seat, schedule) -> seat.getMember(), TeamSeat::getMember)
                )
                .filter((seat, schedule, otherSeat) -> otherSeat.isAssigned()
                        && otherSeat.getTeamIndex() > seat.getTeamIndex())
                .join(
                        TeamSchedule.class,
                        Joiners.equal(
                                (seat, schedule, otherSeat) -> otherSeat.getTeamIndex(),
                                TeamSchedule::getTeamIndex
                        )
                )
                .filter((seat, schedule, otherSeat, otherSchedule) ->
                        otherSchedule.getTimeSlot() != null
                                && !schedule.getTimeSlot().equals(otherSchedule.getTimeSlot()))
                .reward(
                        HardSoftScore.ONE_SOFT,
                        (seat, schedule, otherSeat, otherSchedule) ->
                                staggerReward(schedule.getTimeSlot(), otherSchedule.getTimeSlot())
                )
                .asConstraint("Prefer staggered schedule for multi-team members");
    }

    private static int unfilledPenaltyWeight(TeamSeat seat) {
        if ("V1".equals(seat.getPosition())) {
            return UNFILLED_V1_PENALTY;
        }
        if ("V2".equals(seat.getPosition())) {
            return UNFILLED_V2_PENALTY;
        }
        return UNFILLED_SLOT_PENALTY;
    }

    private static int scheduleOverlapReward(MatchMember left, MatchMember right) {
        if (left == null || right == null
                || left.getAvailableTimeSlots() == null
                || right.getAvailableTimeSlots() == null) {
            return 0;
        }
        int overlap = 0;
        for (MatchTimeSlot slot : left.getAvailableTimeSlots()) {
            if (right.getAvailableTimeSlots().contains(slot)) {
                overlap += 1;
                if (overlap >= V2_SCHEDULE_FIT_REWARD_CAP) {
                    return V2_SCHEDULE_FIT_REWARD_CAP;
                }
            }
        }
        return overlap;
    }

    /** Larger same-day gaps score higher; different days get a small flat reward. */
    private static int staggerReward(MatchTimeSlot left, MatchTimeSlot right) {
        if (left == null || right == null) {
            return 0;
        }
        if (!left.getDayOfWeek().equals(right.getDayOfWeek())) {
            return MULTI_TEAM_DIFFERENT_DAY_REWARD;
        }
        int leftMinutes = left.startMinutes();
        int rightMinutes = right.startMinutes();
        if (leftMinutes < 0 || rightMinutes < 0) {
            return 0;
        }
        int halfHoursApart = Math.abs(leftMinutes - rightMinutes) / 30;
        return halfHoursApart * MULTI_TEAM_STAGGER_REWARD_PER_HALF_HOUR;
    }

    private static int rankScore(TeamSeat seat) {
        int rank = seat.getMember().rankOf(seat.getPosition());
        return switch (rank) {
            case 1 -> 80;
            case 2 -> 12;
            case 3 -> 4;
            default -> 0;
        };
    }

    private static int timeBandReward(TeamSchedule schedule) {
        MatchTimeSlot slot = schedule.getTimeSlot();
        if (slot == null) {
            return 0;
        }
        int minutes = slot.startMinutes();
        if (minutes >= 9 * 60 && minutes <= 11 * 60 + 30) {
            return MORNING_SLOT_REWARD;
        }
        if (minutes >= 12 * 60 && minutes <= 13 * 60 + 30) {
            return LUNCH_SLOT_REWARD;
        }
        return 0;
    }
}
