package com.hotel.system.service;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.RoomInventory;
import com.hotel.system.entity.RoomType;
import com.hotel.system.entity.User;
import com.hotel.system.repository.BookingOrderRepository;
import com.hotel.system.repository.RoomInventoryRepository;
import com.hotel.system.repository.RoomTypeRepository;
import com.hotel.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingOrderRepository bookingOrderRepository;

    @Autowired
    private RoomInventoryRepository roomInventoryRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public BookingOrder createBooking(Long userId, Long roomTypeId,
                                       LocalDate checkIn, LocalDate checkOut, int quantity) {
        if (userService.isUserBanned(userId)) {
            throw new IllegalArgumentException("您的账号当前处于预订限制期，无法发起新预订");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("入住日期必须早于离店日期");
        }

        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }

        if (checkIn.isAfter(today.plusDays(7))) {
            throw new IllegalArgumentException("仅支持预订未来一周内的房间");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("预订数量至少为1");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("房型不存在"));

        if (roomType.getStatus() != 1) {
            throw new IllegalArgumentException("该房型已下架");
        }

        // 逐日检查并扣减库存
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            final LocalDate currentDate = date;
            RoomInventory inv = roomInventoryRepository
                    .findByRoomTypeIdAndInventoryDate(roomTypeId, currentDate)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "日期 " + currentDate + " 无库存数据，无法预订"));
            if (inv.getAvailableQuantity() < quantity) {
                throw new IllegalArgumentException(
                        "日期 " + currentDate + " 库存不足，当前剩余 " + inv.getAvailableQuantity() + " 间");
            }
            inv.setAvailableQuantity(inv.getAvailableQuantity() - quantity);
            roomInventoryRepository.save(inv);
            date = date.plusDays(1);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        long nights = checkIn.datesUntil(checkOut).count();
        BigDecimal unitPrice = roomType.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity * nights));

        BookingOrder order = new BookingOrder();
        order.setOrderNo(generateOrderNo());
        order.setUser(user);
        order.setRoomType(roomType);
        order.setCheckInDate(checkIn);
        order.setCheckOutDate(checkOut);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(totalAmount);
        order.setStatus("BOOKED");
        order.setCreatedAt(LocalDateTime.now());

        return bookingOrderRepository.save(order);
    }

    @Transactional
    public void cancelBooking(Long orderId, Long userId) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权取消该订单");
        }

        if (!"BOOKED".equals(order.getStatus())) {
            throw new IllegalArgumentException("该订单当前状态不可取消");
        }

        LocalDate today = LocalDate.now();
        if (!today.isBefore(order.getCheckInDate())) {
            throw new IllegalArgumentException("入住当天及之后不可取消订单，需提前至少一天");
        }

        // 逐日释放库存
        Long roomTypeId = order.getRoomType().getId();
        LocalDate date = order.getCheckInDate();
        while (date.isBefore(order.getCheckOutDate())) {
            roomInventoryRepository.findByRoomTypeIdAndInventoryDate(roomTypeId, date)
                    .ifPresent(inv -> {
                        inv.setAvailableQuantity(inv.getAvailableQuantity() + order.getQuantity());
                        roomInventoryRepository.save(inv);
                    });
            date = date.plusDays(1);
        }

        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        bookingOrderRepository.save(order);

        // 累计取消次数，触发信用惩罚检查
        userService.incrementCancelCount(userId);
    }

    public List<BookingOrder> getUserOrders(Long userId) {
        return bookingOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "ORD" + timestamp + random;
    }
}
