<template>
  <div class="settings-page">
    <section class="settings-hero">
      <div class="hero-copy">
        <h1>主题与外观</h1>
      </div>
      <div class="hero-status">
        <span class="theme-badge">{{ currentThemeLabel }}</span>
        <span class="theme-hint">主题选择保存在本地浏览器</span>
      </div>
    </section>

    <section class="settings-grid">
      <article
        :class="['theme-option', { active: themeMode === 'light' }]"
        @click="changeTheme('light')"
      >
        <div class="theme-preview light">
          <div class="preview-header"></div>
          <div class="preview-sidebar"></div>
          <div class="preview-card large"></div>
          <div class="preview-card"></div>
          <div class="preview-pill"></div>
        </div>
        <div class="theme-option-copy">
          <h3>浅色模式</h3>
          <p>浅灰背景、白色容器和更高的内容识别度，适合白天或长时间浏览。</p>
        </div>
        <el-button
          :type="themeMode === 'light' ? 'primary' : 'default'"
          class="theme-action"
        >
          {{ themeMode === 'light' ? '当前使用中' : '切换到浅色' }}
        </el-button>
      </article>

      <article
        :class="['theme-option', { active: themeMode === 'dark' }]"
        @click="changeTheme('dark')"
      >
        <div class="theme-preview dark">
          <div class="preview-header"></div>
          <div class="preview-sidebar"></div>
          <div class="preview-card large"></div>
          <div class="preview-card"></div>
          <div class="preview-pill"></div>
        </div>
        <div class="theme-option-copy">
          <h3>深色模式</h3>
          <p>降低高亮眩光，提升夜间浏览舒适度，并统一深色容器和信息层级。</p>
        </div>
        <el-button
          :type="themeMode === 'dark' ? 'primary' : 'default'"
          class="theme-action"
        >
          {{ themeMode === 'dark' ? '当前使用中' : '切换到深色' }}
        </el-button>
      </article>
    </section>

    <section class="settings-panel">
      <div class="panel-header">
        <h2>切换说明</h2>
      </div>
      <div class="panel-list">
        <div class="panel-item">
          <strong>即时生效</strong>
          <span>切换主题后，头部、侧栏、子页面、浮层和自定义组件会立即更新。</span>
        </div>
        <div class="panel-item">
          <strong>自动记忆</strong>
          <span>主题模式会保存在本地，下次打开网站时继续使用上次选择。</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'SettingView',
  computed: {
    ...mapState({
      themeMode: (state) => state.common.themeMode
    }),
    currentThemeLabel() {
      return this.themeMode === 'dark' ? '深色模式' : '浅色模式'
    }
  },
  methods: {
    changeTheme(mode) {
      this.$store.commit('changeThemeMode', mode)
      this.$message.success(`已切换为${mode === 'dark' ? '深色' : '浅色'}模式`)
    }
  }
}
</script>

<style scoped lang="less">
.settings-page {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  color: var(--apple-text-primary);
}

.settings-hero,
.settings-panel,
.theme-option {
  border-radius: 24px;
  background: var(--apple-surface);
  box-shadow: var(--apple-shadow);
}

.settings-hero {
  padding: 28px 30px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.hero-copy {
  max-width: 720px;
}

.hero-kicker,
.hero-desc,
.theme-hint,
.theme-option-copy p,
.panel-item span {
  color: var(--apple-text-secondary);
}

.hero-kicker {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.16em;
}

.hero-copy h1,
.theme-option-copy h3,
.panel-header h2 {
  margin: 0;
}

.hero-copy h1 {
  font-size: 34px;
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.hero-desc {
  margin: 12px 0 0;
  max-width: 620px;
  line-height: 1.7;
}

.hero-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.theme-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: var(--apple-blue-soft);
  color: var(--apple-blue);
  font-weight: 600;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.theme-option {
  padding: 22px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.theme-option:hover {
  transform: translateY(-2px);
}

.theme-option.active {
  border-color: rgba(0, 122, 255, 0.28);
  box-shadow: 0 12px 40px rgba(0, 122, 255, 0.14);
}

.theme-preview {
  position: relative;
  height: 190px;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 18px;
}

.theme-preview.light {
  background: linear-gradient(180deg, #f5f5f7 0%, #ececf0 100%);
}

.theme-preview.dark {
  background: linear-gradient(180deg, #101012 0%, #1c1c1e 100%);
}

.preview-header,
.preview-sidebar,
.preview-card,
.preview-pill {
  position: absolute;
  border-radius: 16px;
}

.preview-header {
  top: 14px;
  left: 14px;
  right: 14px;
  height: 28px;
}

.preview-sidebar {
  top: 54px;
  left: 14px;
  width: 84px;
  bottom: 14px;
}

.preview-card {
  left: 112px;
  right: 14px;
  height: 42px;
}

.preview-card.large {
  top: 54px;
  height: 60px;
}

.preview-card:not(.large) {
  top: 124px;
}

.preview-pill {
  bottom: 18px;
  left: 126px;
  width: 120px;
  height: 16px;
}

.theme-preview.light .preview-header,
.theme-preview.light .preview-sidebar,
.theme-preview.light .preview-card,
.theme-preview.light .preview-pill {
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.theme-preview.dark .preview-header,
.theme-preview.dark .preview-sidebar,
.theme-preview.dark .preview-card,
.theme-preview.dark .preview-pill {
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.34);
}

.theme-option-copy p {
  margin: 8px 0 0;
  line-height: 1.65;
}

.theme-action {
  margin-top: 18px;
}

.settings-panel {
  padding: 24px 26px;
}

.panel-header {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--apple-divider);
}

.panel-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.panel-item {
  padding: 18px;
  border-radius: 18px;
  background: var(--apple-surface-soft);
}

.panel-item strong,
.panel-item span {
  display: block;
}

.panel-item span {
  margin-top: 8px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .settings-grid,
  .panel-list {
    grid-template-columns: 1fr;
  }

  .settings-hero {
    flex-direction: column;
  }

  .hero-status {
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .settings-page {
    padding: 18px;
  }

  .settings-hero,
  .settings-panel,
  .theme-option {
    padding: 18px;
    border-radius: 20px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }
}
</style>
