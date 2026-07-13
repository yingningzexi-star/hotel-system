package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/**
 * 每日库存实体 — 对应数据库 room_inventory 表
 * 核心规则：
 * - 每日库存 = 该房型当日可预订的房间数
 * - (room_type_id, inventory_date) 联合唯一，确保同一天同一房型只有一条库存记录
 * - 预订时逐日扣减，取消时逐日恢复
 */
@Data
@Entity
@Table(name = "room_inventory")
@NoArgsConstructor
@AllArgsConstructor
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的房型ID */
    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    /** 库存日期（不含时间） */
    @Column(name = "inventory_date", nullable = false)
    private LocalDate inventoryDate;

    /** 该日可用数量 */
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;
}
