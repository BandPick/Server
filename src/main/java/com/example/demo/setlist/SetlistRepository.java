package com.example.demo.setlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    List<Setlist> findAllByOrderByIdAsc();
}
