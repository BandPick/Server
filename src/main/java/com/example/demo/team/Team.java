package com.example.demo.team;

import jakarta.persistence.*;

@Entity
@Table(name = "team")
public class Team {

    public static final String TYPE_GENERAL = "GENERAL";
    public static final String TYPE_TEAM_SYSTEM = "TEAM_SYSTEM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "setlist_id")
    private Integer setlistId;

    @Column(name = "team_type", nullable = false)
    private String teamType = TYPE_GENERAL;

    @Column(name = "name")
    private String name;

    public Team() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getSetlistId() {
        return setlistId;
    }

    public void setSetlistId(Integer setlistId) {
        this.setlistId = setlistId;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamId() {
        return String.valueOf(id);
    }
}
