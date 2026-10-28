<template>
  <div class="product-card app-card" @click="goDetail">
    <div class="cover">
      <el-image :src="product.coverImage" fit="cover" lazy>
        <template #error>
          <div class="cover-placeholder">
            <el-icon :size="32"><Picture /></el-icon>
            <span>暂无图片</span>
          </div>
        </template>
      </el-image>
      <el-tag
        v-if="showStatus"
        class="status-tag"
        :type="statusInfo.type"
        size="small"
        effect="dark"
      >
        {{ statusInfo.text }}
      </el-tag>
    </div>

    <div class="body">
      <h3 class="title text-ellipsis-2">{{ product.title }}</h3>

      <div class="tags">
        <el-tag v-if="product.conditionLevel" size="small" effect="plain">
          {{ product.conditionLevel }}
        </el-tag>
        <el-tag v-if="product.brand" size="small" type="info" effect="plain">
          {{ product.brand }}
        </el-tag>
        <el-tag v-if="product.warranty" size="small" type="success" effect="plain">
          {{ product.warranty }}
        </el-tag>
      </div>

      <div class="price-row">
        <span class="price"> <small>¥</small>{{ formatPrice(product.price) }} </span>
        <span v-if="showOriginalPrice" class="original-price">
          ¥{{ formatPrice(product.originalPrice) }}
        </span>
      </div>

      <div class="meta">
        <span class="seller-link text-ellipsis" @click.stop="goSeller">
          {{ product.sellerNickname || '未知卖家' }}
        </span>
        <span class="text-secondary">{{ fromNow(product.createTime) }}</span>
      </div>

      <!-- 调用方可以塞额外的操作按钮（如「我的收藏」页的取消收藏） -->
      <div v-if="$slots.actions" class="actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatPrice, fromNow, productStatus } from '@/utils/format'

const props = defineProps({
  product: { type: Object, required: true },
  /** 是否展示状态标签（「我的发布」需要，公开列表不需要） */
  showStatus: { type: Boolean, default: false },
  /** 是否展示划线原价 */
  showOriginalPrice: { type: Boolean, default: true }
})

const router = useRouter()

const statusInfo = computed(() => productStatus(props.product.status))

/** 原价低于售价时不展示，避免出现"原价 100 现价 200"的尴尬 */
const showOriginalPrice = computed(() => {
  const { originalPrice, price } = props.product
  if (!props.showOriginalPrice || !originalPrice) return false
  return Number(originalPrice) > Number(price)
})

function goDetail() {
  router.push(`/product/${props.product.id}`)
}

/**
 * 点卖家昵称进入卖家主页。
 * 必须 @click.stop —— 整张卡片本身是"进商品详情"，不阻止冒泡会两个跳转一起触发。
 */
function goSeller() {
  const sellerId = props.product.sellerId
  if (sellerId) {
    router.push(`/seller/${sellerId}`)
  }
}
</script>

<style scoped>
.product-card {
  overflow: hidden;
  cursor: pointer;
  transition: all 0.22s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--app-shadow-hover);
}

.cover {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #f3f4f6;
}

.cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 100%;
  color: var(--app-text-secondary);
  font-size: 12px;
}

.status-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.body {
  padding: 12px 14px 14px;
}

.title {
  margin: 0 0 8px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--app-text-primary);
}

.tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  height: 24px;
  overflow: hidden;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.price {
  font-size: 20px;
}

.price small {
  font-size: 13px;
  margin-right: 1px;
}

.original-price {
  font-size: 12px;
  color: var(--app-text-secondary);
  text-decoration: line-through;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-regular);
}

.seller-link {
  min-width: 0;
  cursor: pointer;
  transition: color 0.15s;
}

.seller-link:hover {
  color: var(--el-color-primary);
  text-decoration: underline;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--app-border);
}
</style>
