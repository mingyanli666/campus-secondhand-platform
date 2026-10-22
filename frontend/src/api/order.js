import request from '@/utils/request'

/** 下单 */
export function createOrder(data) {
  return request({
    url: '/order',
    method: 'post',
    data
  })
}

/** 我的订单 role: BUY我买到的 / SELL我卖出的 */
export function pageOrders(params) {
  return request({
    url: '/order/page',
    method: 'get',
    params
  })
}

/** 卖家确认订单 */
export function confirmOrder(id) {
  return request({
    url: `/order/${id}/confirm`,
    method: 'put'
  })
}

/** 取消订单 */
export function cancelOrder(id, data) {
  return request({
    url: `/order/${id}/cancel`,
    method: 'put',
    data
  })
}

/** 买家确认完成 */
export function finishOrder(id) {
  return request({
    url: `/order/${id}/finish`,
    method: 'put'
  })
}
