import service from '@/utils/request'

/**
 * 画布版本保存
 */
export const saveCanvas = (param) => {
  return service({
    method: 'post',
    url: '/api/canvasData/save',
    data: param // ✅ 正确带上参数
  })
}
