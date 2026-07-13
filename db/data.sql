-- 确保使用的是正确的数据库
-- USE hotel_db;

-- 1. 插入测试用户 (密码均为 123456 的 BCrypt 加密值：$2a$10$BQz6cXUarm4lx0BtSAjxuu5U4AN/ovcF75zNzWYXMTcGTCc35i5yi)
-- 普通用户
INSERT INTO users (username, password, real_name, phone, role, cancel_count, banned_until, created_at)
VALUES ('user1', '$2a$10$BQz6cXUarm4lx0BtSAjxuu5U4AN/ovcF75zNzWYXMTcGTCc35i5yi', N'张三', '13800138000', 'USER', 0, NULL, GETDATE());

-- 管理员
INSERT INTO users (username, password, real_name, phone, role, cancel_count, banned_until, created_at)
VALUES ('admin1', '$2a$10$BQz6cXUarm4lx0BtSAjxuu5U4AN/ovcF75zNzWYXMTcGTCc35i5yi', N'系统管理员', '13900139000', 'ADMIN', 0, NULL, GETDATE());

-- 2. 插入测试房型
INSERT INTO room_type (name, description, price, total_quantity, image_path, status)
VALUES (N'标准双人间', N'双人床，带独立卫浴，提供免费Wi-Fi。', 199.00, 10, '/images/standard_double.jpg', 1);

INSERT INTO room_type (name, description, price, total_quantity, image_path, status)
VALUES (N'豪华大床房', N'舒适超大双人床，配有大落地窗，包含双人早餐。', 299.00, 5, '/images/deluxe_king.jpg', 1);

INSERT INTO room_type (name, description, price, total_quantity, image_path, status)
VALUES (N'行政套房', N'超大空间套房，配独立客厅，尊享行政礼遇。', 599.00, 2, '/images/executive_suite.jpg', 1);

-- 3. 动态获取房型ID并插入未来 7 天的每日库存数据 (2026-07-13 至 2026-07-19)
DECLARE @StdDoubleId BIGINT, @DeluxeKingId BIGINT, @ExecSuiteId BIGINT;

SELECT @StdDoubleId = id FROM room_type WHERE name = N'标准双人间';
SELECT @DeluxeKingId = id FROM room_type WHERE name = N'豪华大床房';
SELECT @ExecSuiteId = id FROM room_type WHERE name = N'行政套房';

-- 插入未来7天每日库存
INSERT INTO room_inventory (room_type_id, inventory_date, available_quantity) VALUES
(@StdDoubleId, '2026-07-13', 10),
(@StdDoubleId, '2026-07-14', 10),
(@StdDoubleId, '2026-07-15', 10),
(@StdDoubleId, '2026-07-16', 10),
(@StdDoubleId, '2026-07-17', 10),
(@StdDoubleId, '2026-07-18', 10),
(@StdDoubleId, '2026-07-19', 10),

(@DeluxeKingId, '2026-07-13', 5),
(@DeluxeKingId, '2026-07-14', 5),
(@DeluxeKingId, '2026-07-15', 5),
(@DeluxeKingId, '2026-07-16', 5),
(@DeluxeKingId, '2026-07-17', 5),
(@DeluxeKingId, '2026-07-18', 5),
(@DeluxeKingId, '2026-07-19', 5),

(@ExecSuiteId, '2026-07-13', 2),
(@ExecSuiteId, '2026-07-14', 2),
(@ExecSuiteId, '2026-07-15', 2),
(@ExecSuiteId, '2026-07-16', 2),
(@ExecSuiteId, '2026-07-17', 2),
(@ExecSuiteId, '2026-07-18', 2),
(@ExecSuiteId, '2026-07-19', 2);
