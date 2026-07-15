package com.hotel.system.controller;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.User;
import com.hotel.system.repository.BookingOrderRepository;
import com.hotel.system.repository.RoomTypeRepository;
import com.hotel.system.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookingOrderRepository bookingOrderRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "dashboard");

        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        long todayCheckins = bookingOrderRepository.countByStatusAndCheckInDate("PAID", today);
        long todayCheckouts = bookingOrderRepository.countByStatusAndCheckOutDate("CHECKED_IN", today);
        BigDecimal monthRevenue = bookingOrderRepository.sumRevenueBetween(monthStart, monthEnd);
        if (monthRevenue == null) monthRevenue = BigDecimal.ZERO;

        long checkedInNow = bookingOrderRepository.countByStatus("CHECKED_IN");
        long totalRooms = roomTypeRepository.sumTotalQuantityByStatusActive();
        long occupancyPct = totalRooms > 0 ? (checkedInNow * 100) / totalRooms : 0;

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        BigDecimal todayRevenue = bookingOrderRepository.sumRevenueBetween(todayStart, todayEnd);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        long totalOrders = bookingOrderRepository.count();
        long totalUsers = userRepository.count();
        long activeRoomTypes = roomTypeRepository.countByStatus(1);

        List<BookingOrder> recentOrders = bookingOrderRepository.findTop5ByOrderByCreatedAtDesc();

        model.addAttribute("todayCheckins", todayCheckins);
        model.addAttribute("todayCheckouts", todayCheckouts);
        model.addAttribute("monthRevenue", monthRevenue);
        model.addAttribute("occupancyPct", occupancyPct);
        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeRoomTypes", activeRoomTypes);
        model.addAttribute("recentOrders", recentOrders);

        return "admin/dashboard";
    }
}
