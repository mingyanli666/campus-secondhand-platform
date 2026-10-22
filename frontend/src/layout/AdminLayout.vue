<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <el-icon :size="22"><Setting /></el-icon>
        <span v-show="!collapsed">管理后台</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        class="menu"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="header">
        <el-icon class="collapse-btn" :size="18" @click="collapsed = !collapsed">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>

        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="header-right">
          <el-badge :value="pendingCount" :hidden="!pendingCount" class="pending-badge">
            <el-button text @click="router.push('/admin/product')">
              <el-icon><Bell /></el-icon>
              <span class="pending-text">待审核</span>
            </el-button>
          </el-badge>

          <el-divider direction="vertical" />

          <el-tooltip content="返回前台" placement="bottom">
            <el-button text :icon="HomeFilled" @click="router.push('/')" />
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="28" :src="userStore.userInfo?.avatar">
                {{ userStore.nickname.charAt(0) }}
              </el-avatar>
              <span class="nickname">{{ userStore.nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { HomeFilled } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const pendingCount = ref(0)

/**
 * 侧边栏菜单。
 * 只列出「已经实现」的模块 —— 放一堆点了 404 的入口是更糟糕的体验。
 * 后续每做完一个模块（公告管理、数据导出等）再往这里加一行。
 */
const menus = [
  { path: '/admin', title: '数据看板', icon: 'DataLine' },
  { path: '/admin/product', title: '商品管理', icon: 'Goods' },
  { path: '/admin/user', title: '用户管理', icon: 'User' },
  { path: '/admin/category', title: '分类维护', icon: 'Grid' },
  { path: '/admin/report', title: '举报处理', icon: 'Warning' }
]

const currentTitle = computed(() => {
  const matched = menus.find((m) => m.path === route.path)
  return matched ? matched.title : '管理后台'
})

async function loadPendingCount() {
  const stats = await getDashboardStats()
  pendingCount.value = Number(stats.pendingCount || 0)
}

async function handleCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    await userStore.doLogout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(loadPendingCount)
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  flex: none;
  width: 210px;
  background: #1f2937;
  transition: width 0.2s;
  overflow-x: hidden;
}

.sidebar.collapsed {
  width: 64px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 20px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: #cbd5e1;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary);
  color: #fff;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--app-bg);
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 56px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--app-border);
}

.collapse-btn {
  cursor: pointer;
  color: var(--app-text-regular);
}

.breadcrumb {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pending-text {
  margin-left: 4px;
  font-size: 13px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 13px;
  color: var(--app-text-regular);
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
</style>
