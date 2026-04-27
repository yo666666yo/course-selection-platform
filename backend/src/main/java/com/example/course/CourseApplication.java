package com.example.course;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.course.mapper") // 关键：告诉系统去哪里找数据库接口
public class CourseApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseApplication.class, args);
        System.out.println("✅ 恭喜！系统启动成功！");
    }
}