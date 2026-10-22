package com.digital.platform.service;

import com.digital.platform.common.enums.NotificationType;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.vo.NotificationVO;

public interface NotificationService {

    /**
     * 发送站内通知。
     * <p>
     * 所有需要通知用户的地方都调这个方法，而不是各自往表里插数据，
     * 这样"通知长什么样"只在一处定义。
     */
    void send(Long userId, NotificationType type, String title, String content, Long bizId);

    /** 分页查询我的通知 */
    PageResult<NotificationVO> page(Long userId, Long current, Long size);

    /** 未读数量，用于导航栏红点 */
    long unreadCount(Long userId);

    /** 标记单条已读 */
    void markRead(Long userId, Long notificationId);

    /** 全部标记已读 */
    void markAllRead(Long userId);
}
