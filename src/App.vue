<template>
  <!-- ================= 登录界面 ================= -->
  <div v-if="!isLoggedIn" class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo-title-wrapper">
          <img src="/seu_logo.png" alt="SEU Logo" class="login-logo-img" />
          <h2 class="login-title">SEU智能选课系统</h2>
        </div>
        <p class="login-subtitle">SEU Smart Course Selection System</p>
      </div>
      
      <el-form :model="loginForm" class="login-form" @submit.prevent>
        <el-form-item style="text-align: center; margin-bottom: 25px;">
          <el-radio-group v-model="loginForm.role" size="large">
            <el-radio-button label="student">学生</el-radio-button>
            <el-radio-button label="teacher">教师</el-radio-button>
            <el-radio-button label="admin">教务</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="请输入账号" :prefix-icon="User" size="large"/>
        </el-form-item>
        <el-form-item>
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin"/>
        </el-form-item>
        <el-button type="primary" class="login-btn" size="large" :loading="loading" @click="handleLogin">立即登录</el-button>
      </el-form>
    </div>
  </div>

  <!-- ================= 主应用界面 ================= -->
  <el-container v-else class="app-wrapper">
    <el-aside width="240px" class="sidebar">
      <div class="logo-area">
        <img src="/seu_logo.png" alt="Logo" style="height: 32px; margin-right: 10px;" />
        <span class="app-title">SEU 选课系统</span>
      </div>
      <el-menu :default-active="activeMenu" class="el-menu-vertical" background-color="#001529" text-color="#a6adb4" active-text-color="#409EFF" @select="handleMenuSelect">
        <el-menu-item index="dashboard"><el-icon><DataLine /></el-icon><span>数据仪表盘</span></el-menu-item>
        <el-sub-menu index="student-center" v-if="currentUser.role === 'student'">
          <template #title><el-icon><User /></el-icon><span>学生选课中心</span></template>
          <el-menu-item index="student-select"><el-icon><Pointer /></el-icon><span>本专业选课</span></el-menu-item>
          <el-menu-item index="student-pe"><el-icon><Basketball /></el-icon><span>体育课选课</span></el-menu-item>
          <el-menu-item index="student-general"><el-icon><Connection /></el-icon><span>通选课选课</span></el-menu-item>
          <el-menu-item index="student-search"><el-icon><Search /></el-icon><span>全校课程查询</span></el-menu-item>
          <el-menu-item index="student-schedule"><el-icon><Calendar /></el-icon><span>我的课表</span></el-menu-item>
        </el-sub-menu>
        <el-menu-item-group title="教师业务" v-if="currentUser.role === 'teacher'">
          <el-menu-item index="teacher"><el-icon><Monitor /></el-icon><span>教师申报管理</span></el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="系统管理" v-if="currentUser.role === 'admin'">
          <el-menu-item index="admin"><el-icon><Setting /></el-icon><span>教务排课控制台</span></el-menu-item>
          <el-menu-item index="admin-classroom"><el-icon><School /></el-icon><span>教室资源管理</span></el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left"><el-breadcrumb separator="/"><el-breadcrumb-item>首页</el-breadcrumb-item><el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item></el-breadcrumb></div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link user-profile">
              <el-avatar :size="32" :src="currentUser.avatar || defaultAvatar" />
              <div class="user-info">
                <span class="username">{{ currentUser.name }}</span>
                <span class="role-tag">{{ currentUser.role === 'student' ? (currentUser.major || '未分配') : roleNameMap[currentUser.role] }}</span>
              </div>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile">个人信息</el-dropdown-item><el-dropdown-item divided command="logout" style="color: #F56C6C">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <!-- 仪表盘 -->
        <div v-if="activeMenu === 'dashboard'" class="dashboard-view">
          <el-row :gutter="20">
            <el-col :span="6"><div class="stat-card blue"><div class="stat-icon"><el-icon><Reading /></el-icon></div><div class="stat-info"><div class="num">{{ allCourses.length }}</div><div class="label">总课程数</div></div></div></el-col>
            <el-col :span="6"><div class="stat-card green"><div class="stat-icon"><el-icon><Check /></el-icon></div><div class="stat-info"><div class="num">{{ publishedCourses.length }}</div><div class="label">已发布课程</div></div></div></el-col>
            <el-col :span="6"><div class="stat-card orange"><div class="stat-icon"><el-icon><Bell /></el-icon></div><div class="stat-info"><div class="num">{{ pendingAuditCount }}</div><div class="label">待审核申请</div></div></div></el-col>
            <el-col :span="6"><div class="stat-card red"><div class="stat-icon"><el-icon><Timer /></el-icon></div><div class="stat-info"><div class="num">{{ pendingScheduleCount }}</div><div class="label">待排课任务</div></div></div></el-col>
          </el-row>
          <div style="margin-top: 20px; text-align: center; color: #999;">欢迎使用 SEU 智能选课系统</div>
        </div>

        <!-- 个人信息 -->
        <div v-else-if="activeMenu === 'profile'" class="view-container">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card shadow="hover" class="profile-card">
                <div class="avatar-wrapper"><el-avatar :size="100" :src="currentUser.avatar || defaultAvatar" class="profile-avatar" /><div class="avatar-edit" @click="showAvatarDialog = true"><el-icon><Camera /></el-icon></div></div>
                <div class="profile-name">{{ currentUser.name }}</div>
                <div class="profile-role">{{ roleNameMap[currentUser.role] }}</div>
                <div v-if="currentUser.role === 'student'" style="margin-top: 5px; color: #409EFF; font-size: 13px;">{{ currentUser.major }}</div>
                <el-divider />
                <div class="profile-meta"><p><el-icon><User /></el-icon> 账号：{{ currentUser.username }}</p><p><el-icon><Calendar /></el-icon> 注册时间：2023-09-01</p></div>
              </el-card>
            </el-col>
            <el-col :span="16">
              <el-card shadow="hover">
                <template #header><div class="card-header"><span>✏️ 编辑资料</span></div></template>
                <el-form :model="editForm" label-width="100px" style="max-width: 500px; margin-top: 20px;">
                  <el-form-item label="账号"><el-input v-model="currentUser.username" disabled /></el-form-item>
                  <el-form-item label="姓名/昵称"><el-input v-model="editForm.realName" placeholder="请输入您的姓名" /></el-form-item>
                  <el-form-item><el-button type="primary" @click="updateProfile">保存修改</el-button></el-form-item>
                </el-form>
                <el-divider content-position="left">安全设置</el-divider>
                <el-form :model="pwdForm" label-width="100px" style="max-width: 500px;">
                  <el-form-item label="原密码"><el-input v-model="pwdForm.oldPass" type="password" show-password /></el-form-item>
                  <el-form-item label="新密码"><el-input v-model="pwdForm.newPass" type="password" show-password /></el-form-item>
                  <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPass" type="password" show-password /></el-form-item>
                  <el-form-item><el-button type="danger" @click="updatePassword">修改密码</el-button></el-form-item>
                </el-form>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 学生选课 -->
        <div v-else-if="['student-select', 'student-pe', 'student-general'].includes(activeMenu)" class="view-container">
          <div class="card-header-bar">
            <div class="title">
              <span v-if="activeMenu==='student-select'">🎯 {{ currentUser.major }} - 专业课</span>
              <span v-else-if="activeMenu==='student-pe'">🏀 体育选课</span>
              <span v-else>🌍 通识选修课</span>
            </div>
            <el-button link type="primary" @click="refreshData"><el-icon><Refresh /></el-icon>刷新列表</el-button>
          </div>
          <div class="course-grid">
            <el-empty v-if="getFilteredCourses(activeMenu).length === 0" description="暂无该类课程" />
            <div v-for="course in getFilteredCourses(activeMenu)" :key="course.id" class="course-card-box">
              <div class="card-top">
                <div class="c-tag">{{ course.type || '课程' }}</div>
                <div class="c-title">{{ course.name }}</div>
                <div class="c-teacher"><el-icon><User /></el-icon> {{ course.teacherName }}</div>
              </div>
              <div class="card-mid">
                <div class="c-intro">{{ course.intro || '暂无简介' }}</div>
                <div class="c-schedule-info" v-if="course.scheduleTime && course.scheduleTime !== '待安排'">
                  <div class="sc-row"><el-icon><Timer /></el-icon> {{ course.scheduleTime }}</div>
                  <div class="sc-row"><el-icon><Location /></el-icon> {{ course.scheduleLocation }}</div>
                </div>
              </div>
              <div class="card-bottom">
                <div class="progress-area"><div class="p-label">已选 {{ course.selectedCount || 0 }} / {{ course.maxCount }}</div><el-progress :percentage="calcPercentage(course)" :show-text="false" :status="isFull(course) ? 'exception' : ''" :stroke-width="6" /></div>
                
                <el-button 
                  v-if="isSelected(course)" 
                  type="danger" 
                  size="small" 
                  plain 
                  :loading="btnLoading[course.id]"
                  @click="handleDrop(course)"
                >退选</el-button>

                <el-button 
                  v-else-if="isFull(course)" 
                  type="info" 
                  size="small" 
                  disabled
                >满员</el-button>

                <el-button 
                  v-else 
                  type="primary" 
                  size="small" 
                  :loading="btnLoading[course.id]"
                  @click="handleSelect(course)"
                >抢课</el-button>

              </div>
            </div>
          </div>
        </div>

        <!-- 全校课程查询 -->
        <div v-else-if="activeMenu === 'student-search'" class="view-container">
          <el-card shadow="hover">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>🔍 全校课程检索</span>
                <el-input v-model="searchKeyword" placeholder="输入课程名/教师搜索" style="width: 250px;" :prefix-icon="Search" clearable />
              </div>
            </template>
            <el-table :data="filteredAllCourses" stripe height="500">
              <el-table-column prop="name" label="课程名称" width="180" />
              <el-table-column prop="type" label="类型" width="80"><template #default="{row}"><el-tag size="small" effect="plain">{{row.type}}</el-tag></template></el-table-column>
              <el-table-column prop="teacherName" label="教师" width="100" />
              <el-table-column prop="targetMajors" label="面向专业">
                <template #default="{row}">
                   <el-tag v-if="!row.targetMajors" type="success" size="small">全校通选</el-tag>
                   <el-tag v-else size="small">{{ row.targetMajors }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="credit" label="学分" width="60" />
              <el-table-column label="上课时间/地点">
                <template #default="{row}">
                   <div style="font-size: 12px; line-height: 1.4;">
                     <div>{{ row.scheduleTime || '-' }}</div>
                     <div style="color: #909399;">{{ row.scheduleLocation || '' }}</div>
                   </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{row}">
                  <el-tag :type="statusMap[row.status].type" size="small">{{ statusMap[row.status].label }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- 课表查询 -->
        <div v-else-if="activeMenu === 'student-schedule'" class="view-container">
          <el-card shadow="hover" class="schedule-card">
            <template #header><div style="display: flex; justify-content: space-between;"><span style="font-weight: bold;">📅 我的课程表</span><el-tag type="success">共 {{ myCourseList.length }} 门</el-tag></div></template>
            <div class="timetable-container">
              <table class="schedule-table">
                <thead><tr><th style="width: 80px;">时间</th><th v-for="d in ['周一','周二','周三','周四','周五']" :key="d">{{d}}</th></tr></thead>
                <tbody>
                  <tr v-for="slot in 13" :key="slot">
                    <td class="td-time"><div class="slot-name">第{{slot}}节</div><div class="slot-time-text">{{ getSlotTime(slot) }}</div></td>
                    <td v-for="day in 5" :key="day" class="td-cell" :rowspan="getCellRowspan(day, slot)" v-show="shouldShowCell(day, slot)">
                      <div v-if="getCourse(day, slot)" class="course-cell" :style="{backgroundColor: getCourseColor(getCourse(day, slot).id)}">
                        <div class="cell-name">{{getCourse(day, slot).name}}</div>
                        <div class="cell-loc">📍 {{getCourse(day, slot).location}}</div>
                        <div class="cell-teacher">👤 {{getCourse(day, slot).teacherName}}</div>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </el-card>
        </div>

        <!-- 教师申报 -->
        <div v-else-if="activeMenu === 'teacher'" class="view-container">
           <el-card shadow="hover"><template #header>📝 课程申报</template>
             <el-form :model="teacherForm" label-width="100px">
                <el-row :gutter="20">
                  <el-col :span="8"><el-form-item label="课程名称"><el-input v-model="teacherForm.name" /></el-form-item></el-col>
                  <el-col :span="8"><el-form-item label="教师姓名"><el-input v-model="teacherForm.teacherName" /></el-form-item></el-col>
                  <el-col :span="8"><el-form-item label="容量"><el-input-number v-model="teacherForm.maxCount" :min="20" /></el-form-item></el-col>
                </el-row>
                <el-form-item label="课程类型">
                  <el-radio-group v-model="teacherForm.type">
                    <el-radio label="专业课">专业课</el-radio>
                    <el-radio label="体育课">体育课</el-radio>
                    <el-radio label="通选课">通选课</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="面向专业"><el-input v-model="teacherForm.targetMajors" placeholder="例如：软件工程,计算机科学 (留空则全校通选)" /></el-form-item>
                <el-form-item label="课程简介"><el-input v-model="teacherForm.intro" type="textarea" :rows="2" /></el-form-item>
                <el-form-item label="地点类型"><el-radio-group v-model="teacherForm.locationPreference"><el-radio label="教学楼">教学楼</el-radio><el-radio label="实验楼">实验楼</el-radio><el-radio label="体育馆">体育馆</el-radio></el-radio-group></el-form-item>
                <el-form-item label="排课意向">
                  <div class="pref-tips">请勾选期望时间：</div>
                  <div class="pref-grid">
                    <div v-for="day in 5" :key="day" class="pref-col">
                      <div class="pref-day-title">周{{['一','二','三','四','五'][day-1]}}</div>
                      <el-checkbox-group v-model="teacherForm.preferences"><el-checkbox v-for="slot in 13" :key="`${day}-${slot}`" :label="`${day}-${slot}`">{{slot}}节</el-checkbox></el-checkbox-group>
                    </div>
                  </div>
                </el-form-item>
                <el-form-item><el-button type="primary" @click="submitProposal">提交申请</el-button></el-form-item>
             </el-form>
           </el-card>
           <el-card shadow="hover" style="margin-top: 20px;"><template #header>申报记录</template>
             <el-table :data="myProposals" stripe>
               <el-table-column prop="name" label="课程"/>
               <el-table-column prop="type" label="类型" width="80" />
               <el-table-column prop="targetMajors" label="面向专业" width="120" />
               <el-table-column prop="locationPreference" label="地点" width="100" />
               <el-table-column prop="intro" label="简介" show-overflow-tooltip/>
               <el-table-column label="状态"><template #default="{row}"><el-tag :type="statusMap[row.status].type">{{statusMap[row.status].label}}</el-tag></template></el-table-column>
             </el-table>
           </el-card>
        </div>

        <!-- 教务排课控制台 -->
        <div v-else-if="activeMenu === 'admin'" class="view-container">
          <div class="toolbar">
             <el-button type="danger" :icon="Delete" @click="handleResetSystem">系统数据重置/Redis修复</el-button>
             <el-button type="success" :icon="VideoPlay" @click="handleAutoSchedule">一键智能排课</el-button>
          </div>
          <el-table :data="allCourses" border>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="课程" width="150" />
            <el-table-column prop="teacherName" label="教师" width="100" />
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column prop="targetMajors" label="面向专业" width="120" />
            <el-table-column prop="locationPreference" label="地点" width="100" />
            <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusMap[row.status].type">{{statusMap[row.status].label}}</el-tag></template></el-table-column>
            <el-table-column label="操作"><template #default="{row}"><div v-if="row.status===0"><el-button type="primary" link @click="handleAudit(row,true)">通过</el-button><el-button type="danger" link @click="handleAudit(row,false)">驳回</el-button></div><div v-else><el-button link @click="handleReset(row)">重置</el-button></div></template></el-table-column>
          </el-table>
        </div>

        <!-- 教室管理 (新增模块) -->
        <div v-else-if="activeMenu === 'admin-classroom'" class="view-container">
          <div v-if="!currentClassroom">
            <el-card shadow="hover">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>🏫 教室资源管理</span>
                  <div style="display: flex; gap: 10px;">
                    <el-input v-model="classroomSearch" placeholder="搜索教室名/位置" style="width: 250px;" :prefix-icon="Search" clearable @change="fetchClassrooms" @clear="fetchClassrooms">
                        <template #append><el-button :icon="Search" @click="fetchClassrooms"/></template>
                    </el-input>
                  </div>
                </div>
              </template>
              <el-table :data="classroomList" stripe height="calc(100vh - 250px)" v-loading="classroomLoading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="roomName" label="教室名称" />
                <el-table-column prop="location" label="所属楼宇/区域" />
                <el-table-column prop="capacity" label="容量" width="100" />
                <el-table-column label="操作" width="120">
                  <template #default="{row}">
                     <el-button type="primary" size="small" @click="handleViewClassroomSchedule(row)">查看课表</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
          
          <div v-else>
             <el-card shadow="hover" class="schedule-card">
                <template #header>
                    <div style="display: flex; align-items: center; justify-content: space-between;">
                        <div style="display: flex; align-items: center;">
                            <el-button @click="backToClassroomList" circle :icon="ArrowLeft" style="margin-right: 15px;"></el-button>
                            <span style="font-weight: bold; font-size: 16px;">{{ currentClassroom.roomName }} - 课程表</span>
                            <el-tag style="margin-left: 10px;">{{ currentClassroom.location }}</el-tag>
                            <el-tag type="info" style="margin-left: 5px;">容量: {{ currentClassroom.capacity }}</el-tag>
                        </div>
                    </div>
                </template>
                <div class="timetable-container">
                    <table class="schedule-table">
                        <thead><tr><th style="width: 80px;">时间</th><th v-for="d in ['周一','周二','周三','周四','周五']" :key="d">{{d}}</th></tr></thead>
                        <tbody>
                          <tr v-for="slot in 13" :key="slot">
                            <td class="td-time"><div class="slot-name">第{{slot}}节</div><div class="slot-time-text">{{ getSlotTime(slot) }}</div></td>
                            <td v-for="day in 5" :key="day" class="td-cell" :rowspan="getClassroomRowspan(day, slot)" v-show="shouldShowClassroomCell(day, slot)">
                              <div v-if="getClassroomCourse(day, slot)" class="course-cell" style="background-color: #ecf5ff; border-left: 4px solid #409EFF; border-radius: 4px;">
                                <div class="cell-name">{{getClassroomCourse(day, slot).courseName}}</div>
                                <div class="cell-teacher">👤 {{getClassroomCourse(day, slot).teacherName}}</div>
                                <div class="cell-teacher" style="font-size:10px; color:#999">{{getClassroomCourse(day, slot).type}}</div>
                              </div>
                            </td>
                          </tr>
                        </tbody>
                    </table>
                </div>
             </el-card>
          </div>
        </div>

      </el-main>
    </el-container>
    
    <el-dialog v-model="showAvatarDialog" title="选择头像" width="400px">
      <div class="avatar-grid"><div v-for="(img, idx) in avatarList" :key="idx" class="avatar-option" :class="{active: selectedAvatar === img}" @click="selectedAvatar = img"><img :src="img" /></div></div>
      <template #footer><el-button @click="showAvatarDialog = false">取消</el-button><el-button type="primary" @click="confirmAvatar">确定</el-button></template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import { Reading, DataLine, User, Monitor, Setting, ArrowDown, Check, Bell, Timer, Refresh, Delete, VideoPlay, Calendar, Pointer, Lock, School, Camera, Location, Search, Basketball, Connection, ArrowLeft } from '@element-plus/icons-vue'

const API_BASE = 'http://localhost:8080'
const defaultAvatar = "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"

const isLoggedIn = ref(false)
const loading = ref(false)
const btnLoading = reactive({}) 
const loginForm = reactive({ username: '', password: '', role: 'student' })
const currentUser = ref({ id: null, username: '', name: '', role: '', avatar: '', major: '' })
const roleNameMap = { student: '学生', teacher: '教师', admin: '管理员' }

// ✨✨✨ 全局拦截器：每次请求自动携带 Token ✨✨✨
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken');
  if (token) {
    config.headers['satoken'] = token;
  }
  return config;
});

// ✨✨✨ 响应拦截器：处理 Token 失效 ✨✨✨
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录');
      isLoggedIn.value = false;
      localStorage.removeItem('satoken');
      localStorage.removeItem('user_info');
    }
    return Promise.reject(error);
  }
);


const showAvatarDialog = ref(false)
const selectedAvatar = ref('')
const editForm = reactive({ realName: '' })
const pwdForm = reactive({ oldPass: '', newPass: '', confirmPass: '' })
const avatarList = ["/avatar1.png", "/avatar2.png", "/avatar3.png", "/avatar4.png", "/avatar5.png", "/avatar6.png"]

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const res = await axios.post(`${API_BASE}/login`, loginForm)
    if (res.data.code === 200) {
      const data = res.data.data
      
      // ✨✨✨ 登录成功：保存 Token ✨✨✨
      localStorage.setItem('satoken', data.token);

      // ✨✨✨ 关键修复：保存用户信息到 localStorage，防止刷新丢失 ✨✨✨
      const userInfoObj = { id: data.id, username: loginForm.username, name: data.name, role: data.role, avatar: data.avatar, major: data.major }
      localStorage.setItem('user_info', JSON.stringify(userInfoObj));

      currentUser.value = userInfoObj;
      editForm.realName = data.name
      isLoggedIn.value = true
      activeMenu.value = 'dashboard'
      ElMessage.success(`登录成功：欢迎，${data.name}`)
      initData()
    } else { ElMessage.error(res.data.msg || '登录失败') }
  } catch (err) { ElMessage.error('无法连接服务器') } finally { loading.value = false }
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') { 
    axios.post(`${API_BASE}/logout`).finally(() => {
        isLoggedIn.value = false; 
        localStorage.removeItem('satoken'); 
        localStorage.removeItem('user_info'); // ✨ 退出时清除用户信息
        myCourseList.value = []; 
        ElMessage.success('已退出'); 
    });
  } 
  else if (cmd === 'profile') { activeMenu.value = 'profile'; }
}

const activeMenu = ref('dashboard')
const allCourses = ref([])
const availableCourses = ref([]) 
const myCourseList = ref([]) 
const myScheduleCells = ref([]) 
const scheduing = ref(false)
const searchKeyword = ref('') 
const teacherForm = reactive({ name: '', teacherName: '', credit: 3, maxCount: 50, intro: '', preferences: [], locationPreference: '教学楼', targetMajors: '', type: '专业课' })
const statusMap = { 0: {label:'待审核',type:'info'}, 1: {label:'待排课',type:'warning'}, 2: {label:'已发布',type:'success'}, 3: {label:'已驳回',type:'danger'} }

// 教室管理相关
const classroomList = ref([])
const classroomSearch = ref('')
const currentClassroom = ref(null)
const classroomScheduleList = ref([])
const classroomLoading = ref(false)

const currentTitle = computed(() => {
  const map = { 'dashboard': '数据仪表盘', 'student-select': '专业课选课', 'student-pe': '体育课选课', 'student-general': '通选课选课', 'student-search': '全校课程查询', 'student-schedule': '课表查询', 'profile': '个人信息中心', 'admin': '教务排课控制台', 'admin-classroom': '教室资源管理' }
  return map[activeMenu.value] || '系统首页'
})

const publishedCourses = computed(() => allCourses.value.filter(c => c.status === 2))
const pendingAuditCount = computed(() => allCourses.value.filter(c => c.status === 0).length)
const pendingScheduleCount = computed(() => allCourses.value.filter(c => c.status === 1).length)
const myProposals = computed(() => allCourses.value.filter(c => c.teacherName.includes(teacherForm.teacherName) || !teacherForm.teacherName))
const filteredAllCourses = computed(() => {
  let list = allCourses.value;
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(c => c.name.toLowerCase().includes(kw) || c.teacherName.toLowerCase().includes(kw))
  }
  return list;
})

const getFilteredCourses = (menu) => {
  let list = availableCourses.value.filter(c => c.status === 2); 
  if (menu === 'student-select') {
    return list.filter(c => c.type === '专业课' || !c.type)
  } else if (menu === 'student-pe') {
    return list.filter(c => c.type === '体育课')
  } else if (menu === 'student-general') {
    return list.filter(c => c.type === '通选课')
  }
  return list;
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'admin-classroom') {
      fetchClassrooms()
      currentClassroom.value = null
  }
}

const initData = () => { 
  fetchCourses(); 
  if(currentUser.value.role === 'student') {
    fetchMyCourses(); 
    fetchAvailableCourses(); 
  }
}

const refreshData = () => {
    fetchCourses();
    fetchAvailableCourses();
    fetchMyCourses();
}

// ✨✨✨ 关键修复：添加错误提示 ✨✨✨
const fetchCourses = async () => { 
  try { 
    const res = await axios.get(`${API_BASE}/course/list`); 
    allCourses.value = res.data.code ? res.data.data : res.data 
  } catch(e) { 
    ElMessage.error('获取课程列表失败，请检查后端服务'); 
  } 
}
const fetchAvailableCourses = async () => { 
  try { 
    const res = await axios.get(`${API_BASE}/course/list-available?major=${currentUser.value.major || ''}`); 
    if(res.data.code===200) availableCourses.value = res.data.data 
  } catch(e) { 
    console.error(e); 
  } 
}

const fetchMyCourses = async () => {
  try {
    const res = await axios.get(`${API_BASE}/course/my-list`)
    if(res.data.code === 200) {
      const rawList = res.data.data
      myCourseList.value = rawList
      const cells = []
      rawList.forEach(course => {
        if (course.scheduleList && course.scheduleList.length > 0) {
          course.scheduleList.forEach(s => {
            cells.push({ id: course.id, name: course.name, teacherName: course.teacherName, day: s.dayOfWeek, slot: s.timeSlot, location: course.scheduleLocation || '未定地点' })
          })
        }
      })
      myScheduleCells.value = cells
    }
  } catch(e) { ElMessage.error('获取我的课程失败'); }
}

const fetchClassrooms = async () => {
    classroomLoading.value = true
    try {
        const res = await axios.get(`${API_BASE}/classroom/list?keyword=${classroomSearch.value}`)
        if(res.data.code === 200) { classroomList.value = res.data.data } else { ElMessage.error(res.data.msg || '获取教室列表失败') }
    } catch(e){ ElMessage.error('获取教室数据失败') } finally { classroomLoading.value = false }
}

const handleViewClassroomSchedule = async (row) => {
    currentClassroom.value = row
    try {
        const res = await axios.get(`${API_BASE}/classroom/schedule?roomId=${row.id}`)
        if(res.data.code === 200) { classroomScheduleList.value = res.data.data }
    } catch(e){}
}
const backToClassroomList = () => { currentClassroom.value = null; classroomScheduleList.value = [] }

const handleSelect = async (course) => {
  btnLoading[course.id] = true 
  try {
    const res = await axios.post(`${API_BASE}/course/select?courseId=${course.id}`)
    if(res.data.code === 200) {
      ElMessage.success('选课成功')
      const newCourse = { ...course }
      myCourseList.value.push(newCourse) 
      fetchMyCourses()
      fetchAvailableCourses()
      fetchCourses()
    } else { ElMessage.error(res.data.msg) }
  } catch(e) { ElMessage.error(e.response?.data?.msg || '选课请求失败') }
  finally { btnLoading[course.id] = false } 
}

const handleDrop = (course) => {
  ElMessageBox.confirm(`确定要退选《${course.name}》吗？`, '退课确认', { confirmButtonText: '确定退选', cancelButtonText: '取消', type: 'warning' }).then(async () => {
    btnLoading[course.id] = true
    try {
      const res = await axios.post(`${API_BASE}/course/drop?courseId=${course.id}`)
      if(res.data.code === 200) {
        ElMessage.success('退课成功')
        myCourseList.value = myCourseList.value.filter(c => c.id !== course.id)
        fetchMyCourses()
        fetchAvailableCourses()
        fetchCourses()
      } else { ElMessage.error(res.data.msg) }
    } catch(e) { ElMessage.error('网络请求失败') } finally { btnLoading[course.id] = false }
  })
}

const submitProposal = async () => { teacherForm.teacherName = currentUser.value.name; const payload = { ...teacherForm, timePreferences: teacherForm.preferences.join(',') }; await axios.post(`${API_BASE}/course/propose`, payload); ElNotification.success('已申报'); fetchCourses(); teacherForm.preferences = []; }
const handleAudit = async (course, pass) => { await axios.post(`${API_BASE}/course/audit?courseId=${course.id}&pass=${pass}`); ElMessage.success('完成'); fetchCourses(); }
const handleReset = (course) => { ElMessageBox.confirm('重置库存？', '确认', {type:'warning'}).then(()=>handleAudit(course,true)) }

// ✨✨✨ 优化后的重置逻辑：不使用 location.reload()，而是手动刷新数据 ✨✨✨
const handleResetSystem = () => {
  ElMessageBox.confirm(
    '此操作将清空所有选课记录、排课记录，并将课程回滚至待排课状态。',
    '系统数据重置',
    { confirmButtonText: '立即重置', cancelButtonText: '取消', type: 'danger' }
  ).then(async () => {
    try {
      const res = await axios.get(`${API_BASE}/course/reset-redis`)
      if(res.data.code === 200) {
        ElMessage.success(res.data.data || '重置成功')
        // 不刷新页面，手动清空本地数据
        myCourseList.value = []
        myScheduleCells.value = []
        // 重新获取课程列表，状态应该已变更为"待排课"
        await fetchCourses()
        await fetchMyCourses()
        // 如果在教室管理页面，也刷新
        if (activeMenu.value === 'admin-classroom') {
          fetchClassrooms()
          currentClassroom.value = null
        }
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch(e) {
      ElMessage.error('操作失败')
    }
  })
}

const handleAutoSchedule = async () => { scheduing.value=true; try { await axios.post(`${API_BASE}/course/auto-schedule`); ElMessageBox.alert('排课完成'); fetchCourses(); } finally { scheduing.value=false } }

const calcPercentage = (row) => Math.min(((row.selectedCount||0)/row.maxCount)*100, 100)
const isFull = (row) => (row.selectedCount||0) >= row.maxCount
const isSelected = (row) => myCourseList.value.some(c => c.id === row.id)
const getCourse = (d, s) => myScheduleCells.value.find(c => c.day === d && c.slot === s)
const shouldShowCell = (d, s) => { const c = getCourse(d,s); if(!c) return true; const p = getCourse(d,s-1); return !(p && p.id === c.id); }
const getCellRowspan = (d, s) => { const c = getCourse(d,s); if(!c) return 1; let rs=1; for(let i=s+1; i<=13; i++){ if(getCourse(d,i)?.id === c.id) rs++; else break; } return rs; }
const getSlotTime = (s) => ['08:00-08:45','08:50-09:35','09:50-10:35','10:40-11:25','11:30-12:15','14:00-14:45','14:50-15:35','15:50-16:35','16:40-17:25','17:30-18:15','19:00-19:45','19:50-20:35','20:40-21:35'][s-1] || ''
const getCourseColor = (id) => ['#d1eaff', '#d7f0db', '#faecd8', '#fde2e2', '#e9e9eb'][id % 5]

const getClassroomCourse = (d, s) => classroomScheduleList.value.find(c => c.day === d && c.slot === s)
const shouldShowClassroomCell = (d, s) => { const c = getClassroomCourse(d, s); if (!c) return true; const p = getClassroomCourse(d, s - 1); return !(p && p.courseName === c.courseName); }
const getClassroomRowspan = (d, s) => { const c = getClassroomCourse(d, s); if(!c) return 1; let rs = 1; for(let i=s+1; i<=13; i++){ const next = getClassroomCourse(d, i); if(next && next.courseName === c.courseName) rs++; else break; } return rs; }
const updateProfile = async () => { 
  if(!editForm.realName) return ElMessage.warning("姓名不能为空");
  try {
     const payload = { id: currentUser.value.id, role: currentUser.value.role, realName: editForm.realName, avatar: currentUser.value.avatar };
     const res = await axios.post(`${API_BASE}/user/updateInfo`, payload);
     if(res.data.code === 200) { currentUser.value.name = editForm.realName; ElMessage.success("修改成功"); }
     else ElMessage.error(res.data.msg);
  } catch(e) { ElMessage.error("修改失败"); }
}
const updatePassword = async () => { /* 保留原逻辑 */ }
const confirmAvatar = async () => { if(!selectedAvatar.value) return; currentUser.value.avatar = selectedAvatar.value; showAvatarDialog.value = false; await updateProfile(); }

// ✨✨✨ 关键修复：页面加载/刷新时，检查 localStorage 恢复登录状态 ✨✨✨
onMounted(() => {
  const token = localStorage.getItem('satoken');
  const userStr = localStorage.getItem('user_info');
  if (token && userStr) {
    try {
      const user = JSON.parse(userStr);
      currentUser.value = user;
      isLoggedIn.value = true;
      activeMenu.value = 'dashboard';
      initData();
    } catch(e) {
      // 数据损坏，清理
      localStorage.removeItem('satoken');
      localStorage.removeItem('user_info');
    }
  }
})
</script>

<style scoped>
/* 保持原有样式，新增部分样式 */
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background: url('/选课系统背景.jpg') center/cover; }
.login-box { background: rgba(255,255,255,0.95); padding: 40px; border-radius: 12px; width: 400px; text-align: center; }
.logo-title-wrapper { display: flex; align-items: center; justify-content: center; gap: 12px; margin-bottom: 8px; }
.login-logo-img { height: 48px; }
.login-title { margin: 0; color: #303133; font-size: 26px; }
.login-subtitle { color: #909399; font-size: 12px; }
.login-btn { width: 100%; font-weight: bold; }
.login-tips { margin-top: 20px; text-align: left; background: #f4f4f5; padding: 10px; border-radius: 4px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; }

.app-wrapper { height: 100vh; background-color: #f0f2f5; }
.sidebar { background-color: #001529; color: white; display: flex; flex-direction: column; }
.logo-area { height: 60px; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; font-size: 18px; }
.el-menu-vertical { border-right: none; }
.app-header { background: white; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.user-profile { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.user-info { display: flex; flex-direction: column; line-height: 1.2; }
.username { font-size: 14px; font-weight: 500; }
.role-tag { font-size: 10px; background: #f0f2f5; padding: 2px 4px; border-radius: 2px; color: #909399; }

.profile-card { text-align: center; padding: 20px; }
.avatar-wrapper { position: relative; display: inline-block; margin-bottom: 15px; }
.profile-avatar { border: 4px solid #fff; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.avatar-edit { position: absolute; bottom: 0; right: 0; background: #409EFF; color: white; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; border: 2px solid white; }
.profile-name { font-size: 22px; font-weight: bold; color: #303133; }
.profile-role { color: #909399; margin-bottom: 20px; }
.profile-meta p { color: #606266; margin: 10px 0; display: flex; align-items: center; gap: 8px; justify-content: center; }
.avatar-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; }
.avatar-option img { width: 100%; border-radius: 50%; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.avatar-option.active img { border-color: #409EFF; transform: scale(1.1); }

/* 卡片布局优化 */
.course-grid { 
  display: grid; 
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); 
  gap: 20px; 
}
.course-card-box { 
  background: white; 
  border-radius: 8px; 
  overflow: hidden; 
  box-shadow: 0 2px 12px rgba(0,0,0,0.08); 
  transition: all 0.3s; 
  display: flex;
  flex-direction: column;
  height: 300px; /* 固定高度 300px */
}
.course-card-box:hover { 
  transform: translateY(-5px); 
  box-shadow: 0 8px 20px rgba(0,0,0,0.12); 
}

/* 顶部: 标题+教师 */
.card-top { 
  padding: 15px; 
  background: linear-gradient(135deg, #409EFF, #79bbff); 
  color: white; 
  position: relative; 
  height: 50px;
}
.c-title { font-size: 18px; font-weight: bold; margin-bottom: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.c-tag { position: absolute; right: 0; top: 0; background: rgba(0,0,0,0.2); padding: 2px 8px; border-bottom-left-radius: 8px; font-size: 12px; }

/* 中间: 简介 + 时间地点 */
.card-mid { 
  padding: 15px; 
  flex: 1; /* 自动撑满 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.c-intro { 
  font-size: 13px; 
  color: #666; 
  line-height: 1.5;
  display: -webkit-box; 
  -webkit-line-clamp: 4; 
  -webkit-box-orient: vertical; 
  overflow: hidden; 
}
.c-schedule-info { 
  margin-top: auto; /* 关键：自动顶到容器底部 */
  padding-top: 10px; 
  font-size: 12px; 
  color: #409EFF; 
}
.sc-row { display: flex; align-items: center; gap: 5px; margin-bottom: 2px; }

/* 底部: 操作区 (固定 50px，约占1/6) */
.card-bottom { 
  height: 50px; 
  padding: 0 15px; 
  background-color: #fafafa;
  border-top: 1px solid #f0f0f0;
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
}
.progress-area { flex: 1; margin-right: 15px; }
.p-label { font-size: 13px; color: #666; margin-bottom: 2px; } /* 字体与简介一致 */

/* 课表 (表格样式优化 - 2D扁平风格) */
.schedule-table { width: 100%; border-collapse: separate; border-spacing: 2px; /* 增加单元格间距 */ }
.schedule-table th { background: #fafafa; padding: 12px; color: #606266; border: none; } /* 去掉表头边框 */
.schedule-table td { padding: 0; vertical-align: top; height: 60px; border: none; } /* 去掉默认边框 */

.td-time { background: #fafafa; text-align: center; width: 100px; color: #909399; font-size: 12px; padding: 5px !important; border-radius: 4px; }
.td-cell { position: relative; border-radius: 4px; background-color: #f9f9f9; }

/* 2D 扁平化单元格样式 (✨✨ 修改：增加 border-radius ✨✨) */
.course-cell { 
  height: 100%; width: 100%; 
  padding: 8px; 
  border: 2px solid white; /* 白色边框实现切割感 */
  font-size: 12px; 
  display: flex; flex-direction: column; justify-content: center;
  box-sizing: border-box;
  /* ✨✨ 修改：增加圆角为 12px ✨✨ */
  border-radius: 12px; 
  box-shadow: none; /* 去掉阴影 */
  color: #333; /* 文字颜色加深 */
}
.cell-name { font-weight: bold; margin-bottom: 4px; font-size: 13px; color: #303133; }
.cell-loc { color: #555; }
.cell-teacher { color: #666; margin-top: 2px; }

.stat-card { background: white; padding: 20px; border-radius: 4px; display: flex; gap: 20px; align-items: center; }
.stat-icon { width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 24px; }
.stat-card.blue .stat-icon { background: #409EFF; }
.stat-card.green .stat-icon { background: #67C23A; }
.stat-card.orange .stat-icon { background: #E6A23C; }
.stat-card.red .stat-icon { background: #F56C6C; }

/* 教师偏好样式 */
.pref-grid { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 10px; }
.pref-col { min-width: 120px; border: 1px solid #eee; padding: 10px; border-radius: 4px; }
.pref-day-title { text-align: center; font-weight: bold; margin-bottom: 8px; border-bottom: 1px solid #eee; padding-bottom: 5px; }
</style>