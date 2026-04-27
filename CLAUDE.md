# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

All commands run from the respective subdirectory.

### Frontend (`frontend/`)

```bash
cd frontend
npm install        # install deps (first-time setup)
npm run dev        # start Vite dev server
npm run build      # production build to frontend/dist/
npm run preview    # preview production build
```

### Backend (`backend/`)

```bash
cd backend
mvn spring-boot:run    # start Spring Boot on http://localhost:8080
```

There is no test runner and no linter configured. Do not invent `npm test` or `npm run lint`.

## Architecture

This is a **full-stack** monorepo: Vue 3 frontend + Spring Boot backend.

### Frontend

The frontend calls the backend at `http://localhost:8080` — the URL is hardcoded as `API_BASE` at `frontend/src/App.vue:379`. When the UI shows "无法连接服务器" or fetch errors, the backend isn't running.

Auth token is stored in `localStorage` under the key `satoken` (sa-token convention) and user info under `user_info`. These are both restored in `onMounted` (`frontend/src/App.vue:674`).

## Architecture

**This is a single-component SPA.** Virtually all application logic, state, and markup live in `frontend/src/App.vue` (~830 lines). `frontend/src/main.js` only mounts the app and registers Element Plus globally. `frontend/src/components/HelloWorld.vue` is leftover Vite scaffolding and is unused.

There is **no Vue Router, no Pinia/Vuex, no API client module, no services layer**. Before adding new structure, understand that the current code intentionally keeps everything in `App.vue`:

- **"Routing"** is a single `activeMenu` ref switched by `handleMenuSelect`; each "page" is a `v-if="activeMenu === '...'"` block inside `<el-main>`. The menu itself is rendered conditionally on `currentUser.role` (`student` / `teacher` / `admin`).
- **State** is local `ref`/`reactive` in `App.vue`: `allCourses`, `availableCourses`, `myCourseList`, `myScheduleCells`, `classroomList`, etc.
- **API calls** are inline `axios.get/post` using the global `API_BASE`. There is no interceptor wiring or centralized error handling — each fetcher handles its own try/catch and `ElMessage`.

### Domain model

- **Three roles**: `student`, `teacher`, `admin`. Role is returned by `/login` and drives menu visibility.
- **Course lifecycle** (`statusMap` at `frontend/src/App.vue:465`): `0` 待审核 → `1` 待排课 → `2` 已发布, or `3` 已驳回.
  - Teachers submit via `POST /course/propose` (status 0).
  - Admins approve/reject via `POST /course/audit?courseId=&pass=` (0→1 or 0→3).
  - Admin `POST /course/auto-schedule` batch-assigns time+room and moves courses to status 2.
  - `GET /course/reset-redis` wipes selections and rolls courses back to 待排课.
- **Course types**: `专业课` / `体育课` / `通选课` — `getFilteredCourses(menu)` filters `availableCourses` by type for the three student selection views.
- **Schedule grid**: days are `1–5` (周一–周五), slots are `1–13` with fixed times in `getSlotTime`. Multi-slot courses are rendered via the `shouldShowCell` / `getCellRowspan` pair — when editing the timetable, both must stay consistent or cells will duplicate. The same pattern is mirrored for classroom schedules (`shouldShowClassroomCell` / `getClassroomRowspan`).

### Backend endpoints the frontend depends on

Auth: `POST /login`, `POST /logout`, `POST /user/updateInfo`
Course: `GET /course/list`, `GET /course/list-available?major=`, `GET /course/my-list`, `POST /course/select?courseId=`, `POST /course/drop?courseId=`, `POST /course/propose`, `POST /course/audit?courseId=&pass=`, `POST /course/auto-schedule`, `GET /course/reset-redis`
Classroom: `GET /classroom/list?keyword=`, `GET /classroom/schedule?roomId=`

Response convention: `{ code: 200, data, msg }`. `fetchCourses` tolerates a bare array as a fallback.

### Assets

Static images in `frontend/public/` are referenced by absolute paths from templates/CSS (e.g. `/seu_logo.png`, `/avatar1.png`, `/选课系统背景.jpg`). Filenames include Chinese characters — preserve them exactly when editing.

### UI language

All user-facing strings are **Simplified Chinese**. Match the existing tone (terse, uses emoji in card headers like `📝`, `🏫`, `🔍`) when adding new UI.

### Backend

Spring Boot 2.7.14 + Java 8 + MyBatis-Plus + MySQL + Redis + Sa-Token.

- **Entry**: `backend/src/main/java/com/example/course/CourseApplication.java`
- **Controllers**: `LoginController` (auth), `CourseController` (course CRUD, select/drop, audit, auto-schedule), `ClassroomController` (list/schedule), `UserController` (profile update)
- **Entities**: `Course`, `Classroom`, `CourseSchedule`, `StudentCourse`, `UserAdmin/UserStudent/UserTeacher`
- **Config**: `SaTokenConfigure` (auth), `MybatisPlusConfig` (ORM), `RedisConfig`
- **Service**: `CourseServiceImpl` (business logic), `AutoScheduleService` (auto scheduling algorithm)
- **Common**: `Result.java` (unified response wrapper), `GlobalExceptionHandler.java`

Database is MySQL (`course_db` on `localhost:3306`), Redis for caching. Auth uses Sa-Token with `satoken` key (mirrored in frontend localStorage).
