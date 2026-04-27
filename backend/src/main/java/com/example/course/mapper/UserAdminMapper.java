package com.example.course.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.course.entity.UserAdmin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAdminMapper extends BaseMapper<UserAdmin> {}