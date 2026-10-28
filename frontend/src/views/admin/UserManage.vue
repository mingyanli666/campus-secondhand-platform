<template>
  <div class="user-manage">
    <!-- 筛选 -->
    <div class="filter-bar app-card">
      <el-input
        v-model.trim="query.keyword"
        placeholder="搜索账号 / 昵称 / 手机号"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
        @clear="reload"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px" @change="reload">
        <el-option label="普通用户" value="USER" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>

      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
        <el-option label="正常" :value="1" />
        <el-option label="已禁用" :value="0" />
      </el-select>

      <el-button type="primary" @click="reload">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatar">
                {{ (row.nickname || row.username).charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <div class="user-nick">
                  {{ row.nickname || '未设置昵称' }}
                  <el-tag v-if="row.role === 'ADMIN'" size="small" type="danger" effect="plain">
                    管理员
                  </el-tag>
                </div>
                <div class="text-secondary user-name">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="手机号" width="130">
          <template #default="{ row }">
            <span class="text-secondary">{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="学校/区域" width="140">
          <template #default="{ row }">
            <span class="text-secondary text-ellipsis">{{ row.campus || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="信誉分" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ low: row.creditScore < 60 }">{{ row.creditScore }}</span>
          </template>
        </el-table-column>

        <el-table-column label="发布商品" width="100" align="center">
          <template #default="{ row }">
            <el-link
              v-if="row.productCount > 0"
              type="primary"
              :underline="false"
              @click="goUserProducts(row)"
            >
              {{ row.productCount }} 件
            </el-link>
            <span v-else class="text-secondary">0 件</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="150">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="最后登录" width="150">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.lastLoginTime) || '从未登录' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              size="small"
              type="danger"
              plain
              :disabled="row.id === myId"
              @click="handleToggleStatus(row, 0)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              plain
              @click="handleToggleStatus(row, 1)"
            >
              启用
            </el-button>

            <el-button size="small" type="warning" plain @click="handleResetPassword(row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的用户" />
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

    <!-- 重置密码结果 -->
    <el-dialog v-model="pwdVisible" title="密码已重置" width="420px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="新密码只显示这一次"
        description="关闭后无法再次查看。请立即复制并转告该用户，建议其登录后自行修改。"
        class="pwd-alert"
      />

      <div class="pwd-box">
        <span class="pwd-value">{{ newPassword }}</span>
        <el-button type="primary" plain size="small" :icon="CopyDocument" @click="copyPassword">
          复制
        </el-button>
      </div>

      <template #footer>
        <el-button type="primary" @click="pwdVisible = false">我已记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { adminPageUsers, resetUserPassword, updateUserStatus } from '@/api/admin'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const myId = computed(() => userStore.userInfo?.id)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  role: null,
  status: null
})

async function loadList() {
  loading.value = true
  try {
    const data = await adminPageUsers(query, 'admin:user:list')
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.current = 1
  loadList()
}

function resetQuery() {
  query.keyword = ''
  query.role = null
  query.status = null
  reload()
}

function goUserProducts(row) {
  // 跳转到商品管理并按卖家筛选需要额外参数，这里先用关键词搜昵称近似实现
  router.push({ path: '/admin/product', query: { keyword: row.nickname } })
}

async function handleToggleStatus(row, status) {
  const action = status === 1 ? '启用' : '禁用'
  const tip =
    status === 1
      ? `确定要启用「${row.nickname}」的账号吗？`
      : `禁用后「${row.nickname}」将无法登录，其已上架的商品仍会展示。确定禁用吗？`

  try {
    await ElMessageBox.confirm(tip, `${action}账号`, { type: 'warning' })
  } catch {
    return
  }

  await updateUserStatus(row.id, { status })
  ElMessage.success(`已${action}`)
  loadList()
}

const pwdVisible = ref(false)
const newPassword = ref('')

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定重置「${row.nickname}」的密码吗？重置后原密码立即失效。`,
      '重置密码',
      { type: 'warning' }
    )
  } catch {
    return
  }

  newPassword.value = await resetUserPassword(row.id)
  pwdVisible.value = true
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(newPassword.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('浏览器不允许自动复制，请手动选中复制')
  }
}

onMounted(loadList)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.table-card {
  padding: 4px 0 16px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-nick {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}

.user-name {
  font-size: 12px;
  margin-top: 2px;
}

.low {
  color: #ef4444;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px 0;
}

.pwd-alert {
  margin-bottom: 16px;
}

.pwd-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: #f8fafc;
  border: 1px dashed var(--app-border);
  border-radius: 8px;
}

.pwd-value {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--el-color-primary);
  user-select: all;
}
</style>
