package com.example.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("course")
public class Course implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String teacherName;
    private Integer maxCount;
    private Integer selectedCount;

    // 0: 待审核, 1: 待排课, 2: 已发布, 3: 驳回
    private Integer status;

    private Integer credit;
    private String timePreferences;
    private String locationPreference;
    private String targetMajors;
    private String type;
    private String intro;

    // ✨✨✨ 乐观锁版本号 ✨✨✨
    @Version
    private Integer version;

    // --- 下面是不存数据库的展示字段 ---
    @TableField(exist = false)
    private String scheduleTime; // e.g. "周一 1-2节"

    @TableField(exist = false)
    private String scheduleLocation;

    @TableField(exist = false)
    private List<CourseSchedule> scheduleList;
}