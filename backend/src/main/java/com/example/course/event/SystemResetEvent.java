package com.example.course.event;

import org.springframework.context.ApplicationEvent;

/**
 * 教务一键重置系统事件。
 * 清空所有选课记录、排课记录，课程回滚至待排课。
 */
public class SystemResetEvent extends ApplicationEvent {
    private final String adminName;

    public SystemResetEvent(Object source, String adminName) {
        super(source);
        this.adminName = adminName;
    }

    public String getAdminName() { return adminName; }
}
