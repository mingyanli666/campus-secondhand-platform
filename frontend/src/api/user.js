import request from '@/utils/request'

/** 注册 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/** 登录，返回 { token, userInfo } */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/** 退出登录（后端无状态，这里主要用于记录日志/扩展） */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/** 获取当前登录用户信息 */
export function getProfile() {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

/** 修改个人资料 */
export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

/** 修改密码 */
export function changePassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

/** 上传头像 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
