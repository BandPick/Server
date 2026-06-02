package com.example.demo.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "session")
@IdClass(SessionId.class)
public class Session {

    @Column(name = "id", insertable = false, updatable = false)
    private Integer id;

    @Id
    @Column(name = "setlist_id")
    private Long setlistId;

    @Id
    @Column(name = "position", columnDefinition = "session_position")
    @ColumnTransformer(write = "?::session_position")
    private String position;

    @Id
    @Column(name = "extra")
    private String extra = "";

    public Session() {
    }

    public Integer getId() {
        return id;
    }

    public Long getSetlistId() {
        return setlistId;
    }

    public void setSetlistId(Long setlistId) {
        this.setlistId = setlistId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
