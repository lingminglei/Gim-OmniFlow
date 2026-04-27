import service from '@/utils/request'

/**
 * 获取文件列表相关接口
 */
export const getNewsTop10 = (param) => {
  return service({
    method: 'get',
    url: '/api/news/getListTop10',
    params: param // ✅ 正确带上参数
  })
}