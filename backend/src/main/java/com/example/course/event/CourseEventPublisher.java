package com.example.course.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 课程域事件发布器。
 * 封装 Spring ApplicationEventPublisher，提供类型安全的发布方法。
 */
@Component
public class CourseEventPublisher {

    private final ApplicationEventPublisher publisher;

    public CourseEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void courseSelected(Long courseId, Long studentId, String courseName, int remaining) {
        publisher.publishEvent(new CourseSelectedEvent(this, courseId, studentId, courseName, remaining));
    }

    public void courseDropped(Long courseId, Long studentId, String courseName, int remaining) {
        publisher.publishEvent(new CourseDroppedEvent(this, courseId, studentId, courseName, remaining));
    }

    public void courseProposed(Long courseId, String courseName, String teacherName) {
        publisher.publishEvent(new CourseProposedEvent(this, courseId, courseName, teacherName));
    }

    public void courseAudited(Long courseId, String courseName, boolean passed) {
        publisher.publishEvent(new CourseAuditedEvent(this, courseId, courseName, passed));
    }

    public void scheduleGenerated(int count) {
        publisher.publishEvent(new ScheduleGeneratedEvent(this, count));
    }

    public void systemReset(String adminName) {
        publisher.publishEvent(new SystemResetEvent(this, adminName));
    }
}
