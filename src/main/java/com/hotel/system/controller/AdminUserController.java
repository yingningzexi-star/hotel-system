package com.hotel.system.controller;

import com.hotel.system.entity.User;
import com.hotel.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping("")
    public String listUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "users");

        List<User> users = userService.getAllUsers(keyword);

        int size = 10;
        int total = users.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        model.addAttribute("users", users.subList(fromIndex, toIndex));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", total);
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    @PostMapping("/{id}/ban")
    public String banUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.banUser(id);
        redirectAttributes.addFlashAttribute("success", "用户已被封禁 7 天");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.unbanUser(id);
        redirectAttributes.addFlashAttribute("success", "用户已解封");
        return "redirect:/admin/users";
    }
}
