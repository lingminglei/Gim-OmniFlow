<template>
  <div class="docs-page">
    <aside class="docs-sidebar">
      <section class="sidebar-brand">
        <div class="brand-mark">
          <i class="el-icon-folder-opened"></i>
        </div>
        <div class="brand-copy">
          <h2>网盘工作台</h2>
        </div>
      </section>

      <section class="sidebar-panel">
        <div class="sidebar-panel-body">
          <div class="nav-scroll">
            <el-menu :default-active="String(fileType)" class="side-menu nav-menu" @select="handleSelect">
              <el-menu-item v-for="item in navItems" :key="item.key" :index="String(item.key)">
                <span class="nav-icon">
                  <i :class="item.icon"></i>
                </span>
                <span class="nav-copy">
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.description }}</small>
                </span>
              </el-menu-item>
            </el-menu>
          </div>

          <section class="capacity-card">
            <div class="capacity-header">
              <div class="capacity-copy">
                <h3>网盘容量计量</h3>
              </div>
            </div>

            <div class="capacity-progress" aria-hidden="true">
              <div class="capacity-progress-track">
                <div class="capacity-progress-fill" :style="{ width: `${storageProgressPercent}%` }"></div>
              </div>
            </div>
            <div class="capacity-grid">
              <article v-for="metric in storageMetrics" :key="metric.label" class="capacity-metric">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}{{ metric.unit }}</strong>
              </article>
            </div>
          </section>
        </div>
      </section>
    </aside>

    <main class="docs-main">
      <section class="workspace-hero">
        <div class="hero-copy">
          <h1>{{ currentSection.title }}</h1>
          <p>{{ currentSection.description }}</p>
        </div>

        <div class="hero-stats">
        </div>
      </section>

      <section class="workspace-shell">
        <header class="workspace-toolbar">
          <OperationMenu :fileType="fileType" :filePath="filePath" @getSearchFileList="getSearchFileList"
            @getTableDataByType="getTableDataByType" />
        </header>

        <section class="workspace-breadcrumb">
          <BreadCrumb class="breadcrumb" :fileType="fileType" :filePath="filePath"
            @getTableDataByType="getTableDataByType" />
        </section>

        <section class="workspace-canvas">
          <FileTable v-if="fileModel === 0" :fileType="fileType" :filePath="filePath" :fileList="fileList"
            :loading.sync="loading" @getTableDataByType="getTableDataByType" @click.native.right="handleClickRight" />

          <FileGrid v-if="fileModel === 1" :fileType="fileType" :filePath="filePath" :fileList="fileList"
            :loading="loading" @getTableDataByType="getTableDataByType" @click.native.right="handleClickRight" />

          <FileTimeLine v-if="fileModel === 2 && fileType === 1" class="timeline-view" :fileList="fileList"
            :loading.sync="loading" @getTableDataByType="getTableDataByType" @click.native.right="handleClickRight" />
        </section>

        <footer v-if="fileType !== 6" class="workspace-pagination">
          <div class="pagination-copy">
          </div>
          <div style="float: right;">
            <el-pagination :current-page="pageData.currentPage" :page-size="pageData.pageCount" :total="pageData.total"
              :page-sizes="[10, 50, 100, 200]"
              :layout="screenWidth <= 768 ? 'total, prev, next, jumper' : 'sizes, total, prev, pager, next'"
              @current-change="handleCurrentChange" @size-change="handleSizeChange" />
          </div>

        </footer>
      </section>
    </main>
  </div>
</template>

<script>
import BreadCrumb from '@/components/file/components/BreadCrumb.vue'
import FileGrid from '@/components/file/components/FileGrid.vue'
import FileTimeLine from '@/components/file/components/FileTimeLine.vue'
import FileTable from '@/components/file/FileTable.vue'
import OperationMenu from '@/components/file/OperationMenu.vue'
import {
  getFileListByPath,
  getFileListByType,
  getRecoveryFile,
  getMyShareFileList,
  searchFile
} from '@/api/file.js'

const NAV_ITEMS = [
  { key: 0, label: '全部文件', description: '所有资源', icon: 'el-icon-folder' },
  { key: 1, label: '图片', description: '视觉素材', icon: 'el-icon-picture-outline' },
  { key: 2, label: '文档', description: '办公内容', icon: 'el-icon-document' },
  { key: 3, label: '视频', description: '影像文件', icon: 'el-icon-video-camera' },
  { key: 4, label: '音乐', description: '音频资源', icon: 'el-icon-headset' },
  { key: 5, label: '其他', description: '混合类型', icon: 'el-icon-takeaway-box' },
  { key: 6, label: '回收站', description: '近期删除', icon: 'el-icon-delete' }
]

const SECTION_META = {
  0: {
    label: '全部文件',
    title: '统一管理全部文件',
    description: '按目录维度组织所有文件与文件夹，保留上传、批量操作、搜索与预览的完整能力。'
  },
  1: {
    label: '图片',
    title: '图片资源空间',
    description: '更高效地浏览图片与封面素材，支持列表、网格和时间线三种浏览模式。'
  },
  2: {
    label: '文档',
    title: '文档协作空间',
    description: '集中处理文档内容，兼顾检索效率和结构化阅读体验。'
  },
  3: {
    label: '视频',
    title: '视频文件空间',
    description: '面向大体积媒体文件的归档与管理，保持清晰目录结构与操作效率。'
  },
  4: {
    label: '音乐',
    title: '音频文件空间',
    description: '在统一的工作台内管理音频素材与录音文件，减少分类切换成本。'
  },
  5: {
    label: '其他',
    title: '其他类型文件',
    description: '集中查看未归入标准媒体类别的文件，保持整个空间的整洁与可检索性。'
  },
  6: {
    label: '回收站',
    title: '回收站管理',
    description: '在不影响原有恢复逻辑的前提下，统一查看已删除文件并执行恢复或清理操作。'
  },
  8: {
    label: '我的分享',
    title: '我的分享',
    description: '查看已分享文件与分享目录，保留现有分享链路和详情能力。'
  }
}

const VIEW_LABELS = {
  0: '列表',
  1: '网格',
  2: '时间线'
}

export default {
  name: 'Pan',
  components: {
    BreadCrumb,
    FileGrid,
    FileTable,
    FileTimeLine,
    OperationMenu
  },
  data() {
    return {
      isRefreshData: '',
      fileNameSearch: '',
      loading: true,
      fileList: [],
      pageData: {
        currentPage: 1,
        pageCount: 10,
        total: 0
      },
      STORAGE_METRICS: [
        { label: '已用容量', value: 100.4, unit: 'GB', percent: 65 },
        { label: '分配容量', value: 1.2, unit: 'TB', percent: 100 }
      ]
    }
  },
  created() {
    this.setPageCount()
    this.getTableDataByType()
  },
  watch: {
    filePath() {
      if (this.isDocsRoute && [0, 8].includes(this.fileType)) {
        this.setPageCount()
        this.getTableDataByType()
      }
    },
    fileType() {
      if (this.isDocsRoute) {
        this.setPageCount()
        this.getTableDataByType()
      }
    },
    fileModel() {
      this.setPageCount()
    },
    refreshData(newVal) {
      console.log('refreshData changed:', newVal)
      this.showFileList()
    }
  },
  computed: {
    fileType() {
      return this.$route.query.fileType ? Number(this.$route.query.fileType) : 0
    },
    filePath() {
      return this.$route.query.filePath ? this.$route.query.filePath : '/'
    },
    fileModel() {
      return this.$store.getters.fileModel
    },
    screenWidth() {
      return this.$store.state.common.screenWidth
    },
    isDocsRoute() {
      return ['File', 'docs'].includes(this.$route.name)
    },
    navItems() {
      return NAV_ITEMS
    },
    currentSection() {
      return SECTION_META[this.fileType] || SECTION_META[0]
    },
    currentViewLabel() {
      return VIEW_LABELS[this.fileModel] || VIEW_LABELS[0]
    },
    currentItemCount() {
      return Array.isArray(this.fileList) ? this.fileList.length : 0
    },
    totalItemCount() {
      return Number(this.pageData.total || this.currentItemCount || 0)
    },
    totalPages() {
      if (this.fileType === 6) {
        return 1
      }
      if (!this.pageData.pageCount) {
        return 1
      }
      return Math.max(1, Math.ceil(this.totalItemCount / this.pageData.pageCount))
    },
    paginationLabel() {
      return this.fileType === 6 ? '全部' : `${this.pageData.pageCount} / 页`
    },
    totalPagesLabel() {
      return this.fileType === 6 ? '回收站视图' : `${this.totalPages} 页`
    },
    storageProgressPercent() {

      const used = this.STORAGE_METRICS[0];

      const total = this.STORAGE_METRICS[1];

      const usedGB = this.toGB(used.value, used.unit);
      const totalGB = this.toGB(total.value, total.unit);

      if (totalGB <= 0) return 0;

      const percent = (usedGB / totalGB) * 100;
      // 限制在 0-100 之间，并保留一位小数
      return Math.max(0, Math.min(Number(percent.toFixed(1)), 100));
    },
    storageMetrics() {
      // 容量卡先保留占位结构，后续只需要把 value 替换成真实接口数据。
      return this.STORAGE_METRICS
    }
  },
  methods: {
    // 通用单位转换器
    toGB(value, unit) {
      const map = { 'MB': 1 / 1024, 'GB': 1, 'TB': 1024 };
      return value * (map[unit.toUpperCase()] || 1);
    },
    handleSelect(key) {
      const fileType = Number(key)

      this.$router.push({
        name: this.$route.name,
        query: {
          ...this.$route.query,
          fileType,
          filePath: '/'
        }
      })
    },
    handleClickRight(event) {
      event.preventDefault()
      if (![6, 8].includes(this.fileType)) {
        this.$openBox
          .contextMenu({
            selectedFile: undefined,
            domEvent: event,
            serviceEl: this
          })
          .then((res) => {
            if (res === 'confirm') {
              this.getTableDataByType()
            }
          })
      }
    },
    setPageCount() {
      this.pageData.currentPage = 1
      if (this.fileModel === 0) {
        this.pageData.pageCount = 10
      }
      if (this.fileModel === 1) {
        this.pageData.pageCount = 100
      }
    },
    getTableDataByType() {
      this.loading = true
      if (Number(this.fileType)) {
        switch (Number(this.fileType)) {
          case 6:
            this.showFileRecovery()
            break
          case 8:
            this.showMyShareFile()
            break
          default:
            this.showFileList()
            break
        }
      } else {
        this.showFileList()
      }
    },
    showFileList() {
      const data = {
        fileType: this.fileType,
        filePath: this.filePath,
        currentPage: this.pageData.currentPage,
        pageCount: this.pageData.pageCount
      }
      if (this.refreshData) {
        data.fileType = this.refreshData
      }
      getFileListByPath(data)
        .then((res) => {
          if (res && res.data && res.data.code === 200) {
            this.fileList = res.data.data.records
            this.pageData.total = Number(res.data.data.total)
          } else {
            this.$message.error(res.data.message)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleCurrentChange(currentPage) {
      this.pageData.currentPage = currentPage
      this.getTableDataByType()
    },
    handleSizeChange(pageCount) {
      this.pageData.pageCount = pageCount
      this.getTableDataByType()
    },
    showFileRecovery() {
      getRecoveryFile()
        .then((res) => {
          if (res.data.code === 200) {
            this.fileList = res.data.data
            this.pageData.total = Array.isArray(res.data.data) ? res.data.data.length : 0
          } else {
            this.$message.error(res.data.message)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    showMyShareFile() {
      const data = {
        shareFilePath: this.filePath,
        shareBatchNum: this.$route.query.shareBatchNum,
        currentPage: this.pageData.currentPage,
        pageCount: this.pageData.pageCount
      }
      getMyShareFileList(data)
        .then((res) => {
          if (res.success) {
            this.fileList = res.dataList
            this.pageData.total = Number(res.total)
          } else {
            this.$message.error(res.message)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    showFileListByType() {
      const data = {
        fileType: this.fileType,
        currentPage: this.pageData.currentPage,
        pageCount: this.pageData.pageCount
      }
      getFileListByType(data)
        .then((res) => {
          if (res.success) {
            this.fileList = res.dataList
            this.pageData.total = Number(res.total)
          } else {
            this.$message.error(res.message)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    getSearchFileList(fileName) {
      this.loading = true
      searchFile({
        currentPage: this.pageData.currentPage,
        pageCount: this.pageData.pageCount,
        fileName
      })
        .then((res) => {
          if (res.success) {
            this.fileList = res.dataList.map((item) => {
              return {
                ...item,
                highlightFields: item.highLight.fileName[0]
              }
            })
            this.pageData.total = res.data.totalHits
          } else {
            this.$message.error(res.message)
          }
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style lang="less" scoped>
@page-bg: #09111f;
@panel-bg: rgba(14, 24, 42, 0.82);
@panel-surface: rgba(255, 255, 255, 0.04);
@panel-surface-strong: rgba(255, 255, 255, 0.06);
@border-color: rgba(148, 163, 184, 0.16);
@border-strong: rgba(110, 168, 255, 0.22);
@text-main: #edf4ff;
@text-sub: #a4b3ca;
@text-muted: #6f809e;
@accent: #6ea8ff;
@shadow-lg: 0 28px 60px rgba(0, 0, 0, 0.32);

.docs-page {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 24px;
  min-height: 78vh;
  padding: 24px;
  border-radius: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(110, 168, 255, 0.18), transparent 28%),
    radial-gradient(circle at right center, rgba(124, 140, 255, 0.14), transparent 24%),
    linear-gradient(145deg, rgba(6, 13, 25, 0.98), rgba(9, 17, 31, 0.96));
  box-shadow: @shadow-lg;
}

.docs-page::before,
.docs-page::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(8px);
}

.docs-page::before {
  top: -120px;
  right: 10%;
  width: 260px;
  height: 260px;
  background: rgba(124, 140, 255, 0.1);
}

.docs-page::after {
  bottom: -140px;
  left: 18%;
  width: 320px;
  height: 320px;
  background: rgba(110, 168, 255, 0.12);
}

.docs-sidebar,
.docs-main {
  position: relative;
  z-index: 1;
  min-height: 0;
}

.docs-sidebar {
  width: 280px;
  flex: 0 0 280px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 20px;
}

.docs-main {
  flex: 1;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 20px;
  min-width: 0;
}

.sidebar-brand,
.sidebar-panel,
.workspace-hero,
.workspace-shell {
  background: @panel-bg;
  border: 1px solid @border-color;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(18px);
}

.sidebar-brand {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 18px;
  border-radius: 24px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: @text-main;
  font-size: 24px;
  background: linear-gradient(145deg, rgba(110, 168, 255, 0.92), rgba(124, 140, 255, 0.7));
  box-shadow: 0 14px 30px rgba(110, 168, 255, 0.24);
}

.brand-copy {
  min-width: 0;
}

.brand-copy h2 {
  margin: 6px 0 8px;
  font-size: 20px;
  line-height: 1.3;
  color: @text-main;
}

.brand-copy p,
.hero-copy p,
.breadcrumb-copy p,
.pagination-copy p,
.capacity-note {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: @text-sub;
}

.brand-eyebrow,
.hero-badge,
.panel-subtitle,
.breadcrumb-title,
.pagination-title,
.capacity-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: @accent;
}

.sidebar-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 18px;
  border-radius: 24px;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: @text-main;
}

.sidebar-panel-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.nav-scroll {
  min-height: 0;
  flex: 1;
  display: flex;
  overflow: auto;
  padding-right: 4px;
}

.nav-scroll::-webkit-scrollbar {
  width: 6px;
}

.nav-scroll::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.28);
  border-radius: 999px;
}

.side-menu.nav-menu {
  border-right: none;
  background: transparent;
  height: 100%;
  min-height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-menu {
  ::v-deep .el-menu-item {
    flex: 1 1 0;
    height: auto;
    min-height: 0;
    display: flex;
    align-items: center;
    gap: 12px;
    line-height: 1;
    padding: 8px 12px !important;
    border-radius: 16px;
    border: 1px solid transparent;
    background: rgba(255, 255, 255, 0.02);
    transition: all 0.22s ease;
  }

  ::v-deep .el-menu-item:hover {
    background: rgba(110, 168, 255, 0.08) !important;
    border-color: rgba(110, 168, 255, 0.16);
  }

  ::v-deep .el-menu-item.is-active {
    background: linear-gradient(145deg, rgba(110, 168, 255, 0.14), rgba(124, 140, 255, 0.1)) !important;
    border-color: @border-strong;
  }
}

.nav-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  color: @accent;
  font-size: 15px;
  flex-shrink: 0;
}

.nav-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.nav-copy strong {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.15;
  color: @text-main;
}

.nav-copy small {
  font-size: 11px;
  line-height: 1.15;
  color: @text-muted;
}

.capacity-card {
  margin-top: 14px;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.capacity-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.capacity-copy h3 {
  margin: 8px 0 0;
  font-size: 18px;
  line-height: 1.3;
  color: @text-main;
}

.capacity-status {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.12);
  color: @text-sub;
  font-size: 12px;
  white-space: nowrap;
}

.capacity-progress {
  margin: 18px 0 12px;
}

.capacity-progress-track {
  position: relative;
  height: 12px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.capacity-progress-fill {
  min-width: 0;
  height: 100%;
  background: linear-gradient(90deg, 
    rgba(46, 204, 113, 0.6),  /* 主绿色起点 */
    rgba(163, 228, 215, 0.9),  /* 中间高亮区 */
    rgba(46, 204, 113, 0.6)); /* 主绿色终点 */
  background-size: 220% 100%;
  animation: capacityPlaceholder 2.4s linear infinite;

  transition: width 0.3s ease;
}

.capacity-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.capacity-metric {
  padding: 12px;
  border-radius: 16px;
  background: @panel-surface;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.capacity-metric span,
.hero-stat span {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: @text-muted;
  letter-spacing: 0.02em;
}

.capacity-metric strong,
.hero-stat strong {
  color: @text-main;
  font-size: 15px;
  font-weight: 600;
}

.workspace-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 26px;
  border-radius: 24px;
}

.hero-copy {
  max-width: 620px;
}

.hero-copy h1 {
  margin: 10px 0 12px;
  font-size: 30px;
  line-height: 1.2;
  color: @text-main;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
  min-width: 360px;
}

.hero-stat {
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.workspace-shell {
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  overflow: hidden;
  height: 80vh;
}

.workspace-toolbar,
.workspace-breadcrumb,
.workspace-pagination {
  padding: 18px 22px;
  text-align: right;
}

.workspace-toolbar {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.workspace-breadcrumb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.breadcrumb-copy {
  width: 240px;
  flex-shrink: 0;
}

.workspace-canvas {
  min-height: 0;
  flex: 1;
  display: flex;
  padding: 0 22px 22px;
  height: 720px;
  max-height: 720px;
  overflow: hidden;
}

.workspace-canvas>* {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.workspace-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(7, 12, 24, 0.26));
}

.pagination-copy {
  flex-shrink: 0;
}

.workspace-toolbar ::v-deep .operation-menu-wrapper {
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.workspace-toolbar ::v-deep .el-button--primary {
  border-color: rgba(110, 168, 255, 0.26);
  background: linear-gradient(145deg, rgba(110, 168, 255, 0.86), rgba(124, 140, 255, 0.74));
}

.workspace-toolbar ::v-deep .el-input__inner,
.workspace-pagination ::v-deep .el-input__inner {
  color: @text-main;
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(148, 163, 184, 0.14);
}

.workspace-toolbar ::v-deep .el-input__inner:focus,
.workspace-pagination ::v-deep .el-input__inner:focus {
  border-color: rgba(110, 168, 255, 0.3);
}

.workspace-breadcrumb ::v-deep .breadcrumb-wrapper {
  height: auto;
  padding: 0;
  background: transparent;
  border-bottom: none;
  box-shadow: none;
}

.workspace-breadcrumb ::v-deep .title,
.workspace-breadcrumb ::v-deep .tech-title {
  color: @text-muted !important;
}

.workspace-breadcrumb ::v-deep .el-breadcrumb__inner,
.workspace-breadcrumb ::v-deep .el-breadcrumb__inner a {
  color: @text-main !important;
}

.workspace-breadcrumb ::v-deep .el-breadcrumb__separator {
  color: @text-muted !important;
}

.workspace-pagination ::v-deep .el-pagination {
  color: @text-sub;
}

.workspace-pagination ::v-deep .btn-prev,
.workspace-pagination ::v-deep .btn-next,
.workspace-pagination ::v-deep .el-pager li {
  background: transparent !important;
  color: @text-main !important;
}

.workspace-pagination ::v-deep .el-pager li.active {
  border-radius: 8px;
  background: rgba(110, 168, 255, 0.12) !important;
  color: @accent !important;
}

@keyframes capacityPlaceholder {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -20% 0;
  }
}

@media (max-width: 1280px) {
  .docs-page {
    gap: 18px;
    padding: 18px;
  }

  .docs-sidebar {
    width: 256px;
    flex-basis: 256px;
  }

  .workspace-hero {
    flex-direction: column;
  }

  .hero-stats {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 960px) {
  .docs-page {
    flex-direction: column;
    min-height: auto;
  }

  .docs-sidebar {
    width: 100%;
    flex-basis: auto;
    display: flex;
    flex-direction: column;
  }

  .docs-main {
    display: flex;
    flex-direction: column;
  }

  .workspace-breadcrumb,
  .workspace-pagination {
    flex-direction: column;
    align-items: flex-start;
  }

  .breadcrumb-copy {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .docs-page {
    padding: 14px;
    border-radius: 22px;
  }

  .sidebar-brand,
  .sidebar-panel,
  .workspace-hero,
  .workspace-toolbar,
  .workspace-breadcrumb,
  .workspace-pagination {
    padding-left: 16px;
    padding-right: 16px;
  }

  .hero-stats,
  .capacity-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .workspace-canvas {
    height: 560px;
    max-height: 560px;
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
