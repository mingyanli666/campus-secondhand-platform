/**
 * 提交信息规范（Conventional Commits）。
 *
 * 格式：<type>(<scope>): <subject>
 * 例：feat(web): 请求层竞态治理
 *     fix(api): 修复禁用用户后旧 token 仍可用
 *     docs: 补充 Redis 缓存实现讲解
 *
 * 为什么放在仓库根目录而不是 frontend/？
 * 提交规范管的是整个仓库的提交，后端（backend/）的提交同样要遵守，
 * 所以它属于仓库级工具，前端专属的 eslint / prettier 才放在 frontend/。
 */
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // 提交信息是中文，这两条规则对中文不适用：
    // subject-case 会要求句子大小写（中文没有大小写）
    // subject-full-stop 会禁止句号结尾（中文习惯上可以用）
    'subject-case': [0],
    'subject-full-stop': [0],

    // 首行长度上限。commitlint 按显示宽度计算，一个汉字算 2 列，
    // 所以 100 大约相当于「50 个汉字 + 前缀」，够用且逼着把话说短。
    'header-max-length': [2, 'always', 100],

    // 正文和页脚的中文没有按 100 列折行的习惯，不强制
    'body-max-line-length': [0],
    'footer-max-line-length': [0]
  }
}
