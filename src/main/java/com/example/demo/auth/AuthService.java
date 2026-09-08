package com.example.demo.auth;

import com.example.demo.auth.dao.AdminDao;
import com.example.demo.auth.dao.UserDao;
import com.example.demo.auth.dto.AdminLoginRequest;
import com.example.demo.auth.dto.AdminLoginResponse;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.entity.AdminEntity;
import com.example.demo.auth.entity.UserEntity;
import com.example.demo.auth.vo.LoginUserVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserDao userDao;
    private final AdminDao adminDao;

    public AuthService(UserDao userDao, AdminDao adminDao) {
        this.userDao = userDao;
        this.adminDao = adminDao;
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

    @Transactional
    public LoginResponse register(LoginRequest request) {
        String normalizedCode = request.code().trim();
        String normalizedName = request.name().trim();

        if (userDao.findByCode(normalizedCode).isPresent()) {
            return new LoginResponse(false, "이미 등록된 학번입니다.", null);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setCode(normalizedCode);
        userEntity.setName(normalizedName);
        UserEntity saved = userDao.save(userEntity);

        LoginUserVo userVo = new LoginUserVo(saved.getId(), saved.getCode(), saved.getName());
        return new LoginResponse(true, "계정 등록에 성공했습니다. 로그인해 주세요.", userVo);
    }

    @Transactional(readOnly = true)
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        String username = request.username().trim();
        String password = request.password();

        AdminEntity admin = adminDao.findById(username).orElse(null);
        if (admin == null || !admin.getPassword().equals(password)) {
            return new AdminLoginResponse(false, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return new AdminLoginResponse(true, "기획자 로그인에 성공했습니다.");
    }
}
