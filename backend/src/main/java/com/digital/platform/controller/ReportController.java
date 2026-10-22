package com.digital.platform.controller;

import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.ReportCreateDTO;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.ReportService;
import com.digital.platform.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧举报接口。
 * <p>
 * 只提供"提交"和"看我自己的"两个能力。处理举报是管理员的事，
 * 放在 AdminReportController 里 —— 两边的权限模型完全不同。
 */
@Tag(name = "14-举报", description = "举报违规商品、查看我的举报")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "举报商品")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ReportCreateDTO dto) {
        Long id = reportService.create(UserContext.userId(), dto);
        return Result.success("举报已提交，平台会尽快核实", id);
    }

    @Operation(summary = "我提交的举报")
    @GetMapping("/mine")
    public Result<PageResult<ReportVO>> mine(ReportQuery query) {
        return Result.success(reportService.pageMine(UserContext.userId(), query));
    }
}
