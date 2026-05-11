package com.example.course.event;

import org.springframework.context.ApplicationEvent;

/**
 * 自动排课完成事件。
 * 所有待排课课程均已分配时间+教室。
 */
public class ScheduleGeneratedEvent extends ApplicationEvent {
    private final int scheduledCount;

    public ScheduleGeneratedEvent(Object source, int scheduledCount) {
        super(source);
        this.scheduledCount = scheduledCount;
    }

    public int getScheduledCount() { return scheduledCount; }
}
