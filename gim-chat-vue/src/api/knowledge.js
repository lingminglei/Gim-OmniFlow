import service from '@/utils/request'

/**
 * 查询知识库列表
 */
export const getFileListByPath = (param) => {
  return service({
    method: 'get',
    url: '/api/knowledge/queryKnowledgeList',
    params: param // ✅ 正确带上参数
  })
}

/**
 * 查询知识库文件列表
 */
export const getFileListByType = (param) => {
  return service({
    method: 'get',
    url: '/api/knowledge/queryKnowledgeFileList',
    params: param
  })
}


/**
 * 创建知识库
 */
export const createKnowledge = (data) => {
  return service({
    method: 'post',
    url: '/api/knowledge/create',
    data // ✅ 正确带上数据
  })
}

/**
 * 删除知识库
 */
export const deleteKnowledge = (data) => {
  return service({
    method: 'get',
    url: '/api/knowledge/delete',
    params: data
  })
}

/**
 * 知识库信息编辑
 */
export const handleEditKnowledge = (data) => {
  return service({
    method: 'post',
    url: '/api/knowledge/edit',
    data
  })
}

/**
 * 知识库文件删除
 */
export const deleteKnowledgeFile = (data) => {
  return service({
    method: 'get',
    url: '/api/knowledge/deleteFile',
    params: data
  })
}