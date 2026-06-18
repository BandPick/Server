package com.example.demo.auth;

import com.example.demo.auth.dao.UserDao;
import com.example.demo.auth.dto.UserResponse;
import com.example.demo.auth.dto.UserUpsertRequest;
import com.example.demo.auth.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userDao.findAll().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getCode(),
                        user.getName()
                ))
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserUpsertRequest request) {
        userDao.findByCode(request.code())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("이미 사용 중인 학번(code)입니다.");
                });

        UserEntity userEntity = new UserEntity();
        userEntity.setCode(request.code());
        userEntity.setName(request.name());
        return toResponse(userDao.save(userEntity));
    }

    @Transactional
    public UserResponse updateUser(long userId, UserUpsertRequest request) {
        UserEntity target = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        userDao.findByCode(request.code())
                .ifPresent(user -> {
                    if (!user.getId().equals(target.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 학번(code)입니다.");
                    }
                });

        target.setCode(request.code());
        target.setName(request.name());
        return toResponse(userDao.save(target));
    }

    @Transactional
    public void deleteUser(long userId) {
        UserEntity target = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        userDao.delete(target);
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getCode(),
                user.getName()
        );
    }
}
