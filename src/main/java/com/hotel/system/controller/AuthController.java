package com.hotel.system.controller;

import com.hotel.system.entity.User;
import com.hotel.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

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
            
            // 如果是管理员，进入管理端（先做重定向，实际开发中管理员可能会有特定主页）
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin/test";
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
        
        model.addAttribute("user", currentUser);
        return "profile";
    }

    @GetMapping("/403")
    public String forbiddenPage() {
        return "403";
    }

    // 辅助测试：模拟管理员后台路径，用来验证普通用户越权拦截
    @GetMapping("/admin/test")
    @ResponseBody
    public String adminTest(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        return "Hello Admin! Current user: " + (currentUser != null ? currentUser.getRealName() : "Anonymous");
    }
}
