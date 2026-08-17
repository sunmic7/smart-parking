# 🅿️ 智能停车管理系统

Smart Parking Management System

基于 Spring Boot 3 + Vue 3 的智能停车场管理系统，支持车牌识别、车辆入场/出场计费、月租车管理、实时车位统计等功能。

## 系统截图

| 登录页 | 车位地图 |
|-------|---------|
| 科技感动态背景 + 粒子动画 | 高德地图可视化车位分布 |

## 功能特性

### 🚗 核心业务
- **车牌识别** — 对接百度 OCR，上传图片自动识别车牌号
- **车辆入场** — 识别车牌 → 判断包月/临时车 → 记录入场时间
- **车辆出场** — 自动计算停车费用（临时车）→ 生成支付记录 → 释放车位
- **月租车管理** — 月卡续费、到期自动提醒

### 🅿️ 车位管理
- **实时车位地图** — 高德地图标注车位，空闲/占用状态一目了然
- **车位统计** — 总车位、已用、空闲、月租占比可视化图表
- **批量操作** — 批量添加/导入车位

### 📊 数据统计
- **收入分析** — 日/月/年收费统计趋势图
- **车位利用率** — 柱状图展示各区域使用情况

### 👥 系统管理
- **用户管理** — 管理员账号的增删改查
- **角色权限** — 细粒度的菜单权限控制（编辑/只读）
- **操作日志** — 关键操作全程记录，可追溯

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.5.11, Java 17 |
| **ORM** | MyBatis-Plus 3.5.5 |
| **数据库** | MySQL + Redis（缓存） |
| **安全** | JWT 认证 + Spring Security Crypto（密码加密） |
| **AI 集成** | 百度 AI SDK（车牌识别 OCR） |
| **工具** | Lombok, AOP（操作日志切面） |
| **前端框架** | Vue 3 + Vite 8 |
| **UI 组件** | Element Plus |
| **地图** | 高德地图 JS API |

## 快速开始

### 环境要求

- **JDK 17** 或更高版本
- **Maven** 3.8+
- **MySQL** 8.0+
- **Redis**（可选，用于缓存）
- **Node.js** 18+（前端）

### 1. 导入数据库

```sql
mysql -u root -p < sql/parking_system.sql
```

### 2. 配置后端

编辑 `src/main/resources/application.yml`：

```yaml
# 数据库连接
spring.datasource.url: jdbc:mysql://localhost:3306/parking_system
spring.datasource.username: root
spring.datasource.password: your_password

# 百度 AI（车牌识别）— 需替换为你自己的密钥
baidu.ai.appId: your_baidu_app_id
baidu.ai.apiKey: your_baidu_api_key
baidu.ai.secretKey: your_baidu_secret_key

# 图片上传目录
upload.path: D:/parking-uploads/
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

### 4. 配置 & 启动前端

编辑 `frontend/.env`：

```env
# 高德地图 Key（需替换为你自己的）
VITE_AMAP_KEY=your_amap_key_here

# 后端 API 地址
VITE_API_BASE_URL=http://localhost:8080
```

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问系统

浏览器打开 `http://localhost:5173`（或 Vite 提示的地址）

## 项目结构

```
smart-parking/
├── src/main/java/com/parking/smart_parking/
│   ├── controller/          # REST 控制器
│   │   ├── PlateRecognizeController.java   # 车牌识别 + 入场/出场
│   │   ├── ParkLotController.java          # 车位管理
│   │   ├── ParkRecordController.java       # 停车记录
│   │   └── ...
│   ├── service/             # 业务逻辑层
│   ├── entity/              # 数据实体
│   ├── mapper/              # MyBatis 映射
│   ├── config/              # 配置类（CORS、Redis、密码加密等）
│   ├── utils/JwtUtils.java  # JWT 工具
│   └── aspect/              # 操作日志切面
├── src/main/resources/
│   └── application.yml      # 应用配置（含密钥占位符）
├── frontend/                # Vue 3 前端
│   ├── src/views/           # 页面组件
│   │   ├── Login.vue        # 登录页（含粒子动画）
│   │   ├── Dashboard.vue    # 数据仪表盘
│   │   ├── ParkLotMap.vue   # 车位地图（高德地图）
│   │   └── ...
│   └── .env                 # 环境变量（含 Key 占位符）
└── sql/
    └── parking_system.sql   # 数据库建表脚本
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/plate/recognize` | POST | 上传图片识别车牌 |
| `/api/plate/entry` | POST | 车辆入场登记 |
| `/api/plate/exit` | POST | 车辆出场计费 |
| `/api/park-lot/list` | GET | 车位列表 |
| `/api/park-record/page` | GET | 停车记录分页 |
| `/api/park-payment/page` | GET | 支付记录分页 |
| `/api/statistics/revenue` | GET | 收入统计 |
| `/api/statistics/occupancy` | GET | 车位利用率 |
| `/api/login` | POST | 管理员登录 |
| `/api/sys-user/**` | — | 用户管理 |
| `/api/sys-role/**` | — | 角色权限 |

## ⚠️ 密钥说明

本仓库中的 API 密钥已替换为占位符，使用前需自行申请：

1. **百度 AI 车牌识别** → [百度 AI 控制台](https://console.bce.baidu.com/ai/#/ai/ocr/overview/index)
2. **高德地图 JSAPI** → [高德开放平台](https://lbs.amap.com/)

## 许可证

本项目仅供学习交流使用。