import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

/**
 * 路由表。
 * meta.requiresAuth  是否需要登录
 * meta.requiresAdmin 是否需要管理员角色
 * meta.title         浏览器标题 / 面包屑
 */
const routes = [
  {
    path: '/',
    component: () => import('@/layout/UserLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'product/list',
        name: 'ProductList',
        component: () => import('@/views/product/List.vue'),
        meta: { title: '商品列表' }
      },
      {
        path: 'product/:id(\\d+)',
        name: 'ProductDetail',
        component: () => import('@/views/product/Detail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'seller/:id(\\d+)',
        name: 'SellerHome',
        component: () => import('@/views/user/SellerHome.vue'),
        meta: { title: '卖家主页' }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/product/Publish.vue'),
        meta: { title: '发布商品', requiresAuth: true }
      },
      {
        path: 'publish/:id(\\d+)',
        name: 'EditProduct',
        component: () => import('@/views/product/Publish.vue'),
        meta: { title: '编辑商品', requiresAuth: true }
      },
      {
        path: 'user/profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'user/products',
        name: 'MyProducts',
        component: () => import('@/views/user/MyProducts.vue'),
        meta: { title: '我的发布', requiresAuth: true }
      },
      {
        path: 'user/favorites',
        name: 'MyFavorites',
        component: () => import('@/views/user/MyFavorites.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'user/orders',
        name: 'MyOrders',
        component: () => import('@/views/user/MyOrders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'user/reports',
        name: 'MyReports',
        component: () => import('@/views/user/MyReports.vue'),
        meta: { title: '我的举报', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    // 父路由上的 meta 会合并到所有子路由，所以这里写一次就够了
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'product',
        name: 'AdminProduct',
        component: () => import('@/views/admin/ProductManage.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'category',
        name: 'AdminCategory',
        component: () => import('@/views/admin/CategoryManage.vue'),
        meta: { title: '分类维护' }
      },
      {
        path: 'report',
        name: 'AdminReport',
        component: () => import('@/views/admin/ReportManage.vue'),
        meta: { title: '举报处理' }
      }
    ]
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  // 标题
  document.title = to.meta.title
    ? `${to.meta.title} - 二手数码平台`
    : '二手数码信息发布与交流平台'

  // 需要登录
  if (to.meta.requiresAuth && !userStore.isLogin) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 需要管理员
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('没有访问权限')
    return { path: '/' }
  }

  // 已登录用户不再访问登录/注册页
  if ((to.path === '/login' || to.path === '/register') && userStore.isLogin) {
    return { path: '/' }
  }

  return true
})

export default router
