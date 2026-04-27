import service from '@/utils/request'

/**
 * 处理用户登录
 */
export const handleLogin = (data) => {
  return service({
    method: 'post',
    url: '/api/user/doLogin',
    data
  })
}

/**
 * 处理用户登出
 */
export const handleLogout = (params) => {
  return service({
    method: 'get',
    url: '/api/user/logout',
    params: params
  })
}

/**
 * 获取图片验证码
 */
export const getPicturCode = (params) => {
  return service({
    method: 'get',
    url: '/api/commonApi/captcha',
    params: params
  })
}

/**
 * 用户信息页面：用户信息查询
 */
export const getUserRanksInfo = (params) => {
  return service({
    method: 'get',
    url: '/api/user/getRanksInfo',
    params: params
  })
}

/**
 * 获取支付Token
 */
export const getPayToken = (params) => {
  return service({
    method: 'get',
    url: '/api/user/getPayToken',
    params: params
  })
} 