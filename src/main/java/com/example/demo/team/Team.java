package com.example.demo.team;

import jakarta.persistence.*;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "setlist_id")
    private Integer setlistId;

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

    public String getTeamId() {
        return String.valueOf(id);
    }
}