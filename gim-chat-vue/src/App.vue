<template>
  <div id="app" :class="['app-shell', { 'with-layout': showLayout }]">
    <div class="app-backdrop"></div>

    <template v-if="showLayout">
      <header class="app-header">
        <div class="header-brand">
          <button type="button" class="collapse-btn" @click="toggleSidebar">
            <i :class="isSidebarCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>

          <div class="brand-mark">G</div>
          <div class="brand-copy">
            <h1>Gim Chat</h1>
          </div>
        </div>

        <div class="header-actions">
          <el-button class="header-theme-btn" @click="toggleTheme">
            <i :class="themeMode === 'dark' ? 'el-icon-sunny' : 'el-icon-moon'"></i>
            {{ currentThemeLabel }}
          </el-button>

          <template v-if="isLogin">
            <div class="user-chip">
              <div class="user-avatar">
                <img :src="userInfo.avatarUrl || defaultAvatar" alt="用户头像" />
              </div>
              <div class="user-copy">
                <strong>{{ userInfo.userName || '已登录用户' }}</strong>
              </div>
            </div>

            <el-button type="text" class="logout-btn" @click="doLogout">
              退出登录
            </el-button>
          </template>

          <template v-else>
            <el-button type="primary" @click="doLogin">立即登录</el-button>
          </template>
        </div>
      </header>

      <div class="app-body">
        <aside class="shell-sidebar" :class="{ collapsed: isSidebarCollapsed }">
          <div class="sidebar-topbar">
            <!-- <span class="theme-mini-tag">{{ currentThemeLabel }}</span> -->
          </div>

          <nav class="sidebar-nav">
            <router-link
              v-for="item in navItems"
              :key="item.to"
              :to="item.to"
              class="sidebar-link"
              active-class="is-active"
            >
              <span class="nav-icon">
                <img v-if="item.image" :src="item.image" :alt="item.label" />
                <i v-else :class="item.icon"></i>
              </span>
              <span v-if="!isSidebarCollapsed" class="nav-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.description }}</small>
              </span>
            </router-link>
          </nav>

          <div class="sidebar-footer">
            <!-- <el-button class="header-theme-btn small" @click="toggleTheme">
              切换{{ themeMode === 'dark' ? '浅色' : '深色' }}
            </el-button> -->
          </div>
        </aside>

        <main class="main-view-container">
          <div class="main-view-content">
            <transition name="fade-transform" mode="out-in">
              <router-view />
            </transition>
          </div>
        </main>
      </div>
    </template>

    <main v-else class="auth-view-host">
      <transition name="fade-transform" mode="out-in">
        <router-view />
      </transition>
    </main>

    <el-dialog
      title="身份验证"
      :visible.sync="loginDialogVisible"
      width="420px"
      center
      :show-close="true"
      :close-on-click-modal="false"
      custom-class="inline-login-panel"
    >
      <div class="dialog-content-wrapper">
        <h2 class="login-title">用户登录</h2>
        <p class="login-subtitle">欢迎连接 Gim-Chat 智能终端</p>

        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="inline-login-form">
          <el-form-item prop="phone">
            <el-input
              v-model="loginForm.phone"
              placeholder="请输入手机号"
              prefix-icon="el-icon-mobile-phone"
            ></el-input>
          </el-form-item>

          <el-form-item prop="passWord">
            <el-input
              v-model="loginForm.passWord"
              type="password"
              show-password
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
            ></el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-submit-btn" @click="submitLogin">
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import Vue from 'vue'
import { handleLogin, handleLogout } from '@/api/user'
import WebSocketService from '@/plugins/ws'
import chatService from '@/services/chatService'
import openAiIcon from '@/assets/images/avatar/openai.svg'

const NAV_ITEMS = [
  { to: '/index', label: '首页', description: '概览与统计', icon: 'el-icon-s-home' },
  { to: '/chat', label: '站内信', description: '消息协作', icon: 'el-icon-message' },
  { to: '/group', label: '工具箱', description: '效率工作台', icon: 'el-icon-s-grid' },
  { to: '/docs', label: '网盘中心', description: '文件与资源', icon: 'el-icon-folder-opened' },
  { to: '/ai', label: 'AI 助手', description: '智能问答', image: openAiIcon },
  { to: '/orders', label: '订单中心', description: '支付与账单', icon: 'el-icon-tickets' },
  { to: '/setting', label: '系统设置', description: '偏好与外观', icon: 'el-icon-setting' }
]

export default {
  name: 'AppShell',
  data() {
    return {
      defaultAvatar: 'https://iknow-pic.cdn.bcebos.com/962bd40735fae6cd17bafbff1db30f2442a70f25',
      loginDialogVisible: false,
      loginForm: {
        phone: '',
        passWord: ''
      },
      loginRules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          {
            pattern: /^1[3-9]\d{9}$/,
            message: '请输入合法的手机号',
            trigger: 'blur'
          }
        ],
        passWord: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      isSidebarCollapsed: false,
      navItems: NAV_ITEMS
    }
  },
  computed: {
    ...mapState({
      themeMode: (state) => state.common.themeMode
    }),
    showLayout() {
      return this.$route.name !== 'login'
    },
    isLogin() {
      return this.$store.getters['user/isLogin']
    },
    userInfo() {
      return this.$store.state.user.userInfo || {}
    },
    currentThemeLabel() {
      return this.themeMode === 'dark' ? '深色模式' : '浅色模式'
    }
  },
  watch: {
    themeMode: {
      immediate: true,
      handler(mode) {
        this.applyTheme(mode)
      }
    }
  },
  mounted() {
    window.addEventListener('beforeunload', this.clearLoginState)
    this.applyTheme(this.themeMode)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.clearLoginState)
  },
  methods: {
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed
    },
    toggleTheme() {
      const nextMode = this.themeMode === 'dark' ? 'light' : 'dark'
      this.$store.commit('changeThemeMode', nextMode)
    },
    applyTheme(mode) {
      const resolvedMode = mode === 'dark' ? 'dark' : 'light'
      document.documentElement.setAttribute('data-theme', resolvedMode)
      document.body.setAttribute('data-theme', resolvedMode)
    },
    clearLoginState() {
      const navigationEntry = performance.getEntriesByType('navigation')[0]
      if (navigationEntry && navigationEntry.type !== 'reload') {
        chatService.disconnect({ clearState: true })
        this.$store.dispatch('user/logout')
      }
    },
    submitLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (!valid) {
          return
        }

        const param = { ...this.loginForm }
        handleLogin(param)
          .then((res) => {
            const data = res.data.data
            if (res.data.code === 200 && data.token) {
              const token = data.token
              const user = data.userInfo
              this.$store.dispatch('user/login', {
                token,
                userInfo: user
              })
              this.loginDialogVisible = false
              this.$message.success('连接成功，欢迎回来')
              this.initWebSocket(user.userId)
            } else {
              this.$message.error(res.data.message || '登录失败')
            }
          })
          .catch((error) => {
            console.error(error)
            this.$message.error('网络错误或服务异常')
          })
      })
    },
    doLogin() {
      this.$router.push({ name: 'login' })
    },
    async doLogout() {
      const param = {
        userId: this.userInfo.userId
      }

      try {
        const res = await handleLogout(param)
        if (res && res.data && res.data.code === 200) {
          this.$store.dispatch('user/logout')
        } else {
          this.$message.error(res.data.message)
          this.$router.push({ name: 'login' })
        }
      } finally {
        chatService.disconnect({ clearState: true })
        this.$message.success('已安全断开连接')
        this.$router.push({ name: 'login' })
      }
    },
    initWebSocket(userId) {
      if (this.wsService) {
        this.wsService.close()
      }
      this.wsService = new WebSocketService(this.$store)
      this.wsService.connect(userId)
      this.$ws = this.wsService
      Vue.prototype.$ws = this.wsService
      this.$ws.send({})
    }
  }
}
</script>

<style lang="less" scoped>
.app-shell {
  position: relative;
  --shell-available-height: calc(100vh - 126px);
  min-height: 100vh;
  background: var(--apple-bg);
  color: var(--apple-text-primary);
  overflow-x: hidden;
  overflow-y: visible;
}

.app-shell.with-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 16px 18px 18px;
  box-sizing: border-box;
  overflow: hidden;
}

.app-backdrop {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.14), transparent 34%),
    radial-gradient(circle at 85% 12%, rgba(255, 255, 255, 0.5), transparent 24%),
    radial-gradient(circle at 50% 100%, rgba(0, 122, 255, 0.08), transparent 28%);
  opacity: 0.9;
}

.app-header,
.shell-sidebar {
  position: relative;
  z-index: 2;
}

.app-header {
  padding: 16px 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-radius: 22px;
  background: var(--apple-glass-bg);
  box-shadow: var(--apple-shadow);
  backdrop-filter: var(--apple-backdrop);
}

.header-brand,
.header-actions,
.user-chip {
  display: flex;
  align-items: center;
}

.header-brand {
  gap: 14px;
}

.collapse-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: var(--apple-surface-soft);
  color: var(--apple-text-primary);
  cursor: pointer;
  box-shadow: var(--apple-shadow);
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: var(--apple-blue);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  box-shadow: var(--apple-shadow);
}

.brand-copy h1,
.brand-copy p {
  margin: 0;
}

.brand-copy h1 {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.brand-copy p {
  margin-top: 4px;
  color: var(--apple-text-secondary);
  font-size: 13px;
}

.header-actions {
  gap: 12px;
}

.header-theme-btn {
  min-height: 42px;
  padding: 0 16px;
}

.header-theme-btn.small {
  width: 100%;
}

.user-chip {
  gap: 12px;
  padding: 8px 12px;
  min-width: 0;
  border-radius: 16px;
  background: var(--apple-surface-soft);
  box-shadow: var(--apple-shadow);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.user-copy strong,
.user-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-copy span {
  margin-top: 2px;
  font-size: 12px;
  color: var(--apple-text-secondary);
}

.logout-btn {
  padding: 0 8px;
}

.app-body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  flex: 1;
  min-height: 0;
  gap: 18px;
  padding-top: 18px;
  overflow: hidden;
}

.shell-sidebar {
  width: 300px;
  padding: 18px 16px;
  position: relative;
  align-self: stretch;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-radius: 24px;
  background: var(--apple-glass-bg);
  box-shadow: var(--apple-shadow);
  backdrop-filter: var(--apple-backdrop);
  transition: width 0.28s ease, padding 0.28s ease;
}

.shell-sidebar.collapsed {
  width: 96px;
  padding-inline: 12px;
}

.sidebar-topbar,
.sidebar-footer {
  display: flex;
  align-items: center;
}

.sidebar-footer {
  margin-top: auto;
  flex-shrink: 0;
}

.theme-mini-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--apple-blue);
  background: var(--apple-blue-soft);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  min-height: 60px;
  border-radius: 18px;
  text-decoration: none;
  color: var(--apple-text-primary);
  background: transparent;
  transition: background-color 0.24s ease, transform 0.24s ease;
}

.sidebar-link:hover {
  transform: translateY(-1px);
}

.sidebar-link.is-active {
  box-shadow: var(--apple-shadow);
}

.nav-icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  flex-shrink: 0;
  font-size: 18px;
}

.nav-icon img {
  width: 20px;
  height: 20px;
}

.nav-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.nav-copy strong,
.nav-copy small {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-copy strong {
  font-size: 15px;
  font-weight: 600;
}

.nav-copy small {
  margin-top: 4px;
  color: var(--apple-text-secondary);
  font-size: 12px;
}

.main-view-container {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.main-view-content {
  position: relative;
  z-index: 1;
  min-height: 100%;
}

.auth-view-host {
  position: relative;
  z-index: 1;
  min-height: 100vh;
}

.dialog-content-wrapper {
  padding-bottom: 8px;
}

.login-title,
.login-subtitle {
  margin: 0;
  text-align: center;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.login-subtitle {
  margin-top: 8px;
  color: var(--apple-text-secondary);
}

.inline-login-form {
  margin-top: 24px;
}

.login-submit-btn {
  width: 100%;
  min-height: 44px;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.fade-transform-enter,
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

::v-deep(.inline-login-panel) {
  border-radius: 24px;
}

@media (max-width: 1200px) {
  .shell-sidebar {
    width: 272px;
  }
}

@media (max-width: 960px) {
  .app-shell.with-layout {
    height: auto;
    min-height: 100vh;
    padding: 12px;
    overflow: visible;
  }

  .app-header {
    padding: 14px;
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .app-body {
    flex-direction: column;
    padding-top: 12px;
    overflow: visible;
  }

  .shell-sidebar,
  .shell-sidebar.collapsed {
    position: relative;
    top: 0;
    width: 100%;
    height: auto;
    max-height: none;
    overflow: visible;
    align-self: auto;
  }

  .shell-sidebar.collapsed {
    padding-inline: 16px;
  }

  .shell-sidebar.collapsed .nav-copy {
    display: flex;
  }
}

@media (max-width: 640px) {
  .app-shell.with-layout {
    padding: 12px;
  }

  .app-header {
    padding: 12px;
  }

  .user-chip {
    width: 100%;
  }

  .header-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .main-view-content {
    --shell-available-height: auto;
    min-height: auto;
  }

  .main-view-container {
    overflow: visible;
  }
}
</style>
