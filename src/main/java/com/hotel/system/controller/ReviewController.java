package com.hotel.system.controller;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.Review;
import com.hotel.system.entity.User;
import com.hotel.system.service.BookingService;
import com.hotel.system.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/reviews/write/{orderId}")
    public String writeReviewPage(@PathVariable Long orderId,
                                  HttpSession session,
                                  Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        BookingOrder order = bookingService.getOrderById(orderId);
        if (!order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/orders";
        }

        if (!"COMPLETED".equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已完成的订单才能评价");
        }

        if (reviewService.hasReviewed(orderId)) {
            throw new IllegalArgumentException("该订单已评价过");
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", currentUser);
        return "review-form";
    }

    @PostMapping("/reviews")
    public String submitReview(@RequestParam Long orderId,
                               @RequestParam Long roomTypeId,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        reviewService.createReview(currentUser.getId(), roomTypeId, orderId, rating, comment);
        redirectAttributes.addFlashAttribute("success", "评价提交成功！感谢您的反馈。");
        return "redirect:/orders";
    }

    @GetMapping("/reviews")
    public String myReviews(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Review> reviews = reviewService.getUserReviews(currentUser.getId());
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentUser", currentUser);
        return "reviews";
    }
}
