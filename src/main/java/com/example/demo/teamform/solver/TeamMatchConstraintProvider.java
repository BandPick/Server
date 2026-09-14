package com.example.demo.teamform.solver;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.example.demo.teamform.solver.domain.TeamSchedule;
import com.example.demo.teamform.solver.domain.TeamSeat;

/**
 * Hard / soft constraints for team-system matching.
 *
 * <p>Two planning variables are linked here:
 * who sits in each {@link TeamSeat}, and when each {@link TeamSchedule} rehearses.
 */
public class TeamMatchConstraintProvider implements ConstraintProvider {

    static final int UNFILLED_SLOT_PENALTY = 50;
    static final int DOUBLE_UP_PENALTY = 20;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                sessionMustBeInMemberPreference(factory),
                atMostTwoSlotsInSameTeam(factory),
                doubleUpMustIncludeVocal(factory),
                vocalMustOutrankInstrumentToDoubleUp(factory),
                maxTeamCountExceeded(factory),
                teamTimeSlotUnavailableForMember(factory),
                memberDoubleBookedAcrossTeams(factory),
                unfilledSlotPenalty(factory),
                preferHigherRankSession(factory),
                preferHigherSkillLevel(factory),
                discourageDoubleUp(factory)
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

    /** H4: every assigned member must be free at the team's rehearsal slot. */
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
     * H5: a member on multiple teams cannot have those teams share a time slot.
     * Dual-up on the same team is one team, so it does not fire.
     */
    Constraint memberDoubleBookedAcrossTeams(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .join(
                        TeamSchedule.class,
                        Joiners.equal(TeamSeat::getTeamIndex, TeamSchedule::getTeamIndex)
                )
                .filter((seat, schedule) -> schedule.getTimeSlot() != null)
                .groupBy(
                        (seat, schedule) -> seat.getMember(),
                        ConstraintCollectors.countDistinct(
                                (TeamSeat seat, TeamSchedule schedule) -> seat.getTeamIndex()
                        ),
                        ConstraintCollectors.countDistinct(
                                (TeamSeat seat, TeamSchedule schedule) -> schedule.getTimeSlot()
                        )
                )
                .filter((member, teamCount, slotCount) -> teamCount > 1 && slotCount < teamCount)
                .penalize(
                        HardSoftScore.ONE_HARD,
                        (member, teamCount, slotCount) -> (int) (teamCount - slotCount)
                )
                .asConstraint("Member double-booked across teams");
    }

    /** S1: prefer filling seats rather than leaving them empty. */
    Constraint unfilledSlotPenalty(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(TeamSeat.class)
                .filter(seat -> !seat.isAssigned() && seat.isOpen())
                .penalize(HardSoftScore.ofSoft(UNFILLED_SLOT_PENALTY))
                .asConstraint("Unfilled session slot");
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

    private static int rankScore(TeamSeat seat) {
        int rank = seat.getMember().rankOf(seat.getPosition());
        return switch (rank) {
            case 1 -> 30;
            case 2 -> 15;
            case 3 -> 5;
            default -> 0;
        };
    }
}
