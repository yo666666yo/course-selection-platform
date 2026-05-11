package com.example.course.event;

import org.springframework.context.ApplicationEvent;

/**
 * 教师提交新课程申报事件。
 */
public class CourseProposedEvent extends ApplicationEvent {
    private final Long courseId;
    private final String courseName;
    private final String teacherName;

    public CourseProposedEvent(Object source, Long courseId,
                               String courseName, String teacherName) {
        super(source);
        this.courseId = courseId;
        this.courseName = courseName;
        this.teacherName = teacherName;
    }

    public Long getCourseId()     { return courseId; }
    public String getCourseName()  { return courseName; }
    public String getTeacherName() { return teacherName; }
}
