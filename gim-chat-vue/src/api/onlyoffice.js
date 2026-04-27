// onlyoffice 相关接口
import service from '@/utils/request'

/**
 * 创建文档
 */
export const createOfficeFile = (param) => {
  return service({
    method: 'post',
    url: '/api/file/createFile',
    data: param // ✅ 正确带上参数
  })
}

// 编辑文档
export const editOfficeFile = (p) => post('/office/editofficefile', p)
// 查看文档
export const previewOfficeFile = (p) => post('/office/previewofficefile', p)
