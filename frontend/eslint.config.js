import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'
import prettier from 'eslint-config-prettier'

/**
 * ESLint 扁平配置（ESLint 9 起使用，不再需要 .eslintrc）。
 *
 * 配置顺序有讲究，后面的会覆盖前面的：
 *   1. ignores          —— 构建产物和依赖目录不检查
 *   2. js.recommended   —— JS 通用规则
 *   3. pluginVue        —— Vue 3 推荐规则（模板 + script）
 *   4. 自定义覆盖        —— 按项目实际情况放宽或加严
 *   5. prettier         —— 必须放最后：关掉所有与 Prettier 冲突的格式类规则，
 *                          让「格式」归 Prettier 管、「代码质量」归 ESLint 管
 */
export default [
  {
    ignores: ['dist/**', 'coverage/**', 'node_modules/**']
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser
      }
    },
    rules: {
      // 组件文件名用单词就够了（ProductCard、Detail），
      // 强制多单词只会逼出 ProductCardComponent 这种更差的名字
      'vue/multi-word-component-names': 'off',

      // 未使用变量：允许 _ 开头的参数（约定俗成的「这个参数我知道但用不到」）
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],

      // 允许 warn / error，其余 console 提示出来但不报错 ——
      // 排查问题时临时打日志不该阻塞提交
      'no-console': ['warn', { allow: ['warn', 'error'] }],

      // 下面几条是 eslint-plugin-vue 的格式类规则，与 Prettier 重叠且容易互相打架，
      // 统一关掉，格式交给 Prettier 负责
      'vue/attributes-order': 'off',
      'vue/html-self-closing': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/html-indent': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/first-attribute-linebreak': 'off',

      // 业务上确实需要放宽的
      'vue/require-default-prop': 'off',
      'vue/no-v-html': 'off'
    }
  },

  {
    // 配置文件与单元测试跑在 Node 里，需要 Node 全局变量（process、__dirname 等）
    files: ['*.config.js', 'vite.config.js', 'src/**/*.spec.js'],
    languageOptions: {
      globals: {
        ...globals.node
      }
    }
  },

  prettier
]
