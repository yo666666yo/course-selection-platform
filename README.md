# 选课平台 (Course Selection Platform)

前后端分离的在线选课系统。

## 项目结构

```
├── frontend/          # Vue 3 + Vite 前端
│   ├── src/
│   │   ├── App.vue    # 主应用组件
│   │   ├── main.js    # 入口文件
│   │   └── style.css  # 全局样式
│   ├── public/        # 静态资源
│   └── index.html
├── backend/           # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/course/
│       │   ├── controller/   # 控制器
│       │   ├── service/      # 业务逻辑
│       │   ├── entity/       # 实体类
│       │   ├── mapper/       # MyBatis 映射器
│       │   ├── config/       # 配置类
│       │   └── common/       # 通用工具
│       └── resources/
│           └── application.yml
└── docs/              # 文档
```

## 启动方式

### 前端
```bash
cd frontend
npm install
npm run dev      # 开发模式，默认 http://localhost:5173
npm run build    # 生产构建到 frontend/dist/
```

### 后端
```bash
cd backend
mvn spring-boot:run    # 后端运行在 http://localhost:8080
```

前端通过 axios 调用后端 API，API 基地址在 `frontend/src/App.vue` 中配置。
