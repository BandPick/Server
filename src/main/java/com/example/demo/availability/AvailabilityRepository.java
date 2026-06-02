package com.example.demo.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Integer> {

    List<Availability> findByUserId(Long userId);
}