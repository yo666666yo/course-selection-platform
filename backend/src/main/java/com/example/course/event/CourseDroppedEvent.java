package com.example.course.event;

import org.springframework.context.ApplicationEvent;

/**
 * 学生退课成功事件。
 */
public class CourseDroppedEvent extends ApplicationEvent {
    private final Long courseId;
    private final Long studentId;
    private final String courseName;
    private final int remainingStock;

    public CourseDroppedEvent(Object source, Long courseId, Long studentId,
                              String courseName, int remainingStock) {
        super(source);
        this.courseId = courseId;
        this.studentId = studentId;
        this.courseName = courseName;
        this.remainingStock = remainingStock;
    }

    public Long getCourseId()      { return courseId; }
    public Long getStudentId()     { return studentId; }
    public String getCourseName()  { return courseName; }
    public int getRemainingStock() { return remainingStock; }
}
