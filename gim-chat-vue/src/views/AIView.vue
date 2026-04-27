<template>
  <div class="ai-page">
    <input ref="fileInput" type="file" accept=".txt,.pdf,.doc,.mp4,.docx,image/*" multiple style="display: none"
      @change="handleFileSelect" />

    <div v-if="isMobile && sidebarVisible" class="sidebar-mask" @click="closeSidebar"></div>

    <aside :class="['session-sidebar', { 'mobile-open': sidebarVisible }]">
      <div class="sidebar-top">
        <div class="sidebar-brand">
          <img src="@/assets/images/avatar/openai.svg" alt="AI" class="brand-icon" />
          <div>
            <!-- <p class="sidebar-label">AI WORKSPACE</p> -->
            <h2>智能对话</h2>
          </div>
        </div>

        <el-button type="primary" class="new-chat-btn" :disabled="loading" @click="createNewSession">
          <i class="el-icon-plus"></i>
          新对话
        </el-button>
      </div>

      <div class="sidebar-summary">
        <div :class="['status-pill', socketStatus]">
          <span class="status-dot"></span>
          {{ socketStatusLabel }}
        </div>
        <div class="summary-line">
          <span>当前模型</span>
          <strong>{{ selectedModel }}</strong>
        </div>
        <div class="summary-line">
          <span>知识库</span>
          <strong>{{ currentKBLabel }}</strong>
        </div>
      </div>

      <div class="session-list">
        <button v-for="session in sessions" :key="session.id" type="button" :disabled="loading"
          :class="['session-item', { active: session.id === activeSessionId }]" @click="activateSession(session.id)">
          <div class="session-title">{{ getSessionTitle(session) }}</div>
          <div class="session-preview">{{ getSessionPreview(session) }}</div>
        </button>
      </div>
    </aside>

    <main class="conversation-shell">
      <header class="conversation-header">
        <div class="header-left">
          <button v-if="isMobile" type="button" class="mobile-menu-btn" @click="toggleSidebar">
            <i class="el-icon-s-unfold"></i>
          </button>

          <div class="header-copy">
            <!-- <p class="header-kicker">ChatGPT Style Workspace</p> -->
            <h1>{{ getSessionTitle(currentSession) }}</h1>
          </div>
        </div>

        <div class="header-actions">
          <span :class="['header-status', socketStatus]">{{ socketStatusShortLabel }}</span>
          <el-button v-if="socketStatus === 'closed' || socketStatus === 'error'" type="text" class="reconnect-btn"
            @click="manualReconnect">
            重新连接
          </el-button>
        </div>
      </header>

      <div :class="['conversation-main', { 'is-empty': isEmptyConversation }]">
        <section v-if="isEmptyConversation" class="welcome-stage">
          <div class="welcome-card">
            <div class="welcome-badge-wrap">
              <div class="welcome-badge">
                <img src="@/assets/images/avatar/openai.svg" alt="AI Avatar" class="welcome-icon" />
              </div>
            </div>
            <div class="welcome-copy">
              <p class="welcome-kicker">智能问答 / 文件理解 / 知识库检索</p>
              <h3><span>有什么可以帮你？</span></h3>
              <p class="welcome-text">{{ welcomeText }}</p>
            </div>
          </div>
        </section>

        <section v-else ref="chatList" class="conversation-body">
          <div class="conversation-inner">
            <div v-for="msg in activeMessages" :key="msg.id"
              :class="['message-row', msg.role === 'user' ? 'user-row' : 'ai-row']">
              <div v-if="msg.role === 'ai'" class="message-avatar">
                <img src="@/assets/images/avatar/openai.svg" alt="AI Avatar" class="avatar-img" />
              </div>

              <div :class="['message-card', msg.role === 'user' ? 'user-card' : 'ai-card']">
                <div v-if="msg.role === 'ai'" class="message-role">AI 助手</div>
                <div class="message-content" v-html="msg.renderedContent"></div>
              </div>
            </div>

            <div v-if="loading" class="streaming-row">
              <div class="message-avatar">
                <img src="@/assets/images/avatar/openai.svg" alt="AI Avatar" class="avatar-img" />
              </div>
              <div class="streaming-card">
                <span>AI 正在思考</span>
                <div class="dot-flash">
                  <div v-for="index in 3" :key="index" class="dot" :style="{ animationDelay: index * 0.2 + 's' }"></div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <div :class="['composer-shell', { 'center-shell': isEmptyConversation }]">
          <div v-if="selectedFiles.length > 0" class="file-list">
            <div v-for="(file, index) in selectedFiles" :key="file.name + index" class="file-item">
              <span>{{ file.name }}</span>
              <button type="button" class="remove-file" @click="removeFile(index)">×</button>
            </div>
          </div>

          <div class="composer-card">
            <div class="composer-main">
              <button type="button" class="icon-btn" @click="triggerFileSelect">
                <i class="el-icon-paperclip"></i>
              </button>

              <div class="composer-input-wrap">
                <textarea ref="inputBox" v-model="userInput" class="composer-input" rows="1"
                  placeholder="给 AI 发送消息，或上传文件后继续提问" @keyup.enter="handleSend" @paste="handlePaste"></textarea>
              </div>

              <button type="button" class="send-btn"
                :disabled="loading || (!trimmedUserInput && selectedFiles.length === 0)" @click="handleSend">
                <i class="el-icon-position"></i>
              </button>
            </div>

            <div class="composer-tools">
              <div class="tool-group">
                <span class="tool-label">模型</span>
                <el-select v-model="selectedModel" size="mini" class="toolbar-select" popper-class="ai-select-popper"
                  :popper-append-to-body="true">
                  <el-option v-for="item in selectedModelOptions" :key="item.value" :label="item.label"
                    :value="item.value" />
                </el-select>
              </div>

              <div class="tool-group">
                <span class="tool-label">知识库</span>
                <el-select v-model="selectedKB" size="mini" class="toolbar-select" popper-class="ai-select-popper"
                  :popper-append-to-body="true">
                  <el-option v-for="item in selectedKBOptions" :key="item.value" :label="item.label"
                    :value="item.value" />
                </el-select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { uploadFile } from '@/api/file'
import { getFileListByPath } from '@/api/knowledge.js'
import request from '@/utils/request'
import { getAiModelList } from '@/api/ai'

const DEFAULT_WELCOME_TEXT = '嗨，我是 AI 助手。你可以直接提问，也可以上传文件、关联知识库后继续发起问答。'
const DEFAULT_KB_OPTION = {
  label: '未使用知识库',
  value: ''
}

marked.setOptions({
  breaks: true,
  gfm: true,
  highlight(code, lang) {
    return lang && hljs.getLanguage(lang)
      ? hljs.highlight(code, { language: lang }).value
      : hljs.highlightAuto(code).value
  }
})

export default {
  name: 'AIView',
  data() {
    return {
      selectedModel: '',
      selectedModelOptions: [],
      selectedKB: '',
      selectedKBOptions: [
        DEFAULT_KB_OPTION
      ],
      socketStatus: 'connecting',
      socket: null,
      allowReconnect: true,
      reconnectAttempts: 0,
      maxReconnectAttempts: 20,
      reconnectDelay: 2000,
      reconnectTimer: null,
      selectedFiles: [],
      sessions: [],
      activeSessionId: '',
      sessionSeed: 1,
      userInput: '',
      loading: false,
      currentAiMessageId: null,
      typingQueue: [],
      typingTimer: null,
      isTyping: false,
      isMobile: false,
      sidebarVisible: false
    }
  },
  computed: {
    currentSession() {
      return this.sessions.find((session) => session.id === this.activeSessionId) || null
    },
    activeMessages() {
      return this.currentSession ? this.currentSession.messages : []
    },
    isEmptyConversation() {
      return !this.activeMessages.some((msg) => msg.role === 'user')
    },
    welcomeText() {
      const firstAiMessage = this.activeMessages.find((msg) => msg.role === 'ai')
      return firstAiMessage ? firstAiMessage.content : DEFAULT_WELCOME_TEXT
    },
    trimmedUserInput() {
      return this.userInput.trim()
    },
    currentKBLabel() {
      const target = this.selectedKBOptions.find((item) => item.value === this.selectedKB)
      return target ? target.label : DEFAULT_KB_OPTION.label
    },
    socketStatusLabel() {
      const labelMap = {
        connecting: '连接中',
        open: '已连接',
        closed: '已断开',
        error: '连接异常'
      }

      return labelMap[this.socketStatus] || '未知状态'
    },
    socketStatusShortLabel() {
      const labelMap = {
        connecting: '连接中',
        open: '在线',
        closed: '断开',
        error: '异常'
      }

      return labelMap[this.socketStatus] || '未知'
    }
  },
  watch: {
    userInput() {
      this.adjustInputHeight()
    }
  },
  mounted() {
    this.allowReconnect = true
    this.handleResize()
    this.adjustInputHeight()
    this.initializePage()
    window.addEventListener('resize', this.handleResize)

    this.getAiModelList();//获取AI配置列表
  },
  activated() {
    this.allowReconnect = true

    if (!this.socket || this.socket.readyState === WebSocket.CLOSED) {
      this.initSocket()
    }

    if (!this.sessions.length) {
      this.initializeSessions()
    }
  },
  beforeDestroy() {
    this.allowReconnect = false
    this.resetTypingState()

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    if (this.socket) {
      const currentSocket = this.socket
      this.socket = null
      currentSocket.close()
    }

    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    //获取AI配置列表
    getAiModelList() {
      getAiModelList().then(res => {
        if (res.status === 200) {
          console.log(res.data)
            this.selectedModelOptions = res.data.map(item => {
              return {
                label: item.modelName,
                value: item.alias
              };
            });
          this.selectedModel = this.selectedModelOptions[0].label
        } else {
          this.$message.error('模型列表参数获取失败！')
        }
      })
    },
    async initializePage() {
      this.initSocket()
       await this.initializeSessions()
      this.getKnowledgeOptions()
      this.adjustInputHeight()
    },
    async initializeSessions() {
      const loaded = await this.loadHistorySessions()

      if (!loaded) {
        this.ensureDefaultSession()
      }

      this.renderExistingMessages()
      this.$nextTick(() => {
        this.scrollToBottom()
        this.refreshHighlight()
      })
    },
    ensureDefaultSession() {
      if (this.sessions.length > 0) {
        return
      }

      const session = this.createSession()
      this.sessions = [session]
      this.activeSessionId = session.id
    },
    createSession() {
      const sessionIndex = this.sessionSeed
      this.sessionSeed += 1
      const conversationId = this.generateConversationId()

      return {
        id: conversationId,
        conversationId,
        memoryId: '',
        roundCount: 0,
        lastRound: null,
        fallbackTitle: `新对话 ${sessionIndex}`,
        createdAt: new Date().toISOString(),
        messages: [
          // {
          //   id: this.generateId(),
          //   role: 'ai',
          //   content: DEFAULT_WELCOME_TEXT,
          //   renderedContent: marked.parse(DEFAULT_WELCOME_TEXT)
          // }
        ]
      }
    },
    createNewSession() {
      if (this.loading) {
        return
      }

      const session = this.createSession()
      this.sessions = [session, ...this.sessions]
      this.activeSessionId = session.id
      this.resetComposerState()
      this.resetTypingState()
      this.closeSidebar()
      this.adjustInputHeight()
    },
    async loadHistorySessions() {
      try {
        const response = await request({
          method: 'get',
          url: `${this.$config.baseContext}/ai/chat/history/sessions`
        })

        const records = Array.isArray(response?.data?.data) ? response.data.data : []

        if (!records.length) {
          this.sessions = []
          this.activeSessionId = ''
          return false
        }

        const sessions = records.map((record, index) => this.normalizeHistorySession(record, index))

        this.sessions = sessions
        this.activeSessionId = sessions[0].id
        return true
      } catch (error) {
        console.error('[Chat] 历史会话加载失败', error)
        this.sessions = []
        this.activeSessionId = ''
        return false
      }
    },
    normalizeHistorySession(record, index) {
      const conversationId = record?.conversationId || this.generateConversationId()
      const history = Array.isArray(record?.history) ? record.history : []
      const lastRound = record?.lastRound || null
      const fallbackTitle = this.trimText(
        lastRound?.user || history[0]?.user || `历史会话 ${index + 1}`,
        18
      )

      const messages = []

      history.forEach((round, roundIndex) => {
        if (round?.user) {
          messages.push({
            id: `${conversationId}-user-${round.rounds || roundIndex + 1}`,
            role: 'user',
            content: round.user,
            renderedContent: marked.parse(round.user)
          })
        }

        if (round?.ai) {
          messages.push({
            id: `${conversationId}-ai-${round.rounds || roundIndex + 1}`,
            role: 'ai',
            content: round.ai,
            renderedContent: marked.parse(round.ai)
          })
        }
      })

      return {
        id: conversationId,
        conversationId,
        memoryId: record?.memoryId || '',
        roundCount: Number(record?.roundCount) || history.length || 0,
        lastRound,
        history,
        fallbackTitle,
        createdAt: new Date().toISOString(),
        messages: messages.length > 0
          ? messages
          : [
            {
              id: this.generateId(),
              role: 'ai',
              content: DEFAULT_WELCOME_TEXT,
              renderedContent: marked.parse(DEFAULT_WELCOME_TEXT)
            }
          ]
      }
    },
    activateSession(sessionId) {
      if (this.loading || sessionId === this.activeSessionId) {
        return
      }

      this.activeSessionId = sessionId
      this.resetComposerState()
      this.resetTypingState()
      this.closeSidebar()
      this.adjustInputHeight()
      this.scrollToBottom()
      this.refreshHighlight()
    },
    getSessionTitle(session) {
      if (!session) {
        return '新对话'
      }

      const firstUserMessage = session.messages.find((message) => message.role === 'user')
      const title = session.lastRound?.user || firstUserMessage?.content || session.fallbackTitle

      if (!title) {
        return '新对话'
      }

      return this.trimText(title.replace(/\s+/g, ' '), 18)
    },
    getSessionPreview(session) {
      if (!session) {
        return '暂无消息'
      }

      if (session.lastRound?.ai || session.lastRound?.user) {
        return this.trimText((session.lastRound.ai || session.lastRound.user).replace(/\s+/g, ' '), 26)
      }

      const lastMessage = [...session.messages].reverse().find((message) => message.role === 'user' || message.content !== DEFAULT_WELCOME_TEXT)

      if (!lastMessage) {
        return '等待开始新的对话'
      }

      return this.trimText(lastMessage.content.replace(/\s+/g, ' '), 26)
    },
    trimText(text, maxLength) {
      if (!text) {
        return ''
      }

      return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
    },
    renderExistingMessages() {
      this.sessions.forEach((session) => {
        session.messages.forEach((message) => {
          if (!message.renderedContent) {
            message.renderedContent = marked.parse(message.content)
          }
        })
      })
    },
    getKnowledgeOptions() {
      getFileListByPath().then((res) => {
        if (res && res.data && res.data.data && res.data.data.records) {
          const remoteOptions = res.data.data.records.map((item) => ({
            label: item.name,
            value: item.knowledgeCode
          }))

          this.selectedKBOptions = [DEFAULT_KB_OPTION, ...remoteOptions]
        }
      })
    },
    adjustInputHeight() {
      this.$nextTick(() => {
        const element = this.$refs.inputBox

        if (!element) {
          return
        }

        element.style.height = 'auto'
        const lineHeight = parseInt(getComputedStyle(element).lineHeight, 10) || 24
        const maxHeight = lineHeight * 3 + 20
        element.style.height = `${Math.min(element.scrollHeight, maxHeight)}px`
      })
    },
    toggleSidebar() {
      this.sidebarVisible = !this.sidebarVisible
    },
    closeSidebar() {
      if (this.isMobile) {
        this.sidebarVisible = false
      }
    },
    handleResize() {
      this.isMobile = window.innerWidth <= 960
      this.sidebarVisible = !this.isMobile
    },
    triggerFileSelect() {
      if (this.$refs.fileInput) {
        this.$refs.fileInput.click()
      }
    },
    handlePaste(event) {
      const clipboardData = event.clipboardData

      if (!clipboardData) {
        return
      }

      const files = []

      for (const item of clipboardData.items) {
        if (item.kind === 'file') {
          const file = item.getAsFile()

          if (file) {
            files.push(file)
          }
        }
      }

      if (files.length > 0) {
        this.selectedFiles.push(...files)
        event.preventDefault()
      }
    },
    manualReconnect() {
      this.allowReconnect = true

      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }

      this.reconnectAttempts = 0
      this.initSocket()
    },
    handleFileSelect(event) {
      const files = Array.from(event.target.files || [])

      if (!files.length) {
        return
      }

      this.selectedFiles.push(...files)
      event.target.value = null
    },
    removeFile(index) {
      this.selectedFiles.splice(index, 1)
    },
    generateId() {
      return Date.now() + Math.random()
    },
    generateConversationId() {
      if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID()
      }

      return `conv-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
    },
    updateSessionLastRound(session, payload = {}) {
      if (!session) {
        return
      }

      const nextRoundCount = payload.incrementRound
        ? (Number(session.roundCount) || 0) + 1
        : (Number(payload.rounds) || Number(session.roundCount) || 0)

      const nextLastRound = {
        user: payload.user !== undefined ? payload.user : session.lastRound?.user || '',
        ai: payload.ai !== undefined ? payload.ai : session.lastRound?.ai || '',
        rounds: payload.rounds || nextRoundCount
      }

      this.$set(session, 'roundCount', nextRoundCount)
      this.$set(session, 'lastRound', nextLastRound)
    },
    getSocketUrl() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'

      const token = localStorage.getItem('token') || ''

      const baseWs = `${protocol}//127.0.0.1:9876`
      return `${baseWs}/chat-ws?token=${encodeURIComponent(token)}`
    },
    finishStreamingRound() {
      this.loading = false
      this.currentAiMessageId = null

      if (this.typingQueue.length === 0) {
        this.refreshHighlight()
      }
    },
    initSocket() {
      if (this.socket) {
        const previousSocket = this.socket
        this.socket = null
        previousSocket.close()
      }

      const socket = new WebSocket(this.getSocketUrl())
      this.socket = socket
      this.socketStatus = 'connecting'

      socket.addEventListener('open', () => {
        if (this.socket !== socket) {
          return
        }

        this.socketStatus = 'open'
        this.reconnectAttempts = 0

        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer)
          this.reconnectTimer = null
        }
      })

      socket.addEventListener('message', (event) => {
        if (this.socket !== socket) {
          return
        }

        if (event.data === '[DONE]') {
          this.finishStreamingRound()
          return
        }

        this.queueTextForTyping(event.data)
      })

      socket.addEventListener('close', () => {
        if (this.socket !== socket) {
          return
        }

        this.socketStatus = 'closed'
        this.finishStreamingRound()
        this.tryReconnect()
      })

      socket.addEventListener('error', () => {
        if (this.socket !== socket) {
          return
        }

        this.socketStatus = 'error'
        this.finishStreamingRound()
        this.tryReconnect()
      })
    },
    tryReconnect() {
      if (!this.allowReconnect || this.reconnectAttempts >= this.maxReconnectAttempts || this.reconnectTimer) {
        return
      }

      const delay = this.reconnectDelay * (this.reconnectAttempts + 1)

      this.reconnectTimer = setTimeout(() => {
        this.reconnectAttempts += 1
        this.initSocket()
      }, delay)
    },
    async handleSend() {
      const messageText = this.userInput.trim()

      if (!messageText && this.selectedFiles.length === 0) {
        return
      }

      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        console.warn('[Chat] WebSocket 未连接，消息发送失败')
        return
      }

      const session = this.currentSession

      if (!session) {
        return
      }

      const conversationId = session.conversationId || this.generateConversationId()

      if (!session.conversationId) {
        this.$set(session, 'conversationId', conversationId)
      }

      const fileNames = this.selectedFiles.map((file) => file.name).join(', ')
      const contentWithFiles = this.selectedFiles.length > 0
        ? (messageText ? `${messageText}\n\n📎 附件: ${fileNames}` : `📎 附件: ${fileNames}`)
        : messageText

      session.messages.push({
        id: this.generateId(),
        role: 'user',
        content: contentWithFiles,
        renderedContent: marked.parse(contentWithFiles)
      })
      this.updateSessionLastRound(session, {
        user: contentWithFiles,
        ai: '',
        incrementRound: true
      })

      this.userInput = ''
      this.loading = true
      this.scrollToBottom()

      const context = session.messages
        .map((message) => (message.role === 'user' ? '用户：' : 'AI：') + message.content)
        .join('\n')

      try {
        if (this.selectedFiles.length > 0) {
          const fileUrlList = []

          for (let index = 0; index < this.selectedFiles.length; index += 1) {
            const uploaded = await this.uploadFile(this.selectedFiles[index])

            uploaded.forEach((url) => {
              fileUrlList.push(url)
            })
          }

          this.safeSend({
            type: 'file',
            conversationId,
            name: '附件',
            modelName: this.selectedModel,
            data: fileUrlList,
            knowledgeCode: this.selectedKB,
            aiRole: 'default',
            text: messageText || '',
            context
          })

          this.selectedFiles = []
        } else {
          this.safeSend({
            type: 'text',
            conversationId,
            modelName: this.selectedModel,
            knowledgeCode: this.selectedKB,
            aiRole: 'default',
            text: messageText,
            context
          })
        }
      } catch (error) {
        console.error('[Chat] 消息发送失败', error)
        this.loading = false
      }
    },
    safeSend(data) {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(typeof data === 'string' ? data : JSON.stringify(data))
      } else {
        console.warn('[Chat] WebSocket 未连接，消息未发送')
      }
    },
    queueTextForTyping(text) {
      const chars = Array.from(text || '')
      this.typingQueue.push(...chars)

      if (!this.isTyping) {
        this.startTyping()
      }
    },
    startTyping() {
      const session = this.currentSession

      if (!session) {
        return
      }

      this.isTyping = true

      let lastMessage = session.messages.find((message) => message.id === this.currentAiMessageId)

      if (!lastMessage) {
        lastMessage = {
          id: this.generateId(),
          role: 'ai',
          content: '',
          renderedContent: ''
        }

        session.messages.push(lastMessage)
        this.currentAiMessageId = lastMessage.id
      }

      const typeNext = () => {
        if (this.typingQueue.length === 0) {
          this.isTyping = false
          this.refreshHighlight()
          return
        }

        const nextChar = this.typingQueue.shift()
        lastMessage.content += nextChar
        lastMessage.renderedContent = marked.parse(lastMessage.content)
        this.updateSessionLastRound(session, {
          user: session.lastRound?.user || '',
          ai: lastMessage.content,
          rounds: session.lastRound?.rounds || session.roundCount
        })
        this.$forceUpdate()
        this.scrollToBottom()

        this.typingTimer = setTimeout(typeNext, 20)
      }

      typeNext()
    },
    resetTypingState() {
      if (this.typingTimer) {
        clearTimeout(this.typingTimer)
        this.typingTimer = null
      }

      this.typingQueue = []
      this.isTyping = false
      this.currentAiMessageId = null
    },
    resetComposerState() {
      this.userInput = ''
      this.selectedFiles = []
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const element = this.$refs.chatList

        if (element) {
          element.scrollTop = element.scrollHeight
        }
      })
    },
    refreshHighlight() {
      this.$nextTick(() => {
        hljs.highlightAll()
      })
    },
    async uploadFile(file) {
      const formData = new FormData()
      formData.append('file', file)

      const response = await uploadFile(formData)
      return response.data.data || []
    }
  }
}
</script>

<style lang="less" scoped>
@page-bg: #0f1219;
@panel-bg: rgba(20, 25, 40, 0.72);
@panel-bg-strong: rgba(26, 31, 44, 0.88);
@text-main: #ececec;
@text-sub: #a3a3a3;
@text-faint: #737373;
@border-color: rgba(255, 255, 255, 0.08);
@border-strong: rgba(255, 255, 255, 0.12);
@accent: #10a37f;
@accent-soft: rgba(16, 163, 127, 0.18);
@shadow-main: 0 24px 60px rgba(0, 0, 0, 0.35);

.ai-page {
  display: flex;
  min-height: 760px;
  background:
    radial-gradient(circle at 12% 18%, rgba(0, 242, 255, 0.1), transparent 24%),
    radial-gradient(circle at 88% 12%, rgba(125, 42, 232, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(15, 18, 25, 0.78), rgba(26, 31, 44, 0.46));
  color: @text-main;
  overflow: hidden;
  position: relative;
}

.sidebar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 19;
}

.session-sidebar {
  width: 280px;
  flex: 0 0 280px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 20px 16px;
  background: @panel-bg;
  border-right: 1px solid @border-color;
  box-shadow:
    inset -1px 0 0 rgba(255, 255, 255, 0.04),
    0 20px 40px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(16px);
  z-index: 20;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  width: 36px;
  height: 36px;
  padding: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid @border-color;
}

.sidebar-label {
  margin: 0 0 4px;
  color: @text-faint;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.sidebar-brand h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.new-chat-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #10a37f, #0f8c6f);
  box-shadow: 0 14px 32px rgba(16, 163, 127, 0.28);
}

.sidebar-summary {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid @border-color;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid transparent;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status-pill.connecting,
.header-status.connecting {
  color: #f5c05d;
}

.status-pill.open,
.header-status.open {
  color: #6ee7b7;
}

.status-pill.closed,
.status-pill.error,
.header-status.closed,
.header-status.error {
  color: #f87171;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: @text-sub;
}

.summary-line strong {
  color: @text-main;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.session-list::-webkit-scrollbar {
  width: 4px;
}

.session-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999px;
}

.session-item {
  width: 100%;
  min-height: 92px;
  max-height: 92px;
  text-align: left;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.035);
  color: @text-main;
  cursor: pointer;
  transition: 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.session-item:hover:not(:disabled) {
  border-color: @border-strong;
  background: rgba(255, 255, 255, 0.06);
}

.session-item.active {
  border-color: rgba(16, 163, 127, 0.26);
  background: linear-gradient(135deg, rgba(16, 163, 127, 0.16), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.session-item:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.session-title {
  font-size: 14px;
  font-weight: 600;
  color: @text-main;
}

.session-preview {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: @text-sub;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.conversation-shell {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.conversation-header {
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid @border-color;
  background: rgba(20, 25, 40, 0.62);
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.mobile-menu-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid @border-color;
  background: rgba(255, 255, 255, 0.04);
  color: @text-main;
}

.header-kicker {
  margin: 0 0 4px;
  color: @text-faint;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.header-copy h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 68px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid @border-color;
}

.reconnect-btn {
  color: @text-main;
}

.conversation-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.conversation-main.is-empty {
  position: relative;
  justify-content: flex-start;
  padding: clamp(72px, 11vh, 118px) 32px 56px;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 4%, rgba(255, 255, 255, 0.72), transparent 36%),
    radial-gradient(circle at 22% 18%, rgba(0, 122, 255, 0.055), transparent 28%),
    radial-gradient(circle at 82% 12%, rgba(52, 199, 89, 0.045), transparent 27%),
    linear-gradient(180deg, #fbfcfd 0%, #f6f8fb 58%, #eef2f6 100%);
}

.conversation-main.is-empty::before {
  display: none;
}

.welcome-stage {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  background: transparent;
}

.welcome-card {
  position: relative;
  width: min(720px, 100%);
  text-align: center;
  padding: 0 18px;
  border: 0;
  outline: 0;
  background: transparent;
  box-shadow: none;
  animation: welcomeRise 0.46s ease both;
}

.welcome-badge-wrap {
  position: relative;
  z-index: 1;
  width: 68px;
  height: 68px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  background: radial-gradient(circle at 50% 8%, rgba(255, 255, 255, 0.85), rgba(255, 255, 255, 0.18) 64%, transparent 74%);
}

.welcome-badge {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(236, 246, 255, 0.9) 100%);
  border: 0;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.075), inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.welcome-icon {
  width: 27px;
  height: 27px;
}

.welcome-copy {
  position: relative;
  z-index: 1;
}

.welcome-kicker {
  margin: 0 auto 14px;
  padding: 0;
  color: rgba(60, 60, 67, 0.54);
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
  background: transparent;
  border: 0;
  box-shadow: none;
  backdrop-filter: none;
}

.welcome-card h2 {
  margin: 0;
  color: #1d1d1f;
  font-size: clamp(40px, 5.2vw, 60px);
  line-height: 1.04;
  font-weight: 760;
  letter-spacing: -0.06em;
}

.welcome-card h2 span {
  background: linear-gradient(180deg, #101114 8%, #2b2f38 54%, #5d6673 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-text {
  width: min(540px, 100%);
  margin: 18px auto 0;
  color: rgba(60, 60, 67, 0.56);
  font-size: 15px;
  line-height: 1.75;
}

.conversation-body {
  flex: 1;
  overflow-y: auto;
  padding: 28px 24px;
}

.conversation-body::-webkit-scrollbar {
  width: 6px;
}

.conversation-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999px;
}

.conversation-inner {
  width: min(920px, 100%);
  margin: 0 auto;
}

.message-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.ai-row {
  align-items: flex-start;
}

.user-row {
  justify-content: flex-end;
}

.message-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid @border-color;
}

.avatar-img {
  width: 20px;
  height: 20px;
}

.message-card {
  min-width: 0;
}

.ai-card {
  flex: 1;
  width: 100%;
  padding: 18px 22px;
  border-radius: 22px;
  border: 1px solid @border-color;
  background: rgba(20, 25, 40, 0.64);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.18);
}

.user-card {
  max-width: min(720px, 78%);
  padding: 14px 18px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: linear-gradient(135deg, rgba(0, 110, 255, 0.2), rgba(125, 42, 232, 0.22));
  box-shadow: 0 16px 28px rgba(0, 0, 0, 0.2);
}

.message-role {
  margin-bottom: 12px;
  color: @accent;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.message-content {
  color: @text-main;
  font-size: 15px;
  line-height: 1.85;
  word-break: break-word;
}

.message-content ::v-deep p {
  margin: 0 0 14px;
}

.message-content ::v-deep p:last-child {
  margin-bottom: 0;
}

.message-content ::v-deep ul,
.message-content ::v-deep ol {
  padding-left: 22px;
  margin: 10px 0;
}

.message-content ::v-deep li {
  margin-bottom: 6px;
}

.message-content ::v-deep a {
  color: #7dd3fc;
  text-decoration: none;
  border-bottom: 1px solid rgba(125, 211, 252, 0.35);
}

.message-content ::v-deep blockquote {
  margin: 16px 0;
  padding: 10px 16px;
  border-left: 3px solid rgba(255, 255, 255, 0.18);
  color: @text-sub;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 0 14px 14px 0;
}

.message-content ::v-deep h1,
.message-content ::v-deep h2,
.message-content ::v-deep h3,
.message-content ::v-deep h4 {
  margin: 18px 0 10px;
  color: #ffffff;
  font-weight: 600;
}

.message-content ::v-deep pre {
  margin: 18px 0;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #111111 !important;
  overflow-x: auto;
}

.message-content ::v-deep pre code {
  background: transparent !important;
  color: #e5e7eb;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.7;
}

.message-content ::v-deep code:not(pre code) {
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #f5f5f5;
  font-size: 13px;
}

.streaming-row {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.streaming-card {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 16px;
  border: 1px solid @border-color;
  background: rgba(20, 25, 40, 0.58);
  color: @text-sub;
  font-size: 13px;
}

.dot-flash {
  display: flex;
  align-items: center;
}

.dot {
  width: 6px;
  height: 6px;
  margin-left: 6px;
  border-radius: 50%;
  background: @accent;
  animation: flashDot 1.2s infinite ease-in-out;
}

.composer-shell {
  width: 100%;
  padding: 16px 24px 24px;
  border-top: 1px solid @border-color;
  background: linear-gradient(180deg, rgba(15, 18, 25, 0), rgba(20, 25, 40, 0.76) 24%, rgba(20, 25, 40, 0.94));
}

.center-shell {
  position: relative;
  z-index: 1;
  width: min(780px, 100%);
  margin: clamp(44px, 7vh, 72px) auto 0;
  padding: 0;
  border-top: none;
  background: transparent;
}

.conversation-main.is-empty .composer-card {
  width: min(780px, 100%);
  padding: 10px 12px 11px;
  border-radius: 30px;
  border: 1px solid rgba(60, 60, 67, 0.065);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 16px 44px rgba(15, 23, 42, 0.085), inset 0 1px 0 rgba(255, 255, 255, 0.96);
  backdrop-filter: saturate(180%) blur(28px);
}

.conversation-main.is-empty .file-item {
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(60, 60, 67, 0.1);
  color: #1d1d1f;
}

.conversation-main.is-empty .remove-file {
  color: rgba(60, 60, 67, 0.56);
}

.conversation-main.is-empty .composer-input-wrap {
  min-height: 56px;
  border-radius: 24px;
  border: 0;
  background: rgba(248, 250, 252, 0.76);
}

.conversation-main.is-empty .composer-input {
  color: #1d1d1f;
  font-size: 16px;
}

.conversation-main.is-empty .composer-input::placeholder {
  color: rgba(60, 60, 67, 0.45);
}

.conversation-main.is-empty .icon-btn {
  border-color: rgba(60, 60, 67, 0.1);
  background: rgba(255, 255, 255, 0.74);
  color: rgba(60, 60, 67, 0.66);
}

.conversation-main.is-empty .icon-btn:hover {
  color: #007aff;
  border-color: rgba(0, 122, 255, 0.24);
  background: rgba(0, 122, 255, 0.08);
}

.conversation-main.is-empty .send-btn {
  border-radius: 16px;
  background: linear-gradient(180deg, #5eb7ff 0%, #007aff 100%);
  box-shadow: 0 16px 34px rgba(0, 122, 255, 0.28);
}

.conversation-main.is-empty .send-btn:disabled {
  background: #d6dbe3;
  color: #ffffff;
  opacity: 1;
  box-shadow: none;
}

.conversation-main.is-empty .composer-tools {
  margin-top: 11px;
  padding-top: 9px;
  border-top: 1px solid rgba(60, 60, 67, 0.045);
}

.conversation-main.is-empty .tool-label {
  color: rgba(60, 60, 67, 0.56);
  font-weight: 700;
}

.conversation-main.is-empty ::v-deep .toolbar-select .el-input__inner {
  border: 1px solid rgba(60, 60, 67, 0.07);
  background: rgba(248, 250, 252, 0.72);
  color: #1d1d1f;
}

.conversation-main.is-empty ::v-deep .toolbar-select .el-input__icon {
  color: rgba(60, 60, 67, 0.54);
}

.file-list {
  width: min(920px, 100%);
  margin: 0 auto 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.file-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(20, 25, 40, 0.7);
  border: 1px solid @border-color;
  color: @text-main;
  font-size: 12px;
}

.remove-file {
  border: none;
  background: transparent;
  color: @text-sub;
  cursor: pointer;
  font-size: 14px;
}

.composer-card {
  width: min(920px, 100%);
  margin: 0 auto;
  padding: 12px 14px 10px;
  border-radius: 22px;
  border: 1px solid @border-strong;
  background: @panel-bg-strong;
  box-shadow: @shadow-main;
  backdrop-filter: blur(18px);
}

.composer-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-btn,
.send-btn {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: 0.2s ease;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.05);
  color: @text-sub;
}

.icon-btn:hover {
  color: @text-main;
  border-color: @border-strong;
}

.send-btn {
  background: linear-gradient(135deg, #10a37f, #0f8c6f);
  color: #fff;
  box-shadow: 0 12px 24px rgba(16, 163, 127, 0.28);
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.composer-input-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 0 0px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.035);  
}

.composer-input {
  width: 100%;
  min-height: 24px;
  max-height: 112px;
  padding: 12px 12px;
  border: none;
  resize: none;
  outline: none;
  background: transparent;
  color: @text-main;
  font-size: 15px;
  line-height: 1.6;
  font-family: 'Helvetica Neue', 'PingFang SC', sans-serif;
}

.composer-input::placeholder {
  color: @text-faint;
}

.composer-input::-webkit-scrollbar {
  width: 4px;
}

.composer-input::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.14);
  border-radius: 999px;
}

.composer-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tool-label {
  color: @text-faint;
  font-size: 12px;
}

.toolbar-select {
  width: 220px;
}

::v-deep .toolbar-select .el-input__inner {
  height: 34px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(20, 25, 40, 0.72);
  color: @text-main;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

::v-deep .toolbar-select .el-input__icon {
  line-height: 34px;
  color: @text-sub;
}

@media (min-width: 961px) {
  .ai-page {
    height: var(--shell-available-height);
    min-height: var(--shell-available-height);
  }
}

@keyframes flashDot {

  0%,
  100% {
    opacity: 0.25;
    transform: scale(0.8);
  }

  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@keyframes welcomeRise {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 960px) {
  .session-sidebar {
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    transform: translateX(-100%);
    transition: transform 0.24s ease;
  }

  .session-sidebar.mobile-open {
    transform: translateX(0);
  }

  .conversation-header {
    padding: 0 16px;
  }

  .conversation-main.is-empty {
    justify-content: flex-start;
    padding: 48px 16px 36px;
  }

  .conversation-main.is-empty::before {
    display: none;
  }

  .conversation-body {
    padding: 20px 16px;
  }

  .composer-shell {
    padding: 12px 16px 16px;
  }

  .toolbar-select {
    width: 170px;
  }
}

@media (max-width: 640px) {
  .welcome-card h2 {
    font-size: 34px;
  }

  .welcome-card {
    padding: 0 6px;
  }

  .welcome-badge-wrap {
    width: 60px;
    height: 60px;
    margin-bottom: 18px;
  }

  .welcome-badge {
    width: 48px;
    height: 48px;
    border-radius: 16px;
  }

  .welcome-kicker {
    max-width: 100%;
    letter-spacing: 0.06em;
    white-space: nowrap;
  }

  .welcome-text {
    font-size: 14px;
  }

  .conversation-main.is-empty .composer-card {
    border-radius: 24px;
    padding: 12px;
  }

  .conversation-main.is-empty .composer-main {
    align-items: flex-end;
  }

  .conversation-main.is-empty .composer-input-wrap {
    min-height: 48px;
  }

  .message-row {
    gap: 12px;
  }

  .message-avatar {
    width: 32px;
    height: 32px;
    flex-basis: 32px;
  }

  .user-card {
    max-width: 88%;
  }

  .composer-card {
    border-radius: 20px;
  }

  .composer-tools {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-select {
    width: 100%;
  }

  .tool-group {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<style lang="less">
.ai-select-popper {
  min-width: 220px !important;
  max-width: min(360px, calc(100vw - 24px));
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: rgba(26, 31, 44, 0.98);
  box-shadow: 0 18px 30px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
}

.ai-select-popper .el-scrollbar__wrap {
  max-height: 280px;
}

.ai-select-popper .el-select-dropdown__item {
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-select-popper .el-select-dropdown__item.hover,
.ai-select-popper .el-select-dropdown__item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
}

.ai-select-popper .el-select-dropdown__item.selected {
  color: #10a37f;
  font-weight: 600;
  background: rgba(16, 163, 127, 0.08);
}
</style>
