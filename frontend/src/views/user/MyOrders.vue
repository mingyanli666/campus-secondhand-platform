<template>
  <div class="app-container my-orders">
    <div class="page-head">
      <h2>我的订单</h2>
    </div>

    <!-- 视角切换：买到的 / 卖出的 -->
    <el-tabs v-model="role" class="role-tabs" @tab-change="handleRoleChange">
      <el-tab-pane name="BUY">
        <template #label>
          <span>我买到的</span>
          <el-badge v-if="buyCount" :value="buyCount" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane name="SELL">
        <template #label>
          <span>我卖出的</span>
          <el-badge v-if="sellCount" :value="sellCount" class="tab-badge" />
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 状态筛选 -->
    <div class="status-filter">
      <span
        v-for="s in STATUS_FILTERS"
        :key="s.label"
        class="filter-item"
        :class="{ active: query.status === s.value }"
        @click="selectStatus(s.value)"
      >
        {{ s.label }}
      </span>
    </div>

    <!-- 订单列表 -->
    <div v-loading="loading" class="list">
      <div v-for="order in list" :key="order.id" class="order-card app-card">
        <!-- 头部：订单号 + 状态 -->
        <div class="order-head">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="text-secondary">{{ formatTime(order.createTime, true) }}</span>
          <el-tag :type="statusOf(order.status).type" size="small" effect="light" class="head-status">
            {{ order.statusText || statusOf(order.status).text }}
          </el-tag>
        </div>

        <!-- 主体：商品快照 + 交易信息 -->
        <div class="order-body">
          <el-image
            class="order-cover"
            :src="order.productCover"
            fit="cover"
            @click="goProduct(order.productId)"
          >
            <template #error>
              <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>

          <div class="order-info">
            <div class="order-title" @click="goProduct(order.productId)">
              {{ order.productTitle }}
            </div>

            <div class="order-price">
              <span class="price"><small>¥</small>{{ formatPrice(order.price) }}</span>
            </div>

            <div class="order-meta text-secondary">
              <span>
                {{ order.myRole === 'BUYER' ? '卖家' : '买家' }}：
                {{ order.myRole === 'BUYER' ? order.sellerNickname : order.buyerNickname }}
              </span>
              <span v-if="order.tradeLocation">· 交易地点：{{ order.tradeLocation }}</span>
            </div>

            <div v-if="order.remark" class="order-remark text-secondary">
              买家留言：{{ order.remark }}
            </div>

            <el-alert
              v-if="order.cancelReason && (order.status === 3 || order.status === 4)"
              :type="order.status === 3 ? 'info' : 'warning'"
              :closable="false"
              class="cancel-alert"
            >
              {{ order.status === 3 ? '取消原因' : '关闭原因' }}：{{ order.cancelReason }}
            </el-alert>

            <div v-if="order.finishTime" class="text-secondary finish-time">
              成交时间：{{ formatTime(order.finishTime, true) }}
            </div>
          </div>
        </div>

        <!-- 操作区：按钮完全由后端返回的 actions 决定 -->
        <div class="order-actions">
          <el-button size="small" @click="goProduct(order.productId)">查看商品</el-button>
          <el-button v-if="order.actions?.canComment" size="small" @click="goComment(order)">
            联系对方
          </el-button>

          <el-button
            v-if="order.actions?.canConfirm"
            size="small"
            type="primary"
            :loading="actingId === order.id"
            @click="handleConfirm(order)"
          >
            确认接单
          </el-button>

          <el-button
            v-if="order.actions?.canFinish"
            size="small"
            type="success"
            :loading="actingId === order.id"
            @click="handleFinish(order)"
          >
            确认完成
          </el-button>

          <el-button
            v-if="order.actions?.canCancel"
            size="small"
            type="danger"
            plain
            :loading="actingId === order.id"
            @click="handleCancel(order)"
          >
            取消订单
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !list.length" :description="emptyText">
      <el-button type="primary" @click="router.push('/product/list')">去逛逛</el-button>
    </el-empty>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="loadList"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmOrder, finishOrder, pageOrders } from '@/api/order'
import { formatPrice, formatTime, orderStatus } from '@/utils/format'

const STATUS_FILTERS = [
  { label: '全部', value: null },
  { label: '待确认', value: 0 },
  { label: '已确认', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 },
  { label: '已关闭', value: 4 }
]

const router = useRouter()

const loading = ref(false)
const actingId = ref(null)
const list = ref([])
const total = ref(0)
const role = ref('BUY')
const buyCount = ref(0)
const sellCount = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  role: 'BUY',
  status: null
})

const statusOf = orderStatus

const emptyText = computed(() =>
  role.value === 'BUY' ? '还没有买过东西' : '还没有卖出过东西'
)

async function loadList() {
  loading.value = true
  try {
    const data = await pageOrders(query)
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

/** 两个视角的待处理数量，显示在 Tab 上提示"有事要办" */
async function loadCounts() {
  const [buy, sell] = await Promise.all([
    // 买家关心"已确认"（该去交易了），卖家关心"待确认"（该处理了）
    pageOrders({ current: 1, size: 1, role: 'BUY', status: 1 }),
    pageOrders({ current: 1, size: 1, role: 'SELL', status: 0 })
  ])
  buyCount.value = Number(buy.total)
  sellCount.value = Number(sell.total)
}

function handleRoleChange() {
  query.role = role.value
  query.status = null
  query.current = 1
  loadList()
}

function selectStatus(value) {
  query.status = value
  query.current = 1
  loadList()
}

function handleSizeChange() {
  query.current = 1
  loadList()
}

function goProduct(productId) {
  if (productId) {
    router.push(`/product/${productId}`)
  }
}

function goComment(order) {
  // 留言是挂在商品下的，跳到商品详情页的留言区
  router.push(`/product/${order.productId}`)
}

async function handleConfirm(order) {
  try {
    await ElMessageBox.confirm(
      `确认接下「${order.productTitle}」这笔订单吗？确认后请尽快与买家联系完成交易。`,
      '确认接单',
      { type: 'warning' }
    )
  } catch {
    return
  }

  actingId.value = order.id
  try {
    await confirmOrder(order.id)
    ElMessage.success('已确认订单')
    refresh()
  } finally {
    actingId.value = null
  }
}

async function handleFinish(order) {
  try {
    await ElMessageBox.confirm(
      `确认「${order.productTitle}」这笔交易已经完成吗？确认后商品会标记为已售出，且无法撤销。`,
      '确认完成',
      { type: 'warning', confirmButtonText: '确认完成' }
    )
  } catch {
    return
  }

  actingId.value = order.id
  try {
    await finishOrder(order.id)
    ElMessage.success('交易已完成')
    refresh()
  } finally {
    actingId.value = null
  }
}

async function handleCancel(order) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      '取消原因会通知给对方，建议写清楚，避免误会',
      '取消订单',
      {
        inputPlaceholder: '如：已经买到别的了 / 联系不上对方',
        inputValidator: () => true,
        confirmButtonText: '确定取消',
        cancelButtonText: '再想想'
      }
    )
    reason = value || ''
  } catch {
    return
  }

  actingId.value = order.id
  try {
    await cancelOrder(order.id, { reason })
    ElMessage.success('订单已取消')
    refresh()
  } finally {
    actingId.value = null
  }
}

function refresh() {
  loadList()
  loadCounts()
}

onMounted(() => {
  loadList()
  loadCounts()
})
</script>

<style scoped>
.my-orders {
  padding: 24px 16px 48px;
}

.page-head h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.tab-badge {
  margin-left: 6px;
}

.status-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 4px 0 16px;
}

.filter-item {
  padding: 5px 14px;
  border-radius: 14px;
  font-size: 13px;
  color: var(--app-text-regular);
  cursor: pointer;
  transition: all 0.15s;
}

.filter-item:hover {
  color: var(--el-color-primary);
}

.filter-item.active {
  color: #fff;
  background: var(--el-color-primary);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.order-card {
  padding: 0 20px 16px;
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: var(--app-shadow-hover);
}

.order-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--app-border);
}

.order-no {
  font-family: 'Consolas', 'Monaco', monospace;
  color: var(--app-text-regular);
}

.head-status {
  margin-left: auto;
}

.order-body {
  display: flex;
  gap: 16px;
  padding: 16px 0;
}

.order-cover {
  flex: none;
  width: 110px;
  height: 110px;
  border-radius: 6px;
  background: #f3f4f6;
  cursor: pointer;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--app-text-secondary);
}

.order-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-title {
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.order-title:hover {
  color: var(--el-color-primary);
}

.order-price .price {
  font-size: 20px;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
}

.order-remark,
.finish-time {
  font-size: 12px;
}

.cancel-alert {
  padding: 4px 10px;
}

.cancel-alert :deep(.el-alert__content) {
  font-size: 12px;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px dashed var(--app-border);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
