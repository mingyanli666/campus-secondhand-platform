<template>
  <div class="app-container profile-page">
    <h2>个人中心</h2>

    <el-row :gutter="20">
      <!-- 左侧：头像与概要 -->
      <el-col :xs="24" :md="8">
        <div class="summary app-card">
          <el-upload
            class="avatar-upload"
            :show-file-list="false"
            accept="image/*"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
          >
            <el-avatar :size="96" :src="userStore.userInfo?.avatar">
              {{ userStore.nickname.charAt(0) }}
            </el-avatar>
            <div class="avatar-tip">点击更换头像</div>
          </el-upload>

          <div class="summary-name">{{ userStore.userInfo?.nickname }}</div>
          <div class="text-secondary">{{ userStore.userInfo?.username }}</div>

          <div class="summary-stats">
            <div class="stat">
              <div class="stat-value">{{ userStore.userInfo?.creditScore ?? 100 }}</div>
              <div class="stat-label">信誉分</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ roleText }}</div>
              <div class="stat-label">账号类型</div>
            </div>
          </div>

          <div class="summary-time text-secondary">
            注册于 {{ formatTime(userStore.userInfo?.createTime) }}
          </div>
        </div>
      </el-col>

      <!-- 右侧：表单 -->
      <el-col :xs="24" :md="16">
        <div class="app-card form-card">
          <el-tabs v-model="activeTab">
            <!-- 基本资料 -->
            <el-tab-pane label="基本资料" name="profile">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="88px"
                class="form-body"
              >
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model.trim="profileForm.nickname" maxlength="20" show-word-limit />
                </el-form-item>

                <el-form-item label="手机号" prop="phone">
                  <el-input
                    v-model.trim="profileForm.phone"
                    maxlength="11"
                    placeholder="选填，方便买家联系"
                  />
                </el-form-item>

                <el-form-item label="邮箱" prop="email">
                  <el-input v-model.trim="profileForm.email" placeholder="选填" />
                </el-form-item>

                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="profileForm.gender">
                    <el-radio :value="0">保密</el-radio>
                    <el-radio :value="1">男</el-radio>
                    <el-radio :value="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="学校/区域" prop="campus">
                  <el-input
                    v-model.trim="profileForm.campus"
                    maxlength="100"
                    placeholder="如 XX大学 东校区"
                  />
                </el-form-item>

                <el-form-item label="个人简介" prop="intro">
                  <el-input
                    v-model="profileForm.intro"
                    type="textarea"
                    :rows="3"
                    maxlength="255"
                    show-word-limit
                    placeholder="介绍一下自己，让买家更信任你"
                  />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveProfile">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 修改密码 -->
            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="88px"
                class="form-body"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model.trim="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model.trim="pwdForm.newPassword" type="password" show-password />
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model.trim="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" :loading="changing" @click="submitPassword">
                    确认修改
                  </el-button>
                </el-form-item>
                <el-alert
                  type="info"
                  :closable="false"
                  show-icon
                  title="修改密码后当前登录状态会失效，需要重新登录"
                />
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword, updateProfile } from '@/api/user'
import { uploadAvatar } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('profile')
const saving = ref(false)
const changing = ref(false)

const profileFormRef = ref()
const pwdFormRef = ref()

const roleText = computed(() => (userStore.isAdmin ? '管理员' : '普通用户'))

const profileForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  gender: 0,
  campus: '',
  intro: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  nickname: [
    { required: true, message: '请填写昵称', trigger: 'blur' },
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function beforeAvatarUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('头像必须是图片文件')
    return false
  }
  if (file.size / 1024 / 1024 >= 10) {
    ElMessage.error('头像不能超过 10MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options) {
  const url = await uploadAvatar(options.file)
  userStore.setUserInfo({ avatar: url })
  ElMessage.success('头像已更新')
  // 返回地址而不是手动调 options.onSuccess：
  // el-upload 会把 Promise 的 resolve 值交给 onSuccess，
  // 两者都做会导致回调触发两次、第二次把结果覆盖成 undefined
  return url
}

async function saveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data = await updateProfile(profileForm)
    userStore.setUserInfo(data)
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

async function submitPassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  changing.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    // 密码变了，旧 token 继续用会有隐患，直接退出让用户重新登录
    userStore.clearLogin()
    router.push('/login')
  } finally {
    changing.value = false
  }
}

onMounted(async () => {
  // 每次都从后端拉最新数据，避免本地缓存过期
  const data = await userStore.loadProfile()
  Object.assign(profileForm, {
    nickname: data.nickname || '',
    phone: data.phone || '',
    email: data.email || '',
    gender: data.gender ?? 0,
    campus: data.campus || '',
    intro: data.intro || ''
  })
})
</script>

<style scoped>
.profile-page {
  padding: 24px 16px 48px;
}

.profile-page h2 {
  margin: 0 0 18px;
  font-size: 20px;
}

.summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 20px 24px;
}

.avatar-upload {
  text-align: center;
  cursor: pointer;
}

.avatar-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.summary-name {
  margin-top: 14px;
  font-size: 17px;
  font-weight: 600;
}

.summary-stats {
  display: flex;
  gap: 40px;
  margin: 22px 0 16px;
}

.stat {
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.stat-label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.summary-time {
  font-size: 12px;
}

.form-card {
  padding: 8px 24px 16px;
}

.form-body {
  max-width: 460px;
  margin-top: 12px;
}
</style>
