<template>
  <div class="knowledge-page">
    <section class="knowledge-hero">
      <div class="hero-copy">
        <span class="hero-kicker">KNOWLEDGE HUB</span>
        <h1 class="hero-title">知识库中心</h1>
        <p class="hero-desc">
          统一管理知识库资产、文件接入与知识沉淀，保持结构清晰、内容可追踪、维护更高效。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" icon="el-icon-plus" class="hero-action-btn" @click="showCreate = true">
          创建知识库
        </el-button>
      </div>
    </section>

    <section class="knowledge-summary-grid">
      <article class="knowledge-summary-card">
        <span class="summary-label">知识库总数</span>
        <strong class="summary-value">{{ summaryMetrics.totalLibraries }}</strong>
        <span class="summary-foot">按当前列表返回结果统计</span>
      </article>
      <article class="knowledge-summary-card">
        <span class="summary-label">文件总数</span>
        <strong class="summary-value">{{ summaryMetrics.totalFiles }}</strong>
        <span class="summary-foot">汇总所有知识库内的文件数量</span>
      </article>
      <article class="knowledge-summary-card">
        <span class="summary-label">最近更新</span>
        <strong class="summary-value summary-value--time">{{ summaryMetrics.latestUpdate }}</strong>
        <span class="summary-foot">取当前列表中最近一次更新时间</span>
      </article>
    </section>

    <section class="knowledge-library-shell">
      <header class="section-header">
        <div>
          <h2>知识库概览</h2>
          <p>浏览全部知识库概况，点击卡片查看文件详情与管理操作。</p>
        </div>
        <span class="section-meta">{{ knowledgeCards.length }} 个知识库</span>
      </header>

      <knowledge-list
        ref="knowledgeList"
        :active-code="selectedKnowledgeCode"
        @card-click="handleCardClick"
        @loaded="handleKnowledgeLoaded"
        @loadding="handleLoadding"
      />
    </section>

    <section class="knowledge-detail-shell">
      <template v-if="selectedKnowledge">
        <header class="detail-header">
          <div class="detail-copy">
            <span class="detail-kicker">CURRENT KNOWLEDGE BASE</span>
            <h2>{{ selectedKnowledge.name }}</h2>
            <p>{{ selectedKnowledge.description || '暂无描述，当前知识库已就绪，可继续上传与管理文件。' }}</p>
          </div>
          <div class="detail-metrics">
            <div class="detail-metric">
              <span>知识库编码</span>
              <strong>{{ selectedKnowledge.knowledgeCode || '--' }}</strong>
            </div>
            <div class="detail-metric">
              <span>文件数量</span>
              <strong>{{ formatCount(selectedKnowledge.fileCount) }}</strong>
            </div>
            <div class="detail-metric">
              <span>最近更新时间</span>
              <strong>{{ selectedKnowledge.updateTimeStr || selectedKnowledge.updateTime || '--' }}</strong>
            </div>
          </div>
        </header>

        <file-list
          :files="fileList"
          :title="selectedKnowledge.name"
          :knowledge-code="selectedKnowledge.knowledgeCode"
          @fileDelete="handleFileDelete"
          @dataLoadding="handleFileUpload"
        />
      </template>

      <div v-else class="detail-empty-state">
        <div class="detail-empty-card">
          <span class="detail-empty-kicker">DETAIL WORKSPACE</span>
          <h3>请选择一个知识库</h3>
          <p>点击上方任一知识库卡片后，这里会展示该知识库的文件清单、上传入口和相关操作。</p>
        </div>
      </div>
    </section>

    <knowledge-create-modal :visible.sync="showCreate" @submit="handleCreate" />
  </div>
</template>

<script>
import KnowledgeList from '../knowledge/components/KnowledgeList.vue'
import FileList from '../knowledge/components/FileList.vue'
import { getFileListByType, createKnowledge, deleteKnowledgeFile } from '@/api/knowledge'
import KnowledgeCreateModal from '../knowledge/components/KnowledgeCreateModal.vue'

export default {
  name: 'KnowledgeView',
  components: { KnowledgeList, FileList, KnowledgeCreateModal },
  data() {
    return {
      selectedKnowledge: null,
      fileList: [],
      showCreate: false,
      knowledgeCards: []
    }
  },
  computed: {
    selectedKnowledgeCode() {
      return this.selectedKnowledge ? this.selectedKnowledge.knowledgeCode : ''
    },
    summaryMetrics() {
      const totalLibraries = this.knowledgeCards.length
      const totalFiles = this.knowledgeCards.reduce((sum, item) => {
        return sum + this.formatCount(item.fileCount)
      }, 0)

      const latestRecord = this.knowledgeCards.reduce((latest, current) => {
        const latestTimestamp = this.getTimestamp(latest)
        const currentTimestamp = this.getTimestamp(current)
        return currentTimestamp > latestTimestamp ? current : latest
      }, null)

      return {
        totalLibraries,
        totalFiles,
        latestUpdate: latestRecord
          ? latestRecord.updateTimeStr || latestRecord.updateTime || '--'
          : '--'
      }
    }
  },
  methods: {
    formatCount(value) {
      const count = Number(value)
      return Number.isFinite(count) ? count : 0
    },
    getTimestamp(record) {
      if (!record) {
        return 0
      }
      const rawValue = record.updateTime || record.updateTimeStr || record.createTime || ''
      const timestamp = new Date(rawValue).getTime()
      return Number.isFinite(timestamp) ? timestamp : 0
    },
    handleKnowledgeLoaded(list) {
      this.knowledgeCards = Array.isArray(list) ? list : []

      if (!this.selectedKnowledgeCode) {
        return
      }

      const matched = this.knowledgeCards.find(item => item.knowledgeCode === this.selectedKnowledgeCode)
      if (matched) {
        this.selectedKnowledge = matched
      } else {
        this.selectedKnowledge = null
        this.fileList = []
      }
    },
    handleFileUpload() {
      if (this.selectedKnowledgeCode) {
        this.getList(this.selectedKnowledgeCode)
      }
    },
    handleFileDelete(file) {
      const param = {
        fileId: file.id
      }

      deleteKnowledgeFile(param).then(res => {
        if (res.data.code === 200) {
          this.$message.success('文件删除成功')
          this.getList(this.selectedKnowledgeCode)
        } else {
          this.$message.error(res.data.message || '文件删除失败，请稍后重试')
        }
      }).catch(() => {
        this.$message.error('删除文件失败，请稍后重试')
      })
    },
    async handleCreate(data) {
      if (!data || !data.name) {
        this.$message.error('知识库名称不能为空')
        return
      }

      if (!data || !data.description) {
        this.$message.error('知识库描述不能为空')
        return
      }

      const res = await createKnowledge({ ...data })

      if (res.data.code === 200) {
        this.$message.success('知识库创建成功')
        this.showCreate = false
        this.$refs.knowledgeList.getList()
      } else {
        this.$message.error(res.data.message || '知识库创建失败，请稍后重试')
      }
    },
    handleCardClick(card) {
      this.selectedKnowledge = card
      this.getList(card.knowledgeCode)
    },
    async getList(code) {
      const res = await getFileListByType({
        knowledgeCode: code
      })

      if (res.data.code === 200) {
        this.fileList = Array.isArray(res.data.data) ? res.data.data : []
      } else {
        this.fileList = []
      }
    },
    handleLoadding() {
      this.$refs.knowledgeList.getList()
    }
  }
}
</script>

<style scoped lang="less">
.knowledge-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 8px 0 24px;
  color: var(--apple-text-primary);
}

.knowledge-hero,
.knowledge-library-shell,
.knowledge-detail-shell,
.knowledge-summary-card {
  background: var(--apple-surface);
  border-radius: var(--apple-radius-xl);
  box-shadow: var(--apple-shadow);
}

.knowledge-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  background:
    radial-gradient(circle at top right, rgba(0, 122, 255, 0.12), transparent 30%),
    radial-gradient(circle at bottom left, rgba(94, 92, 230, 0.12), transparent 28%),
    var(--apple-surface);
}

.hero-copy {
  max-width: 720px;
}

.hero-kicker,
.detail-kicker,
.detail-empty-kicker {
  display: inline-block;
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--apple-text-secondary);
}

.hero-title {
  margin: 0;
  font-size: 34px;
  line-height: 1.12;
  color: var(--apple-text-primary);
}

.hero-desc,
.section-header p,
.detail-copy p,
.detail-empty-card p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: var(--apple-text-secondary);
}

.hero-action-btn {
  min-width: 138px;
  height: 44px;
}

.knowledge-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.knowledge-summary-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 24px;
}

.summary-label,
.summary-foot,
.section-meta,
.detail-metric span {
  color: var(--apple-text-secondary);
}

.summary-label,
.detail-metric span {
  font-size: 13px;
}

.summary-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--apple-text-primary);
}

.summary-value--time {
  font-size: 22px;
}

.summary-foot {
  font-size: 13px;
}

.knowledge-library-shell,
.knowledge-detail-shell {
  padding: 24px 28px 28px;
}

.section-header,
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.section-header {
  margin-bottom: 8px;
}

.section-header h2,
.detail-copy h2 {
  margin: 0;
  font-size: 24px;
  color: var(--apple-text-primary);
}

.section-meta {
  padding: 8px 12px;
  border-radius: 999px;
  background: var(--apple-blue-soft);
  color: var(--apple-blue);
  font-size: 13px;
  font-weight: 600;
}

.detail-header {
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--apple-divider);
}

.detail-copy {
  max-width: 640px;
}

.detail-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 14px;
  min-width: 420px;
}

.detail-metric {
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--apple-surface-soft);
  box-shadow: var(--apple-shadow-inset);
}

.detail-metric strong {
  display: block;
  margin-top: 8px;
  word-break: break-word;
  color: var(--apple-text-primary);
}

.detail-empty-state {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-empty-card {
  width: min(520px, 100%);
  padding: 36px 32px;
  text-align: center;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent),
    var(--apple-surface-soft);
  box-shadow: var(--apple-shadow-inset);
}

.detail-empty-card h3 {
  margin: 0;
  font-size: 28px;
  color: var(--apple-text-primary);
}

@media (max-width: 1280px) {
  .knowledge-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-header {
    flex-direction: column;
  }

  .detail-metrics {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 900px) {
  .knowledge-page {
    gap: 18px;
  }

  .knowledge-hero,
  .knowledge-library-shell,
  .knowledge-detail-shell {
    padding: 20px;
  }

  .knowledge-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .knowledge-summary-grid,
  .detail-metrics {
    grid-template-columns: 1fr;
  }

  .summary-value {
    font-size: 28px;
  }
}
</style>
