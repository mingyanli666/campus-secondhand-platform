<template>
  <div v-loading="loading" class="app-container detail-page">
    <template v-if="product">
      <el-alert
        v-if="product.status === 4"
        type="error"
        show-icon
        :closable="false"
        class="status-alert"
      >
        <template #title> 审核未通过：{{ product.rejectReason || '未填写原因' }} </template>
        修改商品信息后可以重新提交审核。
      </el-alert>
      <el-alert
        v-else-if="product.status === 0"
        type="warning"
        show-icon
        :closable="false"
        class="status-alert"
        title="该商品正在等待平台审核，审核通过后才会出现在商品列表中"
      />
      <el-alert
        v-else-if="product.status === 2"
        type="info"
        show-icon
        :closable="false"
        class="status-alert"
        :title="product.rejectReason ? '该商品已被平台下架' : '该商品已下架，买家无法看到'"
      >
        <template v-if="product.rejectReason">
          下架原因：{{ product.rejectReason }}。修改后可以重新提交审核。
        </template>
      </el-alert>

      <el-row :gutter="24">
        <!-- 左：图片 -->
        <el-col :xs="24" :md="10">
          <div class="gallery app-card">
            <el-image
              class="main-image"
              :src="activeImage"
              :preview-src-list="product.images"
              fit="contain"
              preview-teleported
            >
              <template #error>
                <div class="image-placeholder">
                  <el-icon :size="40"><Picture /></el-icon>
                  <span>暂无图片</span>
                </div>
              </template>
            </el-image>

            <div v-if="product.images?.length > 1" class="thumbs">
              <div
                v-for="(img, index) in product.images"
                :key="index"
                class="thumb"
                :class="{ active: activeImage === img }"
                @click="activeImage = img"
              >
                <el-image :src="img" fit="cover" />
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右：信息 -->
        <el-col :xs="24" :md="14">
          <div class="info app-card">
            <div class="price-row">
              <span class="price"><small>¥</small>{{ formatPrice(product.price) }}</span>
              <span v-if="showOriginalPrice" class="original-price">
                原价 ¥{{ formatPrice(product.originalPrice) }}
              </span>
              <el-tag v-if="product.conditionLevel" effect="plain">{{
                product.conditionLevel
              }}</el-tag>
              <el-tag v-if="product.warranty" type="success" effect="plain">{{
                product.warranty
              }}</el-tag>
            </div>

            <h1 class="title">{{ product.title }}</h1>

            <div class="stats text-secondary">
              <span
                ><el-icon><View /></el-icon> {{ product.viewCount }} 浏览</span
              >
              <span
                ><el-icon><Star /></el-icon> {{ product.favoriteCount }} 收藏</span
              >
              <span
                ><el-icon><Clock /></el-icon> {{ fromNow(product.createTime) }}</span
              >
            </div>

            <el-descriptions :column="2" border size="small" class="params">
              <el-descriptions-item label="分类">{{
                product.categoryName || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="品牌">{{ product.brand || '-' }}</el-descriptions-item>
              <el-descriptions-item label="型号">{{ product.model || '-' }}</el-descriptions-item>
              <el-descriptions-item label="购入时间">{{
                product.purchaseDate || '-'
              }}</el-descriptions-item>
              <el-descriptions-item v-for="item in specList" :key="item.key" :label="item.key">
                {{ item.value }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="trade-info">
              <div class="trade-item">
                <el-icon><Location /></el-icon>
                <span>交易地点：{{ product.tradeLocation || '面议' }}</span>
              </div>
              <div class="trade-item">
                <el-icon><Phone /></el-icon>
                <span>联系方式：{{ product.contact || '下单后可见' }}</span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="actions">
              <template v-if="product.owner">
                <el-button type="primary" @click="router.push(`/publish/${product.id}`)">
                  编辑商品
                </el-button>
                <el-button v-if="product.status === 1" @click="handleOffShelf"> 下架 </el-button>
                <el-button
                  v-if="product.status === 2 || product.status === 4"
                  type="warning"
                  @click="handleResubmit"
                >
                  重新提交审核
                </el-button>
                <el-button type="danger" plain @click="handleDelete">删除</el-button>
              </template>
              <template v-else>
                <el-button v-if="product.status === 1" type="primary" @click="openOrderDialog">
                  <el-icon><ShoppingCart /></el-icon>
                  立即下单
                </el-button>
                <el-button
                  :type="product.favorite ? 'default' : 'primary'"
                  :plain="product.status === 1"
                  :loading="favoriteLoading"
                  @click="toggleFavorite"
                >
                  <el-icon v-if="product.favorite"><StarFilled /></el-icon>
                  <el-icon v-else><Star /></el-icon>
                  {{ product.favorite ? '已收藏' : '收藏' }}
                </el-button>
                <el-button @click="scrollToComments">
                  <el-icon><ChatDotRound /></el-icon>
                  留言咨询
                </el-button>
                <el-button text type="info" @click="openReportDialog">
                  <el-icon><Warning /></el-icon>
                  举报
                </el-button>
              </template>
            </div>
          </div>

          <!-- 卖家卡片：点击进入卖家主页，看他还有没有别的闲置 -->
          <div class="seller app-card" @click="goSeller">
            <el-avatar :size="48" :src="product.sellerAvatar">
              {{ (product.sellerNickname || '?').charAt(0) }}
            </el-avatar>
            <div class="seller-info">
              <div class="seller-name">
                {{ product.sellerNickname || '未知卖家' }}
                <el-tag size="small" type="success" effect="plain">
                  信誉 {{ product.sellerCreditScore ?? 100 }}
                </el-tag>
              </div>
              <div class="text-secondary seller-campus">
                {{ product.sellerCampus || '未填写所在区域' }}
              </div>
            </div>
            <span class="seller-more">
              查看 TA 的闲置
              <el-icon><ArrowRight /></el-icon>
            </span>
          </div>

          <!-- 描述 -->
          <div class="description app-card">
            <h3>商品描述</h3>
            <p>{{ product.description }}</p>
          </div>
        </el-col>
      </el-row>

      <!-- 留言咨询：独立组件，逻辑不混在详情页里 -->
      <div ref="commentsRef">
        <ProductComments
          :product-id="product.id"
          :is-owner="!!product.owner"
          :commentable="product.status === 1 || product.status === 3"
        />
      </div>
    </template>

    <!-- 下单弹窗 -->
    <el-dialog v-model="orderVisible" title="确认下单" width="480px">
      <div class="order-product">
        <el-image class="order-product-cover" :src="product?.coverImage" fit="cover" />
        <div class="order-product-info">
          <div class="order-product-title">{{ product?.title }}</div>
          <div class="price"><small>¥</small>{{ formatPrice(product?.price) }}</div>
        </div>
      </div>

      <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-position="top">
        <el-form-item label="联系方式" prop="contact">
          <el-input
            v-model.trim="orderForm.contact"
            maxlength="100"
            placeholder="微信 / QQ / 手机号，卖家要用它联系你"
          />
        </el-form-item>

        <el-form-item label="交易地点" prop="tradeLocation">
          <el-input
            v-model.trim="orderForm.tradeLocation"
            maxlength="100"
            :placeholder="product?.tradeLocation || '如：学校东门'"
          />
        </el-form-item>

        <el-form-item label="给卖家留言" prop="remark">
          <el-input
            v-model="orderForm.remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            placeholder="如：我明天下午有空，方便面交吗？"
          />
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="提交后需要卖家确认，确认后才算成交。建议当面验货再付款。"
      />

      <template #footer>
        <el-button @click="orderVisible = false">取消</el-button>
        <el-button type="primary" :loading="ordering" @click="submitOrder"> 提交订单 </el-button>
      </template>
    </el-dialog>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportVisible" title="举报商品" width="480px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="请如实举报。恶意举报会影响你的账号信誉，平台会记录举报行为。"
        class="report-alert"
      />

      <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-position="top">
        <el-form-item label="举报类型" prop="type">
          <el-radio-group v-model="reportForm.type">
            <el-radio v-for="t in REPORT_TYPES" :key="t.value" :value="t.value">
              {{ t.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="举报说明" prop="reason">
          <el-input
            v-model.trim="reportForm.reason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请具体说明问题，如：商品图片是从官网盗用的，实物与描述不符"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" :loading="reporting" @click="submitReport"> 提交举报 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  ChatDotRound,
  ShoppingCart,
  Star,
  StarFilled,
  Warning
} from '@element-plus/icons-vue'
import { deleteProduct, getProduct, offShelfProduct, resubmitProduct } from '@/api/product'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { createOrder } from '@/api/order'
import { createReport } from '@/api/report'
import ProductComments from '@/components/ProductComments.vue'
import { formatPrice, fromNow } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const product = ref(null)
const activeImage = ref('')
const commentsRef = ref()
const favoriteLoading = ref(false)

/**
 * 收藏 / 取消收藏。
 * 只改本地那一份数据、不发第二次请求 —— 因为收藏接口本身就返回了成功，
 * 再拉一次详情是多余的往返。
 */
async function toggleFavorite() {
  if (!userStore.isLogin) {
    ElMessage.warning('登录后才能收藏商品')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  favoriteLoading.value = true
  try {
    if (product.value.favorite) {
      await removeFavorite(product.value.id)
      product.value.favorite = false
      product.value.favoriteCount = Math.max(0, (product.value.favoriteCount || 1) - 1)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(product.value.id)
      product.value.favorite = true
      product.value.favoriteCount = (product.value.favoriteCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } finally {
    favoriteLoading.value = false
  }
}

function scrollToComments() {
  commentsRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** 进入卖家主页，看他还有没有别的闲置 */
function goSeller() {
  const sellerId = product.value?.sellerId
  if (sellerId) {
    router.push(`/seller/${sellerId}`)
  }
}

/* ---------------- 下单 ---------------- */
const orderVisible = ref(false)
const ordering = ref(false)
const orderFormRef = ref()

const orderForm = reactive({
  contact: '',
  tradeLocation: '',
  remark: ''
})

const orderRules = {
  contact: [
    { required: true, message: '请填写联系方式，否则卖家联系不到你', trigger: 'blur' },
    { max: 100, message: '联系方式不能超过 100 个字符', trigger: 'blur' }
  ],
  tradeLocation: [{ max: 100, message: '交易地点不能超过 100 个字符', trigger: 'blur' }],
  remark: [{ max: 255, message: '留言不能超过 255 个字符', trigger: 'blur' }]
}

function openOrderDialog() {
  if (!userStore.isLogin) {
    ElMessage.warning('登录后才能下单')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  // 用资料里的手机号预填，省得每次手打
  orderForm.contact = userStore.userInfo?.phone || ''
  orderForm.tradeLocation = product.value?.tradeLocation || ''
  orderForm.remark = ''
  orderVisible.value = true
}

async function submitOrder() {
  const valid = await orderFormRef.value.validate().catch(() => false)
  if (!valid) return

  ordering.value = true
  try {
    await createOrder({
      productId: product.value.id,
      contact: orderForm.contact,
      tradeLocation: orderForm.tradeLocation,
      remark: orderForm.remark
    })
    orderVisible.value = false
    ElMessage.success('下单成功，等待卖家确认')

    // 弹窗提示去订单页，而不是强行跳转 —— 用户可能还想再看看这个商品
    try {
      await ElMessageBox.confirm('订单已提交，要去「我的订单」看看吗？', '下单成功', {
        confirmButtonText: '去看看',
        cancelButtonText: '继续浏览',
        type: 'success'
      })
      router.push('/user/orders')
    } catch {
      // 用户选择继续浏览，什么都不做
    }
  } finally {
    ordering.value = false
  }
}

/* ---------------- 举报 ---------------- */
const REPORT_TYPES = [
  { value: 1, label: '虚假信息' },
  { value: 2, label: '违禁物品' },
  { value: 3, label: '价格欺诈' },
  { value: 4, label: '其他' }
]

const reportVisible = ref(false)
const reporting = ref(false)
const reportFormRef = ref()

const reportForm = reactive({
  type: 1,
  reason: ''
})

const reportRules = {
  type: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
  reason: [
    { required: true, message: '请说明举报理由', trigger: 'blur' },
    { min: 5, max: 255, message: '举报理由为 5-255 个字符', trigger: 'blur' }
  ]
}

function openReportDialog() {
  if (!userStore.isLogin) {
    ElMessage.warning('登录后才能举报')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  reportForm.type = 1
  reportForm.reason = ''
  reportVisible.value = true
}

async function submitReport() {
  const valid = await reportFormRef.value.validate().catch(() => false)
  if (!valid) return

  reporting.value = true
  try {
    await createReport({
      productId: product.value.id,
      type: reportForm.type,
      reason: reportForm.reason
    })
    reportVisible.value = false
    ElMessage.success('举报已提交，平台会尽快核实')
  } catch {
    // 错误提示（如"你已经举报过该商品"）由请求拦截器统一弹出，弹窗保持打开
  } finally {
    reporting.value = false
  }
}

/** 原价高于售价才展示，避免出现"原价 100 现价 200" */
const showOriginalPrice = computed(() => {
  if (!product.value?.originalPrice) return false
  return Number(product.value.originalPrice) > Number(product.value.price)
})

/** 参数配置存的是 JSON 文本，这里解析成可遍历的键值对 */
const specList = computed(() => {
  if (!product.value?.spec) return []
  try {
    return Object.entries(JSON.parse(product.value.spec)).map(([key, value]) => ({ key, value }))
  } catch {
    return []
  }
})

async function loadDetail() {
  loading.value = true
  try {
    const data = await getProduct(route.params.id)
    product.value = data
    activeImage.value = data.images?.[0] || data.coverImage || ''
  } catch {
    // 商品不存在或不可见时，退回到列表页，避免用户卡在空白页
    router.replace('/product/list')
  } finally {
    loading.value = false
  }
}

async function handleOffShelf() {
  try {
    await ElMessageBox.confirm('下架后买家将无法看到该商品，确定下架吗？', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await offShelfProduct(product.value.id)
  ElMessage.success('已下架')
  loadDetail()
}

async function handleResubmit() {
  await resubmitProduct(product.value.id)
  ElMessage.success('已提交审核，请等待平台处理')
  loadDetail()
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定删除吗？', '警告', {
      type: 'warning',
      confirmButtonText: '确定删除'
    })
  } catch {
    return
  }
  await deleteProduct(product.value.id)
  ElMessage.success('已删除')
  router.push('/user/products')
}

// 从编辑页返回后重新拉取，保证看到的是最新数据
watch(() => route.params.id, loadDetail)

onMounted(loadDetail)
</script>

<style scoped>
.detail-page {
  padding: 20px 16px 48px;
}

.status-alert {
  margin-bottom: 16px;
}

.gallery {
  padding: 16px;
}

.main-image {
  width: 100%;
  height: 380px;
  background: #fafafa;
  border-radius: 6px;
}

.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 380px;
  color: var(--app-text-secondary);
}

.thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  overflow-x: auto;
}

.thumb {
  flex: none;
  width: 64px;
  height: 64px;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.thumb.active {
  border-color: var(--el-color-primary);
}

.thumb :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.info {
  padding: 20px 24px 24px;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.price {
  font-size: 30px;
  color: #ef4444;
  font-weight: 700;
}

.price small {
  font-size: 16px;
}

.original-price {
  font-size: 13px;
  color: var(--app-text-secondary);
  text-decoration: line-through;
}

.title {
  margin: 0 0 12px;
  font-size: 20px;
  line-height: 1.45;
}

.stats {
  display: flex;
  gap: 18px;
  font-size: 13px;
  margin-bottom: 16px;
}

.stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.params {
  margin-bottom: 16px;
}

.trade-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  margin-bottom: 18px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.trade-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-text-regular);
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.seller {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.seller:hover {
  box-shadow: var(--app-shadow-hover);
}

.seller:hover .seller-more {
  color: var(--el-color-primary);
}

.seller-info {
  flex: 1;
  min-width: 0;
}

.seller-more {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--app-text-secondary);
  transition: color 0.2s;
}

.seller-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 4px;
}

.seller-campus {
  font-size: 12px;
}

.description {
  margin-top: 16px;
  padding: 18px 20px 22px;
}

.description h3 {
  margin: 0 0 12px;
  font-size: 15px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
}

.description p {
  margin: 0;
  line-height: 1.9;
  color: var(--app-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- 下单弹窗 ---------- */
.order-product {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 18px;
  background: #f8fafc;
  border-radius: 6px;
}

.order-product-cover {
  flex: none;
  width: 64px;
  height: 64px;
  border-radius: 6px;
  background: #e5e7eb;
}

.order-product-info {
  min-width: 0;
}

.order-product-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.order-product-info .price {
  font-size: 18px;
}

.order-product-info .price small {
  font-size: 12px;
}

.report-alert {
  margin-bottom: 16px;
}
</style>
