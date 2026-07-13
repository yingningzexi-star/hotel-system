package com.hotel.system.controller;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.User;
import com.hotel.system.service.BookingService;
import com.hotel.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @PostMapping("/booking")
    public String createBooking(@RequestParam Long roomTypeId,
                                 @RequestParam LocalDate checkIn,
                                 @RequestParam LocalDate checkOut,
                                 @RequestParam(defaultValue = "1") int quantity,
                                 HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            bookingService.createBooking(currentUser.getId(), roomTypeId, checkIn, checkOut, quantity);
            model.addAttribute("success", "预订成功！");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String myOrders(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        userService.checkAndUpdateBannedStatus(currentUser);

        List<BookingOrder> orders = bookingService.getUserOrders(currentUser.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("today", LocalDate.now());
        return "orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            bookingService.cancelBooking(id, currentUser.getId());
            model.addAttribute("success", "订单已取消");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "redirect:/orders";
    }
}
