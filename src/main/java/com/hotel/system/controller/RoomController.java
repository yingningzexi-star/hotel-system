package com.hotel.system.controller;

import com.hotel.system.entity.User;
import com.hotel.system.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 用户端 — 房型浏览与搜索控制器
 * B成员负责：用户可预订房型的展示、按日期查询可用房间
 *
 * 关联：C 成员的预订模块将通过此页面的房型信息发起预订
 */
@Controller
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    /**
     * 房型浏览页（用户端首页）
     * GET /rooms
     *
     * 支持按入住/离店日期筛选可用房型
     */
    @GetMapping("")
    public String listRooms(@RequestParam(value = "checkIn", required = false) String checkInStr,
                            @RequestParam(value = "checkOut", required = false) String checkOutStr,
                            Model model, HttpSession session) {

        // 当前登录用户（可为 null，未登录也能浏览）
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        // 解析日期参数
        LocalDate checkInDate = null;
        LocalDate checkOutDate = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            if (checkInStr != null && !checkInStr.isEmpty()) {
                checkInDate = LocalDate.parse(checkInStr, formatter);
            }
            if (checkOutStr != null && !checkOutStr.isEmpty()) {
                checkOutDate = LocalDate.parse(checkOutStr, formatter);
            }

            // 执行查询
            List<Map<String, Object>> availableRooms = roomService.getAvailableRooms(checkInDate, checkOutDate);
            model.addAttribute("availableRooms", availableRooms);

            // 回填日期到页面
            model.addAttribute("checkInDate", checkInDate != null ? checkInDate : LocalDate.now().plusDays(1));
            model.addAttribute("checkOutDate", checkOutDate != null ? checkOutDate : LocalDate.now().plusDays(2));

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            // 默认显示未来数据
            model.addAttribute("checkInDate", LocalDate.now().plusDays(1));
            model.addAttribute("checkOutDate", LocalDate.now().plusDays(2));

            // 无筛选查询
            try {
                List<Map<String, Object>> availableRooms = roomService.getAvailableRooms(null, null);
                model.addAttribute("availableRooms", availableRooms);
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }

        model.addAttribute("activePage", "rooms");
        return "rooms";
    }

    /**
     * 房型详情页
     * GET /rooms/{id}
     */
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

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/rooms";
        }

        return "room-detail";
    }
}
