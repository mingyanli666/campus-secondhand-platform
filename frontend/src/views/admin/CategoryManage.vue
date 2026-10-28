<template>
  <div class="category-manage">
    <div class="head-bar app-card">
      <div>
        <h3>分类管理</h3>
        <p class="text-secondary">
          排序值越小越靠前。停用后发布商品时不再显示该分类，已发布的商品不受影响。
        </p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list" stripe row-key="id">
        <el-table-column label="排序" width="80" align="center">
          <template #default="{ row }">
            <span class="sort-value">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <el-table-column label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-icon :size="20" color="var(--el-color-primary)">
              <component :is="iconOf(row.icon)" />
            </el-icon>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="分类名称" min-width="160" />

        <el-table-column label="商品数" width="110" align="center">
          <template #default="{ row }">
            <el-link
              v-if="row.productCount > 0"
              type="primary"
              :underline="false"
              @click="goProducts(row)"
            >
              {{ row.productCount }} 件
            </el-link>
            <span v-else class="text-secondary">0 件</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="启用"
              inactive-text="停用"
              @change="(val) => handleToggleStatus(row, val ? 1 : 0)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
            <el-tooltip
              :disabled="row.productCount === 0"
              content="该分类下还有商品，无法删除"
              placement="top"
            >
              <span>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :disabled="row.productCount > 0"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="还没有分类" />
        </template>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="540px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="50" show-word-limit placeholder="如：平板电脑" />
        </el-form-item>

        <el-form-item label="图标" prop="icon">
          <el-select v-model="form.icon" filterable placeholder="选择一个图标" style="width: 100%">
            <el-option v-for="name in ICON_OPTIONS" :key="name" :label="name" :value="name">
              <div class="icon-option">
                <el-icon :size="16"><component :is="name" /></el-icon>
                <span>{{ name }}</span>
              </div>
            </el-option>
            <template #prefix>
              <el-icon v-if="form.icon" :size="16">
                <component :is="iconOf(form.icon)" />
              </el-icon>
            </template>
          </el-select>
          <div class="form-tip">
            图标名必须真实存在，否则前台渲染不出来。这里只列出验证过的图标。
          </div>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="form-tip inline">越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { resolveIcon } from '@/utils/icons'
import {
  adminListCategories,
  createCategory,
  deleteCategory,
  updateCategory,
  updateCategoryStatus
} from '@/api/admin'

/**
 * 可选的图标名清单。
 * 全部是核对过 Element Plus 确实存在的图标 —— 之前吃过亏：
 * 数据库里写了个不存在的 "Gamepad"，构建不报错，页面上就是一个空白格。
 * 用下拉框限定范围，管理员就不会再填错。
 *
 * 这份清单里的图标必须同步登记到 src/utils/icons.js，
 * 否则按需引入后它们不会被注册，下拉框里会全是空白。
 */
const ICON_OPTIONS = [
  'Iphone', 'Cellphone', 'Monitor', 'Notebook', 'Cpu', 'Mouse',
  'Camera', 'Headset', 'Microphone', 'Film', 'VideoPlay', 'Watch',
  'Printer', 'Box', 'Trophy', 'Basketball', 'Football', 'Bicycle',
  'Suitcase', 'ShoppingCart', 'Wallet', 'Present', 'Discount', 'Postcard',
  'Document', 'DocumentCopy', 'Files', 'Reading', 'Grid',
  'Compass', 'Aim', 'MagicStick', 'Lightning', 'Star', 'Bell',
  'Goods', 'MoreFilled'
]

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  name: '',
  icon: 'Goods',
  sort: 0,
  status: 1
})

const rules = {
  name: [
    { required: true, message: '请填写分类名称', trigger: 'blur' },
    { max: 50, message: '分类名称不能超过 50 个字符', trigger: 'blur' }
  ],
  icon: [{ required: true, message: '请选择图标', trigger: 'change' }],
  sort: [{ required: true, message: '请填写排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/** 图标名兜底：万一历史数据里有不存在的图标，退回默认图标而不是渲染空白 */
function iconOf(name) {
  return resolveIcon(name)
}

async function loadList() {
  loading.value = true
  try {
    list.value = await adminListCategories()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  // 新增时默认排到末尾，管理员一般不用改
  form.sort = list.value.length ? Math.max(...list.value.map((c) => c.sort)) + 1 : 1
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.icon = row.icon || 'Goods'
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  form.name = ''
  form.icon = 'Goods'
  form.sort = 0
  form.status = 1
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createCategory({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row, status) {
  await updateCategoryStatus(row.id, { status })
  ElMessage.success(status === 1 ? '已启用' : '已停用')
  loadList()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.name}」吗？删除后前台将不再显示该分类。`,
      '删除分类',
      { type: 'warning', confirmButtonText: '确定删除' }
    )
  } catch {
    return
  }

  await deleteCategory(row.id)
  ElMessage.success('已删除')
  loadList()
}

function goProducts(row) {
  router.push({ path: '/admin/product', query: { categoryId: row.id } })
}

onMounted(loadList)
</script>

<style scoped>
.head-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  margin-bottom: 16px;
}

.head-bar h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.head-bar p {
  margin: 0;
  font-size: 12px;
}

.table-card {
  padding: 4px 0 16px;
}

.sort-value {
  font-family: 'Consolas', 'Monaco', monospace;
  color: var(--app-text-regular);
}

.icon-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-secondary);
}

.form-tip.inline {
  margin: 0 0 0 10px;
  display: inline;
}
</style>
