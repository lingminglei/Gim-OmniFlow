<template>
  <el-dialog
    :visible.sync="visible"
    width="640px"
    :close-on-click-modal="false"
    :show-close="true"
    :append-to-body="true"
    custom-class="knowledge-modal"
    @close="handleClose"
  >
    <div class="knowledge-modal__content">
      <header class="knowledge-modal__header">
        <div class="knowledge-modal__icon">
          <i class="el-icon-edit-outline"></i>
        </div>
        <div>
          <span class="knowledge-modal__kicker">EDIT KNOWLEDGE BASE</span>
          <h3>编辑知识库</h3>
          <p>更新当前知识库的名称和描述，保存后知识库列表会立即刷新。</p>
        </div>
      </header>

      <el-form ref="form" :model="form" :rules="rules" label-position="top" class="knowledge-form">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="知识库描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请补充当前知识库的内容范围和用途"
          />
        </el-form-item>
      </el-form>

      <footer class="knowledge-modal__footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存修改</el-button>
      </footer>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'KnowledgeEditModal',
  props: {
    visible: {
      type: Boolean,
      required: true
    },
    dataInfo: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      form: {
        knowledgeCode: '',
        name: '',
        description: ''
      },
      rules: {
        name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
        description: [{ max: 500, message: '最多输入 500 个字符', trigger: 'blur' }]
      }
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(isVisible) {
        if (isVisible) {
          this.syncForm()
        }
      }
    },
    dataInfo: {
      deep: true,
      handler() {
        if (this.visible) {
          this.syncForm()
        }
      }
    }
  },
  methods: {
    syncForm() {
      this.form = {
        knowledgeCode: this.dataInfo.knowledgeCode || '',
        name: this.dataInfo.name || '',
        description: this.dataInfo.description || ''
      }
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.$emit('submit', { ...this.form })
        }
      })
    },
    handleClose() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style lang="less">
.knowledge-modal {
  border-radius: 28px !important;
  background: var(--apple-surface-elevated) !important;
  box-shadow: var(--apple-shadow-lg) !important;
  overflow: hidden;
}

.knowledge-modal .el-dialog__header {
  display: none;
}

.knowledge-modal .el-dialog__body {
  padding: 0 !important;
}

.knowledge-modal__content {
  padding: 28px 28px 24px;
  color: var(--apple-text-primary);
}

.knowledge-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 22px;
}

.knowledge-modal__icon {
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: var(--apple-blue-soft);
  color: var(--apple-blue);
  font-size: 28px;
}

.knowledge-modal__kicker {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: var(--apple-text-secondary);
}

.knowledge-modal__header h3 {
  margin: 0;
  font-size: 24px;
  color: var(--apple-text-primary);
}

.knowledge-modal__header p {
  margin: 8px 0 0;
  line-height: 1.65;
  color: var(--apple-text-secondary);
}

.knowledge-form .el-form-item {
  margin-bottom: 18px;
}

.knowledge-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}
</style>
