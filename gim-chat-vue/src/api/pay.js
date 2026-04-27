import service from '@/utils/request'

// 创建充值订单，identifier 由支付页传入的 payToken 承担幂等作用
export const createRechargeOrder = (data) => {
  return service({
    method: 'post',
    url: '/api/pay/recharge/tryPay',
    data
  })
}

// 确认支付并换取可展示给用户的支付链接
export const confirmRechargeOrder = (data) => {
  return service({
    method: 'post',
    url: '/api/pay/recharge/confirm',
    data
  })
}

// 根据支付订单号轮询最新订单状态
export const queryRechargeOrder = (payOrderId) => {
  return service({
    method: 'get',
    url: `/api/pay/recharge/order/${payOrderId}`
  })
}
