package com.example.demo.auth;

import com.example.demo.auth.dao.UserDao;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.entity.UserEntity;
import com.example.demo.auth.vo.LoginUserVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedCode = request.code().trim();
        String normalizedName = request.name().trim();

        UserEntity user = userDao.findByCodeAndName(normalizedCode, normalizedName).orElse(null);

        if (user == null) {
            return new LoginResponse(false, "학번/고유코드 또는 이름이 올바르지 않습니다.", null);
        }

        LoginUserVo userVo = new LoginUserVo(user.getId(), user.getCode(), user.getName());
        return new LoginResponse(true, "로그인에 성공했습니다.", userVo);
    }
}
