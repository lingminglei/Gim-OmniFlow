import service from '@/utils/request'

/**
 * 获取文件列表相关接口
 */
export const createPrompt = (param) => {
  return service({
    method: 'post',
    url: '/api/ai/video/createPrompt',
    data: param // ✅ 正确带上参数
  })
}

/**
 * 文生成视频
 */
export const generatorVideo = (param) => {
  return service({
    method: 'post',
    url: '/api/ai/video/createVideoV2',
    data: param
  })
}

/**
 * 文生视频：按照分片进行生成视频
 */
export const generatorVideoPro = (param) => {
  return service({
    method: 'post',
    url: '/api/ai/video/createVideoBySlice',
    data: param
  })
}

/**
 * 查询视频生成状态
 */
export const queryVideoResult2 = (param) => {
  return service({
    method: 'get',
    url: '/api/ai/video/queryResult',
    params: param
  })
}


/**
 * 图片生成
 */
export const generatorImage = (param) => {
  return service({
    method: 'post',
    // url: '/api/ai/video/textToImage',
    url: '/api/ai/video/textToImageV2',
    data: param
  })
}
/**
 * 
 * @param {MQ 改造} param 
 * @returns 
 */
export const generatorImageV2 = (param) => {
  return service({
    method: 'post',
    url: '/api/ai/video/textToImageV2',
    data: param
  })
}

/**
 * 查询图片生成
 */
export const queryResultTextToImage = (param) => {
  return service({
    method: 'get',
    // url: '/api/ai/video/queryResultTextToImage',
    url: '/api/ai/video/queryResultTextToImageV2',
    params: param
  })
}
/**
 * 
 * @param {MQ 改造} param 
 * @returns 
 */
export const queryResultTextToImageV2 = (param) => {
  return service({
    method: 'post',
    url: '/api/ai/video/textToImageV2',
    data: param
  })
}

/**
 * 分片镜头提示词生成
 */
export const generatorSlicePrompt = (param) => {
  return service({
    method: 'post',
    url: '/api/ai/video/createPromptV2',
    data: param
  })
}

/**
 * 获取模型配置列表
 */
export const getAiModelList = (param) => {
  return service({
    method: 'get',
    url: '/api/ai/models/list',
    data: param
  })
}
