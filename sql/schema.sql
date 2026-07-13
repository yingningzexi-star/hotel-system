-- 确保使用的是正确的数据库，如需手动创建请先执行 CREATE DATABASE hotel_db;
-- USE hotel_db;

-- 1. 删除已存在的表（遵循外键约束删除顺序）
IF OBJECT_ID('booking_order', 'U') IS NOT NULL DROP TABLE booking_order;
IF OBJECT_ID('room_inventory', 'U') IS NOT NULL DROP TABLE room_inventory;
IF OBJECT_ID('room_type', 'U') IS NOT NULL DROP TABLE room_type;
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;

-- 2. 创建用户表 users
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL, -- 存储加密后的密码 (如 BCrypt 格式)
    real_name NVARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    cancel_count INT NOT NULL DEFAULT 0,
    banned_until DATETIME2 NULL, -- 禁止预订截止时间，NULL 表示当前未被禁订
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- 3. 创建房型表 room_type
CREATE TABLE room_type (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_quantity INT NOT NULL DEFAULT 0,
    image_path VARCHAR(255) NULL,
    status INT NOT NULL DEFAULT 1 -- 1: 启用, 0: 禁用
);

-- 4. 创建每日库存表 room_inventory
CREATE TABLE room_inventory (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    inventory_date DATE NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT FK_room_inventory_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id) ON DELETE CASCADE,
    CONSTRAINT UQ_room_inventory_type_date UNIQUE (room_type_id, inventory_date)
);

-- 5. 创建订单表 booking_order
CREATE TABLE booking_order (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL, -- 下单时单价快照
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    cancelled_at DATETIME2 NULL,
    CONSTRAINT FK_booking_order_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_booking_order_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id)
);
