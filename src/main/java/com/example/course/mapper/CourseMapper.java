package com.example.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    // 这里什么都不用写，MyBatis-Plus 已经帮你写好了所有增删改查的方法
}