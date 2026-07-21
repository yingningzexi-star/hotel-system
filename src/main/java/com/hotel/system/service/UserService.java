package com.hotel.system.service;

import com.hotel.system.entity.User;
import com.hotel.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注册新用户
     */
    @Transactional
    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER"); // 注册默认角色为普通用户
        user.setCancelCount(0);
        user.setBannedUntil(null);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 用户登录验证
     */
    @Transactional
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 检查并更新禁订状态
        checkAndUpdateBannedStatus(user);

        return user;
    }

    /**
     * 增加取消订单次数并根据需要进行惩罚
     */
    @Transactional
    public void incrementCancelCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        user.setCancelCount(user.getCancelCount() + 1);

        if (user.getCancelCount() >= 3) {
            // 累计取消 3 次，自当前时间起禁订 7 天
            user.setBannedUntil(LocalDateTime.now().plusDays(7));
        }

        userRepository.save(user);
    }

    /**
     * 检查并主动刷新用户的禁订状态（如果已过期则解禁并清空取消次数）
     */
    @Transactional
    public boolean checkAndUpdateBannedStatus(User user) {
        if (user.getBannedUntil() == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(user.getBannedUntil())) {
            // 禁订期已过，自动解除禁订并重置取消次数
            user.setBannedUntil(null);
            user.setCancelCount(0);
            userRepository.save(user);
            return false;
        }

        return true; // 依然处于禁订期内
    }

    /**
     * 根据ID检查用户当前是否处于禁订状态
     */
    @Transactional
    public boolean isUserBanned(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return checkAndUpdateBannedStatus(user);
    }

    /**
     * 根据ID查找用户（用于刷新 session 中的用户数据）
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 更新用户个人资料（手机号和密码）
     */
    @Transactional
    public void updateProfile(Long userId, String phone, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone.trim());
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("当前密码错误");
            }
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("新密码长度不能少于6位");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
    }

    /**
     * 管理员手动封禁用户
     */
    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setBannedUntil(LocalDateTime.now().plusDays(7));
        user.setCancelCount(3);
        userRepository.save(user);
    }

    /**
     * 管理员手动解封用户
     */
    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setBannedUntil(null);
        user.setCancelCount(0);
        userRepository.save(user);
    }

    /**
     * 获取所有用户列表（支持关键词搜索）
     */
    public List<User> getAllUsers(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return userRepository.findByUsernameContainingOrRealNameContaining(keyword, keyword);
        }
        return userRepository.findAll();
    }

    /**
     * 统计用户总数（Dashboard 统计用）
     */
    public long countTotalUsers() {
        return userRepository.count();
    }
}
