<template>
  <div class="main-container">
    <el-card style="width: 100%;">
      <div slot="header" class="card-header">
        <el-button type="text" icon="el-icon-arrow-left" @click="goBack">{{ text.back }}</el-button>
        <span class="header-title">{{ text.title }}</span>
        <div style="width: 50px;"></div>
      </div>

      <el-row :gutter="20">
        <el-col v-for="item in packages" :key="item.code" :span="8">
          <el-card shadow="hover" class="package-item">
            <h3>{{ item.name }}</h3>
            <div class="price">{{ text.currency }} {{ formatMoney(item.price) }}</div>
            <p>{{ item.points }} {{ text.pointsUnit }}</p>

            <div class="package-features">
              <p>
                <i class="el-icon-check"></i>
                {{ getSupportText(item) }}
              </p>
              <p>
                <i class="el-icon-check"></i>
                {{ getDiscountText(item) }}
              </p>
            </div>

            <el-button
              type="primary"
              :loading="createOrderLoading && selectedPackage && selectedPackage.code === item.code"
              @click="openPayDialog(item)"
            >
              {{ text.payNow }}
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog
      :visible.sync="payVisible"
      width="750px"
      :before-close="handleOrderDialogClose"
      custom-class="full-dialog"
      :close-on-click-modal="false"
      append-to-body
    >
      <div class="order-confirm">
        <div class="info-group">
          <p><span>{{ text.packageName }}:</span> {{ displayPackageName }}</p>
          <p><span>{{ text.payOrderId }}:</span> {{ activeOrder.payOrderId || '--' }}</p>
          <p><span>{{ text.transactionId }}:</span> {{ activeOrder.transactionId || '--' }}</p>
          <p><span>{{ text.createTime }}:</span> {{ formatDate(activeOrder.createTime) }}</p>
          <p><span>{{ text.orderState }}:</span> {{ getStateText(getOrderState(activeOrder)) }}</p>
        </div>

        <div class="price-display">
          {{ text.orderAmount }}: <span>{{ text.currency }} {{ displayAmount }}</span>
        </div>

        <div class="tip-box">
          {{ text.orderTip }}
        </div>

        <div class="channel-selector">
          <el-button
            v-for="method in payMethods"
            :key="method.key"
            :type="selectedPayMethod === method.key ? 'primary' : ''"
            @click="selectedPayMethod = method.key"
          >
            {{ method.label }}
          </el-button>
        </div>

        <div class="footer-actions">
          <span>{{ text.needPay }}: <b>{{ text.currency }} {{ displayAmount }}</b></span>
          <el-button
            type="danger"
            :loading="confirmPayLoading"
            :disabled="!activeOrder.payOrderId"
            @click="confirmToPay"
          >
            {{ text.payNow }}
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="payVisible2"
      width="750px"
      :before-close="handleQrDialogClose"
      custom-class="full-dialog"
      :close-on-click-modal="false"
      append-to-body
    >
      <div class="qr-step">
        <div class="qr-guide-box">
          <i class="el-icon-full-screen" style="font-size: 30px; color: #67c23a;"></i>
          <div class="qr-text">
            <p>{{ text.scanPrefix }}{{ currentPayMethod.qrLabel }}{{ text.scanSuffix }}</p>
            <p style="margin-top: 10px;">{{ text.scanTip }}</p>
          </div>
        </div>

        <div ref="qrcode" class="qr-code-box"></div>

        <p class="countdown-text">
          {{ text.countdown }}:
          <span>{{ timerText }}</span>
        </p>

        <div class="qr-order-info">
          <p>{{ text.payOrderId }}: {{ activeOrder.payOrderId || '--' }}</p>
          <p>{{ text.orderAmount }}: {{ text.currency }} {{ displayAmount }}</p>
        </div>

        <el-link type="primary" :underline="false" style="margin-top: 10px;">
          {{ text.contactTip }}
        </el-link>

        <div class="security-footer">
          <i class="el-icon-lock"></i>
          {{ text.securityTip }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import QRCode from 'qrcodejs2'
import { getPayToken } from '@/api/user'
import { createRechargeOrder, confirmRechargeOrder, queryRechargeOrder } from '@/api/pay'
import { packages } from '@/assets/enums/packageData'

const DEFAULT_COUNTDOWN_SECONDS = 10 * 60
const AUTO_START_QUERY_KEYS = ['packageCode', 'payChannel', 'autoStart', 'sourceTradeOrderId']

const TEXT = {
  back: '返回',
  title: '套餐充值中心',
  currency: 'CNY',
  pointsUnit: '积分',
  payNow: '立即支付',
  packageName: '商品名称',
  payOrderId: '支付订单号',
  transactionId: '交易流水号',
  createTime: '创建时间',
  orderState: '订单状态',
  orderAmount: '订单金额',
  needPay: '需支付',
  scanPrefix: '请使用',
  scanSuffix: '扫一扫',
  scanTip: '扫描二维码完成支付',
  countdown: '支付失效倒计时',
  contactTip: '如支付遇到问题，请联系管理员处理',
  securityTip: 'SSL 加密通道 | 安全支付保障',
  orderTip: '温馨提示：订单创建后会使用页面初始化获取的 payToken 作为幂等 identifier，若支付成功状态有延迟，请耐心等待 1-5 分钟。'
}

const PAY_METHODS = [
  {
    key: 'alipay',
    label: '支付宝',
    channel: 'ALIPAY',
    qrLabel: '支付宝'
  },
  {
    key: 'wechat',
    label: '微信',
    channel: 'WECHAT',
    qrLabel: '微信'
  },
  {
    key: 'mock',
    label: 'MOCK',
    channel: 'MOCK',
    qrLabel: 'MOCK'
  }
]

const ORDER_STATE_TEXT_MAP = {
  TO_PAY: '待支付',
  PAYING: '支付中',
  PAID: '已付款',
  FAILED: '支付失败',
  EXPIRED: '支付超时',
  REFUNDED: '已退款'
}

export default {
  data() {
    return {
      text: TEXT,
      payToken: '',
      payVisible: false,
      payVisible2: false,
      createOrderLoading: false,
      confirmPayLoading: false,
      pollingRequesting: false,
      selectedPackage: null,
      selectedPayMethod: PAY_METHODS[0].key,
      payMethods: PAY_METHODS,
      activeOrder: {},
      packages: [],
      pollingTimer: null,
      countdownTimer: null,
      countdown: DEFAULT_COUNTDOWN_SECONDS,
      timerText: '10:00',
      autoStartHandledKey: ''
    }
  },
  computed: {
    /**
     * 当前选中的支付方式配置。
     */
    currentPayMethod() {
      return this.payMethods.find((item) => item.key === this.selectedPayMethod) || this.payMethods[0]
    },
    /**
     * 优先展示后端快照里的套餐名称，避免页面展示和实际订单不一致。
     */
    displayPackageName() {
      return this.activeOrder.packageNameSnapshot || (this.selectedPackage && this.selectedPackage.name) || '--'
    },
    /**
     * 金额展示以后端订单金额为准，兜底回退到当前套餐价格。
     */
    displayAmount() {
      const amount = this.activeOrder.orderAmount != null
        ? this.activeOrder.orderAmount
        : this.selectedPackage && this.selectedPackage.price

      return this.formatMoney(amount || 0)
    }
  },
  watch: {
    /**
     * 支持从订单中心多次跳回当前支付页继续支付。
     */
    '$route.query': {
      handler() {
        this.tryAutoStartFromRoute()
      },
      deep: true
    }
  },
  async mounted() {
    this.packages = this.normalizePackages(packages)
    await this.fetchPayToken(false)
    this.tryAutoStartFromRoute()
  },
  beforeDestroy() {
    this.clearRuntimeState()
  },
  methods: {
    /**
     * 统一套餐字段，便于页面展示和下单时复用。
     */
    normalizePackages(source) {
      return source.map((pkg, index) => ({
        id: index + 1,
        code: pkg.code,
        name: pkg.displayName,
        price: Number(pkg.price || 0),
        points: Number(pkg.creditAmount || 0)
      }))
    },
    /**
     * 根据套餐价格展示不同的客服权益文案。
     */
    getSupportText(item) {
      return item.price >= 100
        ? '24 小时专属客服'
        : '基础在线支持'
    },
    /**
     * 根据套餐价格展示不同的积分权益文案。
     */
    getDiscountText(item) {
      return item.price >= 100
        ? '积分抵扣额度 +20%'
        : '标准积分抵扣'
    },
    /**
     * 兼容不同返回结构，提取 payToken。
     */
    extractPayToken(response) {
      const payload = response && response.data ? response.data : {}
      const data = payload.data

      if (typeof data === 'string') {
        return data
      }

      if (data && typeof data === 'object') {
        return data.payToken || data.token || data.identifier || data.message || ''
      }

      return payload.message || ''
    },
    /**
     * 接口统一解包，非 200 直接抛错。
     */
    unwrapResponse(response, fallbackMessage) {
      const payload = response && response.data ? response.data : {}

      if (Number(payload.code) !== 200) {
        throw new Error(payload.message || fallbackMessage)
      }

      return payload.data || {}
    },
    /**
     * 合并后端订单快照和前端当前套餐信息，沉淀为页面统一使用的订单对象。
     */
    buildOrder(orderData, pkg, identifier) {
      const packageInfo = pkg || this.findPackageByCode(orderData.packageCode) || this.selectedPackage || {}

      return {
        ...this.activeOrder,
        ...orderData,
        identifier: identifier || this.activeOrder.identifier || '',
        packageCode: orderData.packageCode || packageInfo.code || '',
        packageNameSnapshot: orderData.packageNameSnapshot || packageInfo.name || '',
        creditAmountSnapshot: orderData.creditAmountSnapshot != null
          ? orderData.creditAmountSnapshot
          : (packageInfo.points || 0),
        orderAmount: orderData.orderAmount != null
          ? orderData.orderAmount
          : (packageInfo.price || 0),
        createTime: orderData.createTime || this.activeOrder.createTime || new Date().toISOString()
      }
    },
    /**
     * 页面初始化或支付完成后重新获取 payToken，供下一次下单使用。
     */
    async fetchPayToken(showError = true) {
      try {
        const response = await getPayToken()
        const token = this.extractPayToken(response)

        if (!token) {
          throw new Error('支付 token 为空')
        }

        this.payToken = token

        this.activeOrder.transactionId = token
        return token
      } catch (error) {
        if (showError) {
          this.$message.error(error.message || '获取支付 token 失败')
        }

        return ''
      }
    },
    /**
     * 统一读取订单状态字段，兼容 try / confirm / query 三种接口。
     */
    getOrderState(order) {
      return (order && (order.orderState || order.payOrderState)) || 'TO_PAY'
    },
    /**
     * 状态枚举转页面展示文案。
     */
    getStateText(state) {
      return ORDER_STATE_TEXT_MAP[state] || state || '--'
    },
    /**
     * 判断订单是否已经进入终态。
     */
    isTerminalState(state) {
      return ['PAID', 'FAILED', 'EXPIRED', 'REFUNDED'].includes(state)
    },
    /**
     * 金额统一保留两位小数。
     */
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    /**
     * 时间统一格式化。
     */
    formatDate(value) {
      if (!value) {
        return '--'
      }

      const date = new Date(value)

      if (Number.isNaN(date.getTime())) {
        return value
      }

      const pad = (num) => String(num).padStart(2, '0')

      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    /**
     * 兼容相对路径 payUrl，统一补成完整地址。
     */
    resolvePayUrl(payUrl) {
      if (!payUrl) {
        return ''
      }

      if (/^https?:\/\//.test(payUrl)) {
        return payUrl
      }

      const prefix = payUrl.startsWith('/') ? payUrl : `/${payUrl}`
      return `${window.location.origin}${prefix}`
    },
    /**
     * 优先使用后端过期时间计算二维码倒计时。
     */
    getCountdownSeconds(order) {
      if (!order || !order.payExpireTime) {
        return DEFAULT_COUNTDOWN_SECONDS
      }

      const diff = Math.floor((new Date(order.payExpireTime).getTime() - Date.now()) / 1000)
      return diff > 0 ? diff : 0
    },
    /**
     * 将秒数格式化为 mm:ss。
     */
    updateTimerText() {
      const minutes = Math.floor(this.countdown / 60)
      const seconds = this.countdown % 60
      this.timerText = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    },
    /**
     * 开启二维码倒计时，到期后直接按超时终态收口。
     */
    startCountdown(seconds) {
      this.clearCountdown()
      this.countdown = seconds > 0 ? seconds : 0
      this.updateTimerText()

      if (this.countdown <= 0) {
        this.handleTerminalState('支付已超时，请重新发起', 'warning')
        return
      }

      this.countdownTimer = setInterval(() => {
        if (this.countdown <= 0) {
          this.handleTerminalState('支付已超时，请重新发起', 'warning')
          return
        }

        this.countdown -= 1
        this.updateTimerText()
      }, 1000)
    },
    /**
     * 清理倒计时定时器。
     */
    clearCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    /**
     * 清理订单轮询定时器。
     */
    clearPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }

      this.pollingRequesting = false
    },
    /**
     * 清空上一次二维码渲染结果。
     */
    clearQrCode() {
      const qrEl = this.$refs.qrcode

      if (qrEl) {
        qrEl.innerHTML = ''
      }
    },
    /**
     * 统一回收支付过程中的运行态。
     */
    clearRuntimeState() {
      this.clearPolling()
      this.clearCountdown()
      this.clearQrCode()
      this.countdown = DEFAULT_COUNTDOWN_SECONDS
      this.timerText = '10:00'
    },
    /**
     * 根据套餐编码查找当前页面上的套餐配置。
     */
    findPackageByCode(code) {
      return this.packages.find((item) => item.code === code) || null
    },
    /**
     * 根据支付渠道反查前端按钮配置。
     */
    findPayMethodByChannel(channel) {
      return this.payMethods.find((item) => item.channel === channel) || this.payMethods[0]
    },
    /**
     * 外部带入 payChannel 时，统一同步到当前选中的支付方式按钮。
     */
    selectPayMethodByChannel(channel) {
      this.selectedPayMethod = this.findPayMethodByChannel(channel).key
    },
    /**
     * 如果同套餐存在未结束订单，则优先恢复，避免重复下单。
     */
    canResumeActiveOrder(item) {
      return Boolean(
        item &&
        this.activeOrder.payOrderId &&
        this.activeOrder.packageCode === item.code &&
        !this.isTerminalState(this.getOrderState(this.activeOrder))
      )
    },
    /**
     * 恢复未完成订单。
     * 若已经在支付中并拿到二维码，则直接回到二维码弹框。
     */
    resumeActiveOrder() {
      const state = this.getOrderState(this.activeOrder)

      if (state === 'PAYING' && this.activeOrder.payUrl) {
        this.openQrDialog()
        return
      }

      this.payVisible2 = false
      this.payVisible = true
    },
    /**
     * 读取订单中心透传过来的 query，并自动定位套餐与支付方式。
     */
    getAutoStartPayload() {
      const query = this.$route && this.$route.query ? this.$route.query : {}
      const autoStart = String(query.autoStart || '') === '1'
      const packageCode = query.packageCode ? String(query.packageCode) : ''

      if (!autoStart || !packageCode) {
        return null
      }

      return {
        packageCode,
        payChannel: query.payChannel ? String(query.payChannel) : '',
        sourceTradeOrderId: query.sourceTradeOrderId ? String(query.sourceTradeOrderId) : ''
      }
    },
    /**
     * 清理自动拉起支付使用的内部 query，避免重复触发。
     */
    clearAutoStartQuery() {
      const nextQuery = { ...(this.$route.query || {}) }

      AUTO_START_QUERY_KEYS.forEach((key) => {
        delete nextQuery[key]
      })

      this.$router.replace({
        path: this.$route.path,
        query: nextQuery
      }).catch(() => {})
    },
    /**
     * 订单中心跳转到支付页后，自动选中套餐和支付方式，并复用现有下单逻辑重新创建支付单。
     */
    async tryAutoStartFromRoute() {
      const payload = this.getAutoStartPayload()

      if (!payload) {
        return
      }

      const autoStartKey = JSON.stringify(payload)

      if (this.autoStartHandledKey === autoStartKey || this.createOrderLoading) {
        return
      }

      const targetPackage = this.findPackageByCode(payload.packageCode)

      if (!targetPackage) {
        this.autoStartHandledKey = autoStartKey
        this.clearAutoStartQuery()
        this.$message.error('未找到对应套餐，无法继续支付')
        return
      }

      this.autoStartHandledKey = autoStartKey
      this.selectPayMethodByChannel(payload.payChannel)

      if (!this.payToken) {
        const token = await this.fetchPayToken()
        if (!token) {
          return
        }
      }

      this.clearAutoStartQuery()
      this.openPayDialog(targetPackage, {
        forceCreate: true,
        payMethodKey: this.selectedPayMethod,
        sourceTradeOrderId: payload.sourceTradeOrderId
      })
    },
    /**
     * 点击套餐时创建订单。
     * 支付页默认允许复用同套餐未结束订单，但订单中心“继续支付”会强制重新创建支付单。
     */
    async openPayDialog(item, options = {}) {
      const {
        forceCreate = false,
        payMethodKey = PAY_METHODS[0].key,
        sourceTradeOrderId = ''
      } = options

      this.selectedPackage = item
      this.selectedPayMethod = payMethodKey || PAY_METHODS[0].key

      if (!forceCreate && this.canResumeActiveOrder(item)) {
        this.resumeActiveOrder()
        return
      }

      if (!this.payToken) {
        const token = await this.fetchPayToken()
        if (!token) {
          return
        }
      }

      this.createOrderLoading = true

      try {
        const identifier = this.payToken

        const response = await createRechargeOrder({
          packageCode: item.code,
          identifier,
          payChannel: this.currentPayMethod.channel,
          memo: `recharge-${item.code}`
        })
        const orderData = this.unwrapResponse(response, '创建充值订单失败')

        // 下单成功后统一落到 activeOrder，后续确认支付与轮询都从这里读取。
        this.activeOrder = {
          ...this.buildOrder(orderData, item, identifier),
          sourceTradeOrderId
        }

        if (this.getOrderState(this.activeOrder) === 'PAID' || this.activeOrder.paid) {
          this.handlePaidSuccess(this.activeOrder)
          return
        }

        this.payVisible = true
        this.payVisible2 = false

        // 当前 token 已参与下单，预取下一次支付使用的新 token。
        this.fetchPayToken(false)
      } catch (error) {
        this.$message.error(error.message || '创建充值订单失败')
      } finally {
        this.createOrderLoading = false
      }
    },
    /**
     * 用户确认支付后调用 confirm 接口，拿到真正用于生成二维码的 payUrl。
     */
    async confirmToPay() {
      if (this.confirmPayLoading) {
        return
      }

      console.log('transactionId=',this.activeOrder.transactionId)

      console.log('payOrderId=',this.activeOrder.payOrderId)
      if (!this.activeOrder.transactionId || !this.activeOrder.payOrderId) {
        this.$message.warning('订单信息不完整，请重新创建订单')
        return
      }

      if (this.getOrderState(this.activeOrder) === 'PAYING' && this.activeOrder.payUrl) {
        this.openQrDialog()
        return
      }

      this.confirmPayLoading = true

      try {
        const response = await confirmRechargeOrder({
          transactionId: this.activeOrder.transactionId,
          payOrderId: this.activeOrder.payOrderId,
          payChannel: this.currentPayMethod.channel
        })
        const orderData = this.unwrapResponse(response, '确认支付失败')

        this.activeOrder = this.buildOrder(
          orderData,
          this.selectedPackage || this.findPackageByCode(orderData.packageCode),
          this.activeOrder.identifier
        )

        if (this.getOrderState(this.activeOrder) === 'PAID' || this.activeOrder.paid) {
          this.handlePaidSuccess(this.activeOrder)
          return
        }

        if (!this.resolvePayUrl(this.activeOrder.payUrl)) {
          throw new Error('支付链接为空，无法生成二维码')
        }

        this.openQrDialog()
      } catch (error) {
        this.$message.error(error.message || '确认支付失败')
      } finally {
        this.confirmPayLoading = false
      }
    },
    /**
     * 根据 payUrl 重新渲染二维码。
     */
    renderQRCode(payUrl) {
      const qrEl = this.$refs.qrcode

      if (!qrEl || !payUrl) {
        return
      }

      qrEl.innerHTML = ''
      new QRCode(qrEl, {
        text: payUrl,
        width: 220,
        height: 220
      })
    },
    /**
     * 打开二维码弹框，并同步启动倒计时与订单轮询。
     */
    openQrDialog() {
      this.payVisible = false
      this.payVisible2 = true

      this.$nextTick(() => {
        const payUrl = this.resolvePayUrl(this.activeOrder.payUrl)
        this.renderQRCode(payUrl)
        this.startCountdown(this.getCountdownSeconds(this.activeOrder))
        this.startPolling()
      })
    },
    /**
     * 启动订单轮询，先立即查一次，再按固定间隔持续查询。
     */
    startPolling() {
      this.clearPolling()
      this.pollRechargeOrder()
      this.pollingTimer = setInterval(() => {
        this.pollRechargeOrder()
      }, 3000)
    },
    /**
     * 轮询支付订单状态，并根据终态自动收口页面。
     */
    async pollRechargeOrder() {
      if (this.pollingRequesting || !this.activeOrder.payOrderId) {
        return
      }

      this.pollingRequesting = true

      try {
        const response = await queryRechargeOrder(this.activeOrder.payOrderId)
        const orderData = this.unwrapResponse(response, '查询订单状态失败')

        // 轮询期间持续用服务端最新快照覆盖本地状态，保证二维码和状态展示一致。
        this.activeOrder = this.buildOrder(
          orderData,
          this.selectedPackage || this.findPackageByCode(orderData.packageCode),
          this.activeOrder.identifier
        )

        if (this.getOrderState(this.activeOrder) === 'PAID' || this.activeOrder.paid) {
          this.handlePaidSuccess(this.activeOrder)
          return
        }

        const orderState = this.getOrderState(this.activeOrder)

        if (orderState === 'FAILED') {
          this.handleTerminalState('支付失败，请重新发起', 'error')
        } else if (orderState === 'EXPIRED') {
          this.handleTerminalState('支付已超时，请重新发起', 'warning')
        } else if (orderState === 'REFUNDED') {
          this.handleTerminalState('订单已退款', 'warning')
        }
      } catch (error) {
        console.error('poll recharge order error:', error)
      } finally {
        this.pollingRequesting = false
      }
    },
    /**
     * 支付成功后的统一收口逻辑。
     */
    handlePaidSuccess(order) {
      if (order) {
        this.activeOrder = order
      }

      this.clearRuntimeState()
      this.payVisible = false
      this.payVisible2 = false
      this.$message.success('支付成功')
      this.fetchPayToken(false)
    },
    /**
     * 支付失败、超时、退款等终态的统一收口逻辑。
     */
    handleTerminalState(message, type) {
      this.clearRuntimeState()
      this.payVisible = false
      this.payVisible2 = false

      if (message && this.$message[type]) {
        this.$message[type](message)
      }

      this.fetchPayToken(false)
    },
    /**
     * 关闭订单确认弹框。
     */
    handleOrderDialogClose(done) {
      this.payVisible = false

      if (typeof done === 'function') {
        done()
      }
    },
    /**
     * 关闭二维码弹框时，同时清理倒计时与轮询。
     */
    handleQrDialogClose(done) {
      this.clearRuntimeState()
      this.payVisible2 = false

      if (typeof done === 'function') {
        done()
      }
    },
    /**
     * 返回上一页；没有历史记录时回到首页。
     */
    goBack() {
      if (window.history.length > 1) {
        this.$router.back()
      } else {
        this.$router.push('/')
      }
    }
  }
}
</script>

<style scoped>
.main-container {
  width: 100%;
}

.package-item {
  text-align: center;
  margin-bottom: 20px;
}

.price {
  font-size: 24px;
  color: #f56c6c;
  font-weight: bold;
  margin: 10px 0;
}

.info-group p {
  margin: 8px 0;
}

.info-group span {
  color: #fff;
  margin-right: 10px;
}

.tip-box {
  background: #2a3342;
  color: #ff6b6b;
  padding: 10px;
  font-size: 12px;
  border-radius: 4px;
  margin-bottom: 20px;
  line-height: 1.6;
}

.channel-selector {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 20px;
}

.footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #333;
  padding-top: 15px;
}

.footer-actions b {
  color: #ff6b6b;
  font-size: 18px;
}

.qr-step {
  text-align: center;
  padding: 20px;
}

.full-dialog .el-dialog__body {
  padding: 0 !important;
}

.full-dialog .el-dialog__header {
  display: none;
}

::v-deep .el-dialog {
  background: #1c232f;
}

::v-deep .el-dialog__body {
  background-color: #1c232f;
  padding: 0 !important;
  border-radius: 8px;
}

::v-deep .el-dialog__wrapper {
  z-index: 3000 !important;
}

::v-deep .v-modal {
  z-index: 2999 !important;
}

:deep(.full-dialog) {
  border-radius: 16px !important;
  background: #1c232f !important;
  display: flex;
  flex-direction: column;
  width: 750px;
  max-width: 90vw;
  max-height: 80vh;
  overflow: hidden;
}

:deep(.full-dialog .el-dialog__body) {
  padding: 0 !important;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.order-confirm,
.qr-step {
  padding: 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.order-confirm {
  justify-content: flex-start;
}

.price-display {
  text-align: center;
  font-size: 24px;
  color: #00d4ff;
  margin: 30px 0;
}

.qr-code-box {
  background: #fff;
  padding: 10px;
  border-radius: 8px;
  width: 240px;
  min-height: 240px;
  margin: 20px auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.package-features {
  font-size: 14px;
  color: #00d4ff;
  margin: 15px 0;
  text-align: center;
  padding: 10px;
  border-radius: 6px;
}

.info-group {
  background: #252d3d;
  padding: 20px;
  border-radius: 8px;
  color: #e0e6ed;
}

.security-footer {
  margin-top: 30px;
  color: #409eff;
  font-size: 13px;
  border-top: 1px solid #3d4a5d;
  padding-top: 15px;
  font-weight: 500;
}

.countdown-text {
  font-size: 14px;
  margin-top: 10px;
  color: #fff;
}

.countdown-text span {
  color: #ffca2c !important;
  font-weight: bold;
}

.package-item:hover {
  border: 1px solid #00d4ff;
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.3);
}

.qr-guide-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin: 20px auto 0 auto;
  padding: 12px;
  border-radius: 8px;
  width: 80%;
}

.qr-text {
  text-align: left;
  font-size: 13px;
  color: #fff;
}

.qr-text p {
  margin: 0;
  line-height: 1.4;
}

.qr-text p:first-child {
  font-weight: bold;
  color: #00d4ff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-weight: bold;
  font-size: 16px;
  margin-left: -50px;
}

.qr-order-info {
  color: #dce6f5;
  line-height: 1.8;
}
</style>
