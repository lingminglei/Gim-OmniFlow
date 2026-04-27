import service from '@/utils/request'

/**
 * 获取文件列表相关接口
 */
export const getFileListByPath = (param) => {
  return service({
    method: 'get',
    url: '/api/file/getfilelist',
    params: param // ✅ 正确带上参数
  })
}

/**
 * 删除文件
 * @returns 
 */
export const deleteFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/deletefile',
        data: param
    })
}

/**
 * 预览文件
 * @returns
 */
export const getFilePreview = (param) => {
    return service({
        method: 'get',
        url: '/api/filetransfer/preview',
        params: param
    })
}

/**
 * 知识库：预览文件
 */
export const getFilePreviewKnowledge = (param) => {
    return service({
        method: 'get',
        url: '/api/vectorFileInfo/preview',
        params: param
    })
}


/**
 * 文件编辑
 * @returns 
 */
export const modifyFileContent = (param) => {
    return service({
        method: 'post',
        url: '/api/file/update',
        data: param
    })
}

/**
 * 修改文件名称
 * @returns 
 */
export const renameFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/renamefile',
        data: param
    })
}

/**
 *  创建文件夹
 * @returns 
 */
export const createFold = (param) => {
    return service({
        method: 'post',
        url: '/api/file/createFold',
        data: param
    })
}
/**
 * 
 * @returns 获取树形文件列表
 */
export const getFoldTree = (param) => {
    return service({
        method: 'get',
        url: '/api/file/getfiletree',
        data: param
    })
}
/**
 * 
 * @returns 移动文件夹
 */
export const moveFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/movefile',
        data: param
    })
}
/**
 * 
 * @returns 批量移动文件
 */
export const batchMoveFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/batchmovefile',
        data: param
    })
}
/**
 * 
 * @returns 复制文件
 */
export const copyFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/copyfile',
        data: param
    })
}

export const getRecoveryFile = () => {
    return service({
        method: 'get',
        url: '/api/recoveryFile/list'
    })
}



// 回收站文件删除
export const deleteRecoveryFile = (param) => {
    return service({
        method: 'post',
        url: '/api/recoveryFile/deleterecoveryfile',
        data: param
    })
}

export const restoreRecoveryFile = (p) => {
    return service({
        method: 'post',
        url: '/api/recoveryFile/restorefile',
        data: p
    })
}


export const getFileListByType = () => {
    return service({
        method: 'get',
        url: '/file/selectfilebyfiletype'
    })
}

export const getMyShareFileList = () => {
    return service({
        method: 'get',
        url: '/share/shareList'
    })
}
export const searchFile = () => {
    return service({
        method: 'get',
        url: '/file/search'
    })
}

/**
 * 普通文件上传
 */
export const uploadFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/upload',
        data: param
    })
}

/**
 * 获取文件内容
 */
export const getFileDetail = (param) => {
    return service({
        method: 'get',
        url: '/api/file/detail',
        params: param
    })
}


// 批量删除文件
export const batchDeleteFile = (param) => {
    return service({
        method: 'post',
        url: '/api/file/batchdeletefile',
        data: param
    })
}