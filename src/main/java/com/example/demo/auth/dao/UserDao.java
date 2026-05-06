package com.example.demo.auth.dao;

import com.example.demo.auth.entity.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDao {

    private final UserRepository userRepository;

    public UserDao(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserEntity> findByCodeAndName(String code, String name) {
        return userRepository.findByCodeAndName(code, name);
    }
}
