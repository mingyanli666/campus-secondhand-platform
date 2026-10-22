<template>
  <div class="user-layout">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="app-container header-inner">
        <router-link to="/" class="logo">
          <el-icon :size="24"><Cellphone /></el-icon>
          <span>二手数码</span>
        </router-link>

        <nav class="nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="actions">
          <template v-if="userStore.isLogin">
            <el-button type="primary" plain :icon="Plus" @click="router.push('/publish')">
              发布闲置
            </el-button>
            <NotificationBell />
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="user-entry">
                <el-avatar :size="30" :src="userStore.userInfo?.avatar">
                  {{ userStore.nickname.charAt(0) }}
                </el-avatar>
                <span class="nickname">{{ userStore.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="myOrders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="myProducts">我的发布</el-dropdown-item>
                  <el-dropdown-item command="myFavorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="myReports">我的举报</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      <div class="app-container">
        <p>二手数码产品信息发布与交流平台 · 毕业设计作品</p>
        <p class="text-secondary">本站仅提供信息发布与交流服务，交易请当面验货，谨防受骗</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import NotificationBell from '@/components/NotificationBell.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '首页', path: '/', match: (p) => p === '/' },
  { label: '全部商品', path: '/product/list', match: (p) => p.startsWith('/product') },
  { label: '我的发布', path: '/user/products', match: (p) => p === '/user/products' }
]

function isActive(item) {
  return item.match(route.path)
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'myOrders') {
    router.push('/user/orders')
  } else if (command === 'myProducts') {
    router.push('/user/products')
  } else if (command === 'myFavorites') {
    router.push('/user/favorites')
  } else if (command === 'myReports') {
    router.push('/user/reports')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'logout') {
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
</script>

<style scoped>
.user-layout {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--app-header-height);
  background: #fff;
  border-bottom: 1px solid var(--app-border);
}

.header-inner {
  display: flex;
  align-items: center;
  height: var(--app-header-height);
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
  white-space: nowrap;
}

.nav {
  flex: 1;
  display: flex;
  gap: 8px;
}

.nav-item {
  padding: 6px 12px;
  border-radius: 6px;
  color: var(--app-text-regular);
  transition: all 0.2s;
}

.nav-item:hover,
.nav-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--app-text-regular);
}

.main {
  flex: 1;
}

.footer {
  margin-top: 48px;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--app-text-secondary);
  border-top: 1px solid var(--app-border);
  background: #fff;
}

.footer p {
  margin: 4px 0;
}
</style>
