package com.example.demo.member;

import com.example.demo.common.type.Position;

import java.util.List;

public class Member {

    private String participantNumber;
    private String name;
    private List<Position> positions;

    public Member() {
    }

    public Member(String participantNumber, String name, List<Position> positions) {
        this.participantNumber = participantNumber;
        this.name = name;
        this.positions = positions;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
}