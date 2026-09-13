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
import java.util.Optional;

@Repository
public class TeamFormDao {

    private static final Logger log = LoggerFactory.getLogger(TeamFormDao.class);

    private final JdbcTemplate jdbcTemplate;
    private boolean hasMessageColumn = true;
    private boolean hasTeammatesColumn = false;

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
        try {
            jdbcTemplate.execute(
                    """
                    DO $$
                    BEGIN
                      IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'team_system_form'
                          AND column_name = 'teammates'
                      ) AND NOT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'team_system_form'
                          AND column_name = 'message'
                      ) THEN
                        ALTER TABLE team_system_form RENAME COLUMN teammates TO message;
                      END IF;

                      IF NOT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'team_system_form'
                          AND column_name = 'message'
                      ) THEN
                        ALTER TABLE team_system_form ADD COLUMN message TEXT;
                      END IF;
                    END $$;
                    """
            );
        } catch (Exception ex) {
            log.warn("team_system_form.message 컬럼을 준비하지 못했습니다: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute(
                    """
                    ALTER TABLE team_system_form_position
                    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0
                    """
            );
        } catch (Exception ex) {
            log.warn("team_system_form_position.priority 컬럼을 준비하지 못했습니다: {}", ex.getMessage());
        }
        resolveMessageColumns();
    }

    private void resolveMessageColumns() {
        hasMessageColumn = hasColumn("team_system_form", "message");
        hasTeammatesColumn = hasColumn("team_system_form", "teammates");
        if (!hasMessageColumn && !hasTeammatesColumn) {
            log.warn("team_system_form에 message/teammates 컬럼이 없습니다.");
            hasMessageColumn = true;
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = ?
                      AND column_name = ?
                    """,
                    Integer.class,
                    tableName,
                    columnName
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            log.warn("{}.{} 컬럼 확인에 실패했습니다: {}", tableName, columnName, ex.getMessage());
            return false;
        }
    }

    private String messageInsertColumn() {
        return hasMessageColumn ? "message" : "teammates";
    }

    private String messageSelectExpr() {
        if (hasMessageColumn && hasTeammatesColumn) {
            return "COALESCE(NULLIF(BTRIM(f.message), ''), NULLIF(BTRIM(f.teammates), ''), '')";
        }
        if (hasTeammatesColumn) {
            return "COALESCE(f.teammates, '')";
        }
        return "COALESCE(f.message, '')";
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

    public int insertForm(long userId, String message, int maxTeams) {
        String sql = """
                INSERT INTO team_system_form (user_id, %s, max_teams)
                VALUES (?, ?, ?)
                RETURNING id
                """.formatted(messageInsertColumn());
        Integer teamFormId = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                userId,
                message,
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
                    INSERT INTO team_system_form_position (team_form_id, position, level, priority)
                    VALUES (?, ?, ?, ?)
                    """,
                    teamFormId,
                    position.position(),
                    position.level(),
                    position.priority()
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

    public Optional<TeamFormHeaderRow> findHeaderByUserId(long userId) {
        String sql = """
                SELECT f.id,
                       f.user_id,
                       u.name AS user_name,
                       u.code AS user_code,
                       %s AS message,
                       COALESCE(f.max_teams, 1) AS max_teams,
                       f.created_at
                FROM team_system_form f
                INNER JOIN users u ON u.id = f.user_id
                WHERE f.user_id = ?
                ORDER BY f.id DESC
                """.formatted(messageSelectExpr());
        List<TeamFormHeaderRow> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new TeamFormHeaderRow(
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_code"),
                        rs.getString("message"),
                        rs.getInt("max_teams"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                userId
        );
        return rows.stream().findFirst();
    }

    public List<TeamFormPositionRow> findPositionsByFormId(int teamFormId) {
        return jdbcTemplate.query(
                """
                SELECT team_form_id, position, level, COALESCE(priority, 0) AS priority
                FROM team_system_form_position
                WHERE team_form_id = ?
                ORDER BY CASE WHEN COALESCE(priority, 0) > 0 THEN priority ELSE 999 END, position
                """,
                (rs, rowNum) -> new TeamFormPositionRow(
                        rs.getInt("team_form_id"),
                        rs.getString("position"),
                        rs.getString("level"),
                        rs.getInt("priority")
                ),
                teamFormId
        );
    }

    public List<TeamFormScheduleRow> findSchedulesByFormId(int teamFormId) {
        return jdbcTemplate.query(
                """
                SELECT team_form_id, day_of_week, start_time
                FROM team_system_form_schedule
                WHERE team_form_id = ?
                ORDER BY day_of_week, start_time
                """,
                (rs, rowNum) -> new TeamFormScheduleRow(
                        rs.getInt("team_form_id"),
                        rs.getString("day_of_week"),
                        toLocalTime(rs.getTime("start_time"))
                ),
                teamFormId
        );
    }

    public List<TeamFormHeaderRow> findAllHeaders() {
        String sql = """
                SELECT f.id,
                       f.user_id,
                       u.name AS user_name,
                       u.code AS user_code,
                       %s AS message,
                       COALESCE(f.max_teams, 1) AS max_teams,
                       f.created_at
                FROM team_system_form f
                INNER JOIN users u ON u.id = f.user_id
                ORDER BY u.name
                """.formatted(messageSelectExpr());
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new TeamFormHeaderRow(
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_code"),
                        rs.getString("message"),
                        rs.getInt("max_teams"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                )
        );
    }

    public List<TeamFormPositionRow> findAllPositions() {
        return jdbcTemplate.query(
                """
                SELECT team_form_id, position, level, COALESCE(priority, 0) AS priority
                FROM team_system_form_position
                ORDER BY team_form_id, CASE WHEN COALESCE(priority, 0) > 0 THEN priority ELSE 999 END, position
                """,
                (rs, rowNum) -> new TeamFormPositionRow(
                        rs.getInt("team_form_id"),
                        rs.getString("position"),
                        rs.getString("level"),
                        rs.getInt("priority")
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
