// CourseSchedule.java
package com.example.course.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("course_schedule")
public class CourseSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long classroomId;
    private Integer dayOfWeek; // 1=周一
    private Integer timeSlot;  // 1=第一大节(8:00-9:40)
}