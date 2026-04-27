package com.example.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("classroom")
public class Classroom {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomName; // 教室名称

    private Integer capacity; // 容纳人数

    // 👇 之前报错就是因为少了这一行！
    private String location; // 所属地点 (教学楼/实验楼/体育馆)
}