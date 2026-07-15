# Luxe Stay 酒店在线预订平台

基于 Spring Boot 3.x 的全功能酒店在线预订系统，提供房型浏览搜索、用户认证、订单预订支付、评价系统和管理后台等完整酒店预订体验。

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
| 参数校验 | spring-boot-starter-validation |

## 快速启动

### 1. 数据库准备

在 SQL Server 中创建数据库并执行建表脚本：

```bash
sqlcmd -S localhost -U sa -Q "CREATE DATABASE hotel_db"
sqlcmd -S localhost -U sa -d hotel_db -i db/schema.sql -i db/data.sql
```

### 2. 配置数据库连接

修改 `src/main/resources/application.yml`：

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
├── db/                                    # 数据库脚本
│   ├── schema.sql                         # 建表 DDL
│   └── data.sql                           # 测试数据
├── src/main/java/com/hotel/system/
│   ├── HotelSystemApplication.java        # 应用入口
│   ├── config/
│   │   ├── GlobalExceptionHandler.java    # 全局异常处理
│   │   ├── LoginInterceptor.java          # 登录拦截器
│   │   └── WebConfig.java                 # MVC 配置
│   ├── controller/
│   │   ├── AuthController.java            # 登录/注册/个人中心/编辑资料
│   │   ├── RoomController.java            # 用户端房型浏览与搜索
│   │   ├── BookingController.java         # 预订/支付/订单/取消
│   │   ├── ReviewController.java          # 评价提交与查看
│   │   ├── AdminController.java           # 管理员仪表盘
│   │   ├── AdminRoomController.java       # 管理员房型与库存管理
│   │   ├── AdminOrderController.java      # 管理员订单管理
│   │   └── AdminUserController.java       # 管理员用户管理
│   ├── entity/
│   │   ├── User.java                      # 用户实体
│   │   ├── RoomType.java                  # 房型实体
│   │   ├── RoomInventory.java             # 每日库存实体
│   │   ├── BookingOrder.java              # 订单实体
│   │   └── Review.java                    # 评价实体
│   ├── repository/                        # JPA 数据访问层
│   └── service/
│       ├── UserService.java               # 用户业务逻辑
│       ├── RoomService.java               # 房型与库存管理
│       ├── BookingService.java            # 预订/支付/订单管理
│       └── ReviewService.java             # 评价管理
└── src/main/resources/
    ├── application.yml                    # 应用配置
    └── templates/                         # Thymeleaf 页面
        ├── login.html                     # 登录页
        ├── register.html                  # 注册页
        ├── profile.html                   # 个人中心（编辑资料+我的评价）
        ├── rooms.html                     # 房型浏览（关键词搜索+分页）
        ├── room-detail.html               # 房型详情（数量选择+住客评价）
        ├── orders.html                    # 我的订单（支付/取消/评价入口）
        ├── review-form.html               # 评价表单（星级评分）
        ├── reviews.html                   # 我的评价列表
        ├── 403.html                       # 权限不足提示页
        └── admin/
            ├── dashboard.html             # 管理仪表盘
            ├── room-list.html             # 房型管理
            ├── room-form.html             # 房型新增/编辑
            ├── inventory.html             # 每日库存管理
            ├── orders.html                # 订单管理
            └── users.html                 # 用户管理
```

## 功能模块

### 用户系统
- 注册与登录（BCrypt 密码加密）
- Session 会话管理 + 登录拦截器
- 角色权限控制（USER / ADMIN，404 拦截）
- 个人资料编辑（手机号 + 修改密码）
- 信用管理：累计取消 3 次自动禁订 7 天，期满自动解禁

### 房型浏览与搜索
- 按入住/离店日期查询可用房型，实时显示库存
- 关键词搜索（房型名称模糊匹配）
- 手动分页（9 条/页）
- 房型详情页：多日库存展示、数量选择器
- 预订时逐日库存扣减（JPA 悲观锁防超卖）

### 订单生命周期
- 完整的订单状态机：`BOOKED` → `PAID` → `CHECKED_IN` → `COMPLETED`
- 用户端：预订、模拟支付、取消订单（入住前至少 1 天）
- 取消订单自动释放库存并累计取消次数
- 价格快照机制（下单时锁定单价，不受后续调价影响）

### 评价系统
- 已完成的订单可提交评价（1-5 星评分 + 可选评论）
- 同订单不可重复评价
- 房型详情页展示平均评分与评价列表
- 个人中心查看用户自己的历史评价

### 管理后台
- **仪表盘**：今日入住/退房统计、本月营收、最近订单
- **房型管理**：房型 CRUD、启用/禁用切换、每日库存调整与批量补充
- **订单管理**：多条件筛选（状态/日期/用户名）、办理入住/退房、强制取消
- **用户管理**：关键词搜索、手动封禁/解封（管理员不可被封禁）

### 其他
- 全局异常处理：`IllegalArgumentException` → flash error + 重定向
- 统一深色玻璃态 UI（Glassmorphism）设计风格
- 响应式布局，适配移动端

## 数据库表

| 表名 | 说明 |
|------|------|
| users | 用户表（含取消次数 cancel_count 与禁订截止时间 banned_until） |
| room_type | 房型表（名称、描述、单价、总数量、状态 0=禁用 1=启用） |
| room_inventory | 每日库存表（按 room_type_id + inventory_date 唯一约束） |
| booking_order | 订单表（订单号、状态、单价快照、支付时间、取消时间） |
| review | 评价表（评分 1-5、评论 500 字、order_id 唯一约束） |

ER 关系：`users` 1:N `booking_order` N:1 `room_type` 1:N `room_inventory`
`users` 1:N `review` N:1 `room_type`，`booking_order` 1:1 `review`

## 页面设计

采用深色玻璃态（Glassmorphism）设计风格：

- 深蓝渐变背景（#0f172a → #1e1b4b → #311042）+ 装饰光球
- 毛玻璃卡片（backdrop-filter: blur + 半透明背景）
- 紫色渐变强调色（#6366f1 → #a855f7 → #d946ef）
- Outfit 字体 + Bootstrap Icons 图标
- 统一的导航栏、表单、按钮、状态徽章风格
