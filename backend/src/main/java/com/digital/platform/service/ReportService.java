package com.digital.platform.service;

import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.dto.ReportCreateDTO;
import com.digital.platform.vo.ReportVO;

/**
 * 用户侧举报服务。
 */
public interface ReportService {

    /** 提交举报，返回举报ID */
    Long create(Long userId, ReportCreateDTO dto);

    /** 我提交的举报记录 */
    PageResult<ReportVO> pageMine(Long userId, ReportQuery query);
}
