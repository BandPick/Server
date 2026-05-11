package com.example.demo.auth.dao;

import com.example.demo.auth.entity.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    public List<UserEntity> findAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public Optional<UserEntity> findById(long id) {
        return userRepository.findById(id);
    }

    public Optional<UserEntity> findByCode(String code) {
        return userRepository.findByCode(code);
    }

    public UserEntity save(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }

    public void delete(UserEntity userEntity) {
        userRepository.delete(userEntity);
    }
}
