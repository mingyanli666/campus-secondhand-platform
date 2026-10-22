package com.digital.platform.controller;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.result.Result;
import com.digital.platform.security.RequireRole;
import com.digital.platform.service.AdminDashboardService;
import com.digital.platform.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "07-管理后台·数据看板", description = "平台统计数据")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequireRole(RoleType.ADMIN)
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "平台统计数据")
    @GetMapping("/stats")
    public Result<DashboardVO> stats() {
        return Result.success(adminDashboardService.stats());
    }
}
