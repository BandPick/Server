package com.example.demo.preference;

import com.example.demo.common.type.Position;

public class Preference {

    private String preferenceId;
    private String participantNumber;
    private String songId;
    private Position position;
    private int preferenceRank;

    public Preference() {
    }

    public Preference(String preferenceId, String participantNumber, String songId, Position position, int preferenceRank) {
        this.preferenceId = preferenceId;
        this.participantNumber = participantNumber;
        this.songId = songId;
        this.position = position;
        this.preferenceRank = preferenceRank;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getPreferenceRank() {
        return preferenceRank;
    }

    public void setPreferenceRank(int preferenceRank) {
        this.preferenceRank = preferenceRank;
    }
}