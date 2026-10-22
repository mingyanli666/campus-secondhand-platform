package com.digital.platform.controller;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.ReportHandleDTO;
import com.digital.platform.security.LoginUser;
import com.digital.platform.security.RequireRole;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.AdminReportService;
import com.digital.platform.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "15-管理后台·举报处理", description = "举报列表、处理举报")
@RestController
@RequestMapping("/api/admin/report")
@RequireRole(RoleType.ADMIN)
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "举报列表（待处理优先）")
    @GetMapping("/page")
    public Result<PageResult<ReportVO>> page(ReportQuery query) {
        return Result.success(adminReportService.page(query));
    }

    @Operation(summary = "处理举报")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody ReportHandleDTO dto) {
        LoginUser admin = UserContext.require();
        adminReportService.handle(admin.getUserId(), admin.getUsername(), id, dto);
        return Result.success(dto.getStatus() == 1 ? "已处理并通知举报人" : "已驳回并通知举报人", null);
    }
}
