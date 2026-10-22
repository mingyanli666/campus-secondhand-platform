import request from '@/utils/request'

/** 卖家公开资料 */
export function getSellerProfile(id) {
  return request({
    url: `/seller/${id}`,
    method: 'get'
  })
}

/** 卖家的在售商品 */
export function pageSellerProducts(id, params) {
  return request({
    url: `/seller/${id}/products`,
    method: 'get',
    params
  })
}
