import request from '@/utils/request'

/** 查询商品的留言列表（两级结构，公开接口） */
export function listComments(productId) {
  return request({
    url: '/comment/list',
    method: 'get',
    params: { productId }
  })
}

/** 发布留言（不传 parentId）或回复（传 parentId） */
export function createComment(data) {
  return request({
    url: '/comment',
    method: 'post',
    data
  })
}

/** 删除留言 */
export function deleteComment(id) {
  return request({
    url: `/comment/${id}`,
    method: 'delete'
  })
}
