package com.hotel.system.repository;

import com.hotel.system.entity.BookingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {

    List<BookingOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM BookingOrder o WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<BookingOrder> findByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    Optional<BookingOrder> findByOrderNo(String orderNo);

    List<BookingOrder> findByStatusAndCheckInDate(String status, LocalDate date);

    List<BookingOrder> findByStatusAndCheckOutDate(String status, LocalDate date);

    long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

    long countByStatusAndCheckInDate(String status, LocalDate date);

    long countByStatusAndCheckOutDate(String status, LocalDate date);

    long countByStatus(String status);

    List<BookingOrder> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT o FROM BookingOrder o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate) AND " +
           "(:username IS NULL OR o.user.username LIKE %:username%) " +
           "ORDER BY o.createdAt DESC")
    List<BookingOrder> findWithFilters(@Param("status") String status,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("username") String username);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM BookingOrder o " +
           "WHERE o.status IN ('PAID', 'CHECKED_IN', 'COMPLETED') " +
           "AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
