package com.example.demo.auth.dao;

import com.example.demo.auth.entity.AdminEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdminDao {

    private final AdminRepository adminRepository;

    public AdminDao(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public Optional<AdminEntity> findById(String id) {
        return adminRepository.findById(id);
    }
}
