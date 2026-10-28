import request from '@/utils/request'

/** 平台统计数据 */
export function getDashboardStats() {
  return request({
    url: '/admin/dashboard/stats',
    method: 'get'
  })
}

/** 商品列表（含所有状态） */
export function adminPageProducts(params, requestKey) {
  return request({
    url: '/admin/product/page',
    method: 'get',
    params,
    requestKey
  })
}

/** 审核商品 action: 1通过 2驳回 */
export function auditProduct(id, data) {
  return request({
    url: `/admin/product/${id}/audit`,
    method: 'post',
    data
  })
}

/** 强制下架 */
export function forceOffShelf(id, data) {
  return request({
    url: `/admin/product/${id}/off-shelf`,
    method: 'put',
    data
  })
}

/** 删除商品 */
export function adminDeleteProduct(id) {
  return request({
    url: `/admin/product/${id}`,
    method: 'delete'
  })
}

/** 商品审核历史 */
export function getAuditRecords(id) {
  return request({
    url: `/admin/product/${id}/audit-records`,
    method: 'get'
  })
}

/* ==================== 用户管理 ==================== */

/** 用户列表 */
export function adminPageUsers(params, requestKey) {
  return request({
    url: '/admin/user/page',
    method: 'get',
    params,
    requestKey
  })
}

/** 启用/禁用用户 status: 1启用 0禁用 */
export function updateUserStatus(id, data) {
  return request({
    url: `/admin/user/${id}/status`,
    method: 'put',
    data
  })
}

/** 重置用户密码，返回新密码 */
export function resetUserPassword(id) {
  return request({
    url: `/admin/user/${id}/reset-password`,
    method: 'put'
  })
}

/* ==================== 分类维护 ==================== */

/** 分类列表（含停用） */
export function adminListCategories() {
  return request({
    url: '/admin/category/list',
    method: 'get'
  })
}

/** 新增分类 */
export function createCategory(data) {
  return request({
    url: '/admin/category',
    method: 'post',
    data
  })
}

/** 编辑分类 */
export function updateCategory(id, data) {
  return request({
    url: `/admin/category/${id}`,
    method: 'put',
    data
  })
}

/** 删除分类 */
export function deleteCategory(id) {
  return request({
    url: `/admin/category/${id}`,
    method: 'delete'
  })
}

/** 启用/停用分类 */
export function updateCategoryStatus(id, data) {
  return request({
    url: `/admin/category/${id}/status`,
    method: 'put',
    data
  })
}

/* ==================== 举报处理 ==================== */

/** 举报列表（待处理优先） */
export function adminPageReports(params, requestKey) {
  return request({
    url: '/admin/report/page',
    method: 'get',
    params,
    requestKey
  })
}

/** 处理举报 status: 1成立 2驳回 */
export function handleReport(id, data) {
  return request({
    url: `/admin/report/${id}/handle`,
    method: 'put',
    data
  })
}
