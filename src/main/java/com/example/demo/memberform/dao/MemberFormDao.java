package com.example.demo.memberform.dao;

import com.example.demo.memberform.vo.AvailabilityVo;
import com.example.demo.memberform.vo.PickVo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class MemberFormDao {

    private final JdbcTemplate jdbcTemplate;

    public MemberFormDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteAllByUserId(long userId) {
        jdbcTemplate.update("DELETE FROM form WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM schedule_form WHERE user_id = ?", userId);
    }

    public int insertPicks(long userId, List<PickVo> picks) {
        int inserted = 0;
        for (PickVo pick : picks) {
            inserted += jdbcTemplate.update(
                    """
                    INSERT INTO form (user_id, priority, setlist_id, desired_position, desired_extra)
                    VALUES (?, ?, ?, CAST(? AS session_position), ?)
                    """,
                    userId,
                    pick.priority(),
                    pick.setlistId(),
                    pick.desiredPosition(),
                    pick.desiredExtra()
            );
        }
        return inserted;
    }

    public int insertAvailabilities(long userId, List<AvailabilityVo> availabilities) {
        int inserted = 0;
        for (AvailabilityVo availability : availabilities) {
            inserted += jdbcTemplate.update(
                    """
                    INSERT INTO schedule_form (user_id, available_from, available_to)
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
