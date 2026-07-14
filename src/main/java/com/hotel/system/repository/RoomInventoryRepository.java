package com.hotel.system.repository;

import com.hotel.system.entity.RoomInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日库存数据访问层
 * B成员负责：库存查询与配置
 */
@Repository
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    /** 查询某房型在指定日期的库存 */
    Optional<RoomInventory> findByRoomTypeIdAndInventoryDate(Long roomTypeId, LocalDate inventoryDate);

    /** 查询某房型在一段日期范围内的所有库存记录（含排序） */
    List<RoomInventory> findByRoomTypeIdAndInventoryDateBetweenOrderByInventoryDateAsc(
            Long roomTypeId, LocalDate startDate, LocalDate endDate);

    /** 查询某房型所有库存记录 */
    List<RoomInventory> findByRoomTypeIdOrderByInventoryDateAsc(Long roomTypeId);

    /** 查询某日期范围内所有房型的库存（批量管理用） */
    List<RoomInventory> findByInventoryDateBetweenOrderByRoomTypeIdAscInventoryDateAsc(
            LocalDate startDate, LocalDate endDate);

    /** 删除某房型在指定日期之后的库存（用于重置库存） */
    void deleteByRoomTypeIdAndInventoryDateGreaterThanEqual(Long roomTypeId, LocalDate date);

    /** 批量查询多个房型在日期范围内的库存（用户搜索房型用） */
    @Query("SELECT ri FROM RoomInventory ri WHERE ri.roomTypeId IN :roomTypeIds " +
           "AND ri.inventoryDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ri.roomTypeId, ri.inventoryDate")
    List<RoomInventory> findByRoomTypeIdsAndDateRange(
            @Param("roomTypeIds") List<Long> roomTypeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 批量更新某房型某日的可用库存（扣减/恢复） */
    void deleteByRoomTypeId(Long roomTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ri FROM RoomInventory ri WHERE ri.roomTypeId = :roomTypeId AND ri.inventoryDate IN :dates")
    List<RoomInventory> findByRoomTypeIdAndInventoryDateInForUpdate(
            @Param("roomTypeId") Long roomTypeId,
            @Param("dates") List<LocalDate> dates);
}
