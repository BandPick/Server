package com.example.demo.preference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceRepository extends JpaRepository<Preference, Integer> {

    List<Preference> findByUserId(Long userId);
}