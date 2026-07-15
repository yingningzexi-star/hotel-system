package com.hotel.system.controller;

import com.hotel.system.entity.Review;
import com.hotel.system.entity.User;
import com.hotel.system.service.ReviewService;
import com.hotel.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/profile";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = userService.login(username, password);
            session.setAttribute("currentUser", user);

            // 如果是管理员，跳转到后台房型管理首页
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin/room";
            }
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/profile";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        try {
            userService.register(user);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 实时刷新信用禁订状态
        userService.checkAndUpdateBannedStatus(currentUser);

        // 加载用户评价
        List<Review> reviews = reviewService.getUserReviews(currentUser.getId());
        model.addAttribute("reviews", reviews);
        model.addAttribute("user", currentUser);
        model.addAttribute("currentUser", currentUser);
        return "profile";
    }

    @PostMapping("/profile/edit")
    public String editProfile(@RequestParam(required = false) String phone,
                              @RequestParam(required = false) String currentPassword,
                              @RequestParam(required = false) String newPassword,
                              @RequestParam(required = false) String confirmPassword,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "两次输入的新密码不一致");
                return "redirect:/profile";
            }
        }

        userService.updateProfile(currentUser.getId(), phone, currentPassword, newPassword);
        User refreshed = userService.findById(currentUser.getId());
        session.setAttribute("currentUser", refreshed);
        redirectAttributes.addFlashAttribute("success", "个人资料更新成功");
        return "redirect:/profile";
    }

    @GetMapping("/403")
    public String forbiddenPage() {
        return "403";
    }
}
