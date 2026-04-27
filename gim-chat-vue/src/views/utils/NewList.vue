<template>
  <div class="news-page">
    <section class="news-hero">
      <div class="hero-copy">
        <span class="hero-kicker">TREND SIGNALS</span>
        <h1>今日热榜</h1>
        <p>
          聚合主流平台热点与实时内容变化，帮助快速判断舆情、追踪趋势并定位高价值话题。
        </p>
      </div>

      <div class="hero-meta">
        <div class="hero-time">
          <strong>{{ currentTime }}</strong>
          <span>{{ lunarDate }}</span>
        </div>

        <div class="hero-actions">
          <button class="hero-action" type="button" title="刷新" @click="getData">
            <i class="el-icon-refresh"></i>
          </button>
          <button class="hero-action" type="button" title="更多">
            <i class="el-icon-more"></i>
          </button>
        </div>
      </div>
    </section>

    <section class="news-summary-grid">
      <article class="summary-card">
        <span class="summary-label">监控平台</span>
        <strong class="summary-value">{{ boardList.length }}</strong>
        <small class="summary-foot">覆盖微博、抖音、知乎、资讯与技术平台</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">热榜条目</span>
        <strong class="summary-value">{{ totalItems }}</strong>
        <small class="summary-foot">按当前已加载的平台榜单条目汇总</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">更新频率</span>
        <strong class="summary-value summary-value--small">30 秒</strong>
        <small class="summary-foot">页面维持自动轮询，也可手动刷新单个平台</small>
      </article>
    </section>

    <section class="board-grid">
      <article v-for="(board, index) in boardList" :key="board.title" class="board-card">
        <transition name="fade">
          <div v-if="board.loading" class="loading-mask">
            <div class="loading-spinner"></div>
            <span>加载中</span>
          </div>
        </transition>

        <header class="board-card__header">
          <div class="board-card__title-wrap">
            <div class="board-card__logo-wrap">
              <img v-if="board.icon" :src="`${baseUrl}${board.icon}`" :alt="board.title" class="board-card__logo" />
            </div>
            <div>
              <h2>{{ board.title }}</h2>
              <p>{{ getBoardDescription(board.tag) }}</p>
            </div>
          </div>

          <span class="board-card__tag" :class="getTagClass(board.tag)">{{ board.tag }}</span>
        </header>

        <div class="board-card__body custom-scrollbar">
          <template v-if="board.items.length > 0">
            <button
              v-for="(item, i) in board.items"
              :key="`${board.title}-${i}`"
              class="board-item"
              type="button"
              @click="onToNewsDetails(item)"
            >
              <span class="board-item__rank" :class="`rank-${i + 1}`">{{ i + 1 }}</span>
              <div class="board-item__copy">
                <strong>{{ item.title }}</strong>
                <small>{{ getItemHint(i) }}</small>
              </div>
              <i class="el-icon-arrow-right board-item__arrow"></i>
            </button>
          </template>

          <div v-else class="empty-state">
            <i class="el-icon-document-delete"></i>
            <span>暂无榜单数据</span>
          </div>
        </div>

        <footer class="board-card__footer">
          <span class="board-card__time">
            <i class="el-icon-time"></i>
            {{ getRelativeTime(board.lastUpdate) }}
          </span>
          <button class="refresh-btn" type="button" title="更新数据" @click="refreshBoard(index)">
            <i class="el-icon-refresh"></i>
          </button>
        </footer>
      </article>
    </section>
  </div>
</template>

<script>
import { getNewsTop10 } from '@/api/news'

const DEFAULT_ITEMS = {
  XINLANGB: [
    { title: '2025年度热词有哪些？', url: '#' },
    { title: 'AI 辅助编程进入主流了吗？', url: '#' },
    { title: '未来科技趋势展望报告', url: '#' },
    { title: 'SpaceX 新一代星舰发射成功', url: '#' },
    { title: '全球气候变化峰会开幕', url: '#' }
  ],
  DOUYIN: [
    { title: '春季穿搭灵感合集冲上榜单', url: '#' },
    { title: 'AI 生成视频特效玩法持续走热', url: '#' },
    { title: '短剧叙事模板成为热门创作方向', url: '#' }
  ],
  ZHIHU: [
    { title: '为什么越来越多人开始系统学习 AI？', url: '#' },
    { title: '知识库问答系统落地时最难的环节是什么？', url: '#' },
    { title: '2026 年前端工程化还有哪些关键变化？', url: '#' }
  ],
  PENGPAI: [
    { title: '产业升级带动新一轮数字化投资', url: '#' },
    { title: '多地发布新消费扶持政策', url: '#' },
    { title: '国际科技企业继续加码生成式 AI 布局', url: '#' }
  ],
  WANGYIXINWEN: [
    { title: '青年消费趋势报告发布', url: '#' },
    { title: '热门城市文旅热度持续升温', url: '#' },
    { title: '新品牌如何通过内容营销破圈', url: '#' }
  ],
  CSDN: [
    { title: 'Java 微服务性能优化实践', url: '#' },
    { title: '前端工程中如何构建统一主题系统', url: '#' },
    { title: 'RAG 检索增强应用的典型架构总结', url: '#' }
  ]
}

export default {
  name: 'NewList',
  data() {
    return {
      baseUrl: process.env.VUE_APP_BASE_API,
      currentTime: '',
      lunarDate: '',
      timer: null,
      timer1: null,
      boardList: [
        {
          title: '新浪微博',
          tag: '热搜',
          icon: '/upload/newIcon/xlwb.png',
          dataKey: 'XINLANGB',
          lastUpdate: new Date(Date.now() - 5 * 60 * 1000),
          items: DEFAULT_ITEMS.XINLANGB,
          loading: false
        },
        {
          title: '抖音',
          tag: '视频',
          icon: '/upload/newIcon/dy.png',
          dataKey: 'DOUYIN',
          lastUpdate: new Date(Date.now() - 60 * 60 * 1000),
          items: DEFAULT_ITEMS.DOUYIN,
          loading: false
        },
        {
          title: '知乎',
          tag: '讨论',
          icon: '/upload/newIcon/zh.png',
          dataKey: 'ZHIHU',
          lastUpdate: new Date(Date.now() - 3 * 60 * 60 * 1000),
          items: DEFAULT_ITEMS.ZHIHU,
          loading: false
        },
        {
          title: '澎湃新闻',
          tag: '时事',
          icon: '/upload/newIcon/pp.png',
          dataKey: 'PENGPAI',
          lastUpdate: new Date(Date.now() - 2 * 60 * 60 * 1000),
          items: DEFAULT_ITEMS.PENGPAI,
          loading: false
        },
        {
          title: '网易新闻',
          tag: '综合',
          icon: '/upload/newIcon/wy.png',
          dataKey: 'WANGYIXINWEN',
          lastUpdate: new Date(Date.now() - 30 * 60 * 1000),
          items: DEFAULT_ITEMS.WANGYIXINWEN,
          loading: false
        },
        {
          title: 'CSDN',
          tag: '技术',
          icon: '/upload/newIcon/csdn.png',
          dataKey: 'CSDN',
          lastUpdate: new Date(Date.now() - 10 * 60 * 1000),
          items: DEFAULT_ITEMS.CSDN,
          loading: false
        }
      ]
    }
  },
  computed: {
    totalItems() {
      return this.boardList.reduce((sum, board) => sum + board.items.length, 0)
    }
  },
  methods: {
    getBoardDescription(tag) {
      const descriptionMap = {
        热搜: '追踪全网最强传播话题',
        视频: '观察短视频平台热度迁移',
        讨论: '聚焦问答社区高价值讨论',
        时事: '查看新闻平台实时资讯热点',
        综合: '聚合大众关注的综合热榜',
        技术: '关注开发者社区内容走向'
      }

      return descriptionMap[tag] || '实时观察平台内容热度变化'
    },
    getTagClass(tag) {
      return `tag-${tag}`
    },
    getItemHint(index) {
      if (index === 0) {
        return '当前平台最热内容'
      }
      if (index < 3) {
        return '持续高热度讨论'
      }
      return '热点追踪中'
    },
    onToNewsDetails(row) {
      if (row.url) {
        window.open(row.url)
      }
    },
    applyBoardData(payload = {}) {
      this.boardList = this.boardList.map(board => {
        const items = Array.isArray(payload[board.dataKey]) && payload[board.dataKey].length
          ? payload[board.dataKey]
          : board.items

        return {
          ...board,
          items,
          loading: false,
          lastUpdate: new Date()
        }
      })
    },
    getData() {
      this.boardList.forEach(board => {
        board.loading = true
      })

      getNewsTop10({})
        .then(res => {
          if (res && res.data.code === 200 && res.data.data) {
            this.applyBoardData(res.data.data)
          } else {
            this.boardList.forEach(board => {
              board.loading = false
            })
          }
        })
        .catch(() => {
          this.boardList.forEach(board => {
            board.loading = false
          })
        })
    },
    getRelativeTime(time) {
      if (!time) return ''

      const diff = (Date.now() - new Date(time)) / 1000
      if (diff < 60) return '刚刚更新'
      if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
      if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
      return `${Math.floor(diff / 86400)} 天前`
    },
    refreshBoard(index) {
      this.boardList[index].loading = true

      getNewsTop10({})
        .then(res => {
          if (res && res.data.code === 200 && res.data.data) {
            const board = this.boardList[index]
            const nextItems = Array.isArray(res.data.data[board.dataKey]) && res.data.data[board.dataKey].length
              ? res.data.data[board.dataKey]
              : board.items

            this.$set(this.boardList, index, {
              ...board,
              items: nextItems,
              loading: false,
              lastUpdate: new Date()
            })
          } else {
            this.boardList[index].loading = false
          }
        })
        .catch(() => {
          this.boardList[index].loading = false
        })
    },
    updateTime() {
      const now = new Date()
      const pad = n => n.toString().padStart(2, '0')
      const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

      this.currentTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
      this.lunarDate = `实时热榜监控 · ${week[now.getDay()]}`
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
    this.getData()
    this.timer1 = setInterval(() => {
      this.getData()
    }, 30 * 1000)
  },
  beforeDestroy() {
    clearInterval(this.timer)
    clearInterval(this.timer1)
  }
}
</script>

<style scoped lang="less">
.news-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  color: var(--apple-text-primary);
}

.news-hero,
.summary-card,
.board-card {
  background: var(--apple-surface);
  border-radius: var(--apple-radius-xl);
  box-shadow: var(--apple-shadow);
}

.news-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 30px;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.12), transparent 34%),
    radial-gradient(circle at bottom right, rgba(94, 92, 230, 0.12), transparent 28%),
    var(--apple-surface);
}

.hero-copy {
  max-width: 760px;
}

.hero-kicker {
  display: inline-block;
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--apple-text-secondary);
}

.hero-copy h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.12;
  color: var(--apple-text-primary);
}

.hero-copy p {
  margin: 12px 0 0;
  line-height: 1.75;
  color: var(--apple-text-secondary);
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-time {
  min-width: 260px;
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--apple-surface-soft);
  box-shadow: var(--apple-shadow-inset);
  text-align: right;
}

.hero-time strong {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text-primary);
}

.hero-time span {
  display: block;
  margin-top: 6px;
  color: var(--apple-text-secondary);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.hero-action,
.refresh-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 14px;
  background: var(--apple-surface-soft);
  color: var(--apple-text-secondary);
  box-shadow: var(--apple-shadow);
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.hero-action:hover,
.refresh-btn:hover {
  transform: translateY(-1px);
  background: var(--apple-blue-soft);
  color: var(--apple-blue);
}

.news-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 24px;
}

.summary-label,
.summary-foot {
  color: var(--apple-text-secondary);
}

.summary-label {
  font-size: 13px;
}

.summary-value {
  font-size: 32px;
  line-height: 1.15;
  font-weight: 700;
  color: var(--apple-text-primary);
}

.summary-value--small {
  font-size: 26px;
}

.summary-foot {
  font-size: 13px;
  line-height: 1.55;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: 360px;
  gap: 18px;
  align-items: stretch;
}

.board-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 360px;
  overflow: hidden;
}

.board-card__header,
.board-card__footer {
  padding: 14px 16px 10px;
}

.board-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--apple-divider);
}

.board-card__title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.board-card__logo-wrap {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 14px;
  background: var(--apple-blue-soft);
  box-shadow: var(--apple-shadow-inset);
}

.board-card__logo {
  width: 26px;
  height: 26px;
  object-fit: cover;
  border-radius: 8px;
}

.board-card__title-wrap h2 {
  margin: 0;
  font-size: 17px;
  line-height: 1.25;
  color: var(--apple-text-primary);
}

.board-card__title-wrap p {
  margin: 4px 0 0;
  color: var(--apple-text-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.board-card__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: var(--apple-blue-soft);
  color: var(--apple-blue);
  white-space: nowrap;
}

.board-card__body {
  flex: 1;
  min-height: 0;
  padding: 6px 10px 2px 16px;
  overflow-y: auto;
}

.board-item {
  width: 100%;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 12px;
  align-items: start;
  column-gap: 10px;
  padding: 9px 8px 9px 0;
  border: none;
  border-bottom: 1px solid var(--apple-divider);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s ease;
}

.board-item:hover {
  background: var(--apple-hover);
}

.board-item__rank {
  text-align: center;
  padding-top: 3px;
  font-size: 14px;
  font-weight: 700;
  color: var(--apple-text-secondary);
}

.rank-1 {
  color: #ff453a;
}

.rank-2 {
  color: #ff9f0a;
}

.rank-3 {
  color: #30b0c7;
}

.board-item__copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.board-item__copy strong {
  color: var(--apple-text-primary);
  font-size: 14px;
  line-height: 1.36;
  font-weight: 600;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
  word-break: break-word;
}

.board-item__copy small {
  color: var(--apple-text-secondary);
  font-size: 11px;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.board-item__arrow {
  color: var(--apple-text-secondary);
  font-size: 12px;
  justify-self: end;
  align-self: center;
  opacity: 0.72;
}

.board-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--apple-divider);
  padding-top: 8px;
}

.board-card__time {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--apple-text-secondary);
  font-size: 12px;
}

.loading-mask {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(245, 245, 247, 0.68);
  backdrop-filter: blur(10px);
}

html[data-theme='dark'] .loading-mask {
  background: rgba(0, 0, 0, 0.48);
}

.loading-mask span {
  color: var(--apple-text-secondary);
  font-size: 13px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 3px solid rgba(0, 122, 255, 0.14);
  border-top-color: var(--apple-blue);
  animation: newsSpin 0.9s linear infinite;
}

.empty-state {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--apple-text-secondary);
}

.empty-state i {
  font-size: 28px;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--apple-divider-strong);
  border-radius: 999px;
}

@keyframes newsSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1280px) {
  .board-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-auto-rows: 360px;
  }

  .news-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .news-hero {
    flex-direction: column;
  }

  .hero-meta {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 820px) {
  .board-grid,
  .news-summary-grid {
    grid-template-columns: 1fr;
  }

  .board-grid {
    grid-auto-rows: 340px;
  }

  .news-hero {
    padding: 22px 20px;
  }

  .hero-meta {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-time {
    min-width: 0;
    text-align: left;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .board-card__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .board-card {
    height: 340px;
  }
}
</style>
