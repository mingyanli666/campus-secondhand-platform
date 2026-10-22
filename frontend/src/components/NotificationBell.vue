<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="340"
    trigger="click"
    @show="loadList"
  >
    <template #reference>
      <el-badge :value="unread" :hidden="!unread" :max="99" class="bell-badge">
        <el-icon :size="18" class="bell-icon"><Bell /></el-icon>
      </el-badge>
    </template>

    <div class="notify-panel">
      <div class="notify-head">
        <span class="notify-title">通知</span>
        <el-link
          v-if="unread"
          type="primary"
          :underline="false"
          class="read-all"
          @click="handleReadAll"
        >
          全部已读
        </el-link>
      </div>

      <div v-loading="loading" class="notify-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="notify-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleClick(item)"
        >
          <div class="item-top">
            <span class="item-title">{{ item.title }}</span>
            <span class="item-time text-secondary">{{ fromNow(item.createTime) }}</span>
          </div>
          <div class="item-content text-secondary">{{ item.content }}</div>
        </div>

        <el-empty v-if="!loading && !list.length" description="暂无通知" :image-size="56" />
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUnreadCount, pageNotifications, readAllNotifications, readNotification } from '@/api/notification'
import { fromNow } from '@/utils/format'

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const list = ref([])
const unread = ref(0)

async function loadUnread() {
  unread.value = Number(await getUnreadCount())
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageNotifications({ current: 1, size: 10 })
    list.value = data.records
  } finally {
    loading.value = false
  }
}

async function handleReadAll() {
  await readAllNotifications()
  unread.value = 0
  list.value = list.value.map((n) => ({ ...n, isRead: 1 }))
}

async function handleClick(item) {
  if (item.isRead === 0) {
    await readNotification(item.id)
    item.isRead = 1
    unread.value = Math.max(0, unread.value - 1)
  }
  // 点击通知跳到对应的页面：
  //   1 审核结果 / 3 留言回复 → bizId 是商品ID → 商品详情页
  //   2 订单状态            → bizId 是订单ID → 我的订单页
  if (item.bizId) {
    visible.value = false
    if (item.type === 2) {
      router.push('/user/orders')
    } else if (item.type === 1 || item.type === 3) {
      router.push(`/product/${item.bizId}`)
    }
  }
}

onMounted(loadUnread)

defineExpose({ refresh: loadUnread })
</script>

<style scoped>
.bell-badge {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.bell-icon {
  color: var(--app-text-regular);
}

.bell-icon:hover {
  color: var(--el-color-primary);
}

.notify-panel {
  margin: -12px;
}

.notify-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--app-border);
}

.notify-title {
  font-weight: 600;
  font-size: 14px;
}

.read-all {
  font-size: 12px;
}

.notify-list {
  max-height: 360px;
  overflow-y: auto;
}

.notify-item {
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s;
}

.notify-item:hover {
  background: #f8fafc;
}

.notify-item.unread {
  background: #eff6ff;
}

.item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.item-title {
  font-size: 13px;
  font-weight: 500;
}

.item-time {
  flex: none;
  font-size: 11px;
}

.item-content {
  font-size: 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
