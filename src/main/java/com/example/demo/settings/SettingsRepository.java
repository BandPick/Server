package com.example.demo.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

    Optional<Settings> findTopByOrderByIdAsc();

    /**
     * id가 가장 작은 한 행만 남기고 나머지 설정 행을 삭제합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Settings s WHERE s.id <> :keepId")
    void deleteAllExceptId(@Param("keepId") Long keepId);
}
