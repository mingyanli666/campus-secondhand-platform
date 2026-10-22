<template>
  <div class="app-container my-reports">
    <div class="page-head">
      <h2>我的举报</h2>
      <span class="text-secondary">平台会尽快核实，处理结果会通过站内通知告知你</span>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="item in list" :key="item.id" class="report-card app-card">
        <div class="report-head">
          <el-tag size="small" :type="typeTag(item.type)" effect="plain">
            {{ item.typeText }}
          </el-tag>
          <span class="text-secondary">{{ formatTime(item.createTime, true) }}</span>
          <el-tag size="small" :type="statusTag(item.status)" effect="light" class="head-status">
            {{ item.statusText }}
          </el-tag>
        </div>

        <div class="report-body">
          <div class="product-line" @click="goProduct(item.productId)">
            <el-image class="product-cover" :src="item.productCover" fit="cover">
              <template #error>
                <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <span class="product-title">{{ item.productTitle || '（商品已删除）' }}</span>
          </div>

          <div class="reason">
            <span class="label">举报说明：</span>{{ item.reason }}
          </div>

          <div v-if="item.handleResult" class="result">
            <span class="label">处理结论：</span>{{ item.handleResult }}
          </div>

          <div v-if="item.handleTime" class="handled text-secondary">
            {{ item.handlerName || '平台' }} 于 {{ formatTime(item.handleTime, true) }} 处理
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !list.length" description="还没有提交过举报">
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageMyReports } from '@/api/report'
import { formatTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 10
})

function typeTag(type) {
  return { 1: 'warning', 2: 'danger', 3: 'primary', 4: 'info' }[type] || 'info'
}

function statusTag(status) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[status] || 'info'
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageMyReports(query)
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
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

onMounted(loadList)
</script>

<style scoped>
.my-reports {
  padding: 24px 16px 48px;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
}

.page-head span {
  font-size: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.report-card {
  padding: 14px 20px 16px;
}

.report-head {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--app-border);
}

.head-status {
  margin-left: auto;
}

.report-body {
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-line {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.product-cover {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #f3f4f6;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--app-text-secondary);
}

.product-title {
  font-size: 14px;
  font-weight: 500;
}

.product-line:hover .product-title {
  color: var(--el-color-primary);
}

.reason,
.result {
  font-size: 13px;
  line-height: 1.7;
  color: var(--app-text-regular);
}

.result {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
}

.label {
  color: var(--app-text-secondary);
}

.handled {
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
