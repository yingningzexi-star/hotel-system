package com.hotel.system.controller;

import com.hotel.system.entity.RoomType;
import com.hotel.system.entity.User;
import com.hotel.system.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/rooms")
    public String roomsPage(@RequestParam(required = false) LocalDate checkIn,
                            @RequestParam(required = false) LocalDate checkOut,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<RoomType> rooms;
        if (checkIn != null && checkOut != null) {
            if (!checkIn.isBefore(checkOut)) {
                model.addAttribute("error", "入住日期必须早于离店日期");
                rooms = roomService.findAllActive();
            } else if (checkIn.isBefore(LocalDate.now())) {
                model.addAttribute("error", "入住日期不能早于今天");
                rooms = roomService.findAllActive();
            } else {
                rooms = roomService.findAvailableRooms(checkIn, checkOut, quantity);
                if (rooms.isEmpty()) {
                    model.addAttribute("info", "所选日期范围内暂无可预订房型");
                }
            }
        } else {
            rooms = roomService.findAllActive();
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("quantity", quantity);
        model.addAttribute("today", LocalDate.now());
        if (checkIn != null && checkOut != null && checkIn.isBefore(checkOut)) {
            model.addAttribute("nights", ChronoUnit.DAYS.between(checkIn, checkOut));
        }
        return "rooms";
    }
}
