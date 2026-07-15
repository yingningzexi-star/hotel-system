package com.hotel.system.controller;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.User;
import com.hotel.system.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("")
    public String listOrders(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              @RequestParam(required = false) String username,
                              @RequestParam(defaultValue = "1") int page,
                              HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "orders");

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (startDate != null && !startDate.trim().isEmpty()) {
            start = LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        }

        List<BookingOrder> orders = bookingService.getAllOrders(
                (status != null && status.trim().isEmpty()) ? null : status,
                start, end,
                (username != null && username.trim().isEmpty()) ? null : username);

        int size = 10;
        int total = orders.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        model.addAttribute("orders", orders.subList(fromIndex, toIndex));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", total);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterStartDate", startDate);
        model.addAttribute("filterEndDate", endDate);
        model.addAttribute("filterUsername", username);
        return "admin/orders";
    }

    @PostMapping("/{id}/checkin")
    public String checkin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        BookingOrder order = bookingService.adminCheckin(id);
        redirectAttributes.addFlashAttribute("success",
                "订单 " + order.getOrderNo() + " 已办理入住");
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/checkout")
    public String checkout(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        BookingOrder order = bookingService.adminCheckout(id);
        redirectAttributes.addFlashAttribute("success",
                "订单 " + order.getOrderNo() + " 已办理退房");
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookingService.adminCancelOrder(id);
        redirectAttributes.addFlashAttribute("success", "订单已取消");
        return "redirect:/admin/orders";
    }
}
