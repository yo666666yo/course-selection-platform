package com.example.course.event;

import org.springframework.context.ApplicationEvent;

/**
 * 教务审核课程事件（通过或驳回）。
 */
public class CourseAuditedEvent extends ApplicationEvent {
    private final Long courseId;
    private final String courseName;
    private final boolean passed;  // true=通过(0→1), false=驳回(0→3)

    public CourseAuditedEvent(Object source, Long courseId,
                              String courseName, boolean passed) {
        super(source);
        this.courseId = courseId;
        this.courseName = courseName;
        this.passed = passed;
    }

    public Long getCourseId()    { return courseId; }
    public String getCourseName() { return courseName; }
    public boolean isPassed()     { return passed; }
}
