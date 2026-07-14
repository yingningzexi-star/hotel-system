# Luxe Stay 酒店在线预订平台

基于 Spring Boot 3.x 的酒店在线预订系统，支持房型浏览、用户认证、订单预订与取消、信用管理等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.10 + Spring Data JPA |
| 模板引擎 | Thymeleaf |
| 数据库 | SQL Server (mssql-jdbc) |
| 安全认证 | BCrypt 密码加密 + Session 拦截器 |
| 前端 | Bootstrap 5.3 + Bootstrap Icons + Google Fonts |
| 构建工具 | Maven (mvnw) |
| Java 版本 | 17+ |

## 快速启动

### 1. 数据库准备

在 SQL Server 中创建数据库并执行建表脚本：

```bash
# 创建数据库
sqlcmd -S localhost -U sa -Q "CREATE DATABASE hotel_db"

# 执行建表脚本
sqlcmd -S localhost -U sa -d hotel_db -i db/schema.sql -i db/data.sql
```

### 2. 配置数据库连接

修改 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://<服务器地址>:1433;databaseName=hotel_db;encrypt=true;trustServerCertificate=true;
    username: <用户名>
    password: <密码>
```

### 3. 启动应用

```bash
./mvnw spring-boot:run
```

访问 http://localhost:8080/rooms 进入系统。

### 4. 预置账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| user1 | 123456 | 普通用户 |
| admin1 | 123456 | 管理员 |

## 项目结构

```
HotelSystem/
├── db/                              # 数据库脚本
│   ├── schema.sql                   # 建表 DDL
│   └── data.sql                     # 测试数据
├── src/main/java/com/hotel/system/
│   ├── HotelSystemApplication.java  # 应用入口
│   ├── config/
│   │   ├── LoginInterceptor.java    # 登录拦截器
│   │   └── WebConfig.java           # MVC 配置
│   ├── controller/
│   │   ├── AuthController.java      # 登录/注册/个人中心
│   │   ├── RoomController.java      # 用户端房型浏览
│   │   ├── BookingController.java   # 预订/订单/取消
│   │   └── AdminRoomController.java # 管理员房型管理
│   ├── entity/
│   │   ├── User.java                # 用户实体
│   │   ├── RoomType.java            # 房型实体
│   │   ├── RoomInventory.java       # 每日库存实体
│   │   └── BookingOrder.java        # 订单实体
│   ├── repository/                  # JPA 数据访问层
│   └── service/
│       ├── UserService.java         # 用户业务逻辑
│       ├── RoomService.java         # 房型与库存管理
│       └── BookingService.java      # 预订与取消核心逻辑
└── src/main/resources/
    ├── application.yml              # 应用配置
    └── templates/                   # Thymeleaf 页面
        ├── login.html               # 登录页
        ├── register.html            # 注册页
        ├── profile.html             # 个人中心
        ├── rooms.html               # 房型浏览
        ├── room-detail.html         # 房型详情与预订
        ├── orders.html              # 我的订单
        └── 403.html                 # 权限不足提示页
```

## 功能模块

### 用户认证（组员 A）
- 用户注册与登录（BCrypt 密码加密）
- Session 会话管理
- 登录拦截器（未登录自动跳转登录页）
- 角色权限控制（USER / ADMIN）

### 房型管理（组员 B）
- 房型浏览与多条件搜索
- 按入住/离店日期查询可用房型
- 每日库存管理与实时展示
- 管理员后台：房型 CRUD、库存调整

### 预订与订单（组员 C）
- 多日预订（逐日库存扣减，JPA 悲观锁防超卖）
- 下单时价格快照（防止后续调价影响已有订单）
- 订单列表查看与状态追踪
- 订单取消（提前至少 1 天，自动释放库存）

### 信用管理
- 每次取消订单累计取消次数
- 累计 3 次取消 → 自动禁订 7 天
- 禁订期满自动解禁并重置计数
- 个人中心与订单页实时显示信用状态

## 数据库表

| 表名 | 说明 |
|------|------|
| users | 用户表（含取消次数与禁订截止时间） |
| room_type | 房型表（名称、描述、单价、总数量、状态） |
| room_inventory | 每日库存表（按房型 + 日期唯一约束） |
| booking_order | 订单表（含单价快照、状态、取消时间） |

ER 关系：`users` 1:N `booking_order` N:1 `room_type` 1:N `room_inventory`

## 页面设计

采用深色玻璃态（Glassmorphism）设计风格：
- 深蓝渐变背景 + 装饰光球
- 毛玻璃卡片（backdrop-filter: blur）
- 紫色渐变强调色
- Outfit 字体 + Bootstrap Icons 图标
- 响应式布局（Bootstrap Grid）
