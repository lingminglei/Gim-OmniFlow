<template>
  <div class="knowledge-list">
    <div v-if="cardList.length === 0" class="knowledge-list-empty">
      <el-empty :image-size="180" description="暂无知识库，点击上方按钮创建"></el-empty>
    </div>

    <div v-else class="knowledge-grid">
      <knowledge-card
        v-for="card in cardList"
        :key="card.knowledgeCode || card.name"
        :card="card"
        :active="card.knowledgeCode === activeCode"
        @card-click="$emit('card-click', card)"
        @loadding="$emit('loadding')"
      />
    </div>
  </div>
</template>

<script>
import { getFileListByPath } from '@/api/knowledge'
import KnowledgeCard from './KnowledgeCard.vue'

const CARD_ICONS = [
  'el-icon-notebook-2',
  'el-icon-collection-tag',
  'el-icon-reading',
  'el-icon-document-copy',
  'el-icon-files',
  'el-icon-data-analysis'
]

const CARD_COLORS = ['#007aff', '#5e5ce6', '#34c759', '#ff9f0a', '#30b0c7', '#ff375f']

export default {
  name: 'KnowledgeList',
  components: { KnowledgeCard },
  props: {
    activeCode: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      pageData: {
        current: 1,
        size: 10
      },
      cardList: []
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    decorateCard(record, index) {
      return {
        ...record,
        icon: record.icon || CARD_ICONS[index % CARD_ICONS.length],
        accentColor: record.accentColor || record.bgColor || CARD_COLORS[index % CARD_COLORS.length],
        description: record.description || '暂无描述，建议补充该知识库的定位和内容范围。'
      }
    },
    getList() {
      getFileListByPath({
        ...this.pageData
      }).then(res => {
        if (res.data.code === 200) {
          const records = Array.isArray(res.data.data.records) ? res.data.data.records : []
          this.cardList = records.map((item, index) => this.decorateCard(item, index))
          this.$emit('loaded', this.cardList)
        } else {
          this.cardList = []
          this.$emit('loaded', [])
          this.$message.error(res.data.message)
        }
      }).catch(() => {
        this.cardList = []
        this.$emit('loaded', [])
        this.$message.error('知识库列表加载失败，请稍后重试')
      })
    }
  }
}
</script>

<style scoped lang="less">
.knowledge-list {
  width: 100%;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.knowledge-list-empty {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
  border-radius: 24px;
  background: var(--apple-surface-soft);
  box-shadow: var(--apple-shadow-inset);
}

::v-deep .el-empty__description p {
  color: var(--apple-text-secondary);
}

@media (max-width: 768px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
