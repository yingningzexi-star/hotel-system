package com.hotel.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "room_inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_type_id", "inventory_date"})
})
@NoArgsConstructor
@AllArgsConstructor
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "inventory_date", nullable = false)
    private LocalDate inventoryDate;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;
}
