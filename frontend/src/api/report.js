import request from '@/utils/request'

/** 举报商品 */
export function createReport(data) {
  return request({
    url: '/report',
    method: 'post',
    data
  })
}

/** 我提交的举报 */
export function pageMyReports(params) {
  return request({
    url: '/report/mine',
    method: 'get',
    params
  })
}
