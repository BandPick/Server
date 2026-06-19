package com.example.demo.auth.dao;

import com.example.demo.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByCodeAndName(String code, String name);

    List<UserEntity> findAllByOrderByIdAsc();

    Optional<UserEntity> findByCode(String code);
}
