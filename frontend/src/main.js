import { createApp } from 'vue'
import { createPinia } from 'pinia'

import '@/styles/index.css'

import App from './App.vue'
import router from './router'
import { icons } from '@/utils/icons'

// Element Plus 组件不再全量注册，改由 vite.config.js 里的
// unplugin-vue-components 在模板中出现 <el-xxx> 时按需引入。
// 但 ElMessage / ElMessageBox 是在 <script> 里手动调用的函数，
// 不走模板解析，所以它们各自的样式要在这里显式引入。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

const app = createApp(App)

// 只注册项目真正用到的图标，清单见 utils/icons.js
for (const [name, component] of Object.entries(icons)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)

app.mount('#app')
