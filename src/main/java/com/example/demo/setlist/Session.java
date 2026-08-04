package com.example.demo.setlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.io.Serializable;
import java.util.Objects;

@Entity(name = "SetlistSession")
@Table(name = "session")
@IdClass(Session.SessionId.class)
public class Session {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_id", nullable = false)
    private Setlist setlist;

    @Id
    @Column(name = "position", nullable = false, columnDefinition = "session_position")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private SessionPosition position;

    @Id
    @Column(name = "extra", nullable = false)
    private String extra = "";

    public Session() {
    }

    public Session(Setlist setlist, String position, String extra) {
        this.setlist = setlist;
        this.position = SessionPosition.fromLabel(position);
        this.extra = extra == null ? "" : extra;
    }

    public Setlist getSetlist() {
        return setlist;
    }

    public SessionPosition getPosition() {
        return position;
    }

    public String getExtra() {
        return extra;
    }

    public static class SessionId implements Serializable {
        private Long setlist;
        private SessionPosition position;
        private String extra;

        public SessionId() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SessionId that)) return false;
            return Objects.equals(setlist, that.setlist)
                    && Objects.equals(position, that.position)
                    && Objects.equals(extra, that.extra);
        }

        @Override
        public int hashCode() {
            return Objects.hash(setlist, position, extra);
        }
    }
}
