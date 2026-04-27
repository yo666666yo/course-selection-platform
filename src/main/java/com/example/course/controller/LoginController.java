package com.example.course.controller;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.course.common.Result;
import com.example.course.entity.LoginDTO;
import com.example.course.entity.UserAdmin;
import com.example.course.entity.UserStudent;
import com.example.course.entity.UserTeacher;
import com.example.course.mapper.UserAdminMapper;
import com.example.course.mapper.UserStudentMapper;
import com.example.course.mapper.UserTeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
public class LoginController {

    @Autowired private UserStudentMapper studentMapper;
    @Autowired private UserTeacherMapper teacherMapper;
    @Autowired private UserAdminMapper adminMapper;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String rawPassword = loginDTO.getPassword();
        String role = loginDTO.getRole();

        // 1. ✨✨✨ 密码加密：将用户输入的密码进行 SHA-256 哈希 ✨✨✨
        // 注意：实际生产环境建议加盐 (Salt)，这里为了演示方便使用标准 SHA-256
        String encryptPassword = SaSecureUtil.sha256(rawPassword);

        Long userId = null;
        String realName = "";
        String avatar = "";
        String major = "";

        // 2. 身份校验
        if ("student".equals(role)) {
            UserStudent user = studentMapper.selectOne(new QueryWrapper<UserStudent>().eq("username", username));
            if (user == null || !user.getPassword().equals(encryptPassword)) {
                return Result.error("账号或密码错误");
            }
            userId = user.getId();
            realName = user.getRealName();
            avatar = user.getAvatar();
            major = user.getMajor();
        } else if ("teacher".equals(role)) {
            UserTeacher user = teacherMapper.selectOne(new QueryWrapper<UserTeacher>().eq("username", username));
            if (user == null || !user.getPassword().equals(encryptPassword)) {
                return Result.error("账号或密码错误");
            }
            userId = user.getId();
            realName = user.getRealName();
            avatar = user.getAvatar();
        } else if ("admin".equals(role)) {
            UserAdmin user = adminMapper.selectOne(new QueryWrapper<UserAdmin>().eq("username", username));
            if (user == null || !user.getPassword().equals(encryptPassword)) {
                return Result.error("账号或密码错误");
            }
            userId = user.getId();
            realName = user.getRealName();
            avatar = user.getAvatar();
        } else {
            return Result.error("未知角色");
        }

        // 3. ✨✨✨ Sa-Token 登录：生成 Token 并创建会话 ✨✨✨
        StpUtil.login(userId);

        // 4. 将角色信息存入 Session，防止后续越权操作
        StpUtil.getSession().set("role", role);
        StpUtil.getSession().set("name", realName);
        if("student".equals(role)) {
            StpUtil.getSession().set("major", major);
        }

        // 5. 返回 Token 和用户信息
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenInfo.tokenValue); // 前端需存储此 Token
        data.put("tokenName", tokenInfo.tokenName);
        data.put("id", userId);
        data.put("name", realName);
        data.put("role", role);
        data.put("avatar", avatar);
        data.put("major", major);

        return Result.success(data);
    }

    // 退出登录
    @PostMapping("/logout")
    public Result<String> logout() {
        StpUtil.logout();
        return Result.success("退出成功");
    }
}