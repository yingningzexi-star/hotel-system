package com.hotel.system.repository;

import com.hotel.system.entity.BookingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {

    List<BookingOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<BookingOrder> findByOrderNo(String orderNo);
}
