-- 修复 booking_order 表的 status CHECK 约束
-- 原因：ddl-auto=update 不会修改已有约束，旧约束可能不支持 'PAID'/'CHECKED_IN'/'COMPLETED'

DECLARE @constraintName NVARCHAR(200);

SELECT @constraintName = cc.name
FROM sys.check_constraints cc
    INNER JOIN sys.columns c ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id
    INNER JOIN sys.tables t ON cc.parent_object_id = t.object_id
WHERE t.name = 'booking_order'
  AND c.name = 'status';

IF @constraintName IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(MAX) = 'ALTER TABLE booking_order DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
    PRINT 'Dropped constraint: ' + @constraintName;
END

ALTER TABLE booking_order ADD CONSTRAINT CK_booking_order_status
    CHECK (status IN ('BOOKED', 'PAID', 'CHECKED_IN', 'COMPLETED', 'CANCELLED'));

PRINT 'Added new CHECK constraint with all 5 statuses.';
