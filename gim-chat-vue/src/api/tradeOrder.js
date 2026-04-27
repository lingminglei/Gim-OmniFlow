import service from '@/utils/request'

/**
 * 查询当前用户的支付订单列表。
 * 后端已升级为分页接口，这里固定透传 pageNo 和 pageSize。
 */
export const getPayOrderList = (pageNo, pageSize) => {
  return service({
    method: 'get',
    url: '/api/pay/tradeOrder/getPayOrderList',
    params: {
      pageNo,
      pageSize
    }
  })
}

/**
 * 根据订单号查询订单详情。
 */
export const getPayOrderDetail = (tradeOrderId) => {
  return service({
    method: 'get',
    url: '/api/pay/tradeOrder/getPayOrderDetail',
    params: {
      tradeOrderId
    }
  })
}
