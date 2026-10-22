<template>
  <div class="app-container my-favorites">
    <div class="page-head">
      <h2>我的收藏</h2>
      <span class="text-secondary">共 {{ total }} 件商品</span>
    </div>

    <div v-loading="loading" class="grid">
      <ProductCard v-for="item in list" :key="item.id" :product="item">
        <template #actions>
          <el-button
            link
            type="danger"
            size="small"
            :loading="removingId === item.id"
            @click.stop="handleRemove(item)"
          >
            取消收藏
          </el-button>
        </template>
      </ProductCard>
    </div>

    <el-empty v-if="!loading && !list.length" description="还没有收藏任何商品">
      <el-button type="primary" @click="router.push('/product/list')">去逛逛</el-button>
    </el-empty>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[12, 24, 48]"
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
import { pageFavorites, removeFavorite } from '@/api/favorite'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()

const loading = ref(false)
const removingId = ref(null)
const list = ref([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 12
})

async function loadList() {
  loading.value = true
  try {
    const data = await pageFavorites(query)
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

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${item.title}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }

  removingId.value = item.id
  try {
    await removeFavorite(item.id)
    ElMessage.success('已取消收藏')
    // 当前页只剩这一条时，删完要往前退一页，否则会停在空白页
    if (list.value.length === 1 && query.current > 1) {
      query.current -= 1
    }
    await loadList()
  } finally {
    removingId.value = null
  }
}

onMounted(loadList)
</script>

<style scoped>
.my-favorites {
  padding: 24px 16px 48px;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
