package com.digital.platform.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.cache.ProductCache;
import com.digital.platform.common.enums.AuditAction;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.AuditDTO;
import com.digital.platform.dto.ForceOffShelfDTO;
import com.digital.platform.entity.AuditRecord;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.ProductImage;
import com.digital.platform.mapper.AuditRecordMapper;
import com.digital.platform.mapper.ProductImageMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.impl.AdminProductServiceImpl;
import com.digital.platform.vo.AuditRecordVO;
import com.digital.platform.vo.ProductListVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端商品审核的单元测试。
 * <p>
 * 审核是平台唯一的"准入闸门"，这里出错的影响比前台大得多：
 * <ul>
 *   <li>状态前置校验缺失 → 管理员能把已售出的商品改成上架，数据立刻矛盾</li>
 *   <li>通过时不清理驳回原因 → 商品"已上架"却带着上次的驳回理由</li>
 *   <li>强制下架不关订单 → 买家还在等交易，商品却已经被处置</li>
 *   <li>审核不留痕 → 出了纠纷没有任何记录可查</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("管理端商品审核 - 状态守卫、留痕、联动")
class AdminProductServiceImplTest {

    private static final long ADMIN_ID = 1L;
    private static final String ADMIN_NAME = "平台管理员";
    private static final long PRODUCT_ID = 200L;
    private static final long SELLER_ID = 9L;

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageMapper productImageMapper;
    @Mock
    private AuditRecordMapper auditRecordMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OrderService orderService;
    @Mock
    private ProductCache productCache;

    @InjectMocks
    private AdminProductServiceImpl adminProductService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
        TableInfoHelper.initTableInfo(assistant, ProductImage.class);
        TableInfoHelper.initTableInfo(assistant, AuditRecord.class);
    }

    // ============================================================
    //  审核
    // ============================================================

    @Test
    @DisplayName("商品不存在时拒绝审核")
    void audit_throws_when_product_missing() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

        assertBusinessCode(() -> adminProductService.audit(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                auditDTO(AuditAction.PASS, null)), ResultCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("只有待审核的商品能被审核，否则会把已售出的商品改回上架")
    void audit_throws_when_status_is_not_pending() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.SOLD));

        assertBusinessCode(() -> adminProductService.audit(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                auditDTO(AuditAction.PASS, null)), ResultCode.PRODUCT_STATUS_ILLEGAL);

        // 状态不对时不能产生任何副作用
        verify(auditRecordMapper, never()).insert(any());
        verify(productCache, never()).evict(any());
    }

    @Test
    @DisplayName("驳回必须填写原因，否则卖家不知道该改什么")
    void audit_requires_reason_when_rejecting() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.PENDING));

        assertBusinessCode(() -> adminProductService.audit(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                auditDTO(AuditAction.REJECT, "  ")), ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("审核通过：状态置为上架、写审核记录、清缓存、通知卖家")
    void audit_pass_updates_status_and_notifies() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.PENDING));

        adminProductService.audit(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                auditDTO(AuditAction.PASS, null));

        verify(productMapper).update(eq(null), any());
        verify(auditRecordMapper).insert(org.mockito.ArgumentMatchers.argThat(r ->
                AuditAction.PASS.getCode().equals(r.getAction())));
        verify(productCache).evict(PRODUCT_ID);
        verify(notificationService).send(eq(SELLER_ID), any(), contains("通过"), any(), eq(PRODUCT_ID));
    }

    @Test
    @DisplayName("审核驳回：记录和通知里都要带上原因")
    void audit_reject_carries_reason() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.PENDING));

        adminProductService.audit(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                auditDTO(AuditAction.REJECT, "商品描述与实际不符"));

        ArgumentCaptor<AuditRecord> record = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordMapper).insert(record.capture());
        assertThat(record.getValue().getReason()).isEqualTo("商品描述与实际不符");
        assertThat(record.getValue().getAction()).isEqualTo(AuditAction.REJECT.getCode());

        verify(notificationService).send(eq(SELLER_ID), any(), anyString(),
                contains("商品描述与实际不符"), eq(PRODUCT_ID));
    }

    @Test
    @DisplayName("审核记录存的是标题和操作人快照，不是外键（商品改名后记录不变）")
    void audit_record_stores_snapshots() {
        Product p = product(ProductStatus.PENDING);
        p.setTitle("iPhone 13 128G");
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(p);

        adminProductService.audit(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                auditDTO(AuditAction.PASS, null));

        ArgumentCaptor<AuditRecord> record = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordMapper).insert(record.capture());
        assertThat(record.getValue().getProductTitle()).isEqualTo("iPhone 13 128G");
        assertThat(record.getValue().getAdminName()).isEqualTo(ADMIN_NAME);
    }

    // ============================================================
    //  强制下架 / 删除
    // ============================================================

    @Test
    @DisplayName("只有在售商品能强制下架")
    void forceOffShelf_requires_on_sale() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.SOLD));

        assertBusinessCode(() -> adminProductService.forceOffShelf(ADMIN_ID, ADMIN_NAME,
                        PRODUCT_ID, forceOffDTO("违规")), ResultCode.PRODUCT_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("强制下架要同时关掉进行中的订单，否则买家还在等交易")
    void forceOffShelf_closes_active_orders() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE));

        adminProductService.forceOffShelf(ADMIN_ID, ADMIN_NAME, PRODUCT_ID,
                forceOffDTO("商品描述与实际不符"));

        verify(orderService).closeActiveOrdersByProduct(PRODUCT_ID, "商品描述与实际不符");
        verify(productCache).evict(PRODUCT_ID);
        verify(auditRecordMapper).insert(org.mockito.ArgumentMatchers.argThat(r ->
                AuditAction.FORCE_OFF.getCode().equals(r.getAction())));
    }

    @Test
    @DisplayName("删除商品也要关掉进行中的订单")
    void delete_closes_active_orders() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product(ProductStatus.ON_SALE));

        adminProductService.delete(PRODUCT_ID);

        verify(orderService).closeActiveOrdersByProduct(eq(PRODUCT_ID), anyString());
        verify(productMapper).deleteById(PRODUCT_ID);
        verify(productCache).evict(PRODUCT_ID);
    }

    // ============================================================
    //  查询
    // ============================================================

    @Test
    @DisplayName("管理端列表强制看所有状态，不受前端传参影响")
    void page_forces_all_status() {
        ProductQuery query = new ProductQuery();
        query.setOnlyOnSale(true);   // 模拟前端想只看在售
        when(productMapper.selectProductPage(any(), any())).thenReturn(new Page<ProductListVO>());

        adminProductService.page(query);

        assertThat(query.getOnlyOnSale()).isFalse();
    }

    @Test
    @DisplayName("审核记录返回时带出来「通过/驳回/强制下架」的可读文案")
    void auditRecords_maps_action_text() {
        AuditRecord r = new AuditRecord();
        r.setProductId(PRODUCT_ID);
        r.setAction(AuditAction.FORCE_OFF.getCode());
        when(auditRecordMapper.selectList(any())).thenReturn(List.of(r));

        List<AuditRecordVO> list = adminProductService.auditRecords(PRODUCT_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getActionText()).isEqualTo("强制下架");
    }

    // ============================================================
    //  测试数据构造
    // ============================================================

    private Product product(ProductStatus status) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setUserId(SELLER_ID);
        p.setStatus(status.getCode());
        p.setTitle("待审核商品");
        return p;
    }

    private AuditDTO auditDTO(AuditAction action, String reason) {
        AuditDTO dto = new AuditDTO();
        dto.setAction(action.getCode());
        dto.setReason(reason);
        return dto;
    }

    private ForceOffShelfDTO forceOffDTO(String reason) {
        ForceOffShelfDTO dto = new ForceOffShelfDTO();
        dto.setReason(reason);
        return dto;
    }

    private void assertBusinessCode(org.junit.jupiter.api.function.Executable action,
                                    ResultCode expected) {
        assertThatThrownBy(action::execute)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(expected.getCode());
    }
}
