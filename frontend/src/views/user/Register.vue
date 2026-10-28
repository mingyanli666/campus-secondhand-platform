<template>
  <div class="auth-page">
    <div class="auth-box app-card">
      <div class="auth-header">
        <el-icon :size="32" color="var(--el-color-primary)"><Cellphone /></el-icon>
        <h2>创建账号</h2>
        <p class="text-secondary">注册后即可发布闲置数码、收藏心仪好物</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" placeholder="4-20 位字母或数字" clearable>
            <template #prefix
              ><el-icon><User /></el-icon
            ></template>
          </el-input>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model.trim="form.nickname" placeholder="别人看到的名字" clearable>
            <template #prefix
              ><el-icon><Star /></el-icon
            ></template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model.trim="form.password"
            type="password"
            placeholder="6-20 位"
            show-password
          >
            <template #prefix
              ><el-icon><Lock /></el-icon
            ></template>
          </el-input>
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model.trim="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          >
            <template #prefix
              ><el-icon><Lock /></el-icon
            ></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="agreement">
          <el-checkbox v-model="form.agreement">
            我已阅读并同意《平台使用规范》，承诺不发布虚假信息
          </el-checkbox>
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          注册
        </el-button>

        <div class="auth-footer">
          <span class="text-secondary">已有账号？</span>
          <el-link type="primary" :underline="false" @click="router.push('/login')">去登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  agreement: false
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateAgreement = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请先阅读并同意平台使用规范'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{4,20}$/,
      message: '账号为 4-20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  agreement: [{ validator: validateAgreement, trigger: 'change' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.doRegister({
      username: form.username,
      nickname: form.nickname,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px 0;
  background: linear-gradient(135deg, #eef4ff 0%, #f5f7fa 45%, #e8f0fe 100%);
}

.auth-box {
  width: 420px;
  padding: 40px 36px 32px;
}

.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-header h2 {
  margin: 12px 0 6px;
  font-size: 22px;
}

.auth-header p {
  margin: 0;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.auth-footer {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
}
</style>
