<template>
  <div class="app-container my-products">
    <div class="page-head">
      <h2>我的发布</h2>
      <el-button type="primary" :icon="Plus" @click="router.push('/publish')">
        发布新商品
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane name="0">
        <template #label>
          待审核
          <el-badge v-if="pendingCount" :value="pendingCount" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="在售" name="1" />
      <el-tab-pane label="已下架" name="2" />
      <el-tab-pane label="已售出" name="3" />
      <el-tab-pane label="审核驳回" name="4" />
    </el-tabs>

    <div v-loading="loading" class="list">
      <div v-for="item in list" :key="item.id" class="item app-card">
        <div class="item-main">
          <el-image
            class="item-cover"
            :src="item.coverImage"
            fit="cover"
            @click="goDetail(item.id)"
          >
            <template #error>
              <div class="cover-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>

          <div class="item-info">
            <div class="item-title" @click="goDetail(item.id)">{{ item.title }}</div>
            <div class="item-meta text-secondary">
              <span>{{ item.categoryName || '未分类' }}</span>
              <span v-if="item.conditionLevel">· {{ item.conditionLevel }}</span>
              <span>· {{ fromNow(item.createTime) }}</span>
              <span>· {{ item.viewCount }} 浏览 / {{ item.favoriteCount }} 收藏</span>
            </div>
            <el-alert
              v-if="item.rejectReason && (item.status === 4 || item.status === 2)"
              :type="item.status === 4 ? 'error' : 'warning'"
              :closable="false"
              class="reject-alert"
            >
              {{ item.status === 4 ? '驳回原因' : '下架原因' }}：{{ item.rejectReason }}
            </el-alert>
            <div class="item-price">
              <span class="price"><small>¥</small>{{ formatPrice(item.price) }}</span>
              <el-tag :type="statusOf(item.status).type" size="small" effect="light">
                {{ statusOf(item.status).text }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="item-actions">
          <el-button size="small" @click="goDetail(item.id)">查看</el-button>
          <el-button
            v-if="item.status !== 3"
            size="small"
            type="primary"
            plain
            @click="router.push(`/publish/${item.id}`)"
          >
            编辑
          </el-button>
          <el-button v-if="item.status === 1" size="small" @click="handleOffShelf(item)">
            下架
          </el-button>
          <el-button
            v-if="item.status === 2 || item.status === 4"
            size="small"
            type="warning"
            plain
            @click="handleResubmit(item)"
          >
            重新提交
          </el-button>
          <el-button
            v-if="item.status !== 3"
            size="small"
            type="danger"
            plain
            @click="handleDelete(item)"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !list.length" description="这里还没有商品">
      <el-button type="primary" @click="router.push('/publish')">去发布</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deleteProduct, myProducts, offShelfProduct, resubmitProduct } from '@/api/product'
import { formatPrice, fromNow, productStatus } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const activeTab = ref('all')
const pendingCount = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  status: null
})

const statusOf = productStatus

async function loadList() {
  loading.value = true
  try {
    const data = await myProducts(query, 'my:products')
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function loadPendingCount() {
  const data = await myProducts({ current: 1, size: 1, status: 0 })
  pendingCount.value = Number(data.total)
}

function handleTabChange() {
  query.status = activeTab.value === 'all' ? null : Number(activeTab.value)
  query.current = 1
  loadList()
}

function handleSizeChange() {
  query.current = 1
  loadList()
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

async function handleOffShelf(item) {
  await offShelfProduct(item.id)
  ElMessage.success('已下架')
  refresh()
}

async function handleResubmit(item) {
  await resubmitProduct(item.id)
  ElMessage.success('已提交审核')
  refresh()
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title}」吗？删除后无法恢复。`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除'
    })
  } catch {
    return
  }
  await deleteProduct(item.id)
  ElMessage.success('已删除')
  refresh()
}

function refresh() {
  loadList()
  loadPendingCount()
}

onMounted(() => {
  loadList()
  loadPendingCount()
})
</script>

<style scoped>
.my-products {
  padding: 24px 16px 48px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
}

.tab-badge {
  margin-left: 4px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
  margin-top: 8px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  transition: box-shadow 0.2s;
}

.item:hover {
  box-shadow: var(--app-shadow-hover);
}

.item-main {
  display: flex;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.item-cover {
  flex: none;
  width: 96px;
  height: 96px;
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

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-title {
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.item-title:hover {
  color: var(--el-color-primary);
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 12px;
}

.reject-alert {
  padding: 4px 10px;
}

.reject-alert :deep(.el-alert__content) {
  font-size: 12px;
}

.item-price {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-actions {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
}

.item-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
