package com.example.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime; // 注意导入这个时间包

@Data
@TableName("student_course")
public class StudentCourse {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId; // 对应数据库 student_id

    private Long courseId;  // 对应数据库 course_id

    private LocalDateTime createTime; // 对应数据库 create_time
}