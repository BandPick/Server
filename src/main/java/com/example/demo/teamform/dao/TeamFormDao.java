package com.example.demo.teamform.dao;

import com.example.demo.teamform.vo.TeamFormHeaderRow;
import com.example.demo.teamform.vo.TeamFormPositionRow;
import com.example.demo.teamform.vo.TeamFormScheduleRow;
import com.example.demo.teamform.vo.TeamPositionVo;
import com.example.demo.teamform.vo.TeamScheduleVo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public class TeamFormDao {

    private static final Logger log = LoggerFactory.getLogger(TeamFormDao.class);

    private final JdbcTemplate jdbcTemplate;

    public TeamFormDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureMaxTeamsColumn() {
        try {
            jdbcTemplate.execute(
                    """
                    ALTER TABLE team_system_form
                    ADD COLUMN IF NOT EXISTS max_teams INTEGER NOT NULL DEFAULT 1
                    """
            );
        } catch (Exception ex) {
            log.warn("team_system_form.max_teams 컬럼을 준비하지 못했습니다: {}", ex.getMessage());
        }
    }

    public boolean existsUser(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    public void deleteByUserId(long userId) {
        jdbcTemplate.update("DELETE FROM team_system_form WHERE user_id = ?", userId);
    }

    public int insertForm(long userId, String teammates, int maxTeams) {
        Integer teamFormId = jdbcTemplate.queryForObject(
                """
                INSERT INTO team_system_form (user_id, teammates, max_teams)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                userId,
                teammates,
                maxTeams
        );
        if (teamFormId == null) {
            throw new IllegalStateException("팀제 신청 저장 후 ID를 가져오지 못했습니다.");
        }
        return teamFormId;
    }

    public int insertPositions(int teamFormId, List<TeamPositionVo> positions) {
        int inserted = 0;
        for (TeamPositionVo position : positions) {
            inserted += jdbcTemplate.update(
                    """
                    INSERT INTO team_system_form_position (team_form_id, position, level)
                    VALUES (?, ?, ?)
                    """,
                    teamFormId,
                    position.position(),
                    position.level()
            );
        }
        return inserted;
    }

    public int insertSchedules(int teamFormId, List<TeamScheduleVo> schedules) {
        int inserted = 0;
        for (TeamScheduleVo schedule : schedules) {
            inserted += jdbcTemplate.update(
                    """
                    INSERT INTO team_system_form_schedule (team_form_id, day_of_week, start_time)
                    VALUES (?, ?, ?)
                    """,
                    teamFormId,
                    schedule.dayOfWeek(),
                    Time.valueOf(schedule.startTime())
            );
        }
        return inserted;
    }

    public List<TeamFormHeaderRow> findAllHeaders() {
        return jdbcTemplate.query(
                """
                SELECT f.id,
                       f.user_id,
                       u.name AS user_name,
                       u.code AS user_code,
                       COALESCE(f.teammates, '') AS teammates,
                       COALESCE(f.max_teams, 1) AS max_teams,
                       f.created_at
                FROM team_system_form f
                INNER JOIN users u ON u.id = f.user_id
                ORDER BY u.name
                """,
                (rs, rowNum) -> new TeamFormHeaderRow(
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_code"),
                        rs.getString("teammates"),
                        rs.getInt("max_teams"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                )
        );
    }

    public List<TeamFormPositionRow> findAllPositions() {
        return jdbcTemplate.query(
                """
                SELECT team_form_id, position, level
                FROM team_system_form_position
                ORDER BY team_form_id, position
                """,
                (rs, rowNum) -> new TeamFormPositionRow(
                        rs.getInt("team_form_id"),
                        rs.getString("position"),
                        rs.getString("level")
                )
        );
    }

    public List<TeamFormScheduleRow> findAllSchedules() {
        return jdbcTemplate.query(
                """
                SELECT team_form_id, day_of_week, start_time
                FROM team_system_form_schedule
                ORDER BY team_form_id, day_of_week, start_time
                """,
                (rs, rowNum) -> new TeamFormScheduleRow(
                        rs.getInt("team_form_id"),
                        rs.getString("day_of_week"),
                        toLocalTime(rs.getTime("start_time"))
                )
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalTime toLocalTime(Time time) {
        return time == null ? null : time.toLocalTime();
    }
}
