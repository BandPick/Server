package com.example.demo.auth.dao;

import com.example.demo.auth.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<AdminEntity, String> {
}