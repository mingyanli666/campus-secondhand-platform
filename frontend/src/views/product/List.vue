<template>
  <div class="app-container list-page">
    <!-- 筛选栏 -->
    <div class="filter-bar app-card">
      <div class="filter-row">
        <span class="filter-label">分类</span>
        <div class="filter-options">
          <span
            class="option"
            :class="{ active: query.categoryId === null }"
            @click="selectCategory(null)"
          >
            全部
          </span>
          <span
            v-for="c in categories"
            :key="c.id"
            class="option"
            :class="{ active: query.categoryId === c.id }"
            @click="selectCategory(c.id)"
          >
            {{ c.name }}
          </span>
        </div>
      </div>

      <div class="filter-row">
        <span class="filter-label">成色</span>
        <div class="filter-options">
          <span
            class="option"
            :class="{ active: !query.conditionLevel }"
            @click="selectCondition('')"
          >
            不限
          </span>
          <span
            v-for="c in CONDITION_OPTIONS"
            :key="c"
            class="option"
            :class="{ active: query.conditionLevel === c }"
            @click="selectCondition(c)"
          >
            {{ c }}
          </span>
        </div>
      </div>

      <div class="filter-row">
        <span class="filter-label">价格</span>
        <div class="filter-options price-filter">
          <el-input-number v-model="priceRange[0]" :min="0" :controls="false" placeholder="最低价" />
          <span class="dash">-</span>
          <el-input-number v-model="priceRange[1]" :min="0" :controls="false" placeholder="最高价" />
          <el-button size="small" @click="applyPrice">确定</el-button>
          <el-button size="small" text @click="resetPrice">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="result-count">
        共 <b>{{ total }}</b> 件商品
      </div>
      <div class="sort-group">
        <span
          v-for="s in SORT_OPTIONS"
          :key="s.value"
          class="sort-item"
          :class="{ active: query.sort === s.value }"
          @click="selectSort(s.value)"
        >
          {{ s.label }}
        </span>
      </div>
    </div>

    <!-- 商品网格 -->
    <div v-loading="loading" class="grid">
      <ProductCard v-for="item in list" :key="item.id" :product="item" />
    </div>

    <el-empty v-if="!loading && !list.length" description="没有找到符合条件的商品">
      <el-button v-if="hasFilter" @click="resetAll">清空筛选条件</el-button>
      <el-button v-else type="primary" @click="router.push('/publish')">去发布一件商品</el-button>
    </el-empty>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadList"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listCategories } from '@/api/category'
import { pageProducts } from '@/api/product'
import { CONDITION_OPTIONS } from '@/utils/format'
import ProductCard from '@/components/ProductCard.vue'

const SORT_OPTIONS = [
  { label: '最新发布', value: '' },
  { label: '价格从低到高', value: 'priceAsc' },
  { label: '价格从高到低', value: 'priceDesc' },
  { label: '最多浏览', value: 'hot' }
]

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const priceRange = ref([null, null])

const query = reactive({
  current: 1,
  size: 12,
  categoryId: null,
  keyword: '',
  conditionLevel: '',
  minPrice: null,
  maxPrice: null,
  sort: ''
})

const hasFilter = computed(
  () =>
    query.categoryId !== null ||
    !!query.keyword ||
    !!query.conditionLevel ||
    query.minPrice !== null ||
    query.maxPrice !== null
)

async function loadList() {
  loading.value = true
  try {
    const data = await pageProducts(query, 'product:list')
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

/** 切换筛选条件时都要回到第 1 页，否则会出现"第 5 页没数据"的困惑 */
function reload() {
  query.current = 1
  loadList()
}

function selectCategory(id) {
  query.categoryId = id
  reload()
}

function selectCondition(value) {
  query.conditionLevel = value
  reload()
}

function selectSort(value) {
  query.sort = value
  reload()
}

function applyPrice() {
  query.minPrice = priceRange.value[0]
  query.maxPrice = priceRange.value[1]
  reload()
}

function resetPrice() {
  priceRange.value = [null, null]
  query.minPrice = null
  query.maxPrice = null
  reload()
}

function handleSizeChange() {
  query.current = 1
  loadList()
}

function resetAll() {
  query.categoryId = null
  query.keyword = ''
  query.conditionLevel = ''
  query.minPrice = null
  query.maxPrice = null
  query.sort = ''
  priceRange.value = [null, null]
  reload()
}

onMounted(async () => {
  // 支持从首页分类跳转过来：/product/list?categoryId=1
  if (route.query.categoryId) {
    query.categoryId = Number(route.query.categoryId)
  }
  if (route.query.keyword) {
    query.keyword = String(route.query.keyword)
  }
  categories.value = await listCategories()
  await loadList()
})
</script>

<style scoped>
.list-page {
  padding: 20px 16px 48px;
}

.filter-bar {
  padding: 16px 20px 4px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  font-size: 13px;
}

.filter-label {
  flex: none;
  width: 44px;
  padding-top: 4px;
  color: var(--app-text-secondary);
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.option {
  padding: 4px 12px;
  border-radius: 14px;
  color: var(--app-text-regular);
  cursor: pointer;
  transition: all 0.15s;
}

.option:hover {
  color: var(--el-color-primary);
}

.option.active {
  color: #fff;
  background: var(--el-color-primary);
}

.price-filter {
  align-items: center;
  gap: 8px;
}

.price-filter :deep(.el-input-number) {
  width: 110px;
}

.dash {
  color: var(--app-text-secondary);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 14px;
  font-size: 13px;
}

.result-count b {
  color: var(--el-color-primary);
  font-size: 15px;
}

.sort-group {
  display: flex;
  gap: 4px;
}

.sort-item {
  padding: 5px 12px;
  border-radius: 6px;
  color: var(--app-text-regular);
  cursor: pointer;
  transition: all 0.15s;
}

.sort-item:hover {
  color: var(--el-color-primary);
}

.sort-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 600;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
