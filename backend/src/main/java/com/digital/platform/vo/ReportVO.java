package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "举报记录")
public class ReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    @Schema(description = "被举报商品标题")
    private String productTitle;

    @Schema(description = "被举报商品封面")
    private String productCover;

    @Schema(description = "被举报商品当前状态")
    private Integer productStatus;

    @Schema(description = "举报人ID")
    private Long userId;

    @Schema(description = "举报人昵称")
    private String reporterNickname;

    @Schema(description = "举报类型 1虚假信息 2违禁物品 3价格欺诈 4其他")
    private Integer type;

    @Schema(description = "举报类型文字")
    private String typeText;

    @Schema(description = "举报说明")
    private String reason;

    @Schema(description = "状态 0待处理 1已处理 2已驳回")
    private Integer status;

    @Schema(description = "状态文字")
    private String statusText;

    @Schema(description = "处理结论")
    private String handleResult;

    @Schema(description = "处理人昵称")
    private String handlerName;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
