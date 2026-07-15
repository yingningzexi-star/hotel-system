package com.hotel.system.controller;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.User;
import com.hotel.system.service.BookingService;
import com.hotel.system.service.ReviewService;
import com.hotel.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

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

        BookingOrder order = bookingService.createBooking(
                currentUser.getId(), roomTypeId, checkIn, checkOut, quantity);
        redirectAttributes.addFlashAttribute("success",
                "预订成功！订单号：" + order.getOrderNo());
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
        Set<Long> reviewedOrderIds;
        try {
            reviewedOrderIds = orders.stream()
                    .filter(o -> reviewService.hasReviewed(o.getId()))
                    .map(BookingOrder::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            reviewedOrderIds = Set.of();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("reviewedOrderIds", reviewedOrderIds);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "orders");
        return "orders";
    }

    @PostMapping("/orders/{id}/pay")
    public String payOrder(@PathVariable Long id,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        BookingOrder order = bookingService.payOrder(id, currentUser.getId());
        String amountStr = order.getTotalAmount() != null
                ? String.format("%.2f", order.getTotalAmount())
                : "0.00";
        redirectAttributes.addFlashAttribute("success",
                "支付成功！已支付 ￥" + amountStr);
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        bookingService.cancelBooking(id, currentUser.getId());
        User refreshed = userService.findById(currentUser.getId());
        session.setAttribute("currentUser", refreshed);
        redirectAttributes.addFlashAttribute("success", "订单已取消。"
                + (refreshed.getCancelCount() >= 3
                    ? "累计取消已达 3 次，7 天内禁止发起新预订！"
                    : "当前累计取消 " + refreshed.getCancelCount() + " 次。"));
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/checkin")
    public String userCheckin(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        BookingOrder order = bookingService.userCheckin(id, currentUser.getId());
        redirectAttributes.addFlashAttribute("success",
                "办理入住成功！订单号：" + order.getOrderNo());
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/checkout")
    public String userCheckout(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        BookingOrder order = bookingService.userCheckout(id, currentUser.getId());
        redirectAttributes.addFlashAttribute("success",
                "退房成功！订单号：" + order.getOrderNo());
        return "redirect:/orders";
    }
}
