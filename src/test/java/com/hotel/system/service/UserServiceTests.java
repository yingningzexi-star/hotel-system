package com.hotel.system.service;

import com.hotel.system.entity.User;
import com.hotel.system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 保证测试完成后自动回滚数据，不污染数据库
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRegisterAndLogin() {
        String username = "test_user_unique";
        String password = "mySecurePassword123";

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName("测试用户");
        user.setPhone("13500001111");

        // 1. 测试注册
        User registeredUser = userService.register(user);
        assertNotNull(registeredUser.getId());
        assertEquals("USER", registeredUser.getRole());
        assertEquals(0, registeredUser.getCancelCount());
        assertNull(registeredUser.getBannedUntil());

        // 2. 测试重复注册抛出异常
        User duplicateUser = new User();
        duplicateUser.setUsername(username);
        duplicateUser.setPassword("anotherPassword");
        duplicateUser.setRealName("重名用户");
        duplicateUser.setPhone("13500002222");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(duplicateUser);
        });

        // 3. 测试正确登录
        User loggedUser = userService.login(username, password);
        assertNotNull(loggedUser);
        assertEquals(username, loggedUser.getUsername());

        // 4. 测试错误密码登录抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(username, "wrongPassword");
        });
    }

    @Test
    void testCreditCancelCountAndBannedRules() {
        String username = "test_cancel_user";
        String password = "password123";

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName("取消测试用户");
        user.setPhone("13611112222");

        User registered = userService.register(user);
        Long userId = registered.getId();

        // 1. 初始状态：未禁订
        assertFalse(userService.isUserBanned(userId));
        assertEquals(0, registered.getCancelCount());

        // 2. 模拟取消 1 次
        userService.incrementCancelCount(userId);
        User u1 = userRepository.findById(userId).get();
        assertEquals(1, u1.getCancelCount());
        assertNull(u1.getBannedUntil());
        assertFalse(userService.isUserBanned(userId));

        // 3. 模拟取消 2 次
        userService.incrementCancelCount(userId);
        User u2 = userRepository.findById(userId).get();
        assertEquals(2, u2.getCancelCount());
        assertNull(u2.getBannedUntil());
        assertFalse(userService.isUserBanned(userId));

        // 4. 模拟取消 3 次，触犯规则，触发禁订 7 天
        userService.incrementCancelCount(userId);
        User u3 = userRepository.findById(userId).get();
        assertEquals(3, u3.getCancelCount());
        assertNotNull(u3.getBannedUntil());
        assertTrue(userService.isUserBanned(userId));

        // 5. 模拟禁订时间已过：手动将 banned_until 调整到过去（如 1 小时前）
        u3.setBannedUntil(LocalDateTime.now().minusHours(1));
        userRepository.save(u3);

        // 调用检查方法，应当自动解锁，重置 cancel_count 和 banned_until
        boolean bannedAfterExpiry = userService.isUserBanned(userId);
        assertFalse(bannedAfterExpiry); // 应当解禁为 false

        User unlockedUser = userRepository.findById(userId).get();
        assertEquals(0, unlockedUser.getCancelCount()); // 应当被重置为 0
        assertNull(unlockedUser.getBannedUntil());      // 应当重置为 NULL
    }
}
