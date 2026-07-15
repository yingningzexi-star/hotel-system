package com.hotel.system.repository;

import com.hotel.system.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 房型数据访问层
 * B成员负责：房型CRUD的数据库操作
 */
@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    /** 查询所有启用中的房型（供用户端展示） */
    List<RoomType> findByStatus(Integer status);

    /** 按名称模糊搜索房型（管理员用） */
    List<RoomType> findByNameContaining(String name);

    /** 按名称模糊搜索启用中的房型（用户端用） */
    List<RoomType> findByNameContainingAndStatus(String name, Integer status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(rt.totalQuantity), 0) FROM RoomType rt WHERE rt.status = 1")
    long sumTotalQuantityByStatusActive();

    long countByStatus(Integer status);
}
