package com.hotel.system.controller;

import com.hotel.system.entity.Review;
import com.hotel.system.entity.User;
import com.hotel.system.service.ReviewService;
import com.hotel.system.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("")
    public String listRooms(@RequestParam(value = "checkIn", required = false) String checkInStr,
                            @RequestParam(value = "checkOut", required = false) String checkOutStr,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "9") int size,
                            Model model, HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate checkInDate = null;
        LocalDate checkOutDate = null;

        try {
            if (checkInStr != null && !checkInStr.isEmpty()) {
                checkInDate = LocalDate.parse(checkInStr, formatter);
            }
            if (checkOutStr != null && !checkOutStr.isEmpty()) {
                checkOutDate = LocalDate.parse(checkOutStr, formatter);
            }

            List<Map<String, Object>> availableRooms = roomService.getAvailableRooms(checkInDate, checkOutDate);

            // add rating info to each room
            for (Map<String, Object> item : availableRooms) {
                var room = (com.hotel.system.entity.RoomType) item.get("roomType");
                Double avgRating = reviewService.getRoomAverageRating(room.getId());
                long reviewCount = reviewService.getRoomReviewCount(room.getId());
                item.put("avgRating", avgRating != null ? avgRating : 0.0);
                item.put("reviewCount", reviewCount);
            }

            // keyword filter
            if (keyword != null && !keyword.trim().isEmpty()) {
                availableRooms = availableRooms.stream()
                        .filter(item -> {
                            var room = (com.hotel.system.entity.RoomType) item.get("roomType");
                            return room.getName().toLowerCase().contains(keyword.trim().toLowerCase());
                        })
                        .toList();
            }

            // pagination
            int total = availableRooms.size();
            int totalPages = total > 0 ? (int) Math.ceil((double) total / size) : 0;
            if (page < 0) page = 0;
            if (totalPages > 0 && page >= totalPages) page = totalPages - 1;
            int from = Math.min(page * size, total);
            int to = Math.min(from + size, total);
            List<Map<String, Object>> pagedRooms = availableRooms.subList(from, to);

            model.addAttribute("availableRooms", pagedRooms);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", total);
            model.addAttribute("keyword", keyword);
            model.addAttribute("checkInDate", checkInDate != null ? checkInDate : LocalDate.now().plusDays(1));
            model.addAttribute("checkOutDate", checkOutDate != null ? checkOutDate : LocalDate.now().plusDays(2));

        } catch (Exception e) {
            log.warn("房型列表查询失败，将使用默认参数重试: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("checkInDate", LocalDate.now().plusDays(1));
            model.addAttribute("checkOutDate", LocalDate.now().plusDays(2));
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
            try {
                List<Map<String, Object>> fallbackRooms = roomService.getAvailableRooms(null, null);
                model.addAttribute("availableRooms", fallbackRooms);
            } catch (Exception ex) {
                log.error("回退查询也失败: {}", ex.getMessage(), ex);
                // 保留原始错误信息，不覆盖
                model.addAttribute("availableRooms", java.util.Collections.emptyList());
            }
        }

        model.addAttribute("activePage", "rooms");
        return "rooms";
    }

    @GetMapping("/{id}")
    public String roomDetail(@PathVariable Long id,
                             @RequestParam(value = "checkIn", required = false) String checkInStr,
                             @RequestParam(value = "checkOut", required = false) String checkOutStr,
                             Model model, HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate checkInDate = null;
        LocalDate checkOutDate = null;

        try {
            if (checkInStr != null && !checkInStr.isEmpty()) {
                checkInDate = LocalDate.parse(checkInStr, formatter);
            }
            if (checkOutStr != null && !checkOutStr.isEmpty()) {
                checkOutDate = LocalDate.parse(checkOutStr, formatter);
            }

            Map<String, Object> detail = roomService.getRoomDetailWithAvailability(id, checkInDate, checkOutDate);
            model.addAttribute("detail", detail);
            model.addAttribute("checkInDate", checkInDate);
            model.addAttribute("checkOutDate", checkOutDate);

            // review data
            Double avgRating = reviewService.getRoomAverageRating(id);
            long reviewCount = reviewService.getRoomReviewCount(id);
            List<Review> reviews = reviewService.getRoomReviews(id);
            model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
            model.addAttribute("reviewCount", reviewCount);
            model.addAttribute("reviews", reviews);

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/rooms";
        }

        return "room-detail";
    }
}
