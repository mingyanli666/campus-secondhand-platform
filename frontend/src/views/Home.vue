<template>
  <div class="home">
    <!-- 搜索区 -->
    <section class="hero">
      <div class="app-container hero-inner">
        <h1>闲置数码，物尽其用</h1>
        <p>发布你的闲置手机、电脑、相机，或用更低的价格收到心仪好物</p>
        <div class="search-box">
          <el-input
            v-model.trim="keyword"
            size="large"
            placeholder="搜索品牌、型号，如 iPhone、拯救者、索尼"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        </div>
      </div>
    </section>

    <div class="app-container">
      <!-- 分类导航 -->
      <section class="section">
        <div class="section-head">
          <h3>全部分类</h3>
          <el-link type="primary" :underline="false" @click="router.push('/product/list')">
            浏览全部 <el-icon><ArrowRight /></el-icon>
          </el-link>
        </div>
        <div class="category-grid">
          <div
            v-for="c in categories"
            :key="c.id"
            class="category-item"
            @click="goCategory(c.id)"
          >
            <el-icon :size="22" color="var(--el-color-primary)">
              <component :is="iconOf(c.icon)" />
            </el-icon>
            <span>{{ c.name }}</span>
          </div>
        </div>
      </section>

      <!-- 最新商品 -->
      <section class="section">
        <div class="section-head">
          <h3>最新发布</h3>
          <el-link type="primary" :underline="false" @click="router.push('/product/list')">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </el-link>
        </div>

        <div v-loading="loading" class="grid">
          <ProductCard v-for="item in products" :key="item.id" :product="item" />
        </div>

        <el-empty
          v-if="!loading && !products.length"
          description="平台还没有已上架的商品，快来发布第一件吧"
        >
          <el-button type="primary" @click="router.push('/publish')">发布商品</el-button>
        </el-empty>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as ElIcons from '@element-plus/icons-vue'
import { listCategories } from '@/api/category'
import { pageProducts } from '@/api/product'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()

const keyword = ref('')
const categories = ref([])
const products = ref([])
const loading = ref(false)

/**
 * 分类图标是后台可编辑的数据，管理员万一填了不存在的图标名，
 * 这里兜底成默认图标，避免整块渲染失败。
 */
function iconOf(name) {
  return name && ElIcons[name] ? name : 'Goods'
}

function handleSearch() {
  router.push({
    path: '/product/list',
    query: keyword.value ? { keyword: keyword.value } : {}
  })
}

function goCategory(id) {
  router.push({ path: '/product/list', query: { categoryId: id } })
}

onMounted(async () => {
  loading.value = true
  try {
    categories.value = await listCategories()
    const data = await pageProducts({ current: 1, size: 8 })
    products.value = data.records
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 55%, #60a5fa 100%);
  color: #fff;
  padding: 48px 0 52px;
  margin-bottom: 28px;
}

.hero-inner {
  text-align: center;
}

.hero h1 {
  margin: 0 0 10px;
  font-size: 30px;
  letter-spacing: 1px;
}

.hero p {
  margin: 0 0 24px;
  font-size: 14px;
  opacity: 0.9;
}

.search-box {
  display: flex;
  gap: 10px;
  max-width: 620px;
  margin: 0 auto;
}

.section {
  margin-bottom: 32px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 0;
  font-size: 17px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 84px;
  background: #fff;
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  cursor: pointer;
  font-size: 13px;
  color: var(--app-text-regular);
  transition: all 0.2s;
}

.category-item:hover {
  transform: translateY(-3px);
  color: var(--el-color-primary);
  box-shadow: var(--app-shadow-hover);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  min-height: 120px;
}

@media (max-width: 900px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
