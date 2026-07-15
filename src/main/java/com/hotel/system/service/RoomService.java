package com.hotel.system.service;

import com.hotel.system.entity.RoomInventory;
import com.hotel.system.entity.RoomType;
import com.hotel.system.repository.RoomInventoryRepository;
import com.hotel.system.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房型与库存业务逻辑层
 * B成员负责：房型管理、每日库存配置与查询
 *
 * 业务规则：
 * 1. 新增房型 → 自动生成未来 N 天的初始每日库存
 * 2. 修改房型基本信息 → 不影响已生成的每日库存
 * 3. 禁用房型 → 该房型不在用户端展示，但已生成的订单不受影响
 * 4. 每日库存独立管理，预订时逐日扣减
 */
@Service
public class RoomService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomInventoryRepository roomInventoryRepository;

    /** 新房型默认生成的未来库存天数 */
    private static final int DEFAULT_INVENTORY_DAYS = 60;

    // ==================== 房型管理 ====================

    /**
     * 获取所有房型（管理员用，包含已禁用）
     */
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    /**
     * 获取所有启用中的房型（用户端用）
     */
    public List<RoomType> getActiveRoomTypes() {
        return roomTypeRepository.findByStatus(1);
    }

    /**
     * 按ID获取房型
     */
    public RoomType getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("房型不存在，ID: " + id));
    }

    /**
     * 新增房型 + 自动初始化未来 N 天的每日库存
     */
    @Transactional
    public RoomType createRoomType(RoomType roomType) {
        // 1. 保存房型基本信息
        if (roomType.getStatus() == null) {
            roomType.setStatus(1); // 默认为启用
        }
        RoomType saved = roomTypeRepository.save(roomType);

        // 2. 自动生成未来 DEFAULT_INVENTORY_DAYS 天的每日库存
        generateInventoryForDays(saved.getId(), saved.getTotalQuantity(), LocalDate.now(), DEFAULT_INVENTORY_DAYS);

        return saved;
    }

    /**
     * 更新房型基本信息（名称、描述、单价、总数量、图片、状态）
     * 注意：修改 totalQuantity 时，可以选择是否同步更新未来库存
     */
    @Transactional
    public RoomType updateRoomType(RoomType roomType) {
        RoomType existing = getRoomTypeById(roomType.getId());

        existing.setName(roomType.getName());
        existing.setDescription(roomType.getDescription());
        existing.setPrice(roomType.getPrice());
        existing.setImagePath(roomType.getImagePath());

        // 如果总数量变化，同步更新未来库存的上限
        if (!Objects.equals(roomType.getTotalQuantity(), existing.getTotalQuantity())) {
            existing.setTotalQuantity(roomType.getTotalQuantity());
            // 同步更新从今天开始的库存上限
            syncInventoryQuantity(existing.getId(), roomType.getTotalQuantity());
        }

        existing.setStatus(roomType.getStatus());

        return roomTypeRepository.save(existing);
    }

    /**
     * 切换房型启用/禁用状态
     */
    @Transactional
    public void toggleRoomStatus(Long id) {
        RoomType roomType = getRoomTypeById(id);
        roomType.setStatus(roomType.getStatus() == 1 ? 0 : 1);
        roomTypeRepository.save(roomType);
    }

    /**
     * 按名称搜索房型（管理员用）
     */
    public List<RoomType> searchRoomTypes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllRoomTypes();
        }
        return roomTypeRepository.findByNameContaining(keyword);
    }

    /**
     * 按名称搜索启用中的房型（用户端用）
     */
    public List<RoomType> searchActiveRoomTypes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveRoomTypes();
        }
        return roomTypeRepository.findByNameContainingAndStatus(keyword, 1);
    }

    // ==================== 每日库存管理 ====================

    /**
     * 获取某房型所有库存记录
     */
    public List<RoomInventory> getInventoryByRoomType(Long roomTypeId) {
        return roomInventoryRepository.findByRoomTypeIdOrderByInventoryDateAsc(roomTypeId);
    }

    /**
     * 获取某房型在指定日期范围内的库存
     */
    public List<RoomInventory> getInventoryByDateRange(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        return roomInventoryRepository.findByRoomTypeIdAndInventoryDateBetweenOrderByInventoryDateAsc(
                roomTypeId, startDate, endDate);
    }

    /**
     * 手动更新某日的可用库存（管理员手动调整）
     */
    @Transactional
    public void updateDailyInventory(Long id, Integer availableQuantity) {
        RoomInventory inventory = roomInventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("库存记录不存在，ID: " + id));
        inventory.setAvailableQuantity(availableQuantity);
        roomInventoryRepository.save(inventory);
    }

    /**
     * 批量初始化/补充某房型未来 N 天的库存
     * 已存在的日期不会覆盖，仅补充缺失日期
     */
    @Transactional
    public void generateInventoryForDays(Long roomTypeId, Integer totalQuantity, LocalDate startDate, int days) {
        RoomType roomType = getRoomTypeById(roomTypeId);

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            // 如果该日已有库存记录，跳过
            if (roomInventoryRepository.findByRoomTypeIdAndInventoryDate(roomTypeId, date).isPresent()) {
                continue;
            }
            RoomInventory inventory = new RoomInventory();
            inventory.setRoomTypeId(roomTypeId);
            inventory.setInventoryDate(date);
            inventory.setAvailableQuantity(totalQuantity);
            roomInventoryRepository.save(inventory);
        }
    }

    /**
     * 同步库存上限：更新某房型所有未来日期的库存数量（当 totalQuantity 变化时调用）
     * 仅调整可用数量 > 新总数量 的记录，避免覆盖已预订的扣减
     */
    @Transactional
    public void syncInventoryQuantity(Long roomTypeId, Integer newTotalQuantity) {
        List<RoomInventory> inventories = getInventoryByRoomType(roomTypeId);
        LocalDate today = LocalDate.now();
        for (RoomInventory inv : inventories) {
            if (!inv.getInventoryDate().isBefore(today)) { // 今天及未来的日期
                // 如果当前可用数量 > 新的总数量，则调低到新总数量
                // 但不会调高（避免覆盖已扣减的预订数据）
                if (inv.getAvailableQuantity() > newTotalQuantity) {
                    inv.setAvailableQuantity(newTotalQuantity);
                    roomInventoryRepository.save(inv);
                }
            }
        }
    }

    // ==================== 用户端查询 ====================

    /**
     * 查询用户可预订的房型（仅启用中的房型）
     * 返回每个房型在指定日期范围内是否有可用库存
     */
    public List<Map<String, Object>> getAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null) {
            checkInDate = LocalDate.now().plusDays(1); // 默认明天入住
        }
        if (checkOutDate == null) {
            checkOutDate = checkInDate.plusDays(1); // 默认住1晚
        }
        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            throw new IllegalArgumentException("离店日期必须晚于入住日期");
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }

        // 1. 获取所有启用房型
        List<RoomType> activeRooms = getActiveRoomTypes();

        // 2. 查询这些房型在日期范围内的库存
        List<Long> roomTypeIds = activeRooms.stream().map(RoomType::getId).collect(Collectors.toList());
        List<RoomInventory> inventories = roomInventoryRepository.findByRoomTypeIdsAndDateRange(
                roomTypeIds, checkInDate, checkOutDate.minusDays(1)); // 离店日不占库存

        // 3. 按房型分组
        Map<Long, List<RoomInventory>> inventoryMap = inventories.stream()
                .collect(Collectors.groupingBy(RoomInventory::getRoomTypeId));

        // 4. 计算每晚所需天数
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        // 5. 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (RoomType room : activeRooms) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("roomType", room);

            List<RoomInventory> roomInvList = inventoryMap.get(room.getId());
            if (roomInvList == null) {
                item.put("available", false);
                item.put("minAvailable", 0);
                item.put("totalNights", nights);
                item.put("totalPrice", room.getPrice().multiply(java.math.BigDecimal.valueOf(nights)));
                item.put("inventoryList", Collections.emptyList());
            } else {
                // 取所有日期中可用数量的最小值（瓶颈）
                int minAvailable = roomInvList.stream()
                        .mapToInt(RoomInventory::getAvailableQuantity)
                        .min().orElse(0);
                item.put("available", minAvailable > 0);
                item.put("minAvailable", minAvailable);
                item.put("totalNights", nights);
                item.put("totalPrice", room.getPrice().multiply(java.math.BigDecimal.valueOf(nights)));
                item.put("inventoryList", roomInvList);
            }
            result.add(item);
        }

        return result;
    }

    /**
     * 获取房型详情 + 指定日期范围内的库存情况
     */
    public Map<String, Object> getRoomDetailWithAvailability(Long roomTypeId,
                                                               LocalDate checkInDate,
                                                               LocalDate checkOutDate) {
        RoomType roomType = getRoomTypeById(roomTypeId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomType", roomType);

        if (checkInDate != null && checkOutDate != null) {
            List<RoomInventory> inventories = getInventoryByDateRange(
                    roomTypeId, checkInDate, checkOutDate.minusDays(1));
            result.put("inventoryList", inventories);

            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            result.put("totalNights", nights);
            result.put("totalPrice", roomType.getPrice().multiply(java.math.BigDecimal.valueOf(nights)));
        }

        return result;
    }

    // ==================== 库存扣减/恢复（供 C 成员预订模块调用） ====================

    /**
     * 扣减库存（预订时调用）
     * 逐日扣减，任一日库存不足则回滚
     */
    @Transactional
    public boolean deductInventory(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate, int quantity) {
        LocalDate date = checkInDate;
        while (date.isBefore(checkOutDate)) {
            Optional<RoomInventory> opt = roomInventoryRepository
                    .findByRoomTypeIdAndInventoryDate(roomTypeId, date);
            if (opt.isEmpty() || opt.get().getAvailableQuantity() < quantity) {
                throw new IllegalStateException("库存不足：" + date.toString());
            }
            date = date.plusDays(1);
        }

        // 检查通过，执行扣减
        date = checkInDate;
        while (date.isBefore(checkOutDate)) {
            Optional<RoomInventory> opt = roomInventoryRepository
                    .findByRoomTypeIdAndInventoryDate(roomTypeId, date);
            RoomInventory inv = opt.get();
            inv.setAvailableQuantity(inv.getAvailableQuantity() - quantity);
            roomInventoryRepository.save(inv);
            date = date.plusDays(1);
        }
        return true;
    }

    /**
     * 恢复库存（取消订单时调用）
     */
    @Transactional
    public boolean restoreInventory(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate, int quantity) {
        LocalDate date = checkInDate;
        while (date.isBefore(checkOutDate)) {
            Optional<RoomInventory> opt = roomInventoryRepository
                    .findByRoomTypeIdAndInventoryDate(roomTypeId, date);
            if (opt.isPresent()) {
                RoomInventory inv = opt.get();
                inv.setAvailableQuantity(inv.getAvailableQuantity() + quantity);
                roomInventoryRepository.save(inv);
            }
            date = date.plusDays(1);
        }
        return true;
    }
}
