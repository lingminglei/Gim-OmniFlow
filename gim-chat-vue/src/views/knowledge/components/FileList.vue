<template>
  <section class="knowledge-file-shell">
    <header class="file-shell-header">
      <div>
        <span class="file-shell-kicker">DOCUMENT WORKSPACE</span>
        <h3>文件清单</h3>
        <p>当前知识库“{{ title }}”共 {{ files.length }} 个文件，可在此继续上传、预览和删除。</p>
      </div>

      <div class="file-shell-actions">
        <el-upload
          class="upload-trigger"
          action="/api/knowledge/upload"
          :file-list="fileList"
          :data="{ knowledgeCode: knowledgeCode }"
          :before-upload="beforeUpload"
          :on-success="handleSuccess"
          :on-error="handleError"
          :show-file-list="false"
          :limit="1"
          :on-exceed="handleExceed"
          multiple
          auto-upload
          list-type="text"
        >
          <el-button type="primary" size="small" class="upload-btn">
            <i class="el-icon-upload2"></i>
            上传文件
          </el-button>
        </el-upload>

        <el-link class="view-more-link" @click="handleViewMore">查看全部文件</el-link>
      </div>
    </header>

    <div class="file-table-shell">
      <el-table v-if="files.length" :data="files" class="knowledge-file-table">
        <el-table-column label="文件名称" min-width="260">
          <template slot-scope="scope">
            <div class="file-name-cell">
              <span class="file-icon-badge">
                <i :class="getFileIcon(scope.row.fileType)"></i>
              </span>
              <div class="file-name-copy">
                <span class="file-name-text">{{ scope.row.fileName }}</span>
                <small>{{ scope.row.fileType || '未知类型' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="上传时间" min-width="180" prop="createTime" />
        <el-table-column label="修改时间" min-width="180" prop="updateTime" />
        <el-table-column label="大小" width="110" prop="fileSizeStr" />

        <el-table-column label="状态" width="120">
          <template slot-scope="scope">
            <span
              class="status-chip"
              :class="{ 'is-success': scope.row.statusType === '已处理', 'is-warning': scope.row.statusType !== '已处理' }"
            >
              {{ scope.row.statusType || '待处理' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-view" @click="$file.handleKnowledgeFileNameClick(scope.row)">
              预览
            </el-button>
            <el-button type="text" icon="el-icon-delete" class="delete-btn" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="file-empty-state">
        <el-empty :image-size="150" description="当前知识库暂无文件，点击右上角上传开始接入"></el-empty>
      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'FileList',
  props: {
    title: {
      type: String,
      default: '知识库文件'
    },
    files: {
      type: Array,
      required: true
    },
    knowledgeCode: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      fileList: [],
      allowedTypes: [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain'
      ],
      loadingInstance: null
    }
  },
  methods: {
    beforeUpload(file) {
      const isAllowed = this.allowedTypes.includes(file.type)
      const isLt5M = file.size / 1024 / 1024 < 5

      if (!isAllowed) {
        this.$message.error('仅支持 PDF、Word、Excel、TXT 文档上传')
        return false
      }

      if (!isLt5M) {
        this.$message.error('文件大小不能超过 5MB')
        return false
      }

      this.loadingInstance = this.$loading({
        lock: true,
        text: '文件上传成功后，系统将继续处理，请稍候…',
        spinner: 'el-icon-loading',
        background: 'rgba(15, 23, 42, 0.28)'
      })

      return true
    },
    handleSuccess(response, file) {
      this.closeLoading()
      this.$emit('dataLoadding')
      this.$message.success(`“${file.name}”上传成功`)
      this.fileList = []
    },
    handleError(err, file) {
      this.closeLoading()
      this.$message.error(`“${file.name}”上传失败`)
      this.fileList = []
    },
    handleExceed() {
      this.$message.warning('最多一次上传 1 个文件')
    },
    closeLoading() {
      if (this.loadingInstance) {
        this.loadingInstance.close()
        this.loadingInstance = null
      }
    },
    getFileIcon(fileType) {
      switch (fileType) {
        case 'pdf':
          return 'el-icon-document'
        case 'docx':
          return 'el-icon-document-copy'
        case 'xlsx':
          return 'el-icon-s-data'
        case 'pptx':
          return 'el-icon-s-marketing'
        default:
          return 'el-icon-document'
      }
    },
    handleDelete(file) {
      this.$confirm(`确定删除 ${file.fileName} 吗？`, '删除确认')
        .then(() => {
          this.$emit('fileDelete', file)
        })
        .catch(() => {})
    },
    handleViewMore() {
      this.$message('当前已展示该知识库全部文件')
    }
  }
}
</script>

<style scoped lang="less">
.knowledge-file-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.file-shell-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.file-shell-kicker {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: var(--apple-text-secondary);
}

.file-shell-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--apple-text-primary);
}

.file-shell-header p {
  margin: 8px 0 0;
  line-height: 1.65;
  color: var(--apple-text-secondary);
}

.file-shell-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.upload-btn {
  min-width: 108px;
}

.view-more-link {
  font-size: 13px;
  color: var(--apple-blue) !important;
}

.file-table-shell {
  border-radius: 24px;
  background: var(--apple-surface-soft);
  box-shadow: var(--apple-shadow-inset);
  overflow: hidden;
}

.knowledge-file-table {
  background: transparent !important;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon-badge {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: var(--apple-blue-soft);
  color: var(--apple-blue);
}

.file-name-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name-text {
  color: var(--apple-text-primary);
  font-weight: 600;
}

.file-name-copy small {
  color: var(--apple-text-secondary);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 159, 10, 0.14);
  color: #c76e00;
  font-size: 12px;
  font-weight: 600;
}

.status-chip.is-success {
  background: rgba(52, 199, 89, 0.16);
  color: #1d8a3d;
}

.delete-btn {
  color: #ff453a !important;
}

.file-empty-state {
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

::v-deep .knowledge-file-table .el-table__header-wrapper th {
  color: var(--apple-text-secondary) !important;
  font-weight: 600;
}

::v-deep .knowledge-file-table .el-table__body-wrapper td .cell,
::v-deep .knowledge-file-table .el-table__header-wrapper th .cell,
::v-deep .knowledge-file-table .el-table__body-wrapper td {
  color: var(--apple-text-primary) !important;
}

::v-deep .knowledge-file-table .el-table__body tr:hover > td {
  background: rgba(0, 122, 255, 0.06) !important;
}

::v-deep .el-empty__description p {
  color: var(--apple-text-secondary);
}

@media (max-width: 900px) {
  .file-shell-header {
    flex-direction: column;
  }

  .file-shell-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
