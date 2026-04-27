package com.example.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.course.entity.Course;
import java.util.List;

public interface CourseService extends IService<Course> {
    void selectCourse(Long studentId, Long courseId);
    void dropCourse(Long studentId, Long courseId);

    List<Course> getMyCourses(Long studentId);
    List<Course> getListWithSchedule();
    List<Course> getAvailableCourses(String major);

    // ✨✨ 新增：重置系统数据的方法 ✨✨
    void resetSystem();
}