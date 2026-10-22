import request from '@/utils/request'

/** 收藏商品 */
export function addFavorite(productId) {
  return request({
    url: `/favorite/${productId}`,
    method: 'post'
  })
}

/** 取消收藏 */
export function removeFavorite(productId) {
  return request({
    url: `/favorite/${productId}`,
    method: 'delete'
  })
}

/** 我的收藏列表 */
export function pageFavorites(params) {
  return request({
    url: '/favorite/page',
    method: 'get',
    params
  })
}
