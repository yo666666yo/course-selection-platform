package com.example.course.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.course.entity.Classroom;
import com.example.course.entity.Course;
import com.example.course.entity.CourseSchedule;
import com.example.course.entity.StudentCourse;
import com.example.course.mapper.ClassroomMapper;
import com.example.course.mapper.CourseMapper;
import com.example.course.mapper.CourseScheduleMapper;
import com.example.course.mapper.StudentCourseMapper;
import com.example.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import com.example.course.event.CourseEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired private StudentCourseMapper studentCourseMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private CourseScheduleMapper scheduleMapper;
    @Autowired private ClassroomMapper classroomMapper;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private CourseEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectCourse(Long studentId, Long courseId) {
        // 1. 状态校验
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() != 2) {
            throw new RuntimeException("课程尚未发布或已下架");
        }

        String stockKey = "course:stock:" + courseId;
        String userKey = "course:users:" + courseId;

        // 2. 幂等性校验
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(userKey, studentId))) {
            throw new RuntimeException("您已经选过该课程");
        }

        // 3. ✨✨✨ 核心增加：时间冲突检测 ✨✨✨
        // 获取当前要选课程的时间表
        List<CourseSchedule> newSchedules = scheduleMapper.selectList(new QueryWrapper<CourseSchedule>().eq("course_id", courseId));
        if (newSchedules != null && !newSchedules.isEmpty()) {
            // 获取该学生已选的所有课程
            List<StudentCourse> existingSelections = studentCourseMapper.selectList(new QueryWrapper<StudentCourse>().eq("student_id", studentId));
            if (!existingSelections.isEmpty()) {
                List<Long> existingIds = existingSelections.stream().map(StudentCourse::getCourseId).collect(Collectors.toList());
                // 获取已选课程的时间表
                List<CourseSchedule> existingSchedules = scheduleMapper.selectList(new QueryWrapper<CourseSchedule>().in("course_id", existingIds));

                // 比对时间冲突
                for (CourseSchedule newS : newSchedules) {
                    for (CourseSchedule oldS : existingSchedules) {
                        if (newS.getDayOfWeek().equals(oldS.getDayOfWeek()) && newS.getTimeSlot().equals(oldS.getTimeSlot())) {
                            // 查出冲突课程的名字，给学生友好提示
                            Course conflictCourse = courseMapper.selectById(oldS.getCourseId());
                            throw new RuntimeException("时间冲突！该时段（周" + newS.getDayOfWeek() + " 第" + newS.getTimeSlot() + "节）已选课程：《" + conflictCourse.getName() + "》");
                        }
                    }
                }
            }
        }

        // 4. 预减库存
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        if (stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("手慢了！课程已满员");
        }

        try {
            // 5. DB 落库
            Long count = studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>().eq("student_id", studentId).eq("course_id", courseId));
            if (count > 0) {
                redisTemplate.opsForValue().increment(stockKey);
                redisTemplate.opsForSet().add(userKey, studentId);
                throw new RuntimeException("您已经选过该课程");
            }

            StudentCourse record = new StudentCourse();
            record.setStudentId(studentId);
            record.setCourseId(courseId);
            record.setCreateTime(LocalDateTime.now());
            studentCourseMapper.insert(record);

            update(new UpdateWrapper<Course>().setSql("selected_count = selected_count + 1").eq("id", courseId));
            redisTemplate.opsForSet().add(userKey, studentId);

            eventPublisher.courseSelected(courseId, studentId, course.getName(), (int)(long)stock);

        } catch (Exception e) {
            redisTemplate.opsForValue().increment(stockKey);
            throw e; // 抛出异常供 Controller 捕获
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dropCourse(Long studentId, Long courseId) {
        QueryWrapper<StudentCourse> query = new QueryWrapper<StudentCourse>().eq("student_id", studentId).eq("course_id", courseId);
        if (studentCourseMapper.delete(query) > 0) {
            Course course = courseMapper.selectById(courseId);
            int remaining = redisTemplate.opsForValue().increment("course:stock:" + courseId).intValue();
            courseMapper.update(null, new UpdateWrapper<Course>()
                    .setSql("selected_count = CASE WHEN selected_count > 0 THEN selected_count - 1 ELSE 0 END")
                    .eq("id", courseId));
            redisTemplate.opsForSet().remove("course:users:" + courseId, studentId);

            eventPublisher.courseDropped(courseId, studentId,
                    course != null ? course.getName() : "未知", remaining);
        } else {
            throw new RuntimeException("您未选修该课程");
        }
    }

    @Override
    public List<Course> getMyCourses(Long studentId) {
        List<StudentCourse> records = studentCourseMapper.selectList(new QueryWrapper<StudentCourse>().eq("student_id", studentId));
        if (records.isEmpty()) return new ArrayList<>();
        List<Long> courseIds = records.stream().map(StudentCourse::getCourseId).collect(Collectors.toList());
        List<Course> courses = courseMapper.selectBatchIds(courseIds);
        populateScheduleInfo(courses);
        return courses;
    }

    @Override
    public List<Course> getListWithSchedule() {
        List<Course> courses = this.list();
        populateScheduleInfo(courses);
        return courses;
    }

    @Override
    public List<Course> getAvailableCourses(String major) {
        List<Course> allCourses = this.list(new QueryWrapper<Course>().eq("status", 2));
        populateScheduleInfo(allCourses);
        if (!StringUtils.hasText(major)) return allCourses;
        return allCourses.stream().filter(c -> {
            String targets = c.getTargetMajors();
            return !StringUtils.hasText(targets) || targets.contains(major);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetSystem() {
        // ✨✨✨ 1. 清空所有选课记录 ✨✨✨
        studentCourseMapper.delete(null);

        // ✨✨✨ 2. 清空排课记录 (新增) ✨✨✨
        // 既然要回到“待排课”状态，旧的排课时间表必须清除
        scheduleMapper.delete(null);

        // ✨✨✨ 3. 重置课程状态为 1 (待排课) 并清零计数 ✨✨✨
        // 仅重置状态为 1(待排课), 2(已发布), 3(驳回) 的课程。
        // 状态 0 (待审核) 保持不变，还是待审核。
        Course resetEntity = new Course();
        resetEntity.setStatus(1);
        resetEntity.setSelectedCount(0);

        courseMapper.update(resetEntity, new UpdateWrapper<Course>().in("status", Arrays.asList(1, 2, 3)));

        // 保险起见，状态0的课程计数也清零
        Course zeroEntity = new Course();
        zeroEntity.setSelectedCount(0);
        courseMapper.update(zeroEntity, new UpdateWrapper<Course>().eq("status", 0));

        // ✨✨✨ 4. 清空 Redis ✨✨✨
        Set<String> keys = redisTemplate.keys("course:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 注意：这里不再预热库存，因为课程变成了状态 1，不可选课。

        String adminName = "";
        try { adminName = StpUtil.getSession().getString("name"); } catch (Exception ignored) {}
        eventPublisher.systemReset(adminName);
    }

    private void populateScheduleInfo(List<Course> courses) {
        if (courses.isEmpty()) return;
        List<CourseSchedule> allSchedules = scheduleMapper.selectList(null);
        Map<Long, List<CourseSchedule>> scheduleMap = allSchedules.stream().collect(Collectors.groupingBy(CourseSchedule::getCourseId));
        List<Classroom> classrooms = classroomMapper.selectList(null);
        Map<Long, Classroom> roomMap = classrooms.stream().collect(Collectors.toMap(Classroom::getId, c -> c));
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (Course course : courses) {
            List<CourseSchedule> list = scheduleMap.get(course.getId());
            if (list != null && !list.isEmpty()) {
                course.setScheduleList(list);
                Map<Integer, List<Integer>> daySlots = new TreeMap<>();
                Long roomId = list.get(0).getClassroomId();
                for (CourseSchedule s : list) {
                    daySlots.computeIfAbsent(s.getDayOfWeek(), k -> new ArrayList<>()).add(s.getTimeSlot());
                    roomId = s.getClassroomId();
                }
                StringBuilder timeStr = new StringBuilder();
                for (Map.Entry<Integer, List<Integer>> entry : daySlots.entrySet()) {
                    int day = entry.getKey();
                    if (day < 1 || day > 7) continue;
                    List<Integer> slots = entry.getValue();
                    Collections.sort(slots);
                    String slotStr = compressSlots(slots);
                    if (timeStr.length() > 0) timeStr.append(", ");
                    timeStr.append(days[day - 1]).append(" ").append(slotStr).append("节");
                }
                course.setScheduleTime(timeStr.toString());
                Classroom room = roomMap.get(roomId);
                course.setScheduleLocation(room != null ? room.getRoomName() : "未知");
            } else {
                course.setScheduleTime("待安排");
                course.setScheduleLocation("待定");
                course.setScheduleList(new ArrayList<>());
            }
        }
    }

    private String compressSlots(List<Integer> slots) {
        if (slots.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int start = slots.get(0);
        int prev = start;
        for (int i = 1; i < slots.size(); i++) {
            int curr = slots.get(i);
            if (curr != prev + 1) {
                appendRange(sb, start, prev);
                sb.append(",");
                start = curr;
            }
            prev = curr;
        }
        appendRange(sb, start, prev);
        return sb.toString();
    }

    private void appendRange(StringBuilder sb, int start, int end) {
        if (start == end) sb.append(start);
        else sb.append(start).append("-").append(end);
    }
}