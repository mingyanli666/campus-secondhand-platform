import request from '@/utils/request'

/** 查询启用中的分类列表（公开接口） */
export function listCategories() {
  return request({
    url: '/category/list',
    method: 'get'
  })
}
