package com.example.course.controller;

import com.example.course.common.Result;
import com.example.course.entity.UserAdmin;
import com.example.course.entity.UserStudent;
import com.example.course.entity.UserTeacher;
import com.example.course.mapper.UserAdminMapper;
import com.example.course.mapper.UserStudentMapper;
import com.example.course.mapper.UserTeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserStudentMapper studentMapper;
    @Autowired
    private UserTeacherMapper teacherMapper; // ✨ 注入
    @Autowired
    private UserAdminMapper adminMapper;     // ✨ 注入

    // 1. 更新基本信息 (昵称/头像)
    @PostMapping("/updateInfo")
    public Result<String> updateInfo(@RequestBody Map<String, Object> params) {
        String role = (String) params.get("role");
        Long id = ((Number) params.get("id")).longValue();
        String realName = (String) params.get("realName");
        String avatar = (String) params.get("avatar");

        if ("student".equals(role)) {
            UserStudent user = studentMapper.selectById(id);
            if (user == null) return Result.error("用户不存在");
            user.setRealName(realName);
            if (avatar != null) user.setAvatar(avatar);
            studentMapper.updateById(user);
        }
        else if ("teacher".equals(role)) { // ✨ 教师逻辑
            UserTeacher user = teacherMapper.selectById(id);
            if (user == null) return Result.error("用户不存在");
            user.setRealName(realName);
            if (avatar != null) user.setAvatar(avatar);
            teacherMapper.updateById(user);
        }
        else if ("admin".equals(role)) { // ✨ 管理员逻辑
            UserAdmin user = adminMapper.selectById(id);
            if (user == null) return Result.error("用户不存在");
            user.setRealName(realName);
            if (avatar != null) user.setAvatar(avatar);
            adminMapper.updateById(user);
        }
        else {
            return Result.error("未知角色");
        }

        return Result.success("信息更新成功");
    }

    // 2. 修改密码
    @PostMapping("/updatePassword")
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        String role = params.get("role");
        Long id = Long.valueOf(params.get("id"));
        String oldPass = params.get("oldPass");
        String newPass = params.get("newPass");

        if ("student".equals(role)) {
            UserStudent user = studentMapper.selectById(id);
            if (!user.getPassword().equals(oldPass)) return Result.error("原密码错误");
            user.setPassword(newPass);
            studentMapper.updateById(user);
        }
        else if ("teacher".equals(role)) { // ✨ 教师逻辑
            UserTeacher user = teacherMapper.selectById(id);
            if (!user.getPassword().equals(oldPass)) return Result.error("原密码错误");
            user.setPassword(newPass);
            teacherMapper.updateById(user);
        }
        else if ("admin".equals(role)) { // ✨ 管理员逻辑
            UserAdmin user = adminMapper.selectById(id);
            if (!user.getPassword().equals(oldPass)) return Result.error("原密码错误");
            user.setPassword(newPass);
            adminMapper.updateById(user);
        }
        else {
            return Result.error("未知角色");
        }

        return Result.success("密码修改成功，请重新登录");
    }
}