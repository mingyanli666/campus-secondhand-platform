package com.digital.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.enums.NotificationType;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.enums.ReportStatus;
import com.digital.platform.common.enums.ReportType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.ReportQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.ForceOffShelfDTO;
import com.digital.platform.dto.ReportHandleDTO;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.Report;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.ReportMapper;
import com.digital.platform.service.AdminProductService;
import com.digital.platform.service.AdminReportService;
import com.digital.platform.service.NotificationService;
import com.digital.platform.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportMapper reportMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;
    private final AdminProductService adminProductService;

    @Override
    public PageResult<ReportVO> page(ReportQuery query) {
        // 管理后台不做 userId 限定，这里显式清空，
        // 防止有人从前端塞一个 userId 把列表筛成别人的
        query.setUserId(null);
        Page<ReportVO> page = query.toPage();
        IPage<ReportVO> result = reportMapper.selectReportPage(page, query);
        result.getRecords().forEach(this::fillText);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long adminId, String adminName, Long reportId, ReportHandleDTO dto) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.REPORT_NOT_FOUND);
        }
        // 已处理/已驳回的举报不能再处理一次，否则会重复处置商品、重复通知举报人
        if (!ReportStatus.PENDING.getCode().equals(report.getStatus())) {
            throw new BusinessException(ResultCode.REPORT_ALREADY_HANDLED,
                    "该举报当前状态为「" + ReportStatus.descOf(report.getStatus()) + "」，无法重复处理");
        }

        boolean valid = ReportStatus.HANDLED.getCode().equals(dto.getStatus());
        boolean needOffShelf = valid && Boolean.TRUE.equals(dto.getOffShelf());
        // 记录"这次是否真的下架了商品"，后面通知里要如实描述 ——
        // 商品已经售出/下架时我们会跳过下架动作，这时候不能跟举报人说"已下架"
        boolean offShelfDone = false;

        // 1. 写处理结果
        reportMapper.update(null, Wrappers.<Report>lambdaUpdate()
                .set(Report::getStatus, dto.getStatus())
                .set(Report::getHandleResult, dto.getHandleResult())
                .set(Report::getHandlerId, adminId)
                .set(Report::getHandleTime, LocalDateTime.now())
                .eq(Report::getId, reportId));

        // 2. 举报成立且选了"同时下架" → 直接复用商品管理的处置能力。
        //    forceOffShelf 内部会：下架商品、写审核留痕、关闭进行中的订单、
        //    通知卖家。这一整套能力早就写好了，这里不该重新实现一遍。
        if (needOffShelf) {
            Product product = productMapper.selectById(report.getProductId());
            // 商品可能已经被下架、售出或删除了。
            // 这种情况下"让商品不可见"这个目标**已经达成**，不需要再下架一次 ——
            // 如果硬调 forceOffShelf，它会抛"只有在售商品才能强制下架"，
            // 导致整笔举报处理失败，管理员连处理结论都记不下来。
            if (product != null && ProductStatus.ON_SALE.getCode().equals(product.getStatus())) {
                ForceOffShelfDTO offShelf = new ForceOffShelfDTO();
                // 前缀用"举报处理："而不是"经举报核实："—— 因为管理员的结论里
                // 往往已经写了"经核实…"，两个前缀叠在一起会变成
                // "经举报核实：经核实商品图片系盗用…"，读起来别扭
                offShelf.setReason("举报处理：" + dto.getHandleResult());
                adminProductService.forceOffShelf(adminId, adminName, report.getProductId(), offShelf);
                offShelfDone = true;
            }
        }

        // 3. 通知举报人 —— 无论成立与否都要给反馈，
        //    否则用户举报完就石沉大海，下次就懒得举报了
        Product product = productMapper.selectById(report.getProductId());
        String productName = product == null ? "被举报商品" : "「" + product.getTitle() + "」";

        String title = valid ? "你的举报已处理" : "你的举报未通过";
        String content = valid
                ? "你举报的" + productName + "经核实存在违规"
                  + (offShelfDone ? "，商品已被下架。" : "。")
                  + "处理结论：" + dto.getHandleResult()
                : "你举报的" + productName + "经核实未发现违规。"
                  + "处理结论：" + dto.getHandleResult();

        notificationService.send(report.getUserId(), NotificationType.SYSTEM,
                title, StrUtil.maxLength(content, 400), report.getProductId());

        log.info("管理员 {} 处理举报 {}，结果：{}", adminName, reportId, valid ? "成立" : "不成立");
    }

    private void fillText(ReportVO vo) {
        vo.setTypeText(ReportType.descOf(vo.getType()));
        vo.setStatusText(ReportStatus.descOf(vo.getStatus()));
    }
}
