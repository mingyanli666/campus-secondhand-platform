import request from '@/utils/request'

/** 商品列表 / 检索（公开） */
export function pageProducts(params) {
  return request({
    url: '/product/page',
    method: 'get',
    params
  })
}

/** 商品详情 */
export function getProduct(id) {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}

/** 发布商品 */
export function createProduct(data) {
  return request({
    url: '/product',
    method: 'post',
    data
  })
}

/** 编辑商品 */
export function updateProduct(id, data) {
  return request({
    url: `/product/${id}`,
    method: 'put',
    data
  })
}

/** 删除商品 */
export function deleteProduct(id) {
  return request({
    url: `/product/${id}`,
    method: 'delete'
  })
}

/** 下架商品 */
export function offShelfProduct(id) {
  return request({
    url: `/product/${id}/off-shelf`,
    method: 'put'
  })
}

/** 重新提交审核 */
export function resubmitProduct(id) {
  return request({
    url: `/product/${id}/resubmit`,
    method: 'put'
  })
}

/** 我的发布 */
export function myProducts(params) {
  return request({
    url: '/product/mine',
    method: 'get',
    params
  })
}
