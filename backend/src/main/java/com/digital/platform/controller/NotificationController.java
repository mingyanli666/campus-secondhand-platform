package com.digital.platform.controller;

import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.NotificationService;
import com.digital.platform.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知接口。
 * <p>
 * 所有接口的 userId 都从 token 取，前端无法查询或修改别人的通知。
 */
@Tag(name = "08-站内通知", description = "通知列表、未读数、标记已读")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的通知列表")
    @GetMapping("/page")
    public Result<PageResult<NotificationVO>> page(
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size) {
        return Result.success(notificationService.page(UserContext.userId(), current, size));
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount(UserContext.userId()));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(UserContext.userId(), id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.userId());
        return Result.success();
    }
}
