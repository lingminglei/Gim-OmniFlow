<template>
  <div class="chat-view">
    <div class="chat-view__bg"></div>

    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <div>
          <h2 class="sidebar-title">站内信</h2>
        </div>
        <div :class="['status-pill', connectionStatusClass]">
          <span class="status-pill__dot"></span>
          {{ connectionStatusText }}
        </div>
      </div>

      <div class="sidebar-summary">
        <div class="summary-card">
          <span class="summary-card__label">当前用户</span>
          <strong class="summary-card__value text-ellipsis">{{ userInfo.userName || userInfo.userId || '-' }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-card__label">登录状态</span>
          <strong class="summary-card__value">{{ currentLoginStateText }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-card__label">在线人数</span>
          <strong class="summary-card__value">{{ onlineUserCount }}</strong>
        </div>
      </div>

      <div class="sidebar-tabs" role="tablist" aria-label="站内信列表切换">
        <button
          type="button"
          :class="['sidebar-tab', { active: activeTab === 'recent' }]"
          @click="switchTab('recent')"
        >
          <span>最近会话</span>
          <span class="sidebar-tab__count">{{ recentConversationCount }}</span>
        </button>
        <button
          type="button"
          :class="['sidebar-tab', { active: activeTab === 'online' }]"
          @click="switchTab('online')"
        >
          <span>在线用户</span>
          <span class="sidebar-tab__count">{{ onlineUserCount }}</span>
        </button>
      </div>

      <div class="sidebar-search">
        <el-input
          v-model="searchKeyword"
          size="small"
          clearable
          :placeholder="sidebarSearchPlaceholder"
          class="chat-input"
        >
          <i slot="prefix" class="el-input__icon el-icon-search"></i>
        </el-input>
      </div>

      <div class="sidebar-panel">
        <div class="sidebar-panel__head">
          <div>
            <h3 class="section-title">{{ activeTab === 'recent' ? '最近会话' : '在线用户' }}</h3>
          </div>
          <div class="sidebar-panel__action">
            <span class="sidebar-count">{{ activeTabCount }}</span>
            <el-button type="text" class="link-button" @click="handleSidebarRefresh">
              {{ activeTab === 'recent' ? '刷新会话' : '刷新在线' }}
            </el-button>
          </div>
        </div>

        <div v-if="activeTab === 'recent'" class="sidebar-list">
          <button
            v-for="contact in filteredContacts"
            :key="contact.user_id"
            type="button"
            :class="['sidebar-item', 'conversation-card', { active: contact.user_id === activePeerId }]"
            @click="selectConversation(contact.user_id)"
          >
            <div class="contact-avatar">
              <img
                v-if="contact.avatar_url"
                :src="contact.avatar_url"
                :alt="contact.user_name"
                class="contact-avatar__image"
              >
              <span v-else class="contact-avatar__fallback">{{ getAvatarText(contact.user_name) }}</span>
              <span :class="['contact-avatar__status', contact.online ? 'online' : 'offline']"></span>
            </div>

            <div class="sidebar-item__content">
              <div class="sidebar-item__top">
                <span class="sidebar-item__name">{{ contact.user_name }}</span>
                <span class="sidebar-item__time">{{ formatContactTime(getContactSortValue(contact)) }}</span>
              </div>
              <div class="sidebar-item__bottom">
                <span class="sidebar-item__preview">{{ getConversationPreview(contact) }}</span>
                <span v-if="contact.unread_count > 0" class="contact-unread">
                  {{ contact.unread_count > 99 ? '99+' : contact.unread_count }}
                </span>
              </div>
            </div>
          </button>

          <el-empty
            v-if="filteredContacts.length === 0"
            description="暂无最近会话"
            :image-size="56"
            class="chat-empty"
          />
        </div>

        <div v-else class="sidebar-list">
          <button
            v-for="user in filteredOnlineUsers"
            :key="user.user_id"
            type="button"
            class="sidebar-item online-user-card"
            @click="openOnlineChat(user.user_id)"
          >
            <div class="contact-avatar">
              <img
                v-if="user.avatar_url"
                :src="user.avatar_url"
                :alt="user.user_name"
                class="contact-avatar__image"
              >
              <span v-else class="contact-avatar__fallback">{{ getAvatarText(user.user_name) }}</span>
              <span class="contact-avatar__status online"></span>
            </div>

            <div class="sidebar-item__content">
              <div class="sidebar-item__top">
                <span class="sidebar-item__name">{{ user.user_name }}</span>
                <span class="online-tag">在线</span>
              </div>
              <div class="sidebar-item__bottom sidebar-item__bottom--single">
                <span class="sidebar-item__preview">{{ getOnlineUserSubtitle(user) }}</span>
              </div>
            </div>
          </button>

          <el-empty
            v-if="filteredOnlineUsers.length === 0"
            description="当前没有在线用户"
            :image-size="56"
            class="chat-empty"
          />
        </div>
      </div>
    </aside>

    <section class="chat-panel">
      <template v-if="selectedContact">
        <header class="chat-header">
          <div class="chat-peer">
            <div class="contact-avatar large">
              <img
                v-if="selectedContact.avatar_url"
                :src="selectedContact.avatar_url"
                :alt="selectedContact.user_name"
                class="contact-avatar__image"
              >
              <span v-else class="contact-avatar__fallback">{{ getAvatarText(selectedContact.user_name) }}</span>
              <span :class="['contact-avatar__status', selectedContact.online ? 'online' : 'offline']"></span>
            </div>

            <div class="chat-peer__meta">
              <h3 class="chat-peer__name">{{ selectedContact.user_name }}</h3>
              <p class="chat-peer__status">{{ getPresenceText(selectedContact) }}</p>
            </div>
          </div>

          <div class="chat-header__actions">
            <el-button type="text" class="link-button" @click="refreshConversation">
              刷新会话
            </el-button>
            <el-button type="text" class="link-button" @click="refreshOnlineUsers">
              刷新在线用户
            </el-button>
          </div>
        </header>

        <div v-if="showConnectionBanner" class="chat-banner">
          <span>{{ bannerText }}</span>
          <el-button type="text" class="link-button" @click="reconnectChat">
            立即重连
          </el-button>
        </div>

        <div
          ref="messageScroller"
          class="message-scroller"
          @scroll.passive="handleMessageScroll"
        >
          <div v-if="historyState.loading && currentMessages.length === 0" class="history-hint">
            正在加载聊天记录...
          </div>

          <template v-else>
            <div v-if="historyState.loading && currentMessages.length > 0" class="history-hint">
              正在加载更早消息...
            </div>
            <div v-else-if="!historyState.has_more && currentMessages.length > 0" class="history-hint muted">
              已经到顶了
            </div>

            <div v-if="currentMessages.length === 0" class="empty-conversation">
              <div class="empty-conversation__card">
                <p class="empty-conversation__kicker">PRIVATE CHAT</p>
                <h4>开始与 {{ selectedContact.user_name }} 对话</h4>
                <p>支持历史消息加载、发送状态反馈、失败重试和断线重连。</p>
              </div>
            </div>

            <div
              v-for="message in currentMessages"
              :key="message.id"
              :class="['message-row', isSelfMessage(message) ? 'self' : 'other']"
            >
              <div v-if="!isSelfMessage(message)" class="contact-avatar mini">
                <img
                  v-if="selectedContact.avatar_url"
                  :src="selectedContact.avatar_url"
                  :alt="selectedContact.user_name"
                  class="contact-avatar__image"
                >
                <span v-else class="contact-avatar__fallback">{{ getAvatarText(selectedContact.user_name) }}</span>
              </div>

              <div class="message-box">
                <div class="message-meta">
                  <span class="message-time">{{ formatMessageTime(message.timestamp) }}</span>
                  <span
                    v-if="isSelfMessage(message)"
                    :class="['message-state', getMessageStatusClass(message)]"
                  >
                    {{ getMessageStatusText(message) }}
                  </span>
                </div>

                <div :class="['message-bubble', isSelfMessage(message) ? 'self' : 'other']">
                  <span>{{ message.content }}</span>
                </div>

                <button
                  v-if="isSelfMessage(message) && message.status === 'failed'"
                  type="button"
                  class="retry-button"
                  @click="retryMessage(message)"
                >
                  重新发送
                </button>
              </div>
            </div>
          </template>
        </div>

        <footer class="chat-composer">
          <div class="chat-composer__toolbar">
            <span v-if="chatState.lastError" class="chat-composer__error">{{ chatState.lastError }}</span>
          </div>

          <div class="chat-composer__body">
            <el-input
              v-model="draftMessage"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 6 }"
              resize="none"
              placeholder="输入消息，Enter 发送，Shift + Enter 换行"
              class="chat-textarea"
              @keydown.native="handleComposerKeydown"
            />

            <div class="chat-composer__actions">
              <el-button
                type="primary"
                :disabled="sendingDisabled"
                class="send-button"
                @click="sendCurrentMessage"
              >
                发送消息
              </el-button>
            </div>
          </div>
        </footer>
      </template>

      <div v-else class="chat-panel__empty">
        <div class="empty-panel">
          <p class="empty-panel__kicker">GIM MESSAGE</p>
          <h3>从左侧开始一段新对话</h3>
          <p>切换最近会话或在线用户，点击任意联系人即可开始聊天。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import chatService from '@/services/chatService'

/**
 * 解析会话排序时间。
 * 最近会话严格按照最后消息时间降序；无消息时回退到最后活跃或连接时间。
 */
function resolveRecentTime(contact = {}) {
  return new Date(contact.last_timestamp || contact.last_seen_at || contact.connected_at || 0).getTime()
}

/**
 * 最近会话列表按最近时间纯降序排列，不再让在线状态影响顺序。
 */
function sortRecentContacts(contacts = []) {
  return [...contacts].sort((left, right) => {
    const leftTime = resolveRecentTime(left)
    const rightTime = resolveRecentTime(right)
    if (leftTime !== rightTime) {
      return rightTime - leftTime
    }

    return String(left.user_name || '').localeCompare(String(right.user_name || ''))
  })
}

function createDefaultHistoryState() {
  return {
    loading: false,
    has_more: true,
    next_offset: 0
  }
}

export default {
  name: 'ChatView',
  data() {
    return {
      activeTab: 'recent',
      searchKeyword: '',
      draftMessage: '',
      chatState: chatService.getState(),
      unsubscribeChat: null,
      autoSelected: false,
      prependScrollSnapshot: null,
      shouldStickToBottom: true
    }
  },
  computed: {
    /**
     * 当前登录用户信息，用于消息归属和连接初始化。
     */
    userInfo() {
      return this.$store.state.user.userInfo || {}
    },
    /**
     * 当前登录用户的站内信展示资料。
     * 这里只从现有登录态里取字段，不依赖额外公共模块。
     */
    chatProfile() {
      return {
        user_name: this.userInfo.userName || this.userInfo.nickName || this.userInfo.nickname || this.userInfo.userId || '',
        avatar_url: this.userInfo.avatarUrl || this.userInfo.avatar || this.userInfo.userImg || ''
      }
    },
    /**
     * 当前激活会话 ID。
     */
    activePeerId() {
      return this.chatState.activePeerId || ''
    },
    /**
     * 最近会话总数。
     */
    recentConversationCount() {
      return (this.chatState.contacts || []).length
    },
    /**
     * 在线用户总数。
     */
    onlineUserCount() {
      return (this.chatState.onlineUsers || []).length + this.currentUserOnlineCountOffset
    },
    /**
     * 鍦ㄧ嚎浜烘暟闇€瑕佸皢褰撳墠鑷繁涔熺粺璁″湪鍐呫€?
     * 鐢变簬鍦ㄧ嚎鍒楄〃榛樿浼氭帓闄ょ粍浠跺唴鐨勫綋鍓嶇敤鎴凤紝杩欓噷鐢ㄥ亸绉婚噺琛ュ洖鑷繁銆?
     */
    currentUserOnlineCountOffset() {
      const currentUserId = String(this.userInfo.userId || '').trim()
      if (!currentUserId || !this.chatState.connected || !this.chatState.sessionReady) {
        return 0
      }

      const alreadyIncluded = (this.chatState.onlineUsers || []).some((user) => {
        return String(user.user_id || '').trim() === currentUserId
      })

      return alreadyIncluded ? 0 : 1
    },
    /**
     * 褰撳墠鍦ㄧ嚎鍒楄〃鍦ㄦ悳绱㈡椂涔熻淇濇寔鍖呭惈鑷繁鐨勮鏁板彛寰勩€?
     */
    currentUserMatchesOnlineFilter() {
      if (!this.currentUserOnlineCountOffset) {
        return false
      }

      const keyword = String(this.searchKeyword || '').trim().toLowerCase()
      if (!keyword) {
        return true
      }

      return [
        this.userInfo.userName,
        this.userInfo.nickName,
        this.userInfo.nickname,
        this.userInfo.userId
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    },
    /**
     * 根据关键字过滤并按最近时间降序排列会话联系人。
     */
    filteredContacts() {
      const keyword = String(this.searchKeyword || '').trim().toLowerCase()
      const contacts = sortRecentContacts(this.chatState.contacts || [])
      if (!keyword) {
        return contacts
      }

      return contacts.filter((contact) => {
        return [contact.user_name, contact.last_message]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword))
      })
    },
    /**
     * 在线用户列表只展示当前在线用户，并根据关键字过滤。
     */
    filteredOnlineUsers() {
      const keyword = String(this.searchKeyword || '').trim().toLowerCase()
      const onlineUsers = this.onlineUsersForTest || []
      if (!keyword) {
        return onlineUsers
      }

      return onlineUsers.filter((user) => {
        return String(user.user_name || '').toLowerCase().includes(keyword)
      })
    },
    /**
     * 在线用户面板数据。
     */
    onlineUsersForTest() {
      return this.chatState.onlineUsers || []
    },
    /**
     * 当前选中的联系人；支持从在线测试列表直接发起聊天。
     */
    selectedContact() {
      const contacts = this.chatState.contacts || []
      const onlineUsers = this.chatState.onlineUsers || []
      return contacts.find((contact) => contact.user_id === this.activePeerId)
        || onlineUsers.find((contact) => contact.user_id === this.activePeerId)
        || null
    },
    /**
     * 当前会话的消息列表。
     */
    currentMessages() {
      if (!this.activePeerId) {
        return []
      }
      return this.chatState.messagesByPeer[this.activePeerId] || []
    },
    /**
     * 当前会话的历史加载状态。
     */
    historyState() {
      if (!this.activePeerId) {
        return createDefaultHistoryState()
      }
      return this.chatState.historyByPeer[this.activePeerId] || createDefaultHistoryState()
    },
    /**
     * 左侧头部当前显示的计数值。
     */
    activeTabCount() {
      if (this.activeTab === 'recent') {
        return this.filteredContacts.length
      }

      return this.filteredOnlineUsers.length + (this.currentUserMatchesOnlineFilter ? this.currentUserOnlineCountOffset : 0)
    },
    /**
     * 搜索框占位文案会随当前 Tab 切换。
     */
    sidebarSearchPlaceholder() {
      return this.activeTab === 'recent' ? '搜索最近会话' : '搜索在线用户'
    },
    /**
     * 顶部连接状态文案。
     */
    connectionStatusText() {
      if (this.chatState.connected && this.chatState.sessionReady) {
        return '连接正常'
      }
      if (this.chatState.reconnecting) {
        return '重连中'
      }
      if (this.chatState.connecting) {
        return '连接中'
      }
      return '未连接'
    },
    /**
     * 顶部连接状态样式。
     */
    connectionStatusClass() {
      if (this.chatState.connected && this.chatState.sessionReady) {
        return 'online'
      }
      if (this.chatState.reconnecting || this.chatState.connecting) {
        return 'pending'
      }
      return 'offline'
    },
    /**
     * 是否显示连接告警横幅。
     */
    showConnectionBanner() {
      return Boolean(this.chatState.lastError) || this.chatState.reconnecting
    },
    /**
     * 连接告警文案。
     */
    bannerText() {
      if (this.chatState.reconnecting) {
        return '站内信连接已中断，正在自动重连...'
      }
      return this.chatState.lastError || '站内信连接异常'
    },
    /**
     * 当前站内信登录状态文案。
     */
    currentLoginStateText() {
      if (this.chatState.connected && this.chatState.sessionReady) {
        return '已登录'
      }
      if (this.chatState.connecting || this.chatState.reconnecting) {
        return '连接中'
      }
      return '未登录'
    },
    /**
     * 发送按钮是否禁用。
     */
    sendingDisabled() {
      return !String(this.draftMessage || '').trim() || !this.activePeerId || !this.chatState.connected || !this.chatState.sessionReady
    }
  },
  watch: {
    /**
     * 切换会话后滚动到底部，方便查看最新消息。
     */
    activePeerId() {
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    /**
     * 消息列表变化时，分别处理历史加载和新增消息的滚动逻辑。
     */
    currentMessages(nextMessages, previousMessages) {
      this.$nextTick(() => {
        const scroller = this.$refs.messageScroller
        if (!scroller) {
          return
        }

        if (this.prependScrollSnapshot) {
          const snapshot = this.prependScrollSnapshot
          this.prependScrollSnapshot = null
          scroller.scrollTop = scroller.scrollHeight - snapshot.scrollHeight + snapshot.scrollTop
          return
        }

        const previousLength = previousMessages ? previousMessages.length : 0
        const appended = nextMessages.length > previousLength
        const lastMessage = nextMessages[nextMessages.length - 1]
        if (!appended || !lastMessage) {
          return
        }

        if (this.shouldStickToBottom || this.isSelfMessage(lastMessage)) {
          this.scrollToBottom()
        }
      })
    }
  },
  mounted() {
    /**
     * 订阅服务层快照，让视图层只负责渲染和交互。
     */
    this.unsubscribeChat = chatService.subscribe((nextState) => {
      const sortedContacts = sortRecentContacts(nextState.contacts || [])
      const shouldAutoSelect = !nextState.activePeerId && sortedContacts.length > 0
      this.chatState = nextState

      if (shouldAutoSelect && !this.autoSelected) {
        this.autoSelected = true
        this.selectConversation(sortedContacts[0].user_id)
      }
    })

    /**
     * 进入聊天页时只建立站内信专用连接，不碰 AI 助手 WebSocket。
     */
    if (this.userInfo.userId) {
      chatService.connect(this.userInfo.userId, this.chatProfile)
    }
  },
  beforeRouteLeave(to, from, next) {
    /**
     * 路由离开聊天页时关闭站内信专用连接，避免公共区常驻连接。
     */
    this.teardownChatSession()
    next()
  },
  beforeDestroy() {
    /**
     * 组件销毁时释放订阅并关闭站内信连接。
     */
    this.teardownChatSession()
  },
  methods: {
    /**
     * 切换左侧列表 Tab。
     */
    switchTab(tab) {
      this.activeTab = tab
    },
    /**
     * 侧栏刷新按钮根据当前 Tab 执行不同刷新动作。
     */
    handleSidebarRefresh() {
      if (this.activeTab === 'online') {
        this.refreshOnlineUsers()
        return
      }

      chatService.requestConversationList()
    },
    /**
     * 统一清理当前页面的站内信订阅和连接。
     */
    teardownChatSession() {
      if (this.unsubscribeChat) {
        this.unsubscribeChat()
        this.unsubscribeChat = null
      }
      chatService.disconnect({ clearState: true })
    },
    /**
     * 选择联系人并切换会话。
     */
    selectConversation(peerId) {
      if (!peerId) {
        return
      }

      this.shouldStickToBottom = true
      chatService.setActiveConversation(peerId)
    },
    /**
     * 从在线用户列表直接发起聊天，并切回最近会话 Tab。
     */
    openOnlineChat(peerId) {
      this.activeTab = 'recent'
      this.selectConversation(peerId)
    },
    /**
     * 刷新会话列表、在线用户和当前会话历史。
     */
    refreshConversation() {
      if (!this.activePeerId) {
        return
      }

      chatService.requestConversationList()
      chatService.requestOnlineUsers()
      chatService.loadHistory(this.activePeerId, { reset: true })
    },
    /**
     * 手动刷新在线用户测试列表。
     */
    refreshOnlineUsers() {
      if (!this.chatState.connected) {
        this.reconnectChat()
        return
      }

      chatService.requestOnlineUsers()
    },
    /**
     * 手动重连站内信服务。
     */
    reconnectChat() {
      if (!this.userInfo.userId) {
        return
      }

      chatService.connect(this.userInfo.userId, this.chatProfile)
    },
    /**
     * 靠近顶部时继续加载更早消息。
     */
    handleMessageScroll() {
      const scroller = this.$refs.messageScroller
      if (!scroller) {
        return
      }

      const distanceToBottom = scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight
      this.shouldStickToBottom = distanceToBottom < 80

      if (scroller.scrollTop <= 60) {
        this.loadOlderMessages()
      }
    },
    /**
     * 加载更早消息，并记录滚动锚点防止页面跳动。
     */
    loadOlderMessages() {
      if (!this.activePeerId || this.historyState.loading || this.historyState.has_more === false) {
        return
      }

      const scroller = this.$refs.messageScroller
      if (scroller) {
        this.prependScrollSnapshot = {
          scrollHeight: scroller.scrollHeight,
          scrollTop: scroller.scrollTop
        }
      }

      chatService.loadHistory(this.activePeerId)
    },
    /**
     * Enter 发送，Shift + Enter 换行。
     */
    handleComposerKeydown(event) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.sendCurrentMessage()
      }
    },
    /**
     * 发送当前输入框消息。
     */
    sendCurrentMessage() {
      const content = String(this.draftMessage || '').trim()
      if (!content) {
        return
      }

      try {
        chatService.sendMessage({
          peerId: this.activePeerId,
          content
        })
        this.draftMessage = ''
        this.shouldStickToBottom = true
      } catch (error) {
        this.$message.error(error.message || '消息发送失败')
      }
    },
    /**
     * 对失败消息进行重发。
     */
    retryMessage(message) {
      try {
        chatService.retryMessage(this.activePeerId, message)
        this.shouldStickToBottom = true
      } catch (error) {
        this.$message.error(error.message || '消息重发失败')
      }
    },
    /**
     * 判断消息是否由当前用户发送。
     */
    isSelfMessage(message) {
      return String(message.sender_id) === String(this.userInfo.userId)
    },
    /**
     * 将消息区滚动到底部。
     */
    scrollToBottom() {
      const scroller = this.$refs.messageScroller
      if (!scroller) {
        return
      }

      scroller.scrollTop = scroller.scrollHeight
    },
    /**
     * 解析当前联系人用于排序与显示的时间字段。
     */
    getContactSortValue(contact) {
      return contact.last_timestamp || contact.last_seen_at || contact.connected_at || null
    },
    /**
     * 会话列表摘要展示。
     */
    getConversationPreview(contact) {
      if (!contact.last_message) {
        return contact.online ? '当前在线，可立即沟通' : '暂无消息'
      }

      if (contact.last_msg_type && contact.last_msg_type !== 'text') {
        return '[非文本消息]'
      }

      return contact.last_message
    },
    /**
     * 在线用户列表的辅助说明文案。
     */
    getOnlineUserSubtitle(user) {
      if (user.connected_at) {
        return `当前在线 · ${this.formatContactTime(user.connected_at)} 加入`
      }
      return '当前在线，可立即发起聊天'
    },
    /**
     * 在线状态文案展示。
     */
    getPresenceText(contact) {
      if (!contact) {
        return ''
      }

      if (contact.online) {
        return contact.is_logged_in ? '在线 · 已登录' : '在线'
      }

      if (contact.last_seen_at) {
        return `离线 · 最后活跃 ${this.formatContactTime(contact.last_seen_at)}`
      }

      return '离线 · 未登录'
    },
    /**
     * 生成头像占位文案。
     */
    getAvatarText(name) {
      const value = String(name || '').trim()
      if (!value) {
        return '聊'
      }
      return value.slice(0, 1).toUpperCase()
    },
    /**
     * 格式化联系人时间。
     */
    formatContactTime(value) {
      if (!value) {
        return ''
      }

      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return ''
      }

      const now = new Date()
      if (now.toDateString() === date.toDateString()) {
        return `${this.padNumber(date.getHours())}:${this.padNumber(date.getMinutes())}`
      }

      return `${this.padNumber(date.getMonth() + 1)}-${this.padNumber(date.getDate())}`
    },
    /**
     * 格式化消息时间。
     */
    formatMessageTime(value) {
      if (!value) {
        return ''
      }

      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return ''
      }

      return `${this.padNumber(date.getHours())}:${this.padNumber(date.getMinutes())}`
    },
    /**
     * 数字补齐到两位。
     */
    padNumber(value) {
      return String(value).padStart(2, '0')
    },
    /**
     * 消息发送状态文案。
     */
    getMessageStatusText(message) {
      const statusMap = {
        sending: '发送中',
        sent: '已发送',
        delivered: '已送达',
        read: '已读',
        failed: '发送失败'
      }

      return statusMap[message.status] || '处理中'
    },
    /**
     * 消息发送状态样式。
     */
    getMessageStatusClass(message) {
      return `state-${message.status || 'sent'}`
    }
  }
}
</script>

<style lang="less" scoped>
@bg-dark: #0f1219;
@bg-panel: rgba(26, 31, 44, 0.82);
@bg-bubble-self: linear-gradient(135deg, rgba(0, 242, 255, 0.16), rgba(125, 42, 232, 0.22));
@primary-color: #00f2ff;
@text-main: #e2e8f0;
@text-sub: #94a3b8;
@border-color: rgba(255, 255, 255, 0.08);
@danger-color: #ff6b6b;
@success-color: #10b981;
@warning-color: #f59e0b;

.chat-view {
  position: relative;
  display: flex;
  min-height: 760px;
  color: @text-main;
  background: transparent;
  overflow: hidden;
}

.chat-view__bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 18%, rgba(0, 242, 255, 0.08), transparent 26%),
    radial-gradient(circle at 88% 12%, rgba(125, 42, 232, 0.1), transparent 25%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.02), transparent 50%);
  pointer-events: none;
}

.chat-sidebar,
.chat-panel {
  position: relative;
  z-index: 1;
}

.chat-sidebar {
  width: 360px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px 16px;
  background: rgba(17, 22, 34, 0.72);
  border-right: 1px solid @border-color;
  backdrop-filter: blur(10px);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.sidebar-kicker,
.section-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: fade(@text-sub, 85%);
}

.sidebar-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid @border-color;
  font-size: 12px;
  white-space: nowrap;
}

.status-pill__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status-pill.online {
  color: @success-color;
  background: fade(@success-color, 10%);
}

.status-pill.pending {
  color: @warning-color;
  background: fade(@warning-color, 10%);
}

.status-pill.offline {
  color: @danger-color;
  background: fade(@danger-color, 10%);
}

.sidebar-search {
  margin-top: 4px;
}

.sidebar-summary {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.summary-card {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid @border-color;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.summary-card__label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: @text-sub;
}

.summary-card__value {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.sidebar-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 6px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid @border-color;
}

.sidebar-tab {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: @text-sub;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.22s ease;
}

.sidebar-tab:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.04);
}

.sidebar-tab.active {
  background: linear-gradient(135deg, rgba(0, 242, 255, 0.14), rgba(125, 42, 232, 0.16));
  color: #ffffff;
  box-shadow: inset 0 0 0 1px fade(@primary-color, 18%);
}

.sidebar-tab__count,
.sidebar-count {
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
  color: @text-sub;
}

.sidebar-tab.active .sidebar-tab__count {
  background: fade(@primary-color, 16%);
  color: @primary-color;
}

.sidebar-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 14px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid @border-color;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.sidebar-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-panel__action {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-list {
  flex: 1;
  min-height: 0;
  margin-top: 14px;
  overflow-y: auto;
  padding-right: 4px;
}

.sidebar-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  color: @text-main;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.22s ease, background 0.22s ease, transform 0.22s ease, box-shadow 0.22s ease;
}

.sidebar-item:hover {
  transform: translateY(-1px);
  border-color: fade(@primary-color, 24%);
  background: rgba(255, 255, 255, 0.06);
}

.conversation-card.active {
  border-color: fade(@primary-color, 36%);
  background: linear-gradient(135deg, rgba(0, 242, 255, 0.1), rgba(125, 42, 232, 0.12));
  box-shadow: inset 0 0 0 1px fade(@primary-color, 14%);
}

.online-user-card {
  background: rgba(255, 255, 255, 0.05);
}

.sidebar-item__content {
  flex: 1;
  min-width: 0;
}

.sidebar-item__top,
.sidebar-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-item__top {
  margin-bottom: 6px;
}

.sidebar-item__bottom--single {
  justify-content: flex-start;
}

.sidebar-item__name {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-item__time {
  flex-shrink: 0;
  font-size: 12px;
  color: @text-sub;
}

.sidebar-item__preview {
  font-size: 13px;
  color: fade(@text-sub, 96%);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.online-tag {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 999px;
  background: fade(@success-color, 14%);
  color: @success-color;
  font-size: 12px;
  font-weight: 600;
}

.online-lab,
.contact-section {
  border: 1px solid @border-color;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  box-shadow: inset 0 0 24px rgba(0, 0, 0, 0.18);
}

.online-lab {
  padding: 16px;
}

.online-lab__head,
.contact-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.online-lab__meta {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.online-lab__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: @text-sub;
}

.online-lab__row + .online-lab__row {
  margin-top: 8px;
}

.online-lab__value {
  color: @text-main;
}

.text-ellipsis {
  max-width: 170px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.online-lab__list {
  margin-top: 14px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 4px;
}

.online-user {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.02);
  color: @text-main;
  text-align: left;
  cursor: pointer;
  transition: all 0.25s ease;
}

.online-user:hover {
  border-color: fade(@primary-color, 24%);
  background: rgba(255, 255, 255, 0.05);
}

.online-user__info {
  flex: 1;
  min-width: 0;
}

.online-user__name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.online-user__intro {
  margin-top: 4px;
  font-size: 12px;
  color: @text-sub;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.online-user__action {
  flex-shrink: 0;
  font-size: 12px;
  color: @primary-color;
}

.contact-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 16px;
}

.contact-section__count {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(0, 242, 255, 0.12);
  color: @primary-color;
  font-size: 12px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
}

.contact-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 14px;
  padding-right: 4px;
}

.contact-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 12px;
  margin-bottom: 10px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.02);
  color: @text-main;
  text-align: left;
  cursor: pointer;
  transition: all 0.25s ease;
}

.contact-card:hover {
  border-color: fade(@primary-color, 20%);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 0 18px fade(@primary-color, 8%);
}

.contact-card.active {
  border-color: fade(@primary-color, 38%);
  background: linear-gradient(90deg, rgba(0, 242, 255, 0.08), rgba(125, 42, 232, 0.08));
  box-shadow: inset 0 0 0 1px fade(@primary-color, 18%);
}

.contact-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 14px;
  border: 1px solid fade(@primary-color, 20%);
  background: linear-gradient(135deg, rgba(0, 242, 255, 0.2), rgba(125, 42, 232, 0.22));
  overflow: hidden;
}

.contact-avatar.large {
  width: 54px;
  height: 54px;
  border-radius: 16px;
}

.contact-avatar.mini {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

.contact-avatar__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.contact-avatar__fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
}

.contact-avatar__status {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid @bg-dark;
}

.contact-avatar__status.online {
  background: @success-color;
}

.contact-avatar__status.offline {
  background: #64748b;
}

.contact-meta {
  flex: 1;
  min-width: 0;
}

.contact-meta__top,
.contact-meta__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.contact-meta__top {
  margin-bottom: 6px;
}

.contact-name {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-time {
  flex-shrink: 0;
  font-size: 12px;
  color: @text-sub;
}

.contact-preview {
  font-size: 13px;
  color: fade(@text-sub, 95%);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-unread {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: @danger-color;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  line-height: 22px;
  text-align: center;
  box-shadow: 0 0 12px fade(@danger-color, 30%);
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 22px;
  min-height: 0;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  background: @bg-panel;
  border: 1px solid @border-color;
  border-radius: 18px 18px 0 0;
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.chat-peer__name {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

.chat-peer__status {
  margin: 0;
  font-size: 13px;
  color: @text-sub;
}

.chat-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  background: fade(@warning-color, 10%);
  border-left: 1px solid @border-color;
  border-right: 1px solid @border-color;
  color: lighten(@warning-color, 10%);
  font-size: 13px;
}

.message-scroller {
  flex: 1;
  overflow-y: auto;
  padding: 18px 22px 22px;
  background: rgba(15, 18, 25, 0.52);
  border-left: 1px solid @border-color;
  border-right: 1px solid @border-color;
}

.history-hint {
  padding: 8px 0 16px;
  text-align: center;
  font-size: 12px;
  color: fade(@primary-color, 88%);
}

.history-hint.muted {
  color: @text-sub;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.message-row.self {
  justify-content: flex-end;
}

.message-box {
  max-width: min(72%, 560px);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message-row.self .message-box {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: @text-sub;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  border: 1px solid @border-color;
  line-height: 1.7;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble.other {
  background: rgba(255, 255, 255, 0.04);
  color: @text-main;
}

.message-bubble.self {
  background: @bg-bubble-self;
  color: #ffffff;
  box-shadow: 0 0 18px fade(@primary-color, 12%);
}

.message-state {
  font-weight: 600;
}

.message-state.state-sending {
  color: @warning-color;
}

.message-state.state-sent,
.message-state.state-delivered {
  color: @text-sub;
}

.message-state.state-read {
  color: @success-color;
}

.message-state.state-failed {
  color: @danger-color;
}

.retry-button {
  padding: 0;
  border: none;
  background: none;
  color: @danger-color;
  font-size: 12px;
  cursor: pointer;
}

.chat-composer {
  padding: 16px 20px 18px;
  background: @bg-panel;
  border: 1px solid @border-color;
  border-top: none;
  border-radius: 0 0 18px 18px;
}

.chat-composer__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 12px;
}

.chat-composer__hint {
  color: @text-sub;
}

.chat-composer__error {
  color: @danger-color;
}

.chat-composer__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-composer__actions {
  display: flex;
  justify-content: flex-end;
}

.link-button {
  color: @primary-color;
}

.send-button {
  min-width: 128px;
  background: linear-gradient(135deg, #006eff, #00f2ff);
  border: none;
  box-shadow: 0 6px 18px fade(@primary-color, 24%);
}

.send-button:hover,
.send-button:focus {
  background: linear-gradient(135deg, #0a7aff, #23f5ff);
}

.chat-panel__empty,
.empty-conversation {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-panel,
.empty-conversation__card {
  width: min(520px, 100%);
  padding: 36px 32px;
  border: 1px solid @border-color;
  border-radius: 18px;
  background: @bg-panel;
  text-align: center;
  box-shadow: inset 0 0 24px rgba(0, 0, 0, 0.22);
}

.empty-panel__kicker,
.empty-conversation__kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: @text-sub;
}

.empty-panel h3,
.empty-conversation__card h4 {
  margin: 0 0 12px;
  color: #ffffff;
}

.empty-panel p,
.empty-conversation__card p {
  margin: 0;
  color: @text-sub;
  line-height: 1.7;
}

.chat-empty {
  margin-top: 12px;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

.chat-empty--compact {
  margin-top: 0;
}

::v-deep .chat-empty .el-empty__description p {
  color: @text-sub;
}

::v-deep .chat-input .el-input__inner,
::v-deep .chat-textarea .el-textarea__inner {
  background: rgba(0, 0, 0, 0.24);
  border: 1px solid @border-color;
  color: @text-main;
  border-radius: 12px;
}

::v-deep .chat-input .el-input__inner:focus,
::v-deep .chat-textarea .el-textarea__inner:focus {
  border-color: fade(@primary-color, 52%);
  box-shadow: 0 0 0 2px fade(@primary-color, 10%);
}

::v-deep .chat-input .el-input__icon {
  color: @text-sub;
}

@media (min-width: 961px) {
  .chat-view {
    height: var(--shell-available-height);
    min-height: var(--shell-available-height);
  }
}

@media (max-width: 1180px) {
  .chat-sidebar {
    width: 320px;
  }

  .text-ellipsis {
    max-width: 140px;
  }
}

@media (max-width: 960px) {
  .chat-view {
    flex-direction: column;
    min-height: 100%;
  }

  .chat-sidebar {
    width: 100%;
    max-height: 520px;
    border-right: none;
    border-bottom: 1px solid @border-color;
  }

  .contact-section {
    min-height: 280px;
  }

  .chat-panel {
    padding: 16px;
  }

  .chat-header,
  .chat-composer__toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .message-box {
    max-width: 84%;
  }
}

@page-bg: #f3f5f8;
@page-bg-soft: #edf1f5;
@card-bg-light: rgba(255, 255, 255, 0.88);
@card-bg-strong: rgba(255, 255, 255, 0.96);
@panel-muted: rgba(248, 250, 252, 0.92);
@apple-border: rgba(15, 23, 42, 0.08);
@apple-border-strong: rgba(15, 23, 42, 0.12);
@apple-text: #1f2937;
@apple-text-sub: #6b7280;
@apple-text-light: #94a3b8;
@apple-primary: #4f8ef7;
@apple-primary-soft: rgba(79, 142, 247, 0.12);
@apple-success: #34c759;
@apple-warning: #f59e0b;
@apple-danger: #ef4444;
@apple-shadow: 0 16px 42px rgba(15, 23, 42, 0.08);
@apple-card-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);

html[data-theme='light'] {
  .chat-view {
    gap: 20px;
    padding: 20px;
    color: @apple-text;
    background: linear-gradient(180deg, @page-bg 0%, @page-bg-soft 100%);
  }

  .chat-view__bg {
    background:
      radial-gradient(circle at top left, rgba(255, 255, 255, 0.86), transparent 30%),
      radial-gradient(circle at top right, rgba(79, 142, 247, 0.08), transparent 24%),
      radial-gradient(circle at bottom left, rgba(52, 199, 89, 0.08), transparent 20%);
  }

  .chat-sidebar {
    width: 300px;
    gap: 16px;
    padding: 22px 18px;
    background: @card-bg-light;
    border-right: none;
    border: 1px solid rgba(255, 255, 255, 0.74);
    border-radius: 28px;
    box-shadow: @apple-shadow;
    backdrop-filter: blur(22px);
  }

  .sidebar-title,
  .section-title,
  .chat-peer__name,
  .empty-panel h3,
  .empty-conversation__card h4 {
    color: @apple-text;
  }

  .sidebar-kicker,
  .section-kicker,
  .empty-panel__kicker,
  .empty-conversation__kicker {
    color: @apple-text-light;
  }

  .status-pill {
    background: rgba(255, 255, 255, 0.86);
    border: 1px solid @apple-border;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  }

  .status-pill.online {
    color: @apple-success;
    background: rgba(52, 199, 89, 0.1);
  }

  .status-pill.pending {
    color: @apple-warning;
    background: rgba(245, 158, 11, 0.12);
  }

  .status-pill.offline {
    color: @apple-danger;
    background: rgba(239, 68, 68, 0.1);
  }

  .summary-card {
    border-radius: 20px;
    background: @card-bg-strong;
    border: 1px solid @apple-border;
    box-shadow: @apple-card-shadow;
  }

  .summary-card__label {
    color: @apple-text-light;
  }

  .summary-card__value {
    color: @apple-text;
  }

  .sidebar-tabs {
    border-radius: 20px;
    background: rgba(226, 232, 240, 0.72);
    border: 1px solid rgba(255, 255, 255, 0.75);
  }

  .sidebar-tab {
    border-radius: 16px;
    color: @apple-text-sub;
  }

  .sidebar-tab:hover {
    color: @apple-text;
    background: transparent;
  }

  .sidebar-tab.active {
    background: @card-bg-strong;
    color: @apple-text;
    box-shadow: @apple-card-shadow;
  }

  .sidebar-tab__count,
  .sidebar-count {
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(15, 23, 42, 0.06);
    color: @apple-text-sub;
  }

  .sidebar-tab.active .sidebar-tab__count {
    background: @apple-primary-soft;
    color: @apple-primary;
  }

  .sidebar-panel {
    border-radius: 24px;
    background: @panel-muted;
    border: 1px solid rgba(255, 255, 255, 0.72);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
  }

  .sidebar-item {
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.84);
    box-shadow: 0 10px 26px rgba(15, 23, 42, 0.04);
    color: @apple-text;
  }

  .sidebar-item:hover {
    border-color: rgba(79, 142, 247, 0.18);
    box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  }

  .conversation-card.active {
    border-color: rgba(79, 142, 247, 0.18);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(238, 244, 255, 0.96));
    box-shadow: 0 16px 30px rgba(79, 142, 247, 0.12);
  }

  .online-user-card {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 246, 0.96));
  }

  .sidebar-item__name {
    color: @apple-text;
  }

  .sidebar-item__time {
    color: @apple-text-light;
  }

  .sidebar-item__preview {
    color: @apple-text-sub;
  }

  .online-tag {
    background: rgba(52, 199, 89, 0.12);
    color: @apple-success;
  }

  .contact-avatar {
    border: none;
    border-radius: 18px;
    background: linear-gradient(180deg, #dfe7ef, #c9d6e3);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
  }

  .contact-avatar.large {
    width: 56px;
    height: 56px;
    border-radius: 20px;
  }

  .contact-avatar.mini {
    border-radius: 14px;
  }

  .contact-avatar__status {
    width: 11px;
    height: 11px;
    border: 2px solid rgba(255, 255, 255, 0.98);
  }

  .contact-avatar__status.online {
    background: @apple-success;
    box-shadow: 0 0 0 0 rgba(52, 199, 89, 0.42);
    animation: onlinePulse 2.2s infinite;
  }

  .contact-avatar__status.offline {
    background: #cbd5e1;
  }

  .contact-name,
  .contact-time,
  .contact-preview {
    color: inherit;
  }

  .contact-unread {
    background: @apple-danger;
    box-shadow: 0 10px 18px rgba(239, 68, 68, 0.22);
  }

  .chat-panel {
    padding: 18px;
    background: @card-bg-light;
    border: 1px solid rgba(255, 255, 255, 0.75);
    border-radius: 30px;
    box-shadow: @apple-shadow;
    backdrop-filter: blur(24px);
  }

  .chat-header {
    padding: 18px 20px;
    background: @card-bg-strong;
    border: 1px solid @apple-border;
    border-radius: 24px 24px 0 0;
  }

  .chat-peer__status,
  .chat-composer__hint,
  .empty-panel p,
  .empty-conversation__card p {
    color: @apple-text-sub;
  }

  .chat-banner {
    background: rgba(245, 158, 11, 0.1);
    border-left: 1px solid @apple-border;
    border-right: 1px solid @apple-border;
    color: #b7791f;
  }

  .message-scroller {
    padding: 22px 20px 24px;
    background: linear-gradient(180deg, rgba(248, 250, 252, 0.78), rgba(243, 246, 249, 0.92));
    border-left: 1px solid @apple-border;
    border-right: 1px solid @apple-border;
  }

  .history-hint {
    color: @apple-primary;
  }

  .history-hint.muted,
  .message-meta {
    color: @apple-text-light;
  }

  .message-bubble {
    border-radius: 20px;
    box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
  }

  .message-bubble.other {
    background: rgba(255, 255, 255, 0.96);
    border: 1px solid rgba(15, 23, 42, 0.06);
    color: @apple-text;
  }

  .message-bubble.self {
    background: linear-gradient(180deg, #dfeeff, #cfe2ff);
    border: 1px solid rgba(79, 142, 247, 0.12);
    color: #1f3b77;
    box-shadow: 0 12px 24px rgba(79, 142, 247, 0.14);
  }

  .message-state.state-sending {
    color: @apple-warning;
  }

  .message-state.state-sent,
  .message-state.state-delivered {
    color: @apple-text-light;
  }

  .message-state.state-read {
    color: @apple-success;
  }

  .message-state.state-failed,
  .retry-button,
  .chat-composer__error {
    color: @apple-danger;
  }

  .chat-composer {
    background: @card-bg-strong;
    border: 1px solid @apple-border;
    border-top: none;
    border-radius: 0 0 24px 24px;
  }

  .link-button {
    color: @apple-primary;
  }

  .send-button {
    min-width: 128px;
    background: linear-gradient(180deg, #6aa7ff, #4f8ef7);
    border: none;
    border-radius: 16px;
    box-shadow: 0 14px 26px rgba(79, 142, 247, 0.22);
  }

  .send-button:hover,
  .send-button:focus {
    background: linear-gradient(180deg, #79b1ff, #5b97fb);
  }

  .empty-panel,
  .empty-conversation__card,
  .chat-empty {
    border: 1px solid @apple-border;
    border-radius: 26px;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: @apple-card-shadow;
  }

  .chat-empty {
    min-height: 220px;
  }

  ::v-deep .chat-empty .el-empty__description p {
    color: @apple-text-sub;
  }

  ::v-deep .chat-input .el-input__inner,
  ::v-deep .chat-textarea .el-textarea__inner {
    background: rgba(255, 255, 255, 0.88);
    border: 1px solid rgba(15, 23, 42, 0.08);
    border-radius: 18px;
    color: @apple-text;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
  }

  ::v-deep .chat-input .el-input__inner {
    height: 42px;
  }

  ::v-deep .chat-input .el-input__inner:focus,
  ::v-deep .chat-textarea .el-textarea__inner:focus {
    border-color: rgba(79, 142, 247, 0.34);
    box-shadow: 0 0 0 4px rgba(79, 142, 247, 0.1);
  }

  ::v-deep .chat-input .el-input__icon {
    color: @apple-text-light;
  }

  @media (max-width: 1180px) {
    .chat-view {
      gap: 16px;
      padding: 16px;
    }

    .chat-sidebar {
      width: 300px;
    }
  }

  @media (max-width: 960px) {
    .chat-sidebar {
      width: 100%;
      max-height: none;
      border-bottom: none;
    }

    .chat-panel {
      padding: 14px;
    }

    .chat-header,
    .chat-composer__toolbar {
      flex-direction: column;
      align-items: flex-start;
    }

    .message-box {
      max-width: 86%;
    }
  }
}

@keyframes onlinePulse {
  0% {
    transform: scale(0.96);
    box-shadow: 0 0 0 0 rgba(52, 199, 89, 0.45);
  }

  55% {
    transform: scale(1);
    box-shadow: 0 0 0 8px rgba(52, 199, 89, 0);
  }

  100% {
    transform: scale(0.96);
    box-shadow: 0 0 0 0 rgba(52, 199, 89, 0);
  }
}

</style>
