package com.example.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.course.entity.StudentCourse; // 如果报错红色，需要先建Entity，看下面
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentCourseMapper extends BaseMapper<StudentCourse> {
}