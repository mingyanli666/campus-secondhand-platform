import request from '@/utils/request'

/** 我的通知列表 */
export function pageNotifications(params) {
  return request({
    url: '/notification/page',
    method: 'get',
    params
  })
}

/** 未读数量 */
export function getUnreadCount() {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

/** 标记单条已读 */
export function readNotification(id) {
  return request({
    url: `/notification/${id}/read`,
    method: 'put'
  })
}

/** 全部标记已读 */
export function readAllNotifications() {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}
