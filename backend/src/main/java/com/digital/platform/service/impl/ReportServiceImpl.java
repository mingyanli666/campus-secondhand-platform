package com.digital.platform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.enums.ReportStatus;
import com.digital.platform.common.enums.ReportType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.ReportCreateDTO;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.Report;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.ReportMapper;
import com.digital.platform.service.ReportService;
import com.digital.platform.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, ReportCreateDTO dto) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 不能举报自己的商品。
        // 否则卖家可以用自己的账号给自己的商品刷举报，或者反过来 ——
        // 拿举报当工具去干扰别人的商品。这两种都不该允许。
        if (product.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CANNOT_REPORT_SELF);
        }

        // 同一个用户对同一个商品只允许有一条"待处理"的举报。
        // 注意加了 status 条件：如果上一次举报已经被驳回了，
        // 用户发现了新的证据，应该允许他再举报一次。
        Long pending = reportMapper.selectCount(Wrappers.<Report>lambdaQuery()
                .eq(Report::getProductId, product.getId())
                .eq(Report::getUserId, userId)
                .eq(Report::getStatus, ReportStatus.PENDING.getCode()));
        if (pending > 0) {
            throw new BusinessException(ResultCode.ALREADY_REPORTED);
        }

        Report report = new Report();
        report.setProductId(product.getId());
        report.setUserId(userId);
        report.setType(dto.getType());
        report.setReason(dto.getReason());
        report.setStatus(ReportStatus.PENDING.getCode());
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);

        log.info("用户 {} 举报商品 {}，类型 {}", userId, product.getId(), ReportType.descOf(dto.getType()));
        return report.getId();
    }

    @Override
    public PageResult<ReportVO> pageMine(Long userId, ReportQuery query) {
        // 强制带上 userId —— 前端传什么都不影响，只能看自己的
        query.setUserId(userId);
        Page<ReportVO> page = query.toPage();
        IPage<ReportVO> result = reportMapper.selectReportPage(page, query);
        result.getRecords().forEach(this::fillText);
        return PageResult.of(result);
    }

    private void fillText(ReportVO vo) {
        vo.setTypeText(ReportType.descOf(vo.getType()));
        vo.setStatusText(ReportStatus.descOf(vo.getStatus()));
    }
}
