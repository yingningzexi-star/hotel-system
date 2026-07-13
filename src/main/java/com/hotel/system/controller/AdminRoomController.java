package com.hotel.system.controller;

import com.hotel.system.entity.RoomInventory;
import com.hotel.system.entity.RoomType;
import com.hotel.system.entity.User;
import com.hotel.system.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理员后台 — 房型与库存管理控制器
 * B成员负责：房型CRUD + 每日库存配置 + 后台管理界面
 *
 * URL前缀：/admin/room
 * 已由 LoginInterceptor 自动拦截并校验 ADMIN 角色
 */
@Controller
@RequestMapping("/admin/room")
public class AdminRoomController {

    @Autowired
    private RoomService roomService;

    // ==================== 房型管理 ====================

    /**
     * 房型列表页（管理后台首页）
     * GET /admin/room
     */
    @GetMapping("")
    public String listRooms(Model model, HttpSession session,
                            @RequestParam(value = "keyword", required = false) String keyword) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        List<RoomType> roomTypes;
        if (keyword != null && !keyword.trim().isEmpty()) {
            roomTypes = roomService.searchRoomTypes(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            roomTypes = roomService.getAllRoomTypes();
        }
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("activePage", "rooms");

        return "admin/room-list";
    }

    /**
     * 新增房型表单页
     * GET /admin/room/create
     */
    @GetMapping("/create")
    public String createRoomForm(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("roomType", new RoomType());
        model.addAttribute("isEdit", false);
        model.addAttribute("activePage", "rooms");

        return "admin/room-form";
    }

    /**
     * 提交新增房型
     * POST /admin/room/create
     */
    @PostMapping("/create")
    public String createRoom(@ModelAttribute RoomType roomType,
                             RedirectAttributes redirectAttributes) {
        try {
            roomService.createRoomType(roomType);
            redirectAttributes.addFlashAttribute("success", "房型「" + roomType.getName() + "」创建成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "创建失败：" + e.getMessage());
        }
        return "redirect:/admin/room";
    }

    /**
     * 编辑房型表单页
     * GET /admin/room/edit/{id}
     */
    @GetMapping("/edit/{id}")
    public String editRoomForm(@PathVariable Long id, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        try {
            RoomType roomType = roomService.getRoomTypeById(id);
            model.addAttribute("roomType", roomType);
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "rooms");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/room";
        }

        return "admin/room-form";
    }

    /**
     * 提交编辑房型
     * POST /admin/room/edit
     */
    @PostMapping("/edit")
    public String updateRoom(@ModelAttribute RoomType roomType,
                             RedirectAttributes redirectAttributes) {
        try {
            roomService.updateRoomType(roomType);
            redirectAttributes.addFlashAttribute("success", "房型「" + roomType.getName() + "」更新成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
        }
        return "redirect:/admin/room";
    }

    /**
     * 切换房型启用/禁用状态
     * POST /admin/room/toggle/{id}
     */
    @PostMapping("/toggle/{id}")
    public String toggleRoomStatus(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            RoomType roomType = roomService.getRoomTypeById(id);
            roomService.toggleRoomStatus(id);
            String statusText = roomType.getStatus() == 1 ? "已禁用" : "已启用";
            redirectAttributes.addFlashAttribute("success",
                    "房型「" + roomType.getName() + "」" + statusText);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "操作失败：" + e.getMessage());
        }
        return "redirect:/admin/room";
    }

    // ==================== 每日库存管理 ====================

    /**
     * 库存管理页 — 查看某房型的每日库存
     * GET /admin/room/inventory/{roomTypeId}
     */
    @GetMapping("/inventory/{roomTypeId}")
    public String inventoryPage(@PathVariable Long roomTypeId, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        try {
            RoomType roomType = roomService.getRoomTypeById(roomTypeId);
            List<RoomInventory> inventories = roomService.getInventoryByRoomType(roomTypeId);

            model.addAttribute("roomType", roomType);
            model.addAttribute("inventories", inventories);
            model.addAttribute("activePage", "inventory");

            // 日期格式化辅助
            model.addAttribute("today", LocalDate.now());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/room";
        }

        return "admin/inventory";
    }

    /**
     * 更新单条库存记录
     * POST /admin/room/inventory/update
     */
    @PostMapping("/inventory/update")
    public String updateInventory(@RequestParam Long inventoryId,
                                  @RequestParam Integer availableQuantity,
                                  @RequestParam Long roomTypeId,
                                  RedirectAttributes redirectAttributes) {
        try {
            roomService.updateDailyInventory(inventoryId, availableQuantity);
            redirectAttributes.addFlashAttribute("success", "库存数量已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
        }
        return "redirect:/admin/room/inventory/" + roomTypeId;
    }

    /**
     * 批量补充未来库存（为指定房型补充未来 N 天的初始库存）
     * POST /admin/room/inventory/generate
     */
    @PostMapping("/inventory/generate")
    public String generateInventory(@RequestParam Long roomTypeId,
                                    @RequestParam(defaultValue = "30") int days,
                                    RedirectAttributes redirectAttributes) {
        try {
            RoomType roomType = roomService.getRoomTypeById(roomTypeId);
            roomService.generateInventoryForDays(roomTypeId, roomType.getTotalQuantity(),
                    LocalDate.now(), days);
            redirectAttributes.addFlashAttribute("success", "已补充未来 " + days + " 天的库存记录");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "补充失败：" + e.getMessage());
        }
        return "redirect:/admin/room/inventory/" + roomTypeId;
    }
}
