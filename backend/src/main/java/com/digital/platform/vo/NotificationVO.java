package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "站内通知")
public class NotificationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "类型 1审核结果 2订单状态 3留言回复 4系统通知")
    private Integer type;

    private String title;

    private String content;

    @Schema(description = "关联业务ID，点击可跳转")
    private Long bizId;

    @Schema(description = "是否已读")
    private Integer isRead;

    private LocalDateTime createTime;
}
