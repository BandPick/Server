package com.example.demo.teamform.solver;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.example.demo.teamform.solver.domain.MatchMember;
import com.example.demo.teamform.solver.domain.TeamSeat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hard / medium / soft constraints for team-system matching.
 * Preference order encoded in soft weights: position rank → schedule → skill.
 */
public class TeamMatchConstraintProvider implements ConstraintProvider {

    private static final Set<String> CORE = Set.of("V", "D", "B");
    private static final Set<String> GUITARS = Set.of("EG1", "EG2");

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                memberMustHaveSkill(factory),
                noDuplicateAssignment(factory),
                memberMaxTeams(factory),
                teamNeedsCommonSchedule(factory),
                rewardCompleteTeams(factory),
                rewardFilledRequiredSeats(factory),
                penalizeIncompleteActiveTeams(factory),
                preferHigherPositionPriority(factory),
                preferScheduleOverlap(factory),
                preferHigherSkill(factory),
                preferScarceScheduleMembers(factory),
                preferScarceAnchorOnEarlyTeams(factory)
        };
    }

    Constraint memberMustHaveSkill(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .filter(seat -> !seat.getMember().canPlay(seat.getPosition()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Member must have skill for position");
    }

    Constraint noDuplicateAssignment(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        TeamSeat.class,
                        Joiners.equal(TeamSeat::getTeamIndex),
                        Joiners.equal(TeamSeat::getMember)
                )
                .filter((left, right) -> left.getMember() != null)
                .filter((left, right) -> !isAllowedDualRole(left, right))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("No duplicate assignment when dual role is invalid");
    }

    Constraint memberMaxTeams(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(
                        TeamSeat::getMember,
                        ConstraintCollectors.toSet(TeamSeat::getTeamIndex)
                )
                .filter((member, teams) -> teams.size() > member.getMaxTeams())
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (member, teams) -> teams.size() - member.getMaxTeams()
                )
                .asConstraint("Member exceeds max teams");
    }

    Constraint teamNeedsCommonSchedule(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(TeamSeat::getTeamIndex, ConstraintCollectors.toList())
                .filter((teamIndex, seats) -> uniqueMemberCount(seats) >= 2)
                .filter((teamIndex, seats) -> MatchMember.commonSlots(membersOf(seats)).isEmpty())
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Team members need common schedule");
    }

    Constraint rewardCompleteTeams(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(TeamSeat::getTeamIndex, ConstraintCollectors.toList())
                .filter((teamIndex, seats) -> isViable(seats))
                .reward(HardMediumSoftScore.of(0, 100, 0))
                .asConstraint("Complete viable team");
    }

    Constraint rewardFilledRequiredSeats(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .filter(seat -> CORE.contains(seat.getPosition()) || GUITARS.contains(seat.getPosition()))
                .reward(HardMediumSoftScore.of(0, 0, 20))
                .asConstraint("Filled required seat");
    }

    Constraint penalizeIncompleteActiveTeams(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(TeamSeat::getTeamIndex, ConstraintCollectors.toList())
                .filter((teamIndex, seats) -> !isViable(seats))
                .penalize(
                        HardMediumSoftScore.of(0, 0, 15),
                        (teamIndex, seats) -> missingRequiredCount(seats)
                )
                .asConstraint("Incomplete active team");
    }

    Constraint preferHigherPositionPriority(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        seat -> Math.max(0, seat.getMember().priorityRank(seat.getPosition()) - 1) * 8
                )
                .asConstraint("Prefer higher position priority");
    }

    Constraint preferScheduleOverlap(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .groupBy(TeamSeat::getTeamIndex, ConstraintCollectors.toList())
                .reward(
                        HardMediumSoftScore.ONE_SOFT,
                        (teamIndex, seats) -> MatchMember.commonSlots(membersOf(seats)).size() * 3
                )
                .asConstraint("Prefer larger schedule overlap");
    }

    Constraint preferHigherSkill(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .reward(
                        HardMediumSoftScore.ONE_SOFT,
                        seat -> seat.getMember().levelScore(seat.getPosition())
                )
                .asConstraint("Prefer higher skill");
    }

    Constraint preferScarceScheduleMembers(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .reward(
                        HardMediumSoftScore.ONE_SOFT,
                        seat -> seat.getMember().scheduleScarcity()
                )
                .asConstraint("Prefer scarce schedule members");
    }

    /**
     * Mirrors the product rule: scarcest people should land on early teams
     * (A, then B when maxTeams allows) with their primary position.
     */
    Constraint preferScarceAnchorOnEarlyTeams(ConstraintFactory factory) {
        return factory.forEach(TeamSeat.class)
                .filter(TeamSeat::isAssigned)
                .filter(seat -> seat.getTeamIndex() <= 1)
                .reward(
                        HardMediumSoftScore.ONE_SOFT,
                        seat -> {
                            int scarcity = seat.getMember().scheduleScarcity();
                            int primaryBonus = seat.getPosition().equals(seat.getMember().primaryPosition()) ? 12 : 0;
                            int earlyTeamBonus = seat.getTeamIndex() == 0 ? 6 : 3;
                            return scarcity + primaryBonus + earlyTeamBonus;
                        }
                )
                .asConstraint("Prefer scarce members on early teams");
    }

    private static boolean isAllowedDualRole(TeamSeat left, TeamSeat right) {
        if (!isVocalGuitarPair(left, right)) {
            return false;
        }
        String guitarPosition = GUITARS.contains(left.getPosition())
                ? left.getPosition()
                : right.getPosition();
        return vocalPreferredOverGuitar(left.getMember(), guitarPosition);
    }

    private static boolean isVocalGuitarPair(TeamSeat left, TeamSeat right) {
        String a = left.getPosition();
        String b = right.getPosition();
        return ("V".equals(a) && GUITARS.contains(b))
                || ("V".equals(b) && GUITARS.contains(a));
    }

    private static boolean vocalPreferredOverGuitar(MatchMember member, String guitarPosition) {
        return rankOf(member, "V") < rankOf(member, guitarPosition);
    }

    private static int rankOf(MatchMember member, String position) {
        Integer priority = member.getPriorities() == null ? null : member.getPriorities().get(position);
        if (priority == null || priority <= 0) {
            return Integer.MAX_VALUE;
        }
        return priority;
    }

    private static List<MatchMember> membersOf(List<TeamSeat> seats) {
        return seats.stream().map(TeamSeat::getMember).filter(member -> member != null).toList();
    }

    private static int uniqueMemberCount(List<TeamSeat> seats) {
        Set<Long> ids = new HashSet<>();
        for (TeamSeat seat : seats) {
            if (seat.isAssigned()) {
                ids.add(seat.getMember().getUserId());
            }
        }
        return ids.size();
    }

    private static boolean hasGuitar(List<TeamSeat> seats) {
        for (TeamSeat seat : seats) {
            if (seat.isAssigned() && GUITARS.contains(seat.getPosition())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDualVocalGuitar(List<TeamSeat> seats) {
        Map<Long, Set<String>> byMember = new HashMap<>();
        for (TeamSeat seat : seats) {
            if (!seat.isAssigned()) {
                continue;
            }
            byMember.computeIfAbsent(seat.getMember().getUserId(), id -> new HashSet<>())
                    .add(seat.getPosition());
        }
        for (Set<String> positions : byMember.values()) {
            if (positions.contains("V") && positions.stream().anyMatch(GUITARS::contains)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isViable(List<TeamSeat> seats) {
        Set<String> filled = new HashSet<>();
        for (TeamSeat seat : seats) {
            if (seat.isAssigned()) {
                filled.add(seat.getPosition());
            }
        }
        if (!filled.containsAll(CORE) || !hasGuitar(seats)) {
            return false;
        }
        if (MatchMember.commonSlots(membersOf(seats)).isEmpty()) {
            return false;
        }
        int unique = uniqueMemberCount(seats);
        if (hasDualVocalGuitar(seats)) {
            return unique >= 3;
        }
        return unique >= 4;
    }

    private static int missingRequiredCount(List<TeamSeat> seats) {
        Set<String> filled = new HashSet<>();
        for (TeamSeat seat : seats) {
            if (seat.isAssigned()) {
                filled.add(seat.getPosition());
            }
        }
        int missing = 0;
        for (String position : CORE) {
            if (!filled.contains(position)) {
                missing++;
            }
        }
        if (!hasGuitar(seats)) {
            missing++;
        }
        return Math.max(1, missing);
    }
}
