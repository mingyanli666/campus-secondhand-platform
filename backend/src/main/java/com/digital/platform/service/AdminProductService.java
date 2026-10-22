package com.digital.platform.service;

import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.dto.AuditDTO;
import com.digital.platform.dto.ForceOffShelfDTO;
import com.digital.platform.vo.AuditRecordVO;
import com.digital.platform.vo.ProductListVO;

import java.util.List;

/**
 * 管理后台 - 商品管理服务。
 * <p>
 * 和面向普通用户的 ProductService 分开，因为两者的关注点完全不同：
 * 前者管"我的商品"，后者管"全平台的商品"。混在一起会导致
 * 越权风险（一个卖家用普通接口调到了管理员的能力）。
 */
public interface AdminProductService {

    /** 全平台商品列表，可按状态筛选 */
    PageResult<ProductListVO> page(ProductQuery query);

    /** 审核商品（通过 / 驳回） */
    void audit(Long adminId, String adminName, Long productId, AuditDTO dto);

    /** 强制下架（违规处理） */
    void forceOffShelf(Long adminId, String adminName, Long productId, ForceOffShelfDTO dto);

    /** 删除商品（逻辑删除） */
    void delete(Long productId);

    /** 查询某个商品的审核历史，用于追溯 */
    List<AuditRecordVO> auditRecords(Long productId);
}
