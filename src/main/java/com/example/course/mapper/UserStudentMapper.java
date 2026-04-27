package com.example.course.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.course.entity.UserStudent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserStudentMapper extends BaseMapper<UserStudent> {}