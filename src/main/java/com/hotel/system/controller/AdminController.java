package com.hotel.system.controller;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.User;
import com.hotel.system.service.BookingService;
import com.hotel.system.service.RoomService;
import com.hotel.system.service.UserService;
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
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "dashboard");

        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        long todayCheckins = bookingService.countTodayCheckins(today);
        long todayCheckouts = bookingService.countTodayCheckouts(today);
        BigDecimal monthRevenue = bookingService.sumRevenueBetween(monthStart, monthEnd);

        long checkedInNow = bookingService.countByStatus("CHECKED_IN");
        long totalRooms = roomService.sumTotalQuantityByActiveRoomTypes();
        long occupancyPct = totalRooms > 0 ? (checkedInNow * 100) / totalRooms : 0;

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        BigDecimal todayRevenue = bookingService.sumRevenueBetween(todayStart, todayEnd);

        long totalOrders = bookingService.countTotalOrders();
        long totalUsers = userService.countTotalUsers();
        long activeRoomTypes = roomService.countActiveRoomTypes();

        List<BookingOrder> recentOrders = bookingService.getRecentOrders(5);

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
