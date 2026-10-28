<template>
  <div class="auth-page">
    <div class="auth-box app-card">
      <div class="auth-header">
        <el-icon :size="32" color="var(--el-color-primary)"><Cellphone /></el-icon>
        <h2>欢迎回来</h2>
        <p class="text-secondary">登录后可发布闲置数码、收藏商品、与卖家在线交流</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        label-position="top"
        @keyup.enter="handleSubmit"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入账号" clearable>
            <template #prefix
              ><el-icon><User /></el-icon
            ></template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model.trim="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          >
            <template #prefix
              ><el-icon><Lock /></el-icon
            ></template>
          </el-input>
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          登录
        </el-button>

        <div class="auth-footer">
          <span class="text-secondary">还没有账号？</span>
          <el-link type="primary" :underline="false" @click="router.push('/register')">
            立即注册
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度为 4-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.doLogin(form)
    ElMessage.success('登录成功')
    // 登录前被拦截的页面，登录后原路返回
    router.push(route.query.redirect || '/')
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
  background: linear-gradient(135deg, #eef4ff 0%, #f5f7fa 45%, #e8f0fe 100%);
}

.auth-box {
  width: 400px;
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
