<template>
  <article class="knowledge-card" :class="{ 'is-active': active }" @click="$emit('card-click', card)">
    <div class="card-top">
      <div class="card-icon-wrap" :style="iconWrapStyle">
        <i :class="card.icon" class="card-icon"></i>
      </div>

      <div class="card-actions" @click.stop>
        <el-dropdown trigger="click" placement="bottom-end" popper-class="knowledge-card-dropdown" @command="handleCommand">
          <button class="action-trigger" type="button" aria-label="更多操作">
            <i class="el-icon-more"></i>
          </button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="edit">编辑</el-dropdown-item>
            <el-dropdown-item command="delete" class="is-danger">删除</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>

    <div class="card-content">
      <h3 class="card-title">{{ card.name }}</h3>
      <p class="card-desc">{{ card.description }}</p>
    </div>

    <footer class="card-meta">
      <span class="meta-item">
        <i class="el-icon-document"></i>
        {{ formatCount(card.fileCount) }} 个文件
      </span>
      <span class="meta-item">{{ card.updateTimeStr || card.updateTime || '--' }}</span>
    </footer>

    <knowledge-edit-modal :visible.sync="showEdit" :data-info="editingData" @submit="handleEditKnowledge" />
  </article>
</template>

<script>
import { deleteKnowledge, handleEditKnowledge as editKnowledge } from '@/api/knowledge'
import KnowledgeEditModal from './KnowledgeEditModal.vue'

export default {
  name: 'KnowledgeCard',
  components: { KnowledgeEditModal },
  props: {
    card: {
      type: Object,
      required: true
    },
    active: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      showEdit: false,
      editingData: {}
    }
  },
  computed: {
    iconWrapStyle() {
      const accent = this.card.accentColor || '#007aff'
      return {
        background: `linear-gradient(135deg, ${this.toRgba(accent, 0.18)}, ${this.toRgba(accent, 0.08)})`,
        color: accent
      }
    }
  },
  methods: {
    formatCount(value) {
      const count = Number(value)
      return Number.isFinite(count) ? count : 0
    },
    toRgba(hexColor, opacity) {
      const normalized = (hexColor || '').replace('#', '')
      if (![3, 6].includes(normalized.length)) {
        return `rgba(0, 122, 255, ${opacity})`
      }

      const hex = normalized.length === 3
        ? normalized.split('').map(char => `${char}${char}`).join('')
        : normalized

      const red = parseInt(hex.slice(0, 2), 16)
      const green = parseInt(hex.slice(2, 4), 16)
      const blue = parseInt(hex.slice(4, 6), 16)

      return `rgba(${red}, ${green}, ${blue}, ${opacity})`
    },
    handleCommand(command) {
      if (command === 'edit') {
        this.handleEdit()
        return
      }

      if (command === 'delete') {
        this.handleDelete()
      }
    },
    handleEdit() {
      this.editingData = JSON.parse(JSON.stringify(this.card))
      this.showEdit = true
    },
    async handleEditKnowledge(data) {
      const res = await editKnowledge({
        ...data
      })

      if (res && res.data.code === 200) {
        this.$message.success('知识库编辑成功')
        this.showEdit = false
        this.$emit('loadding')
      } else {
        this.$message.error(res.data.message || '知识库编辑失败，请稍后重试')
      }
    },
    handleDelete() {
      this.$confirm('此操作将永久删除该知识库，是否继续？', '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteKnowledge({
          knowledgeCode: this.card.knowledgeCode
        }).then(res => {
          if (res.data.code === 200) {
            this.$message.success('知识库删除成功')
            this.$emit('loadding')
          } else {
            this.$message.error('知识库删除失败，请稍后重试')
          }
        }).catch(() => {
          this.$message.error('网络错误，请稍后重试')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="less">
.knowledge-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 220px;
  padding: 20px;
  border-radius: 24px;
  background: var(--apple-surface);
  box-shadow: var(--apple-shadow);
  transition: transform 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease;
  cursor: pointer;
}

.knowledge-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--apple-shadow-lg);
}

.knowledge-card.is-active {
  box-shadow: inset 0 0 0 1px rgba(0, 122, 255, 0.18), var(--apple-shadow-lg);
  background:
    radial-gradient(circle at top right, rgba(0, 122, 255, 0.08), transparent 36%),
    var(--apple-surface);
}

.card-top,
.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-icon-wrap {
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  box-shadow: var(--apple-shadow-inset);
}

.card-icon {
  font-size: 26px;
}

.action-trigger {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--apple-text-secondary);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.action-trigger:hover {
  background: var(--apple-hover);
  color: var(--apple-text-primary);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 90px;
}

.card-title {
  margin: 0;
  font-size: 20px;
  color: var(--apple-text-primary);
}

.card-desc {
  margin: 0;
  color: var(--apple-text-secondary);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  padding-top: 14px;
  border-top: 1px solid var(--apple-divider);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--apple-text-secondary);
}
</style>
