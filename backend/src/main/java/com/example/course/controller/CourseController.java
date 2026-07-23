package com.example.course.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.course.common.Result;
import com.example.course.entity.Course;
import com.example.course.event.CourseEventPublisher;
import com.example.course.mapper.CourseMapper;
import com.example.course.service.CourseService;
import com.example.course.service.impl.AutoScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/course")
@CrossOrigin
public class CourseController {

    @Autowired private CourseService courseService;
    @Autowired private CourseMapper courseMapper;
    @Autowired private AutoScheduleService autoScheduleService;
    @Autowired private CourseEventPublisher eventPublisher;

    // --- 公共查询接口 ---

    @GetMapping("/list")
    public Result<List<Course>> list() {
        return Result.success(courseService.getListWithSchedule());
    }

    @GetMapping("/list-available")
    public Result<List<Course>> listAvailable(@RequestParam(required = false) String major) {
        return Result.success(courseService.getAvailableCourses(major));
    }

    // --- 需权限保护的接口 (IDOR 修复) ---

    @GetMapping("/my-list")
    public Result<List<Course>> myList() {
        // ✨✨✨ 修复 IDOR：从 Token 获取当前用户 ID，而不是从参数获取 ✨✨✨
        long currentId = StpUtil.getLoginIdAsLong();
        return Result.success(courseService.getMyCourses(currentId));
    }

    @PostMapping("/select")
    public Result<String> selectCourse(Long courseId) {
        // ✨✨✨ 修复 IDOR：强制使用当前登录人 ID ✨✨✨
        long currentId = StpUtil.getLoginIdAsLong();

        // 校验角色 (可选，增加安全性)
        String role = StpUtil.getSession().getString("role");
        if (!"student".equals(role)) {
            return Result.error("只有学生可以选课");
        }

        try {
            courseService.selectCourse(currentId, courseId);
            return Result.success("抢课成功！");
        }
        catch (Exception e) { return Result.error(e.getMessage()); }
    }

    @PostMapping("/drop")
    public Result<String> dropCourse(Long courseId) {
        // ✨✨✨ 修复 IDOR ✨✨✨
        long currentId = StpUtil.getLoginIdAsLong();
        try {
            courseService.dropCourse(currentId, courseId);
            return Result.success("退课成功");
        }
        catch (Exception e) { return Result.error(e.getMessage()); }
    }

    // --- 教师/管理员接口 ---

    @PostMapping("/propose")
    public Result<String> proposeCourse(@RequestBody Course course) {
        course.setStatus(0);
        courseMapper.insert(course);
        eventPublisher.courseProposed(course.getId(), course.getName(), course.getTeacherName());
        return Result.success("申报成功");
    }

    @PostMapping("/audit")
    public Result<String> auditCourse(Long courseId, boolean pass) {
        // 简单权限检查
        String role = StpUtil.getSession().getString("role");
        if (!"admin".equals(role)) return Result.error("无权操作");

        Course course = courseMapper.selectById(courseId);
        if (course == null) return Result.error("课程不存在");

        course.setStatus(pass ? 1 : 3);
        courseMapper.updateById(course);
        eventPublisher.courseAudited(courseId, course.getName(), pass);
        return Result.success("操作成功");
    }

    @RequestMapping("/auto-schedule")
    public Result<String> autoSchedule() {
        if (!"admin".equals(StpUtil.getSession().getString("role"))) return Result.error("无权操作");
        Long pendingCount = courseMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Course>().eq("status", 1));
        String result = autoScheduleService.autoSchedule();
        eventPublisher.scheduleGenerated(pendingCount > 0 ? pendingCount.intValue() : 0);
        return Result.success(result);
    }

    @GetMapping("/reset-redis")
    public Result<String> resetSystem() {
        if (!"admin".equals(StpUtil.getSession().getString("role"))) return Result.error("无权操作");
        try {
            courseService.resetSystem();
            return Result.success("系统数据已重置");
        } catch (Exception e) {
            return Result.error("重置失败：" + e.getMessage());
        }
    }
}
