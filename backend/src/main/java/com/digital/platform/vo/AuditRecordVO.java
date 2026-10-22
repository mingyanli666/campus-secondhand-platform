package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "审核记录")
public class AuditRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    @Schema(description = "商品标题快照")
    private String productTitle;

    @Schema(description = "审核人账号")
    private String adminName;

    @Schema(description = "审核动作 1通过 2驳回 3强制下架")
    private Integer action;

    @Schema(description = "审核动作文字")
    private String actionText;

    @Schema(description = "驳回原因")
    private String reason;

    private LocalDateTime createTime;
}
