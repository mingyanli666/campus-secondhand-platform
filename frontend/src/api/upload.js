import request from '@/utils/request'

/**
 * 上传商品图片，返回可直接访问的地址，如 /uploads/product/2026/10/xxx.png
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
