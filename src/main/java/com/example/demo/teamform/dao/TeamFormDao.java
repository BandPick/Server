package com.example.demo.teamform.dao;

import com.example.demo.memberform.vo.AvailabilityVo;
import com.example.demo.teamform.vo.TeamPositionVo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class TeamFormDao {

    private static final Logger log = LoggerFactory.getLogger(TeamFormDao.class);

    private final JdbcTemplate jdbcTemplate;

    public TeamFormDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        try {
            jdbcTemplate.execute(
                    """
                    CREATE TABLE IF NOT EXISTS team_form (
                        user_id BIGINT PRIMARY KEY,
                        preferred_teammates TEXT NOT NULL DEFAULT '',
                        updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
                    )
                    """
            );
            jdbcTemplate.execute(
                    """
                    CREATE TABLE IF NOT EXISTS team_form_position (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        position VARCHAR(16) NOT NULL,
                        proficiency VARCHAR(8) NOT NULL,
                        UNIQUE (user_id, position)
                    )
                    """
            );
            jdbcTemplate.execute(
                    """
                    CREATE TABLE IF NOT EXISTS team_schedule_form (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        available_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                        available_to TIMESTAMP WITHOUT TIME ZONE NOT NULL
                    )
                    """
            );
        } catch (Exception ex) {
            log.warn("팀제 폼 테이블을 준비하지 못했습니다: {}", ex.getMessage());
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

    public void deleteAllByUserId(long userId) {
        jdbcTemplate.update("DELETE FROM team_form_position WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM team_schedule_form WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM team_form WHERE user_id = ?", userId);
    }

    public int insertHeader(long userId, String preferredTeammates) {
        return jdbcTemplate.update(
                """
                INSERT INTO team_form (user_id, preferred_teammates, updated_at)
                VALUES (?, ?, NOW())
                """,
                userId,
                preferredTeammates
        );
    }

    public int insertPositions(long userId, List<TeamPositionVo> positions) {
        int inserted = 0;
        for (TeamPositionVo position : positions) {
            inserted += jdbcTemplate.update(
                    """
                    INSERT INTO team_form_position (user_id, position, proficiency)
                    VALUES (?, ?, ?)
                    """,
                    userId,
                    position.position(),
                    position.proficiency()
            );
        }
        return inserted;
    }

    public int insertAvailabilities(long userId, List<AvailabilityVo> availabilities) {
        int inserted = 0;
        for (AvailabilityVo availability : availabilities) {
            inserted += jdbcTemplate.update(
                    """
                    INSERT INTO team_schedule_form (user_id, available_from, available_to)
                    VALUES (?, ?, ?)
                    """,
                    userId,
                    Timestamp.valueOf(availability.availableFrom()),
                    Timestamp.valueOf(availability.availableTo())
            );
        }
        return inserted;
    }
}
