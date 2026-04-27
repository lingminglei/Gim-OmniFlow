<template>
  <div class="order-page">
    <div class="order-hero">
      <div class="hero-copy">
        <h1>订单中心</h1>
        <p class="hero-desc">
          聚合展示用户最近的支付订单，仅保留商品、金额、状态、支付方式与关键时间，方便快速查看和继续支付。
        </p>
      </div>

      <div class="hero-actions">
        <el-button
          type="primary"
          icon="el-icon-refresh"
          :loading="refreshLoading"
          class="refresh-btn"
          @click="refreshOrders"
        >
          刷新订单
        </el-button>
      </div>
    </div>

    <section class="summary-grid">
      <article class="summary-card cyan">
        <div class="summary-label">订单总数</div>
        <div class="summary-value">{{ summary.totalOrders }}</div>
        <div class="summary-foot">以后端返回的总记录数为准</div>
      </article>

      <article class="summary-card blue">
        <div class="summary-label">已支付单数</div>
        <div class="summary-value">{{ summary.paidOrders }}</div>
        <div class="summary-foot">当前页中已完成支付的订单数量</div>
      </article>

      <article class="summary-card gold">
        <div class="summary-label">已支付金额</div>
        <div class="summary-value">¥{{ formatMoney(summary.totalPaidAmount) }}</div>
        <div class="summary-foot">按当前页已支付金额汇总</div>
      </article>
    </section>

    <section class="order-shell">
      <header class="shell-header">
        <div>
          <h2>最近订单</h2>
        </div>
        <div class="shell-meta">
          <span v-if="refreshLoading">正在同步最新订单...</span>
          <span v-else>共 {{ pagination.total }} 条记录，第 {{ pagination.pageNo }} / {{ displayTotalPages }} 页</span>
        </div>
      </header>

      <div v-if="initialLoading" class="skeleton-grid">
        <article v-for="index in pageSize" :key="index" class="skeleton-card">
          <div class="skeleton-top">
            <div class="skeleton-avatar shimmer"></div>
            <div class="skeleton-copy">
              <div class="skeleton-line shimmer line-lg"></div>
              <div class="skeleton-line shimmer line-md"></div>
            </div>
          </div>
          <div class="skeleton-line shimmer line-xl"></div>
          <div class="skeleton-bottom">
            <div class="skeleton-line shimmer line-sm"></div>
            <div class="skeleton-line shimmer line-sm"></div>
          </div>
        </article>
      </div>

      <div v-else-if="orderList.length" class="order-list">
        <article
          v-for="order in orderList"
          :key="order.orderId"
          class="order-card"
          @click="showOrderDetail(order)"
        >
          <div class="order-card-main">
            <div class="goods-block">
              <div class="goods-avatar">
                <img v-if="order.goodsPicUrl" :src="order.goodsPicUrl" :alt="order.goodsName" />
                <span v-else>{{ getGoodsInitial(order.goodsName) }}</span>
              </div>

              <div class="goods-info">
                <div class="goods-name">{{ order.goodsName || '--' }}</div>
                <div class="goods-id">
                  <span class="mono">订单号 {{ order.orderId || '--' }}</span>
                  <span class="goods-tag">{{ order.goodsId || '--' }}</span>
                </div>
              </div>
            </div>

            <div class="amount-block">
              <div class="amount-label">订单金额</div>
              <div class="amount-value">¥{{ formatMoney(order.orderAmount) }}</div>
              <div class="amount-sub">已支付 ¥{{ formatMoney(order.paidAmount) }}</div>
            </div>
          </div>

          <div class="order-card-side">
            <div class="status-row">
              <span :class="['status-chip', getStatusClass(order.orderState)]">
                {{ getOrderStateText(order.orderState) }}
              </span>
              <span class="channel-chip">{{ getPayChannelText(order.payChannel) }}</span>
            </div>

            <div class="timeline-row">
              <div class="timeline-item">
                <span class="timeline-label">下单确认</span>
                <span>{{ formatDate(order.orderConfirmedTime) }}</span>
              </div>
              <div class="timeline-item">
                <span class="timeline-label">支付成功</span>
                <span>{{ formatDate(order.paySucceedTime) }}</span>
              </div>
            </div>

            <div class="card-actions">
              <span class="stream-text">流水号 {{ order.payStreamId || '--' }}</span>
              <div class="action-group">
                <el-button
                  v-if="canContinuePay(order)"
                  type="text"
                  class="resume-btn"
                  @click.stop="continueToPay(order)"
                >
                  继续支付
                </el-button>
                <el-button type="text" class="detail-btn" @click.stop="showOrderDetail(order)">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-wrap">
        <el-empty description="暂无订单记录"></el-empty>
      </div>

      <footer v-if="pagination.total > 0" class="shell-footer">
        <el-pagination
          background
          class="order-pagination"
          layout="prev, pager, next"
          :current-page="pagination.pageNo"
          :page-size="pageSize"
          :total="pagination.total"
          :hide-on-single-page="pagination.total <= pageSize"
          @current-change="handlePageChange"
        />
      </footer>
    </section>

    <el-dialog
      :visible.sync="detailVisible"
      width="760px"
      custom-class="order-detail-dialog"
      :close-on-click-modal="false"
      :show-close="false"
      append-to-body
    >
      <div
        v-loading="detailLoading"
        element-loading-background="rgba(10, 16, 28, 0.78)"
        class="detail-panel"
      >
        <div class="detail-header">
          <div>
            <h3>{{ detailOrder.goodsName || '订单详情' }}</h3>
          </div>

          <div class="detail-header-actions">
            <el-button
              v-if="canContinuePay(detailOrder)"
              type="primary"
              size="mini"
              class="detail-pay-btn"
              @click="continueToPay(detailOrder)"
            >
              继续支付
            </el-button>
            <span :class="['status-chip', getStatusClass(detailOrder.orderState)]">
              {{ getOrderStateText(detailOrder.orderState) }}
            </span>
            <el-button
              type="text"
              icon="el-icon-close"
              class="detail-close-btn"
              @click="detailVisible = false"
            />
          </div>
        </div>

        <div class="detail-amount">
          <span>订单金额</span>
          <strong>¥{{ formatMoney(detailOrder.orderAmount) }}</strong>
        </div>

        <div class="detail-grid">
          <div class="detail-card">
            <div class="detail-card-title">核心信息</div>
            <div class="detail-item">
              <span>订单号</span>
              <b class="mono">{{ detailOrder.orderId || '--' }}</b>
            </div>
            <div class="detail-item">
              <span>商品名称</span>
              <b>{{ detailOrder.goodsName || '--' }}</b>
            </div>
            <div class="detail-item">
              <span>商品编码</span>
              <b>{{ detailOrder.goodsId || '--' }}</b>
            </div>
            <div class="detail-item">
              <span>支付方式</span>
              <b>{{ getPayChannelText(detailOrder.payChannel) }}</b>
            </div>
            <div class="detail-item">
              <span>支付流水号</span>
              <b class="mono">{{ detailOrder.payStreamId || '--' }}</b>
            </div>
          </div>

          <div class="detail-card">
            <div class="detail-card-title">金额与状态</div>
            <div class="detail-item">
              <span>订单金额</span>
              <b>¥{{ formatMoney(detailOrder.orderAmount) }}</b>
            </div>
            <div class="detail-item">
              <span>商品单价</span>
              <b>¥{{ formatMoney(detailOrder.itemPrice) }}</b>
            </div>
            <div class="detail-item">
              <span>商品数量</span>
              <b>{{ detailOrder.itemCount || 0 }}</b>
            </div>
            <div class="detail-item">
              <span>已支付金额</span>
              <b>¥{{ formatMoney(detailOrder.paidAmount) }}</b>
            </div>
            <div class="detail-item">
              <span>是否超时</span>
              <b>{{ detailOrder.timeout ? '是' : '否' }}</b>
            </div>
          </div>

          <div class="detail-card wide">
            <div class="detail-card-title">时间节点</div>
            <div class="time-line">
              <div class="time-node">
                <label>下单确认时间</label>
                <span>{{ formatDate(detailOrder.orderConfirmedTime) }}</span>
              </div>
              <div class="time-node">
                <label>支付成功时间</label>
                <span>{{ formatDate(detailOrder.paySucceedTime) }}</span>
              </div>
              <div class="time-node">
                <label>支付过期时间</label>
                <span>{{ formatDate(detailOrder.payExpireTime) }}</span>
              </div>
              <div class="time-node">
                <label>订单关闭时间</label>
                <span>{{ formatDate(detailOrder.orderClosedTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPayOrderDetail, getPayOrderList } from '@/api/tradeOrder'

const CONTINUE_PAY_STATES = ['TO_PAY', 'PAYING']

const ORDER_STATE_MAP = {
  CREATED: '待确认',
  TO_PAY: '待支付',
  PAYING: '支付中',
  PAID: '已支付',
  FAILED: '支付失败',
  CLOSED: '已关闭',
  EXPIRED: '已超时',
  REFUNDED: '已退款'
}

const PAY_CHANNEL_MAP = {
  ALIPAY: '支付宝',
  WECHAT: '微信',
  MOCK: 'MOCK'
}

export default {
  name: 'OrderListView',
  data() {
    return {
      initialLoading: true,
      refreshLoading: false,
      detailLoading: false,
      detailVisible: false,
      orderList: [],
      detailOrder: {},
      pageSize: 6,
      pagination: {
        pageNo: 1,
        total: 0,
        pages: 0
      },
      hasLoaded: false
    }
  },
  computed: {
    /**
     * 订单摘要只展示用户最关心的几个指标。
     * 其中总数使用后端 total，本页统计只基于当前页 records。
     */
    summary() {
      const paidOrders = this.orderList.filter((item) => item.orderState === 'PAID').length
      const totalPaidAmount = this.orderList.reduce((sum, item) => {
        return sum + Number(item.paidAmount || 0)
      }, 0)

      return {
        totalOrders: this.pagination.total,
        paidOrders,
        totalPaidAmount
      }
    },
    /**
     * 空列表时仍然给分页区一个稳定的页数显示。
     */
    displayTotalPages() {
      return this.pagination.pages || 1
    }
  },
  mounted() {
    this.fetchOrderList({ pageNo: 1, silent: false })
  },
  methods: {
    /**
     * 查询订单列表。
     * 首次进入页面使用独立骨架屏，后续刷新与翻页只让按钮进入加载态，避免白屏。
     */
    async fetchOrderList({ pageNo = this.pagination.pageNo, silent = true } = {}) {
      const shouldUseInitialLoading = !this.hasLoaded && !silent

      if (shouldUseInitialLoading) {
        this.initialLoading = true
      } else {
        this.refreshLoading = true
      }

      try {
        const response = await getPayOrderList(pageNo, this.pageSize)
        const payload = response && response.data ? response.data : {}

        if (Number(payload.code) !== 200) {
          throw new Error(payload.message || '查询订单列表失败')
        }

        const normalizedPage = this.normalizeOrderPage(payload.data, pageNo)

        this.orderList = normalizedPage.records
        this.pagination.pageNo = normalizedPage.current
        this.pagination.total = normalizedPage.total
        this.pagination.pages = normalizedPage.pages
        this.hasLoaded = true
      } catch (error) {
        this.$message.error(error.message || '查询订单列表失败')
      } finally {
        this.initialLoading = false
        this.refreshLoading = false
      }
    },
    /**
     * 兼容新旧两种列表返回结构。
     * 新版接口直接读取 records/total/current/pages；旧版数组结构则在前端完成分页兜底。
     */
    normalizeOrderPage(data, requestedPageNo) {
      const currentPageNo = Number(requestedPageNo) || 1

      if (Array.isArray(data)) {
        const sortedRecords = this.sortOrderList(data)
        const total = sortedRecords.length
        const totalPages = total > 0 ? Math.ceil(total / this.pageSize) : 0
        const safePageNo = totalPages > 0 ? Math.min(currentPageNo, totalPages) : 1
        const startIndex = (safePageNo - 1) * this.pageSize

        return {
          records: sortedRecords.slice(startIndex, startIndex + this.pageSize),
          total,
          current: safePageNo,
          pages: totalPages
        }
      }

      const pageData = data && typeof data === 'object' ? data : {}
      const records = this.sortOrderList(Array.isArray(pageData.records) ? pageData.records : [])
      const total = Number(pageData.total != null ? pageData.total : records.length)
      const current = Number(pageData.current || currentPageNo)
      const pages = Number(
        pageData.pages != null
          ? pageData.pages
          : (total > 0 ? Math.ceil(total / this.pageSize) : 0)
      )

      return {
        records,
        total,
        current,
        pages
      }
    },
    /**
     * 保持订单卡片按最近时间倒序展示。
     */
    sortOrderList(list) {
      return [...list].sort((left, right) => {
        const leftTime = new Date(
          left.paySucceedTime || left.orderConfirmedTime || left.orderClosedTime || left.payExpireTime || 0
        ).getTime()
        const rightTime = new Date(
          right.paySucceedTime || right.orderConfirmedTime || right.orderClosedTime || right.payExpireTime || 0
        ).getTime()

        return rightTime - leftTime
      })
    },
    /**
     * 手动刷新时保留当前页内容，只更新数据本身。
     */
    refreshOrders() {
      this.fetchOrderList({
        pageNo: this.pagination.pageNo || 1,
        silent: true
      })
    },
    /**
     * 分页切换固定按每页 6 条重新拉取后端数据。
     */
    handlePageChange(pageNo) {
      if (pageNo === this.pagination.pageNo || this.refreshLoading) {
        return
      }

      this.fetchOrderList({
        pageNo,
        silent: true
      })
    },
    /**
     * 查询单条订单详情，并先用列表数据做首屏回显，避免弹框闪白。
     */
    async showOrderDetail(order) {
      if (!order || !order.orderId) {
        return
      }

      this.detailVisible = true
      this.detailLoading = true
      this.detailOrder = { ...order }

      try {
        const response = await getPayOrderDetail(order.orderId)
        const payload = response && response.data ? response.data : {}

        if (Number(payload.code) !== 200) {
          throw new Error(payload.message || '查询订单详情失败')
        }

        this.detailOrder = payload.data || {}
      } catch (error) {
        this.$message.error(error.message || '查询订单详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    /**
     * 仅允许待支付、支付中两种状态继续支付。
     * 继续支付本质上是带着套餐编码跳转到支付页重新创建支付单。
     */
    canContinuePay(order) {
      return Boolean(
        order &&
        order.goodsId &&
        CONTINUE_PAY_STATES.includes(order.orderState)
      )
    },
    /**
     * 继续支付时不恢复旧支付单，而是把商品编码和支付方式透传给 Pay.vue，
     * 由支付页复用现有创建订单逻辑重新拉起支付。
     */
    continueToPay(order) {
      if (!this.canContinuePay(order)) {
        return
      }

      this.$router.push({
        path: '/pay',
        query: {
          packageCode: order.goodsId,
          payChannel: this.normalizePayChannel(order.payChannel),
          autoStart: '1',
          sourceTradeOrderId: order.orderId || ''
        }
      })
    },
    /**
     * 支付渠道只放行已支持的枚举值，异常场景回退到支付宝。
     */
    normalizePayChannel(channel) {
      return ['ALIPAY', 'WECHAT', 'MOCK'].includes(channel) ? channel : 'ALIPAY'
    },
    /**
     * 订单状态中文映射。
     */
    getOrderStateText(state) {
      return ORDER_STATE_MAP[state] || state || '--'
    },
    /**
     * 支付渠道中文映射。
     */
    getPayChannelText(channel) {
      return PAY_CHANNEL_MAP[channel] || channel || '--'
    },
    /**
     * 根据订单状态返回对应的视觉标签样式。
     */
    getStatusClass(state) {
      const classMap = {
        PAID: 'success',
        PAYING: 'warning',
        TO_PAY: 'pending',
        FAILED: 'danger',
        CLOSED: 'muted',
        EXPIRED: 'muted',
        REFUNDED: 'muted'
      }

      return classMap[state] || 'pending'
    },
    /**
     * 金额统一保留两位小数。
     */
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    /**
     * 时间统一格式化为可读形式。
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
     * 没有商品图时，用商品名首字占位。
     */
    getGoodsInitial(name) {
      return (name || '订').slice(0, 1).toUpperCase()
    }
  }
}
</script>

<style scoped>
.order-page {
  min-height: 0;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(0, 242, 255, 0.08), transparent 28%),
    radial-gradient(circle at top right, rgba(125, 42, 232, 0.1), transparent 30%),
    linear-gradient(180deg, rgba(13, 18, 31, 0.98), rgba(19, 25, 39, 0.96));
  color: #e6edf7;
}

.order-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 28px 32px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(10, 16, 28, 0.88), rgba(21, 30, 47, 0.72));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 20px 40px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(16px);
}

.eyebrow,
.detail-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: #69e4ff;
}

.hero-copy h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
  color: #f7fbff;
}

.hero-desc {
  max-width: 720px;
  margin: 14px 0 0;
  color: rgba(226, 232, 240, 0.78);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn {
  min-width: 132px;
  height: 44px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #00c6ff, #2d7eff);
  box-shadow: 0 10px 30px rgba(0, 174, 255, 0.28);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 22px;
}

.summary-card {
  position: relative;
  padding: 22px 24px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(18, 25, 39, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.summary-card::after {
  content: '';
  position: absolute;
  inset: auto -10% -55% auto;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  opacity: 0.2;
  filter: blur(10px);
}

.summary-card.cyan::after {
  background: #27d3ff;
}

.summary-card.blue::after {
  background: #4a72ff;
}

.summary-card.gold::after {
  background: #ffc857;
}

.summary-label {
  position: relative;
  z-index: 1;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.72);
}

.summary-value {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  font-size: 32px;
  font-weight: 700;
  color: #f8fbff;
}

.summary-foot {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.56);
}

.order-shell {
  margin-top: 22px;
  padding: 24px;
  border-radius: 26px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(14, 21, 34, 0.76);
  backdrop-filter: blur(16px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 18px 40px rgba(0, 0, 0, 0.26);
}

.shell-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.shell-header h2 {
  margin: 0;
  font-size: 22px;
  color: #fff;
}

.shell-header p,
.shell-meta {
  margin: 8px 0 0;
  color: rgba(226, 232, 240, 0.68);
  font-size: 13px;
}

.skeleton-grid,
.order-list {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.skeleton-card,
.order-card {
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background:
    linear-gradient(180deg, rgba(25, 34, 52, 0.94), rgba(16, 23, 36, 0.94));
}

.skeleton-card {
  padding: 22px;
}

.skeleton-top,
.skeleton-bottom {
  display: flex;
  align-items: center;
  gap: 16px;
}

.skeleton-copy {
  flex: 1;
}

.skeleton-avatar {
  width: 58px;
  height: 58px;
  border-radius: 16px;
}

.skeleton-line {
  height: 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.line-xl {
  width: 100%;
  height: 66px;
  margin: 18px 0;
  border-radius: 18px;
}

.line-lg {
  width: 54%;
}

.line-md {
  width: 36%;
  margin-top: 12px;
}

.line-sm {
  width: 24%;
}

.shimmer {
  position: relative;
  overflow: hidden;
}

.shimmer::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  transform: translateX(-100%);
  animation: shimmer 1.4s ease-in-out infinite;
}

.order-card {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
  gap: 22px;
  padding: 22px;
  transition: transform 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
  cursor: pointer;
}

.order-card:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 242, 255, 0.24);
  box-shadow: 0 18px 30px rgba(0, 0, 0, 0.22);
}

.order-card-main,
.order-card-side {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 16px;
}

.goods-block {
  display: flex;
  align-items: center;
  gap: 16px;
}

.goods-avatar {
  flex: 0 0 58px;
  width: 58px;
  height: 58px;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 242, 255, 0.2), rgba(125, 42, 232, 0.32));
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.goods-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.goods-id {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.65);
}

.goods-tag,
.channel-chip {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: #e6edf7;
}

.mono,
.stream-text {
  font-family: 'Consolas', 'Courier New', monospace;
}

.amount-block {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.amount-label,
.timeline-label {
  color: rgba(226, 232, 240, 0.64);
  font-size: 12px;
}

.amount-value {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 700;
  color: #8fe8ff;
}

.amount-sub {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.7);
}

.status-row,
.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.status-chip.success {
  color: #80ffc8;
  background: rgba(19, 128, 86, 0.18);
  border-color: rgba(68, 255, 177, 0.2);
}

.status-chip.warning {
  color: #ffd978;
  background: rgba(173, 121, 0, 0.18);
  border-color: rgba(255, 214, 92, 0.18);
}

.status-chip.pending {
  color: #7fe6ff;
  background: rgba(0, 112, 182, 0.18);
  border-color: rgba(55, 196, 255, 0.18);
}

.status-chip.danger {
  color: #ff9d9d;
  background: rgba(171, 43, 43, 0.18);
  border-color: rgba(255, 109, 109, 0.18);
}

.status-chip.muted {
  color: rgba(226, 232, 240, 0.7);
  background: rgba(107, 114, 128, 0.16);
  border-color: rgba(148, 163, 184, 0.14);
}

.timeline-row {
  display: grid;
  gap: 10px;
}

.timeline-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #e7eef8;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-btn,
.resume-btn {
  padding: 0;
}

.detail-btn {
  color: #7fe6ff;
}

.resume-btn {
  color: #ffd978;
}

.empty-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.shell-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.detail-panel {
  color: #eaf1fb;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.detail-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-header h3 {
  margin: 0;
  font-size: 28px;
  color: #fff;
}

.detail-pay-btn {
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ffb347, #ff8c42);
}

.detail-close-btn {
  font-size: 18px;
  color: rgba(226, 232, 240, 0.72);
}

.detail-close-btn:hover {
  color: #fff;
}

.detail-amount {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 22px;
  padding: 18px 22px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(0, 198, 255, 0.14), rgba(82, 98, 255, 0.14));
}

.detail-amount span {
  color: rgba(226, 232, 240, 0.72);
}

.detail-amount strong {
  font-size: 30px;
  color: #fff;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.detail-card {
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(18, 24, 38, 0.82);
}

.detail-card.wide {
  grid-column: 1 / -1;
}

.detail-card-title {
  margin-bottom: 14px;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: #7fe6ff;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.06);
  font-size: 14px;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item span {
  color: rgba(226, 232, 240, 0.68);
}

.detail-item b {
  color: #fff;
  font-weight: 600;
  text-align: right;
}

.time-line {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.time-node {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
}

.time-node label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.62);
}

.time-node span {
  color: #fff;
  line-height: 1.6;
}

::v-deep .order-pagination.is-background .btn-prev,
::v-deep .order-pagination.is-background .btn-next,
::v-deep .order-pagination.is-background .el-pager li {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(230, 237, 247, 0.72);
}

::v-deep .order-pagination.is-background .el-pager li:not(.disabled).active {
  background: linear-gradient(135deg, #00c6ff, #2d7eff);
  color: #fff;
}

::v-deep .order-detail-dialog {
  background: linear-gradient(180deg, rgba(11, 16, 28, 0.98), rgba(20, 27, 42, 0.98));
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.35);
}

::v-deep .order-detail-dialog .el-dialog__header {
  display: none;
}

::v-deep .order-detail-dialog .el-dialog__body {
  padding: 26px;
}

::v-deep .empty-wrap .el-empty__description p {
  color: rgba(226, 232, 240, 0.68);
}

@keyframes shimmer {
  100% {
    transform: translateX(100%);
  }
}

@media (max-width: 1100px) {
  .summary-grid,
  .detail-grid,
  .time-line,
  .order-card {
    grid-template-columns: 1fr;
  }

  .order-card-side {
    padding-top: 4px;
  }
}

@media (max-width: 768px) {
  .order-page {
    padding: 18px;
  }

  .order-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .amount-value,
  .detail-amount strong,
  .summary-value {
    font-size: 24px;
  }

  .status-row,
  .card-actions,
  .timeline-item,
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .shell-footer {
    justify-content: center;
  }
}
</style>
