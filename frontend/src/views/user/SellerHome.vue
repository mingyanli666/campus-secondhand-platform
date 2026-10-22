<template>
  <div class="app-container seller-home">
    <div v-loading="loadingProfile">
      <!-- 卖家资料卡 -->
      <div v-if="seller" class="profile-card app-card">
        <el-avatar :size="72" :src="seller.avatar" class="avatar">
          {{ (seller.nickname || '?').charAt(0) }}
        </el-avatar>

        <div class="profile-main">
          <div class="name-row">
            <h2>{{ seller.nickname || '未设置昵称' }}</h2>
            <el-tag size="small" type="success" effect="plain">
              信誉 {{ seller.creditScore ?? 100 }}
            </el-tag>
            <el-tag v-if="isSelf" size="small" type="primary" effect="plain">这是你</el-tag>
          </div>

          <div class="meta text-secondary">
            <span v-if="seller.campus"><el-icon><Location /></el-icon> {{ seller.campus }}</span>
            <span><el-icon><Clock /></el-icon> {{ formatTime(seller.createTime) }} 加入</span>
          </div>

          <p v-if="seller.intro" class="intro">{{ seller.intro }}</p>

          <div class="stats">
            <div class="stat">
              <span class="num">{{ seller.onSaleCount ?? 0 }}</span>
              <span class="label">在售商品</span>
            </div>
            <div class="stat">
              <span class="num">{{ seller.soldCount ?? 0 }}</span>
              <span class="label">已成交</span>
            </div>
          </div>
        </div>

        <div v-if="isSelf" class="profile-actions">
          <el-button type="primary" plain @click="router.push('/user/products')">
            管理我的商品
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loadingProfile && !seller" description="该用户不存在">
        <el-button type="primary" @click="router.push('/product/list')">去逛逛商品</el-button>
      </el-empty>
    </div>

    <!-- 在售商品 -->
    <template v-if="seller">
      <div class="section-head">
        <h3>
          {{ isSelf ? '我在售的商品' : 'TA 在售的商品' }}
          <span class="count">{{ total }}</span>
        </h3>
        <el-link
          v-if="isSelf && total === 0"
          type="primary"
          :underline="false"
          @click="router.push('/publish')"
        >
          去发布第一件
        </el-link>
      </div>

      <div v-loading="loadingList" class="grid">
        <ProductCard v-for="item in products" :key="item.id" :product="item" />
      </div>

      <el-empty
        v-if="!loadingList && !products.length"
        :description="isSelf ? '你还没有在售的商品' : 'TA 暂时没有在售的商品'"
      />

      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadProducts"
          @size-change="handleSizeChange"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSellerProfile, pageSellerProducts } from '@/api/seller'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loadingProfile = ref(false)
const loadingList = ref(false)
const seller = ref(null)
const products = ref([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 12
})

const isSelf = computed(
  () => userStore.isLogin && String(userStore.userInfo?.id) === String(route.params.id)
)

async function loadProfile() {
  loadingProfile.value = true
  try {
    seller.value = await getSellerProfile(route.params.id)
  } catch {
    // 用户不存在时后端返回 2003，这里退化成"该用户不存在"的空状态
    seller.value = null
  } finally {
    loadingProfile.value = false
  }
}

async function loadProducts() {
  loadingList.value = true
  try {
    const data = await pageSellerProducts(route.params.id, query)
    products.value = data.records
    total.value = Number(data.total)
  } finally {
    loadingList.value = false
  }
}

function handleSizeChange() {
  query.current = 1
  loadProducts()
}

function reloadAll() {
  query.current = 1
  loadProfile()
  loadProducts()
}

onMounted(reloadAll)

// 从一个卖家主页直接跳到另一个卖家主页时（比如从商品详情再点进去），要重新加载
watch(() => route.params.id, reloadAll)
</script>

<style scoped>
.seller-home {
  padding: 24px 16px 48px;
}

.profile-card {
  display: flex;
  gap: 20px;
  padding: 26px 28px;
  margin-bottom: 24px;
}

.avatar {
  flex: none;
  background: var(--el-color-primary-light-8);
  font-size: 26px;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.name-row h2 {
  margin: 0;
  font-size: 20px;
}

.meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 8px;
  font-size: 12px;
}

.meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.intro {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--app-text-regular);
}

.stats {
  display: flex;
  gap: 36px;
  margin-top: 14px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat .num {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.2;
}

.stat .label {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.profile-actions {
  flex: none;
  display: flex;
  align-items: flex-start;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-head h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 17px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
}

.count {
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 400;
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

@media (max-width: 700px) {
  .profile-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .meta,
  .stats {
    justify-content: center;
  }
}
</style>
