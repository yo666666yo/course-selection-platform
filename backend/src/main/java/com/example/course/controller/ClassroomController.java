package com.example.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.course.common.Result;
import com.example.course.entity.Classroom;
import com.example.course.entity.Course;
import com.example.course.entity.CourseSchedule;
import com.example.course.mapper.ClassroomMapper;
import com.example.course.mapper.CourseMapper;
import com.example.course.mapper.CourseScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classroom")
@CrossOrigin
public class ClassroomController {

    @Autowired private ClassroomMapper classroomMapper;
    @Autowired private CourseScheduleMapper scheduleMapper;
    @Autowired private CourseMapper courseMapper;

    /**
     * 获取教室列表，支持按名称或位置搜索
     */
    @GetMapping("/list")
    public Result<List<Classroom>> list(@RequestParam(required = false) String keyword) {
        QueryWrapper<Classroom> query = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.like("room_name", keyword).or().like("location", keyword);
        }
        // 按位置和名称排序，让列表更整齐
        query.orderByAsc("location").orderByAsc("room_name");
        return Result.success(classroomMapper.selectList(query));
    }

    /**
     * 获取指定教室的课表
     */
    @GetMapping("/schedule")
    public Result<List<Map<String, Object>>> getSchedule(@RequestParam Long roomId) {
        // 1. 查出该教室的所有排课记录
        List<CourseSchedule> schedules = scheduleMapper.selectList(
                new QueryWrapper<CourseSchedule>().eq("classroom_id", roomId)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        if (schedules.isEmpty()) return Result.success(result);

        // 2. 批量查询课程信息（为了获取课程名和教师名）
        Set<Long> courseIds = schedules.stream().map(CourseSchedule::getCourseId).collect(Collectors.toSet());
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        // 3. 组装数据
        for (CourseSchedule s : schedules) {
            Course c = courseMap.get(s.getCourseId());
            if (c != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("day", s.getDayOfWeek());
                map.put("slot", s.getTimeSlot());
                map.put("courseName", c.getName());
                map.put("teacherName", c.getTeacherName());
                map.put("type", c.getType());
                map.put("id", c.getId()); // 用于前端判断是否同一门课(rowspan)
                result.add(map);
            }
        }
        return Result.success(result);
    }
}