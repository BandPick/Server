package com.example.demo.team;

import java.util.List;

public class TeamMemberUpdateRequest {

    private List<String> memberIds;

    public TeamMemberUpdateRequest() {
    }

    public TeamMemberUpdateRequest(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
}