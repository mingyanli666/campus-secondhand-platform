package com.digital.platform.runner;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.RoleType;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 首次启动时创建默认账号。
 * <p>
 * 为什么不把账号直接写在 init.sql 里？因为密码要用 BCrypt 加密，
 * 密文写死在 SQL 里既不直观、也容易和代码里的加密方式不一致。
 * 放在程序里生成，账号密码与实现永远对齐，且重复启动不会重复插入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;

    @Override
    public void run(String... args) {
        createIfAbsent("admin", "平台管理员", RoleType.ADMIN.getCode());
        createIfAbsent("zhangsan", "张三", RoleType.USER.getCode());
        createIfAbsent("lisi", "李四", RoleType.USER.getCode());
    }

    private void createIfAbsent(String username, String nickname, String role) {
        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (count > 0) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD, BCrypt.gensalt()));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(Constants.STATUS_ENABLED);
        user.setGender(0);
        user.setCreditScore(100);
        user.setIntro(RoleType.ADMIN.getCode().equals(role) ? "平台管理员账号" : "默认演示账号");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("初始化默认账号: {} / {}", username, DEFAULT_PASSWORD);
    }
}
