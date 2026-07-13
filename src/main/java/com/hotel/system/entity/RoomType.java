package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 房型实体 — 对应数据库 room_type 表
 * B成员负责：房型的基本信息管理（CRUD）
 */
@Data
@Entity
@Table(name = "room_type")
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 房型名称，如"标准双人间"、"豪华大床房" */
    @Column(nullable = false, length = 100)
    private String name;

    /** 房型描述 */
    @Column(length = 500)
    private String description;

    /** 单价（元/晚），下单时以快照为准 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 该房型总房间数（非每日库存），用于计算每日可用量 */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    /** 房型图片路径（相对路径，如 /images/standard_double.jpg） */
    @Column(name = "image_path", length = 255)
    private String imagePath;

    /** 状态：1=启用（可预订），0=禁用（下架） */
    @Column(nullable = false)
    private Integer status = 1;
}
