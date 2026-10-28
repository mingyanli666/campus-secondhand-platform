import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：
    // 模板里出现 <el-table> 才会把 Table 组件和它的样式打进产物，
    // 不再需要 main.js 里 app.use(ElementPlus) 全量注册。
    // 注意：图标不走这里（图标名大量来自数据库，见 src/utils/icons.js 的说明）。
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      // 开发环境把 /api 代理到后端，避免本地跨域配置反复折腾
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 图片也走代理，前端写 /uploads/xx.jpg 即可
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 这里必须用「函数形式」而不是 { element: ['element-plus'] } 这种包名形式。
        // 包名形式会把整包当成一个 entry point，entry 的导出无法被 tree-shaking
        // 裁剪，结果 Element Plus 和图标全量都被保留，按需引入等于白做。
        // 函数形式只负责「把已经进入产物图的模块归到哪个 chunk」，不影响裁剪。
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('element-plus')) return 'element'
          if (id.includes('@vue/') || id.includes('/vue/') || id.includes('pinia')) {
            return 'vue'
          }
          return 'vendor'
        }
      }
    }
  }
})
