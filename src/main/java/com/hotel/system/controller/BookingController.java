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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            BookingOrder order = bookingService.createBooking(
                    currentUser.getId(), roomTypeId, checkIn, checkOut, quantity);
            redirectAttributes.addFlashAttribute("success",
                    "预订成功！订单号：" + order.getOrderNo());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String myOrders(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 刷新信用状态
        userService.checkAndUpdateBannedStatus(currentUser);

        List<BookingOrder> orders = bookingService.getUserOrders(currentUser.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "orders");
        return "orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            bookingService.cancelBooking(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "订单已取消。"
                    + (currentUser.getCancelCount() >= 3
                        ? "累计取消已达 3 次，7 天内禁止发起新预订！"
                        : "当前累计取消 " + currentUser.getCancelCount() + " 次。"));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders";
    }
}
