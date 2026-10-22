package com.digital.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.cache.ProductCache;
import com.digital.platform.common.enums.AuditAction;
import com.digital.platform.common.enums.NotificationType;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.AuditDTO;
import com.digital.platform.dto.ForceOffShelfDTO;
import com.digital.platform.entity.AuditRecord;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.ProductImage;
import com.digital.platform.mapper.AuditRecordMapper;
import com.digital.platform.mapper.ProductImageMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.AdminProductService;
import com.digital.platform.service.NotificationService;
import com.digital.platform.service.OrderService;
import com.digital.platform.vo.AuditRecordVO;
import com.digital.platform.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final NotificationService notificationService;
    private final OrderService orderService;
    private final ProductCache productCache;

    @Override
    public PageResult<ProductListVO> page(ProductQuery query) {
        if (StrUtil.isNotBlank(query.getKeyword())) {
            query.setKeyword(query.getKeyword().trim());
        }
        // 管理后台要看所有状态的商品，所以这里不设 onlyOnSale
        query.setOnlyOnSale(false);
        Page<ProductListVO> page = query.toPage();
        IPage<ProductListVO> result = productMapper.selectProductPage(page, query);
        return PageResult.of(result);
    }

    // ============================================================
    //  审核：一次事务里做三件事
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long adminId, String adminName, Long productId, AuditDTO dto) {
        Product product = requireProduct(productId);

        // 前置状态校验：只有"待审核"的商品才能被审核。
        // 没有这道校验，管理员就能把已售出的商品改成上架状态，数据立刻矛盾。
        if (!ProductStatus.PENDING.getCode().equals(product.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL,
                    "该商品当前状态为「" + ProductStatus.descOf(product.getStatus()) + "」，无法审核");
        }

        boolean pass = AuditAction.PASS.getCode().equals(dto.getAction());

        // 条件校验：驳回必须说明原因，否则卖家不知道该改什么
        if (!pass && StrUtil.isBlank(dto.getReason())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回时必须填写原因，方便卖家修改");
        }

        LocalDateTime now = LocalDateTime.now();

        // ---------- 第 1 件事：更新商品状态 ----------
        // 这里必须用 LambdaUpdateWrapper 而不是 updateById。
        // 因为 updateById 会忽略 null 字段，导致"审核通过"时
        // 旧的 rejectReason 清不掉（会出现"已上架"却带着上次驳回原因的脏数据）。
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getStatus, pass ? ProductStatus.ON_SALE.getCode() : ProductStatus.REJECTED.getCode())
                .set(Product::getRejectReason, pass ? null : dto.getReason())
                .set(pass, Product::getPublishTime, now)
                .set(Product::getAuditTime, now)
                .set(Product::getAuditorId, adminId)
                .set(Product::getUpdateTime, now)
                .eq(Product::getId, productId));

        // ---------- 第 2 件事：写审核记录（可追溯） ----------
        saveRecord(product, adminId, adminName,
                pass ? AuditAction.PASS : AuditAction.REJECT,
                pass ? null : dto.getReason());

        // 审核改变了商品状态和驳回原因，详情缓存必须作废
        productCache.evict(productId);

        // ---------- 第 3 件事：通知卖家 ----------
        if (pass) {
            notificationService.send(product.getUserId(), NotificationType.AUDIT,
                    "商品审核通过",
                    "你发布的「" + product.getTitle() + "」已通过审核，其他用户现在可以看到它了。",
                    productId);
        } else {
            notificationService.send(product.getUserId(), NotificationType.AUDIT,
                    "商品审核未通过",
                    "你发布的「" + product.getTitle() + "」未通过审核。原因：" + dto.getReason()
                            + "。修改后可以重新提交审核。",
                    productId);
        }

        log.info("管理员 {} {} 商品 {}", adminName, pass ? "通过" : "驳回", productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceOffShelf(Long adminId, String adminName, Long productId, ForceOffShelfDTO dto) {
        Product product = requireProduct(productId);

        if (!ProductStatus.ON_SALE.getCode().equals(product.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "只有在售商品才能强制下架");
        }

        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getStatus, ProductStatus.OFF_SHELF.getCode())
                // 复用 rejectReason 字段承载"平台的处置原因"，见下方说明
                .set(Product::getRejectReason, dto.getReason())
                .set(Product::getAuditTime, LocalDateTime.now())
                .set(Product::getAuditorId, adminId)
                .set(Product::getUpdateTime, LocalDateTime.now())
                .eq(Product::getId, productId));

        saveRecord(product, adminId, adminName, AuditAction.FORCE_OFF, dto.getReason());

        productCache.evict(productId);

        // 商品被下架了，它上面没走完的订单不能再挂着 ——
        // 否则买家还在等交易，卖家却已经被平台下架了商品。
        // 这里只传管理员填的原因，"商品已被平台下架"这句话由通知模板统一组织，
        // 否则拼出来会变成"原因：商品已被平台下架：xxx"这种重复的前缀。
        orderService.closeActiveOrdersByProduct(productId, dto.getReason());

        notificationService.send(product.getUserId(), NotificationType.SYSTEM,
                "商品已被下架",
                "你发布的「" + product.getTitle() + "」因违反平台规范已被下架。原因：" + dto.getReason(),
                productId);

        log.info("管理员 {} 强制下架商品 {}", adminName, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long productId) {
        requireProduct(productId);
        // 商品没了，进行中的订单也要一起关掉
        orderService.closeActiveOrdersByProduct(productId, "商品已被平台删除");
        // 商品逻辑删除（保留可追溯性），图片记录物理删除
        productMapper.deleteById(productId);
        productImageMapper.delete(
                Wrappers.<ProductImage>lambdaQuery().eq(ProductImage::getProductId, productId));
        productCache.evict(productId);
    }

    @Override
    public List<AuditRecordVO> auditRecords(Long productId) {
        List<AuditRecord> records = auditRecordMapper.selectList(
                Wrappers.<AuditRecord>lambdaQuery()
                        .eq(AuditRecord::getProductId, productId)
                        .orderByDesc(AuditRecord::getCreateTime));

        return records.stream().map(r -> {
            AuditRecordVO vo = new AuditRecordVO();
            vo.setId(r.getId());
            vo.setProductId(r.getProductId());
            vo.setProductTitle(r.getProductTitle());
            vo.setAdminName(r.getAdminName());
            vo.setAction(r.getAction());
            vo.setActionText(AuditAction.descOf(r.getAction()));
            vo.setReason(r.getReason());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).toList();
    }

    // ============================================================
    //  私有辅助
    // ============================================================

    private Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    /**
     * 写一条审核记录。
     * <p>
     * 注意存的是 productTitle / adminName 的**快照**而不是外键关联：
     * 商品以后改了标题、管理员改了昵称，历史记录仍应显示"当时"的样子，
     * 否则追溯就失真了。这是审计类表设计的通用做法。
     */
    private void saveRecord(Product product, Long adminId, String adminName,
                            AuditAction action, String reason) {
        AuditRecord record = new AuditRecord();
        record.setProductId(product.getId());
        record.setProductTitle(product.getTitle());
        record.setAdminId(adminId);
        record.setAdminName(adminName);
        record.setAction(action.getCode());
        record.setReason(StrUtil.blankToDefault(reason, null));
        record.setCreateTime(LocalDateTime.now());
        auditRecordMapper.insert(record);
    }
}
