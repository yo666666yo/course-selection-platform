package com.example.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.course.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {
}