package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemLockedTeamRequest(
        String name,
        List<TeamSystemLockedMemberRequest> members,
        /**
         * true (default): whole team locked (확정).
         * false: only listed seats are pinned; remaining seats stay open for rematch.
         */
        Boolean fullyLocked
) {
    public TeamSystemLockedTeamRequest(String name, List<TeamSystemLockedMemberRequest> members) {
        this(name, members, true);
    }

    public boolean isFullyLocked() {
        return fullyLocked == null || fullyLocked;
    }
}
