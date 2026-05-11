package com.example.course.event.listener;

import com.example.course.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 课程域事件监听器。
 * 所有监听方法异步执行，不阻塞主业务流程。
 *
 * 当前实现：日志记录 + 审计追踪。
 * 未来可扩展：站内通知、WebSocket 推送、邮件、数据仓库同步等。
 */
@Component
public class CourseEventListener {

    private static final Logger log = LoggerFactory.getLogger(CourseEventListener.class);

    // ==================== 选课 / 退课 ====================

    @Async
    @EventListener
    public void onCourseSelected(CourseSelectedEvent event) {
        log.info("[选课] 学生={} 选了课程=[{}](id={}), 剩余容量={}",
                event.getStudentId(), event.getCourseName(),
                event.getCourseId(), event.getRemainingStock());
    }

    @Async
    @EventListener
    public void onCourseDropped(CourseDroppedEvent event) {
        log.info("[退课] 学生={} 退了课程=[{}](id={}), 剩余容量={}",
                event.getStudentId(), event.getCourseName(),
                event.getCourseId(), event.getRemainingStock());
    }

    // ==================== 课程申报 / 审核 ====================

    @Async
    @EventListener
    public void onCourseProposed(CourseProposedEvent event) {
        log.info("[申报] 教师=[{}] 提交了课程=[{}](id={})",
                event.getTeacherName(), event.getCourseName(), event.getCourseId());
    }

    @Async
    @EventListener
    public void onCourseAudited(CourseAuditedEvent event) {
        String action = event.isPassed() ? "通过" : "驳回";
        log.info("[审核] 课程=[{}](id={}) 被{}",
                event.getCourseName(), event.getCourseId(), action);
    }

    // ==================== 排课 / 重置 ====================

    @Async
    @EventListener
    public void onScheduleGenerated(ScheduleGeneratedEvent event) {
        log.info("[排课] 自动排课完成，共排课 {} 门", event.getScheduledCount());
    }

    @Async
    @EventListener
    public void onSystemReset(SystemResetEvent event) {
        log.info("[重置] 管理员=[{}] 执行了系统数据重置", event.getAdminName());
    }
}
