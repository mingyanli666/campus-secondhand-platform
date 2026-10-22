package com.digital.platform.service;

import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.dto.ReportHandleDTO;
import com.digital.platform.vo.ReportVO;

/**
 * 管理后台举报处理服务。
 * <p>
 * 和用户侧分开，理由和 AdminProductService 一样：
 * 用户侧只有"提交"和"看我自己的"，管理侧是"看全部"和"处置"，
 * 两边的权限模型完全不同，混在一个类里迟早出错。
 */
public interface AdminReportService {

    /** 举报列表（待处理的排在前面） */
    PageResult<ReportVO> page(ReportQuery query);

    /** 处理举报 */
    void handle(Long adminId, String adminName, Long reportId, ReportHandleDTO dto);
}
