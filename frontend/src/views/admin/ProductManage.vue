<template>
  <div class="product-manage">
    <!-- 筛选 -->
    <div class="filter-bar app-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="0">
          <template #label>
            <span>待审核</span>
            <el-badge v-if="counts[0]" :value="counts[0]" class="tab-badge" />
          </template>
        </el-tab-pane>
        <el-tab-pane label="已上架" name="1" />
        <el-tab-pane label="已下架" name="2" />
        <el-tab-pane label="已售出" name="3" />
        <el-tab-pane label="审核驳回" name="4" />
        <el-tab-pane label="全部" name="all" />
      </el-tabs>

      <div class="search-row">
        <el-input
          v-model.trim="query.keyword"
          placeholder="搜索标题、品牌、型号"
          clearable
          style="width: 240px"
          @keyup.enter="reload"
          @clear="reload"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select
          v-model="query.categoryId"
          placeholder="全部分类"
          clearable
          style="width: 160px"
          @change="reload"
        >
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>

        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image class="cell-cover" :src="row.coverImage" fit="cover">
                <template #error>
                  <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="cell-info">
                <div class="cell-title">{{ row.title }}</div>
                <div class="cell-sub text-secondary">
                  {{ row.brand || '未填品牌' }}
                  <span v-if="row.model"> / {{ row.model }}</span>
                  <span v-if="row.conditionLevel"> · {{ row.conditionLevel }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="110" />

        <el-table-column label="卖家" width="120">
          <template #default="{ row }">{{ row.sellerNickname || '-' }}</template>
        </el-table-column>

        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusOf(row.status).type" size="small" effect="light">
              {{ statusOf(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openDetail(row)">详情</el-button>

            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="openAudit(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="openAudit(row, false)">驳回</el-button>
            </template>

            <el-button
              v-if="row.status === 1"
              size="small"
              type="warning"
              plain
              @click="openForceOff(row)"
            >
              强制下架
            </el-button>

            <el-button
              v-if="row.status !== 3"
              size="small"
              type="danger"
              text
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的商品" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadList"
          @size-change="reload"
        />
      </div>
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" :title="auditForm.pass ? '审核通过' : '审核驳回'" width="520px">
      <el-alert
        v-if="auditForm.pass"
        type="success"
        :closable="false"
        show-icon
        title="通过后商品将立即上架，所有用户可见"
        class="dialog-alert"
      />
      <el-alert
        v-else
        type="warning"
        :closable="false"
        show-icon
        title="驳回后商品不会上架，卖家会收到通知，可以修改后重新提交"
        class="dialog-alert"
      />

      <div class="audit-product">
        <div class="audit-title">{{ currentRow?.title }}</div>
        <div class="text-secondary">
          卖家：{{ currentRow?.sellerNickname }} · 分类：{{ currentRow?.categoryName }}
        </div>
      </div>

      <el-form v-if="!auditForm.pass" :model="auditForm" label-position="top">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model.trim="auditForm.reason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请具体说明问题，如：商品描述与图片不符 / 疑似盗用他人图片 / 含违规联系方式"
          />
          <div class="quick-reasons">
            <span class="text-secondary">常用：</span>
            <el-tag
              v-for="r in QUICK_REASONS"
              :key="r"
              class="quick-tag"
              effect="plain"
              @click="auditForm.reason = r"
            >
              {{ r }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button
          :type="auditForm.pass ? 'success' : 'danger'"
          :loading="submitting"
          @click="submitAudit"
        >
          确认{{ auditForm.pass ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 强制下架弹窗 -->
    <el-dialog v-model="forceOffVisible" title="强制下架" width="480px">
      <el-form ref="forceOffFormRef" :model="forceOffForm" :rules="forceOffRules" label-position="top">
        <el-form-item label="下架原因" prop="reason">
          <el-input
            v-model.trim="forceOffForm.reason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="必填。该原因会写入审核记录并通知卖家"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forceOffVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitForceOff">确认下架</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="商品详情" size="620px">
      <div v-if="detail" v-loading="detailLoading" class="detail-body">
        <el-image
          class="detail-cover"
          :src="detail.coverImage"
          :preview-src-list="detail.images"
          fit="contain"
          preview-teleported
        />

        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="售价">¥{{ formatPrice(detail.price) }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ detail.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ detail.conditionLevel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="质保">{{ detail.warranty || '-' }}</el-descriptions-item>
          <el-descriptions-item label="购入时间">{{ detail.purchaseDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易地点">{{ detail.tradeLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系方式" :span="2">{{ detail.contact || '-' }}</el-descriptions-item>
          <el-descriptions-item
            v-for="item in specList"
            :key="item.key"
            :label="item.key"
          >
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>

        <h4>商品描述</h4>
        <p class="detail-text">{{ detail.description }}</p>

        <h4>卖家信息</h4>
        <div class="seller-line">
          <el-avatar :size="36" :src="detail.sellerAvatar">
            {{ (detail.sellerNickname || '?').charAt(0) }}
          </el-avatar>
          <span>{{ detail.sellerNickname }}</span>
          <el-tag size="small" type="success" effect="plain">
            信誉 {{ detail.sellerCreditScore ?? 100 }}
          </el-tag>
          <span class="text-secondary">{{ detail.sellerCampus || '未填写区域' }}</span>
        </div>

        <h4>审核历史</h4>
        <el-timeline v-if="records.length">
          <el-timeline-item
            v-for="r in records"
            :key="r.id"
            :timestamp="formatTime(r.createTime, true)"
            :type="r.action === 1 ? 'success' : r.action === 2 ? 'danger' : 'warning'"
          >
            <div class="record-line">
              <b>{{ r.actionText }}</b>
              <span class="text-secondary">操作人：{{ r.adminName }}</span>
            </div>
            <div v-if="r.reason" class="record-reason text-secondary">{{ r.reason }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无审核记录" :image-size="60" />

        <div class="drawer-actions">
          <template v-if="detail.status === 0">
            <el-button type="success" @click="detailVisible = false; openAudit(currentRow, true)">
              通过
            </el-button>
            <el-button type="danger" @click="detailVisible = false; openAudit(currentRow, false)">
              驳回
            </el-button>
          </template>
          <el-button
            v-if="detail.status === 1"
            type="warning"
            @click="detailVisible = false; openForceOff(currentRow)"
          >
            强制下架
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminDeleteProduct,
  adminListCategories,
  adminPageProducts,
  auditProduct,
  forceOffShelf,
  getAuditRecords
} from '@/api/admin'
import { getProduct } from '@/api/product'
import { formatPrice, formatTime, productStatus } from '@/utils/format'

/** 常用驳回原因，减少管理员手打 */
const QUICK_REASONS = [
  '商品描述与图片不符',
  '疑似盗用他人图片',
  '含有违规联系方式',
  '信息填写不完整',
  '疑似虚假或违禁商品'
]

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const activeTab = ref('0')
const counts = reactive({ 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 })

const query = reactive({
  current: 1,
  size: 10,
  status: 0,
  keyword: '',
  categoryId: null
})

const statusOf = productStatus

/* ---------------- 列表 ---------------- */
async function loadList() {
  loading.value = true
  try {
    const data = await adminPageProducts(query)
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

/** 拉各状态的数量，显示在 Tab 上 */
async function loadCounts() {
  await Promise.all(
    [0, 1, 2, 3, 4].map(async (status) => {
      const data = await adminPageProducts({ current: 1, size: 1, status })
      counts[status] = Number(data.total)
    })
  )
}

function handleTabChange() {
  query.status = activeTab.value === 'all' ? null : Number(activeTab.value)
  query.current = 1
  loadList()
}

function reload() {
  query.current = 1
  loadList()
}

function resetQuery() {
  query.keyword = ''
  query.categoryId = null
  reload()
}

/* ---------------- 审核 ---------------- */
const auditVisible = ref(false)
const currentRow = ref(null)
const auditForm = reactive({ pass: true, reason: '' })

function openAudit(row, pass) {
  currentRow.value = row
  auditForm.pass = pass
  auditForm.reason = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!auditForm.pass && !auditForm.reason) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  submitting.value = true
  try {
    await auditProduct(currentRow.value.id, {
      action: auditForm.pass ? 1 : 2,
      reason: auditForm.pass ? null : auditForm.reason
    })
    ElMessage.success(auditForm.pass ? '已通过，商品已上架' : '已驳回，卖家已收到通知')
    auditVisible.value = false
    refresh()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 强制下架 ---------------- */
const forceOffVisible = ref(false)
const forceOffFormRef = ref()
const forceOffForm = reactive({ reason: '' })

const forceOffRules = {
  reason: [
    { required: true, message: '请填写下架原因', trigger: 'blur' },
    { max: 255, message: '下架原因不能超过 255 个字符', trigger: 'blur' }
  ]
}

function openForceOff(row) {
  currentRow.value = row
  forceOffForm.reason = ''
  forceOffVisible.value = true
}

async function submitForceOff() {
  const valid = await forceOffFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await forceOffShelf(currentRow.value.id, { reason: forceOffForm.reason })
    ElMessage.success('已强制下架，并已通知卖家')
    forceOffVisible.value = false
    refresh()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 删除 ---------------- */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.title}」吗？删除后卖家也无法恢复。`,
      '警告',
      { type: 'warning', confirmButtonText: '确定删除' }
    )
  } catch {
    return
  }
  await adminDeleteProduct(row.id)
  ElMessage.success('已删除')
  refresh()
}

/* ---------------- 详情抽屉 ---------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const records = ref([])

const specList = ref([])

async function openDetail(row) {
  currentRow.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [d, r] = await Promise.all([getProduct(row.id), getAuditRecords(row.id)])
    detail.value = d
    records.value = r
    specList.value = parseSpec(d.spec)
  } finally {
    detailLoading.value = false
  }
}

function parseSpec(spec) {
  if (!spec) return []
  try {
    return Object.entries(JSON.parse(spec)).map(([key, value]) => ({ key, value }))
  } catch {
    return []
  }
}

function refresh() {
  loadList()
  loadCounts()
}

onMounted(async () => {
  categories.value = await adminListCategories()
  // 支持从「用户管理」「分类维护」点商品数跳转过来，带上筛选条件
  if (route.query.keyword) {
    query.keyword = String(route.query.keyword)
  }
  if (route.query.categoryId) {
    query.categoryId = Number(route.query.categoryId)
  }
  loadList()
  loadCounts()
})
</script>

<style scoped>
.filter-bar {
  padding: 6px 20px 16px;
  margin-bottom: 16px;
}

.tab-badge {
  margin-left: 6px;
}

.search-row {
  display: flex;
  gap: 10px;
}

.table-card {
  padding: 4px 0 16px;
}

.product-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.cell-cover {
  flex: none;
  width: 54px;
  height: 54px;
  border-radius: 6px;
  background: #f3f4f6;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--app-text-secondary);
}

.cell-info {
  min-width: 0;
}

.cell-title {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.cell-sub {
  margin-top: 3px;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px 0;
}

.dialog-alert {
  margin-bottom: 16px;
}

.audit-product {
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.audit-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.quick-reasons {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  font-size: 12px;
}

.quick-tag {
  cursor: pointer;
}

.quick-tag:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.detail-body h4 {
  margin: 22px 0 10px;
  font-size: 14px;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.detail-cover {
  width: 100%;
  height: 240px;
  background: #fafafa;
  border-radius: 6px;
}

.detail-desc {
  margin-top: 14px;
}

.detail-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: var(--app-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.seller-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.record-line {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 13px;
}

.record-reason {
  margin-top: 4px;
  font-size: 12px;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--app-border);
}
</style>
