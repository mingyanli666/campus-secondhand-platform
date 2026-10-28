<template>
  <div v-loading="loading" class="dashboard">
    <div class="stat-grid">
      <div
        v-for="card in cards"
        :key="card.label"
        class="stat-card app-card"
        :class="{ clickable: card.path }"
        @click="card.path && router.push(card.path)"
      >
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <el-card shadow="never" class="tips">
      <template #header>平台概览</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="注册用户">{{ stats.userCount || 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="禁用账号"
          >{{ stats.disabledUserCount || 0 }} 个</el-descriptions-item
        >
        <el-descriptions-item label="商品总数"
          >{{ stats.productCount || 0 }} 件</el-descriptions-item
        >
        <el-descriptions-item label="累计浏览量"
          >{{ stats.totalViewCount || 0 }} 次</el-descriptions-item
        >
        <el-descriptions-item label="已售出">{{ stats.soldCount || 0 }} 件</el-descriptions-item>
        <el-descriptions-item label="待审核">
          <span :class="{ warn: stats.pendingCount > 0 }">
            {{ stats.pendingCount || 0 }} 件
            <el-link
              v-if="stats.pendingCount > 0"
              type="primary"
              :underline="false"
              class="go-audit"
              @click="router.push('/admin/product')"
            >
              去处理
            </el-link>
          </span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const stats = ref({})

const cards = computed(() => [
  {
    label: '待审核商品',
    value: stats.value.pendingCount ?? 0,
    icon: 'Clock',
    color: '#f59e0b',
    bg: '#fef3c7',
    path: '/admin/product'
  },
  {
    label: '在售商品',
    value: stats.value.onSaleCount ?? 0,
    icon: 'Goods',
    color: '#10b981',
    bg: '#d1fae5'
  },
  {
    label: '注册用户',
    value: stats.value.userCount ?? 0,
    icon: 'User',
    color: '#2563eb',
    bg: '#dbeafe'
  },
  {
    label: '累计浏览量',
    value: stats.value.totalViewCount ?? 0,
    icon: 'View',
    color: '#8b5cf6',
    bg: '#ede9fe'
  }
])

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
}

.stat-card.clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.stat-card.clickable:hover {
  box-shadow: var(--app-shadow-hover);
  transform: translateY(-2px);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.warn {
  color: #f59e0b;
  font-weight: 600;
}

.go-audit {
  margin-left: 8px;
  font-size: 12px;
}
</style>
