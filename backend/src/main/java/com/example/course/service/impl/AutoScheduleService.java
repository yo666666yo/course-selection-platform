package com.example.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.course.entity.Classroom;
import com.example.course.entity.Course;
import com.example.course.entity.CourseSchedule;
import com.example.course.mapper.ClassroomMapper;
import com.example.course.mapper.CourseMapper;
import com.example.course.mapper.CourseScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoScheduleService {

    @Autowired private CourseMapper courseMapper;
    @Autowired private ClassroomMapper classroomMapper;
    @Autowired private CourseScheduleMapper scheduleMapper;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_SLOTS = 13;

    // 专业忙碌表：Key="专业-周-节", Value="占用课程类型"
    private Map<String, String> majorBusyMap = new HashMap<>();
    // 体育课时间锁：Key="专业", Value="1-1,1-2"
    private Map<String, String> majorPeTimeMap = new HashMap<>();

    @Transactional(rollbackFor = Exception.class)
    public String autoSchedule() {
        // 1. 获取待排课课程
        List<Course> pendingCourses = courseMapper.selectList(new QueryWrapper<Course>().eq("status", 1));
        if (pendingCourses.isEmpty()) return "没有待排课的课程";

        List<Classroom> classrooms = classroomMapper.selectList(null);
        if (classrooms.isEmpty()) return "没有教室资源";

        // 2. 初始化忙碌状态
        initBusyMaps();

        int successCount = 0;
        List<String> failedReasons = new ArrayList<>();

        // 3. 排序：体育课 > 专业课 > 通选课
        pendingCourses.sort((c1, c2) -> getTypePriority(c2.getType()) - getTypePriority(c1.getType()));

        for (Course course : pendingCourses) {
            boolean isScheduled = false;
            String type = course.getType();
            String targetLoc = StringUtils.hasText(course.getLocationPreference()) ? course.getLocationPreference() : "教学楼";
            int totalCredits = course.getCredit();

            // 获取偏好（如果是锁定的体育课，这里返回的是锁定时间）
            String prefs = getTeacherPreferences(course);
            boolean isPeLocked = "体育课".equals(type) && StringUtils.hasText(course.getTargetMajors()) && hasPeLock(course);

            // === 策略 1：优先尝试满足偏好时间 ===
            if (StringUtils.hasText(prefs)) {
                for (Classroom room : classrooms) {
                    if (!isLocationMatch(room.getLocation(), targetLoc)) continue;
                    if (room.getCapacity() < course.getMaxCount()) continue;

                    // 尝试用偏好排课
                    if (trySchedule(course, room, prefs, totalCredits)) {
                        isScheduled = true;
                        break;
                    }
                }
            }

            // === 策略 2：智能调剂（仅当策略1失败 且 不是被锁定的体育课时）===
            // 如果是已锁定的体育课，必须严格遵守同专业同时间，不能随意调剂
            if (!isScheduled && !isPeLocked) {
                for (Classroom room : classrooms) {
                    if (!isLocationMatch(room.getLocation(), targetLoc)) continue;
                    if (room.getCapacity() < course.getMaxCount()) continue;

                    // 传入 null 作为 prefs，表示允许使用任意空闲时间
                    if (trySchedule(course, room, null, totalCredits)) {
                        isScheduled = true;
                        break;
                    }
                }
            }

            if (isScheduled) {
                successCount++;
            } else {
                failedReasons.add("《" + course.getName() + "》(" + type + "): 资源不足或严重冲突");
            }
        }

        return "排课完成: " + successCount + " 门成功，" + failedReasons.size() + " 门失败。\n" + String.join("\n", failedReasons);
    }

    /**
     * 尝试在指定教室排课
     * @param forcePrefs 强制时间偏好（如果不为null，必须先满足这些时间；如果为null，则自动找空位）
     */
    private boolean trySchedule(Course course, Classroom room, String forcePrefs, int totalCredits) {
        List<int[]> proposedSlots = new ArrayList<>();
        int creditsToFill = totalCredits;

        // A. 满足强制偏好
        if (StringUtils.hasText(forcePrefs)) {
            List<int[]> prefSlots = parseSlots(forcePrefs);
            for (int[] p : prefSlots) {
                if (checkConflict(room.getId(), p[0], p[1], course, proposedSlots)) {
                    return false; // 偏好冲突，该方案失败
                }
                proposedSlots.add(p);
                creditsToFill--;
            }
        }

        // B. 自动填充剩余学分
        if (creditsToFill > 0) {
            if (!fillRemainingCredits(room.getId(), course, creditsToFill, proposedSlots)) {
                return false; // 无法填满，该方案失败
            }
        }

        // C. 执行排课
        doSchedule(course, room, proposedSlots);
        updateBusyMap(course, proposedSlots);
        return true;
    }

    private boolean checkConflict(Long roomId, int day, int slot, Course course, List<int[]> currentSolution) {
        // 1. 教室冲突
        Long count = scheduleMapper.selectCount(new QueryWrapper<CourseSchedule>()
                .eq("classroom_id", roomId).eq("day_of_week", day).eq("time_slot", slot));
        if (count > 0) return true;

        // 2. 自身重复检查
        for (int[] p : currentSolution) {
            if (p[0] == day && p[1] == slot) return true;
        }

        // 3. 通选课跳过专业检查
        if ("通选课".equals(course.getType())) return false;

        // 4. 专业冲突检查
        if (StringUtils.hasText(course.getTargetMajors())) {
            for (String major : course.getTargetMajors().split(",")) {
                String key = major + "-" + day + "-" + slot;

                if ("体育课".equals(course.getType())) {
                    // 检查时间锁
                    if (majorPeTimeMap.containsKey(major) && !isTimeInLock(major, day, slot)) return true;
                    // 体育课互斥逻辑：只撞专业课，不撞其他体育课
                    if (majorBusyMap.containsKey(key) && "专业课".equals(majorBusyMap.get(key))) return true;
                } else {
                    // 专业课撞任何都冲突
                    if (majorBusyMap.containsKey(key)) return true;
                }
            }
        }
        return false;
    }

    private String getTeacherPreferences(Course course) {
        if ("体育课".equals(course.getType()) && StringUtils.hasText(course.getTargetMajors())) {
            for (String major : course.getTargetMajors().split(",")) {
                if (majorPeTimeMap.containsKey(major)) return majorPeTimeMap.get(major);
            }
        }
        return course.getTimePreferences();
    }

    private boolean hasPeLock(Course course) {
        if (!StringUtils.hasText(course.getTargetMajors())) return false;
        for (String major : course.getTargetMajors().split(",")) {
            if (majorPeTimeMap.containsKey(major)) return true;
        }
        return false;
    }

    private void updateBusyMap(Course course, List<int[]> slots) {
        if ("通选课".equals(course.getType()) || !StringUtils.hasText(course.getTargetMajors())) return;
        String slotStr = slots.stream().map(p -> p[0] + "-" + p[1]).collect(Collectors.joining(","));
        for (String major : course.getTargetMajors().split(",")) {
            for (int[] p : slots) {
                String key = major + "-" + p[0] + "-" + p[1];
                if (!majorBusyMap.containsKey(key)) majorBusyMap.put(key, course.getType());
                else if ("专业课".equals(course.getType())) majorBusyMap.put(key, "专业课");
            }
            if ("体育课".equals(course.getType())) majorPeTimeMap.put(major, slotStr);
        }
    }

    private boolean isTimeInLock(String major, int day, int slot) {
        String lockStr = majorPeTimeMap.get(major);
        if (lockStr == null) return false;
        String target = day + "-" + slot;
        for (String s : lockStr.split(",")) if (s.equals(target)) return true;
        return false;
    }

    private boolean fillRemainingCredits(Long roomId, Course course, int needed, List<int[]> currentSolution) {
        while (needed >= 2) {
            int[] chunk = findFreeChunk(roomId, course, 2, currentSolution);
            if (chunk == null) return false;
            currentSolution.add(new int[]{chunk[0], chunk[1]});
            currentSolution.add(new int[]{chunk[0], chunk[1]+1});
            needed -= 2;
        }
        while (needed > 0) {
            int[] chunk = findFreeChunk(roomId, course, 1, currentSolution);
            if (chunk == null) return false;
            currentSolution.add(new int[]{chunk[0], chunk[1]});
            needed -= 1;
        }
        return true;
    }

    private int[] findFreeChunk(Long roomId, Course course, int duration, List<int[]> existingSlots) {
        for (int d = 1; d <= 5; d++) {
            for (int s = 1; s <= MAX_SLOTS - duration + 1; s++) {
                boolean chunkOk = true;
                for (int k = 0; k < duration; k++) {
                    if (checkConflict(roomId, d, s + k, course, existingSlots)) {
                        chunkOk = false; break;
                    }
                }
                if (chunkOk) return new int[]{d, s};
            }
        }
        return null;
    }

    private void initBusyMaps() {
        majorBusyMap.clear();
        majorPeTimeMap.clear();
        List<Course> booked = courseMapper.selectList(new QueryWrapper<Course>().eq("status", 2));
        for (Course c : booked) {
            List<CourseSchedule> scheds = scheduleMapper.selectList(new QueryWrapper<CourseSchedule>().eq("course_id", c.getId()));
            if(scheds.isEmpty()) continue;
            List<int[]> slots = scheds.stream().map(s -> new int[]{s.getDayOfWeek(), s.getTimeSlot()}).collect(Collectors.toList());
            updateBusyMap(c, slots);
        }
    }

    private int getTypePriority(String type) {
        if ("体育课".equals(type)) return 3;
        if ("专业课".equals(type)) return 2;
        return 1;
    }

    private List<int[]> parseSlots(String str) {
        List<int[]> list = new ArrayList<>();
        if (!StringUtils.hasText(str)) return list;
        try {
            for(String s : str.split(",")) {
                String[] p = s.split("-");
                list.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])});
            }
        } catch(Exception e) {}
        return list;
    }

    private boolean isLocationMatch(String roomLoc, String targetType) {
        if (roomLoc == null) return false;
        if ("体育馆".equals(targetType)) return roomLoc.contains("体育") || roomLoc.contains("操场") || roomLoc.contains("篮球") || roomLoc.contains("羽毛球");
        if ("实验楼".equals(targetType)) return roomLoc.contains("实验") || roomLoc.contains("机房") || roomLoc.contains("电脑");
        boolean isSpecial = roomLoc.contains("体育") || roomLoc.contains("实验") || roomLoc.contains("机房");
        if ("教学楼".equals(targetType)) return !isSpecial;
        return true;
    }

    private void doSchedule(Course course, Classroom room, List<int[]> slots) {
        for (int[] p : slots) {
            CourseSchedule s = new CourseSchedule();
            s.setCourseId(course.getId());
            s.setClassroomId(room.getId());
            s.setDayOfWeek(p[0]);
            s.setTimeSlot(p[1]);
            scheduleMapper.insert(s);
        }
        course.setStatus(2);
        courseMapper.updateById(course);
        redisTemplate.opsForValue().set("course:stock:" + course.getId(), course.getMaxCount());
    }
}