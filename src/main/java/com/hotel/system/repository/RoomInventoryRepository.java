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

@Repository
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    List<RoomInventory> findByRoomTypeIdAndInventoryDateBetween(Long roomTypeId, LocalDate start, LocalDate end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ri FROM RoomInventory ri WHERE ri.roomType.id = :roomTypeId AND ri.inventoryDate IN :dates")
    List<RoomInventory> findByRoomTypeIdAndInventoryDateInForUpdate(@Param("roomTypeId") Long roomTypeId,
                                                                     @Param("dates") List<LocalDate> dates);
}
