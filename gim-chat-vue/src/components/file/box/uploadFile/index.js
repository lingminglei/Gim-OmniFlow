import Vue from 'vue'
import UploadFile from './Box.vue'

const UploadFileConstructor = Vue.extend(UploadFile)

let uploadFileInstance = null

const showUploadFileBox = ({ params = {}, uploadWay = 0, serviceEl = null, callType = '' }) => {
  return new Promise((resolve) => {
    // 1. 每次都创建新实例（避免复用导致 Promise 不生效）
    const instance = new UploadFileConstructor({
      el: document.createElement('div')
    })

    // 2. 设置属性
    instance.params = params
    instance.uploadWay = uploadWay
    instance.serviceEl = serviceEl
    instance.callType = callType

    // 3. 设置回调
    instance.callback = (res) => {
      resolve(res)
      document.body.removeChild(instance.$el)
      instance.$destroy()
      uploadFileInstance = null // 清空引用
    }

    // 4. 渲染挂载
    document.body.appendChild(instance.$el)

    // 存一份引用（可选）
    uploadFileInstance = instance
  })
}

export default showUploadFileBox
