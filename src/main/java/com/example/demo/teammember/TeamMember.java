package com.example.demo.teammember;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "team_member")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "session_position", columnDefinition = "session_position")
    @ColumnTransformer(write = "?::session_position")
    private String sessionPosition;

    @Column(name = "session_extra")
    private String sessionExtra;

    @Transient
    private String participantNumber;

    public TeamMember() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionPosition() {
        return sessionPosition;
    }

    public void setSessionPosition(String sessionPosition) {
        this.sessionPosition = sessionPosition;
    }

    public String getSessionExtra() {
        return sessionExtra;
    }

    public void setSessionExtra(String sessionExtra) {
        this.sessionExtra = sessionExtra;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }
}
