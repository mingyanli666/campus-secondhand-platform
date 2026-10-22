package com.digital.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.NotificationType;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.entity.Notification;
import com.digital.platform.mapper.NotificationMapper;
import com.digital.platform.service.NotificationService;
import com.digital.platform.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long userId, NotificationType type, String title, String content, Long bizId) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type.getCode());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizId(bizId);
        notification.setIsRead(Constants.UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        log.debug("发送通知给用户 {}：{}", userId, title);
    }

    @Override
    public PageResult<NotificationVO> page(Long userId, Long current, Long size) {
        long pageNum = (current == null || current < 1) ? Constants.DEFAULT_PAGE_NUM : current;
        long pageSize = (size == null || size < 1) ? 10L : Math.min(size, Constants.MAX_PAGE_SIZE);

        // 只能查自己的通知：userId 条件必须带上，
        // 否则改个参数就能读到别人的通知（越权）
        IPage<Notification> page = notificationMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));

        return PageResult.of(page, n -> BeanUtil.copyProperties(n, NotificationVO.class));
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, Constants.UNREAD));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long notificationId) {
        // 带 userId 条件更新：即使别人拿到我的通知ID，也改不动
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .set(Notification::getIsRead, Constants.READ)
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .set(Notification::getIsRead, Constants.READ)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, Constants.UNREAD));
    }
}
