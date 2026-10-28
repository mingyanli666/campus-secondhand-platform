<template>
  <div class="report-manage">
    <!-- 筛选 -->
    <div class="filter-bar app-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="0">
          <template #label>
            <span>待处理</span>
            <el-badge v-if="counts[0]" :value="counts[0]" class="tab-badge" />
          </template>
        </el-tab-pane>
        <el-tab-pane label="已处理" name="1" />
        <el-tab-pane label="已驳回" name="2" />
        <el-tab-pane label="全部" name="all" />
      </el-tabs>

      <el-select
        v-model="query.type"
        placeholder="全部类型"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option v-for="t in REPORT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
    </div>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="被举报商品" min-width="240">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image class="cell-cover" :src="row.productCover" fit="cover">
                <template #error>
                  <div class="cover-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="cell-info">
                <div class="cell-title" @click="goProduct(row.productId)">
                  {{ row.productTitle || '（商品已删除）' }}
                </div>
                <div class="cell-sub text-secondary">ID: {{ row.productId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)" effect="plain">
              {{ row.typeText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="举报说明" min-width="220">
          <template #default="{ row }">
            <div class="reason-text">{{ row.reason }}</div>
          </template>
        </el-table-column>

        <el-table-column label="举报人" width="110">
          <template #default="{ row }">{{ row.reporterNickname || '-' }}</template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)" effect="light">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="举报时间" width="150">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="primary" @click="openHandle(row)">处理</el-button>
            </template>
            <el-button v-else size="small" text type="primary" @click="openDetail(row)">
              查看结果
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的举报" />
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

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理举报" width="560px">
      <div v-if="current" class="handle-target">
        <div class="target-title">{{ current.productTitle || '（商品已删除）' }}</div>
        <div class="text-secondary">
          举报类型：{{ current.typeText }} · 举报人：{{ current.reporterNickname }}
        </div>
        <div class="target-reason">举报说明：{{ current.reason }}</div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="处理结果" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">举报成立（违规）</el-radio>
            <el-radio :value="2">举报不成立（驳回）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理结论" prop="handleResult">
          <el-input
            v-model.trim="form.handleResult"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            :placeholder="
              form.status === 1
                ? '如：经核实图片盗用他人，商品已下架'
                : '如：经核实商品信息与描述一致，未发现违规'
            "
          />
          <div class="form-tip">这段结论会通知给举报人</div>
        </el-form-item>

        <el-form-item v-if="form.status === 1" label="同时下架商品">
          <el-switch v-model="form.offShelf" />
          <span class="form-tip inline">
            开启后会强制下架该商品，并自动关闭进行中的订单、通知卖家
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle"> 确认处理 </el-button>
      </template>
    </el-dialog>

    <!-- 查看结果 -->
    <el-dialog v-model="detailVisible" title="举报处理结果" width="480px">
      <el-descriptions v-if="current" :column="1" border size="small">
        <el-descriptions-item label="商品">
          {{ current.productTitle || '（商品已删除）' }}
        </el-descriptions-item>
        <el-descriptions-item label="举报类型">{{ current.typeText }}</el-descriptions-item>
        <el-descriptions-item label="举报说明">{{ current.reason }}</el-descriptions-item>
        <el-descriptions-item label="处理结果">
          <el-tag size="small" :type="statusTag(current.status)" effect="light">
            {{ current.statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理结论">
          {{ current.handleResult || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处理人">{{ current.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">
          {{ formatTime(current.handleTime, true) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminPageReports, handleReport } from '@/api/admin'
import { formatTime } from '@/utils/format'

const REPORT_TYPES = [
  { value: 1, label: '虚假信息' },
  { value: 2, label: '违禁物品' },
  { value: 3, label: '价格欺诈' },
  { value: 4, label: '其他' }
]

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const activeTab = ref('0')
const counts = reactive({ 0: 0, 1: 0, 2: 0 })

const query = reactive({
  current: 1,
  size: 10,
  status: 0,
  type: null
})

const current = ref(null)

function typeTag(type) {
  return { 1: 'warning', 2: 'danger', 3: 'primary', 4: 'info' }[type] || 'info'
}

function statusTag(status) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[status] || 'info'
}

async function loadList() {
  loading.value = true
  try {
    const data = await adminPageReports(query, 'admin:report:list')
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  await Promise.all(
    [0, 1, 2].map(async (status) => {
      const data = await adminPageReports({ current: 1, size: 1, status })
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

function goProduct(productId) {
  if (productId) {
    router.push(`/product/${productId}`)
  }
}

/* ---------------- 处理 ---------------- */
const handleVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()

const form = reactive({
  status: 1,
  handleResult: '',
  offShelf: true
})

const rules = {
  status: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
  handleResult: [
    { required: true, message: '请填写处理结论', trigger: 'blur' },
    { max: 255, message: '处理结论不能超过 255 个字符', trigger: 'blur' }
  ]
}

function openHandle(row) {
  current.value = row
  form.status = 1
  form.handleResult = ''
  form.offShelf = true
  handleVisible.value = true
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function submitHandle() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await handleReport(current.value.id, {
      status: form.status,
      handleResult: form.handleResult,
      // 驳回时没必要传下架，明确置 false 避免后端误解
      offShelf: form.status === 1 ? form.offShelf : false
    })
    ElMessage.success(form.status === 1 ? '已处理，商品处置完成' : '已驳回')
    handleVisible.value = false
    refresh()
  } finally {
    submitting.value = false
  }
}

function refresh() {
  loadList()
  loadCounts()
}

onMounted(() => {
  loadList()
  loadCounts()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 20px 16px;
  margin-bottom: 16px;
}

.tab-badge {
  margin-left: 6px;
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
  width: 48px;
  height: 48px;
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
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
}

.cell-title:hover {
  color: var(--el-color-primary);
}

.cell-sub {
  margin-top: 2px;
  font-size: 11px;
}

.reason-text {
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-regular);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px 0;
}

.handle-target {
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.target-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.target-reason {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--app-border);
  color: var(--app-text-regular);
  line-height: 1.7;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.form-tip.inline {
  margin: 0 0 0 10px;
  display: inline;
}
</style>
