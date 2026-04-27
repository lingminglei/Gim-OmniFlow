const HISTORY_PAGE_SIZE = 20
const REQUEST_TIMEOUT = 12000
const RECONNECT_BASE_DELAY = 2000
const RECONNECT_MAX_DELAY = 30000

/**
 * 解析站内信专用 WebSocket 地址。
 * 优先读取环境变量；未配置时默认走独立的 `/chat/ws`，避免与旧版 `/ws` 冲突。
 */
function resolveWsRoot() {
  if (process.env.VUE_APP_CHAT_WS_URL) {
    return process.env.VUE_APP_CHAT_WS_URL
  }

  if (typeof window === 'undefined') {
    return 'ws://127.0.0.1:9999/chat/ws'
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname || '127.0.0.1'
  return `${protocol}//${host}:9999/chat/ws`
}

const WS_ROOT = resolveWsRoot()

/**
 * 创建站内信模块的默认状态快照。
 * profile 用于断线重连时再次发送 `session.init`。
 */
function createState() {
  return {
    userId: '',
    profile: {
      user_name: '',
      avatar_url: '',
      is_logged_in: false
    },
    wsUrl: WS_ROOT,
    connected: false,
    connecting: false,
    reconnecting: false,
    sessionReady: false,
    lastError: '',
    contacts: [],
    onlineUsers: [],
    messagesByPeer: {},
    historyByPeer: {},
    activePeerId: ''
  }
}

/**
 * 深拷贝状态快照，避免组件层直接修改服务内部状态。
 */
function deepClone(value) {
  return JSON.parse(JSON.stringify(value))
}

/**
 * 统一当前登录用户的展示资料结构。
 * 这里的数据会在 `session.init` 时上报给后端，并写入 Redis 用户快照。
 */
function createSessionProfile(userId, profile = {}) {
  const normalizedUserId = String(userId || '').trim()
  return {
    user_id: normalizedUserId,
    user_name: profile.user_name || profile.userName || profile.user_name || normalizedUserId,
    avatar_url: profile.avatar_url || profile.avatarUrl || profile.avatar || '',
    is_logged_in: true
  }
}

/**
 * 统一联系人结构，兼容后端字段命名差异。
 */
function normalizeContact(raw = {}) {
  const userId = String(raw.user_id || raw.userId || raw.UserID || '')
  return {
    user_id: userId,
    user_name: raw.user_name || raw.userName || raw.UserName || userId,
    avatar_url: raw.avatar_url || raw.avatarUrl || raw.AvatarURL || '',
    online: Boolean(raw.online !== undefined ? raw.online : raw.status === 'online'),
    is_logged_in: Boolean(raw.is_logged_in !== undefined ? raw.is_logged_in : raw.isLoggedIn !== undefined ? raw.isLoggedIn : raw.online),
    connected_at: raw.connected_at || raw.connectedAt || raw.ConnectedAt || null,
    last_seen_at: raw.last_seen_at || raw.lastSeenAt || raw.LastSeenAt || raw.last_online_time || null,
    last_message: raw.last_message || raw.lastMessage || '',
    last_msg_type: raw.last_msg_type || raw.lastMsgType || raw.lastMessageType || 'text',
    last_timestamp: raw.last_timestamp || raw.lastTimestamp || raw.LastTimestamp || null,
    unread_count: Number(raw.unread_count || raw.unreadCount || 0)
  }
}

/**
 * 统一消息结构，确保视图层只处理一种消息格式。
 */
function normalizeMessage(raw = {}) {
  const senderId = String(raw.sender_id || raw.senderId || '')
  const receiverId = String(raw.receiver_id || raw.receiverId || '')
  return {
    id: raw.id || raw.messageId || raw.client_id || raw.clientId || `msg-${Date.now()}`,
    client_id: raw.client_id || raw.clientId || '',
    sender_id: senderId,
    receiver_id: receiverId,
    content: raw.content || '',
    msg_type: raw.msg_type || raw.msgType || 'text',
    timestamp: raw.timestamp || raw.sendTime || new Date().toISOString(),
    status: raw.status || raw.readStatus || 'sent'
  }
}

/**
 * 统一计算联系人排序时间，优先显示最近消息，其次使用连接时间和最后活跃时间。
 */
function resolveContactSortTime(contact) {
  return new Date(contact.last_timestamp || contact.connected_at || contact.last_seen_at || 0).getTime()
}

/**
 * 联系人按在线状态、最近时间、名称统一排序。
 */
function sortContacts(contacts) {
  return [...contacts].sort((left, right) => {
    if (left.online !== right.online) {
      return left.online ? -1 : 1
    }

    const leftTime = resolveContactSortTime(left)
    const rightTime = resolveContactSortTime(right)
    if (leftTime !== rightTime) {
      return rightTime - leftTime
    }

    return String(left.user_name || '').localeCompare(String(right.user_name || ''))
  })
}

class ChatService {
  constructor() {
    this.state = createState()
    this.listeners = new Set()
    this.socket = null
    this.manualClose = false
    this.reconnectAttempts = 0
    this.reconnectTimer = null
    this.pendingRequests = new Map()
  }

  /**
   * 订阅状态变化。
   */
  subscribe(listener) {
    this.listeners.add(listener)
    listener(this.getState())
    return () => {
      this.listeners.delete(listener)
    }
  }

  /**
   * 返回只读快照供组件层消费。
   */
  getState() {
    return deepClone(this.state)
  }

  /**
   * 建立站内信专用 WebSocket 连接。
   * 前端只需要传入 userId 和展示资料，不与 AI 助手共享连接。
   */
  connect(userId, profile = {}) {
    const normalizedUserId = String(userId || '').trim()
    if (!normalizedUserId) {
      return
    }

    const normalizedProfile = createSessionProfile(normalizedUserId, profile)
    if (this.state.userId && this.state.userId !== normalizedUserId) {
      this.disconnect({ clearState: true })
    }

    this.state.userId = normalizedUserId
    this.state.profile = normalizedProfile

    if (this.socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(this.socket.readyState)) {
      if (this.state.connected && this.state.sessionReady) {
        this.sendSessionInit()
      } else {
        this.emit()
      }
      return
    }

    this.manualClose = false
    this.state.connected = false
    this.state.connecting = true
    this.state.reconnecting = false
    this.state.sessionReady = false
    this.state.lastError = ''
    this.emit()

    const socketUrl = `${WS_ROOT}?userId=${encodeURIComponent(normalizedUserId)}`
    this.socket = new WebSocket(socketUrl)

    this.socket.onopen = () => {
      this.state.connected = true
      this.state.connecting = false
      this.state.reconnecting = false
      this.state.sessionReady = false
      this.state.lastError = ''
      this.reconnectAttempts = 0
      this.emit()

      this.sendSessionInit()
    }

    this.socket.onmessage = (event) => {
      this.handleIncoming(event.data)
    }

    this.socket.onerror = () => {
      this.state.lastError = '站内信连接异常，请稍后重试'
      this.emit()
    }

    this.socket.onclose = () => {
      this.socket = null
      this.state.connected = false
      this.state.connecting = false
      this.state.sessionReady = false

      if (!this.manualClose && this.state.userId) {
        this.failPendingRequests('连接已断开，请稍后重试')
        this.state.reconnecting = true
        this.scheduleReconnect()
      } else {
        this.clearPendingRequests()
        this.state.reconnecting = false
      }

      this.emit()
    }
  }

  /**
   * 主动断开站内信连接。
   */
  disconnect(options = {}) {
    this.manualClose = true
    if (typeof window !== 'undefined') {
      window.clearTimeout(this.reconnectTimer)
    }
    this.reconnectTimer = null
    this.clearPendingRequests()

    if (this.socket) {
      this.socket.close()
      this.socket = null
    }

    this.state.connected = false
    this.state.connecting = false
    this.state.reconnecting = false
    this.state.sessionReady = false

    if (options.clearState) {
      this.resetRuntimeState()
      return
    }

    this.emit()
  }

  /**
   * 设置当前激活会话，并在需要时自动加载历史消息。
   */
  setActiveConversation(peerId) {
    const normalizedPeerId = String(peerId || '').trim()
    this.state.activePeerId = normalizedPeerId
    if (!normalizedPeerId) {
      this.emit()
      return
    }

    this.ensureConversationContact(normalizedPeerId)
    const historyState = this.ensureHistoryState(normalizedPeerId)
    const currentMessages = this.state.messagesByPeer[normalizedPeerId] || []
    if (!currentMessages.length || historyState.next_offset === 0) {
      this.loadHistory(normalizedPeerId, { reset: true })
    }

    const contact = this.findContact(normalizedPeerId)
    if (contact && contact.unread_count > 0) {
      this.markConversationRead(normalizedPeerId)
      return
    }

    this.emit()
  }

  /**
   * 主动刷新会话列表。
   */
  requestConversationList() {
    try {
      this.sendCommand('conversation.list', {})
    } catch (error) {
      this.state.lastError = error.message
      this.emit()
    }
  }

  /**
   * 拉取当前在线的 WebSocket 用户列表，用于联通测试。
   */
  requestOnlineUsers() {
    try {
      this.sendCommand('online.users.list', {})
    } catch (error) {
      this.state.lastError = error.message
      this.emit()
    }
  }

  /**
   * 加载指定会话的历史消息，支持滚动分页。
   */
  loadHistory(peerId, options = {}) {
    const normalizedPeerId = String(peerId || '').trim()
    if (!normalizedPeerId) {
      return
    }

    const historyState = this.ensureHistoryState(normalizedPeerId)
    const shouldReset = Boolean(options.reset)
    if (historyState.loading) {
      return
    }
    if (!shouldReset && historyState.has_more === false) {
      return
    }

    if (shouldReset) {
      historyState.next_offset = 0
      historyState.has_more = true
    }

    historyState.loading = true
    this.emit()

    try {
      const requestId = this.sendCommand('conversation.history', {
        peer_id: normalizedPeerId,
        offset: shouldReset ? 0 : historyState.next_offset,
        limit: HISTORY_PAGE_SIZE
      })

      this.trackPendingRequest(requestId, {
        type: 'history',
        peerId: normalizedPeerId,
        reset: shouldReset
      })
    } catch (error) {
      historyState.loading = false
      this.state.lastError = error.message
      this.emit()
    }
  }

  /**
   * 标记当前会话已读，并同步本地未读状态。
   */
  markConversationRead(peerId) {
    const normalizedPeerId = String(peerId || '').trim()
    if (!normalizedPeerId) {
      return
    }

    const contact = this.findContact(normalizedPeerId)
    if (contact) {
      contact.unread_count = 0
    }

    this.state.messagesByPeer[normalizedPeerId] = (this.state.messagesByPeer[normalizedPeerId] || []).map((message) => {
      if (message.sender_id === normalizedPeerId && message.receiver_id === this.state.userId) {
        return { ...message, status: 'read' }
      }
      return message
    })
    this.emit()

    try {
      this.sendCommand('conversation.read', {
        peer_id: normalizedPeerId
      })
    } catch (error) {
      this.state.lastError = error.message
      this.emit()
    }
  }

  /**
   * 发送一条文本消息。
   * 先插入本地“发送中”消息，待服务端确认后再替换状态。
   */
  sendMessage({ peerId, content, msgType = 'text', clientId = '' }) {
    const normalizedPeerId = String(peerId || '').trim()
    const messageContent = String(content || '').trim()

    if (!normalizedPeerId) {
      throw new Error('请先选择联系人')
    }
    if (!messageContent) {
      throw new Error('消息内容不能为空')
    }

    this.ensureConversationContact(normalizedPeerId)
    const requestClientId = clientId || `client-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
    const optimisticMessage = normalizeMessage({
      id: requestClientId,
      client_id: requestClientId,
      sender_id: this.state.userId,
      receiver_id: normalizedPeerId,
      content: messageContent,
      msg_type: msgType,
      timestamp: new Date().toISOString(),
      status: 'sending'
    })

    this.upsertMessage(normalizedPeerId, optimisticMessage)
    this.touchConversation(normalizedPeerId, optimisticMessage, false)
    this.emit()

    try {
      const requestId = this.sendCommand('message.send', {
        client_id: requestClientId,
        receiver_id: normalizedPeerId,
        content: messageContent,
        msg_type: msgType
      })

      this.trackPendingRequest(requestId, {
        type: 'message',
        peerId: normalizedPeerId,
        clientId: requestClientId
      })

      return requestClientId
    } catch (error) {
      this.markMessageStatus(normalizedPeerId, requestClientId, 'failed')
      this.state.lastError = error.message
      this.emit()
      throw error
    }
  }

  /**
   * 基于失败消息重新发送，保持原消息气泡复用。
   */
  retryMessage(peerId, message) {
    const retryClientId = message.client_id || message.id
    return this.sendMessage({
      peerId,
      content: message.content,
      msgType: message.msg_type || 'text',
      clientId: retryClientId
    })
  }

  /**
   * 统一处理服务端推送事件。
   */
  handleIncoming(rawPayload) {
    let payload
    try {
      payload = JSON.parse(rawPayload)
    } catch (error) {
      this.state.lastError = '收到无法解析的站内信数据'
      this.emit()
      return
    }

    const { type, request_id: requestId, data, error } = payload
    if (type === 'error') {
      this.handleError(requestId, error)
      return
    }

    switch (type) {
      case 'session.ready':
        this.applySessionReady(data || {})
        this.requestConversationList()
        this.requestOnlineUsers()
        if (this.state.activePeerId) {
          this.loadHistory(this.state.activePeerId, { reset: true })
        }
        break
      case 'conversation.list':
        this.applyConversationList(data && data.conversations ? data.conversations : [])
        break
      case 'online.users.list':
        this.applyOnlineUsers(data && data.users ? data.users : [])
        break
      case 'conversation.history':
        this.applyHistory(data || {}, requestId)
        break
      case 'message.sent':
        this.resolvePendingMessage(requestId, data || {})
        break
      case 'message.new':
        this.receiveMessage((data && data.message) || data || {})
        break
      case 'conversation.read':
        this.applyConversationRead(data || {})
        break
      case 'presence.changed':
        this.applyPresence(data || {})
        break
      case 'pong':
      default:
        break
    }
  }

  /**
   * 处理服务端错误事件，并回滚对应请求状态。
   */
  handleError(requestId, errorPayload = {}) {
    const meta = this.clearPendingRequest(requestId)
    if (meta && meta.type === 'message') {
      this.markMessageStatus(meta.peerId, meta.clientId, 'failed')
    }
    if (meta && meta.type === 'history') {
      const historyState = this.ensureHistoryState(meta.peerId)
      historyState.loading = false
    }

    this.state.lastError = errorPayload.message || '请求处理失败'
    this.emit()
  }

  /**
   * 处理 `session.ready`，同步当前连接的资料快照。
   */
  applySessionReady(payload) {
    const normalizedProfile = normalizeContact({
      user_id: payload.user_id || this.state.userId,
      user_name: payload.user_name || this.state.profile.user_name || this.state.userId,
      avatar_url: payload.avatar_url || this.state.profile.avatar_url || '',
      online: payload.online !== undefined ? payload.online : true,
      is_logged_in: payload.is_logged_in !== undefined ? payload.is_logged_in : true
    })

    this.state.profile = {
      user_name: normalizedProfile.user_name,
      avatar_url: normalizedProfile.avatar_url,
      is_logged_in: normalizedProfile.is_logged_in
    }
    this.state.sessionReady = true
    this.state.lastError = ''
    this.emit()
  }

  /**
   * 用服务端会话列表刷新本地联系人状态。
   */
  applyConversationList(serverContacts) {
    const contactMap = new Map((this.state.contacts || []).map((contact) => [contact.user_id, { ...contact }]))

    serverContacts.forEach((rawContact) => {
      const normalized = normalizeContact(rawContact)
      const current = contactMap.get(normalized.user_id) || {}
      contactMap.set(normalized.user_id, {
        ...current,
        ...normalized
      })
    })

    const onlineMap = new Map((this.state.onlineUsers || []).map((contact) => [contact.user_id, contact]))
    this.state.contacts = sortContacts(Array.from(contactMap.values()).map((contact) => {
      const onlineContact = onlineMap.get(contact.user_id)
      if (!onlineContact) {
        return contact
      }
      return {
        ...contact,
        online: true,
        is_logged_in: onlineContact.is_logged_in,
        connected_at: onlineContact.connected_at || contact.connected_at,
        last_seen_at: onlineContact.last_seen_at || contact.last_seen_at
      }
    }))
    this.emit()
  }

  /**
   * 用服务端在线用户列表刷新本地测试面板和联系人在线状态。
   */
  applyOnlineUsers(serverUsers) {
    const nextOnlineUsers = sortContacts(serverUsers.map((user) => ({
      ...normalizeContact(user),
      online: true,
      is_logged_in: true
    })))

    this.state.onlineUsers = nextOnlineUsers
    const onlineMap = new Map(nextOnlineUsers.map((user) => [user.user_id, user]))
    this.state.contacts = sortContacts((this.state.contacts || []).map((contact) => {
      const onlineContact = onlineMap.get(contact.user_id)
      if (!onlineContact) {
        return {
          ...contact,
          online: false,
          is_logged_in: false
        }
      }

      return {
        ...contact,
        ...onlineContact,
        unread_count: contact.unread_count
      }
    }))
    this.emit()
  }

  /**
   * 合并历史消息，并更新分页游标。
   */
  applyHistory(payload, requestId) {
    const peerId = payload.peer_id
    if (!peerId) {
      return
    }

    const meta = this.clearPendingRequest(requestId)
    const historyState = this.ensureHistoryState(peerId)
    const incomingMessages = Array.isArray(payload.messages)
      ? payload.messages.map((message) => normalizeMessage(message))
      : []

    const existingMessages = this.state.messagesByPeer[peerId] || []
    this.state.messagesByPeer[peerId] = meta && meta.reset
      ? this.mergeUniqueMessages([], incomingMessages)
      : this.mergeUniqueMessages(incomingMessages, existingMessages)

    historyState.loading = false
    historyState.next_offset = Number(payload.next_offset || 0)
    historyState.has_more = Boolean(payload.has_more)

    if (peerId === this.state.activePeerId) {
      const contact = this.findContact(peerId)
      if (contact && contact.unread_count > 0) {
        this.markConversationRead(peerId)
        return
      }
    }

    this.emit()
  }

  /**
   * 处理消息发送确认，把“发送中”消息替换为正式消息。
   */
  resolvePendingMessage(requestId, payload) {
    const meta = this.clearPendingRequest(requestId)
    const persistedMessage = normalizeMessage((payload && payload.message) || payload)
    const peerId = (meta && meta.peerId) || persistedMessage.receiver_id || persistedMessage.sender_id
    const clientId = payload.client_id || payload.clientId || (meta && meta.clientId) || persistedMessage.client_id

    if (clientId) {
      this.replaceMessage(peerId, clientId, {
        ...persistedMessage,
        status: 'sent'
      })
    } else {
      this.upsertMessage(peerId, {
        ...persistedMessage,
        status: 'sent'
      })
    }

    this.touchConversation(peerId, persistedMessage, false)
    this.emit()
  }

  /**
   * 处理对端实时推送的新消息。
   */
  receiveMessage(rawMessage) {
    const message = normalizeMessage(rawMessage)
    const peerId = message.sender_id === this.state.userId ? message.receiver_id : message.sender_id
    const isFromCurrentUser = message.sender_id === this.state.userId
    const isActiveConversation = peerId === this.state.activePeerId

    this.ensureConversationContact(peerId)
    this.upsertMessage(peerId, {
      ...message,
      status: isActiveConversation && !isFromCurrentUser ? 'read' : (message.status || 'sent')
    })
    this.touchConversation(peerId, message, !isActiveConversation && !isFromCurrentUser)

    if (isActiveConversation && !isFromCurrentUser) {
      this.markConversationRead(peerId)
      return
    }

    this.emit()
  }

  /**
   * 处理已读回执，分别兼容“自己已读对方消息”和“对方已读自己的消息”两种场景。
   */
  applyConversationRead(payload) {
    const peerId = payload.peer_id || payload.peerId
    const readerId = payload.reader_id || payload.readerId
    if (!peerId) {
      return
    }

    const isCurrentUserReader = String(readerId || '') === String(this.state.userId)
    const contact = this.findContact(peerId)
    if (contact && isCurrentUserReader) {
      contact.unread_count = 0
    }

    this.state.messagesByPeer[peerId] = (this.state.messagesByPeer[peerId] || []).map((message) => {
      if (isCurrentUserReader) {
        if (message.sender_id === peerId && message.receiver_id === this.state.userId) {
          return { ...message, status: 'read' }
        }
        return message
      }

      if (message.sender_id === this.state.userId && message.receiver_id === peerId) {
        return { ...message, status: 'read' }
      }
      return message
    })

    this.emit()
  }

  /**
   * 处理在线状态广播，保持在线测试列表和会话列表同步。
   */
  applyPresence(payload) {
    const userId = String(payload.user_id || payload.userId || '')
    if (!userId || userId === this.state.userId) {
      return
    }

    const online = Boolean(payload.online)
    const isLoggedIn = payload.is_logged_in !== undefined ? Boolean(payload.is_logged_in) : online
    const lastSeenAt = payload.last_seen_at || payload.lastSeenAt || new Date().toISOString()
    const userName = payload.user_name || payload.userName || userId
    const avatarUrl = payload.avatar_url || payload.avatarUrl || ''
    const contact = this.findContact(userId)

    if (contact) {
      contact.user_name = contact.user_name || userName
      contact.avatar_url = contact.avatar_url || avatarUrl
      contact.online = online
      contact.is_logged_in = isLoggedIn
      contact.last_seen_at = lastSeenAt
    }

    const onlineIndex = this.state.onlineUsers.findIndex((user) => user.user_id === userId)
    if (online) {
      const baseContact = contact || this.findOnlineUser(userId) || normalizeContact({
        user_id: userId,
        user_name: userName,
        avatar_url: avatarUrl,
        online: true,
        is_logged_in: true,
        last_seen_at: lastSeenAt
      })
      const nextOnlineUser = {
        ...baseContact,
        user_name: userName || baseContact.user_name,
        avatar_url: avatarUrl || baseContact.avatar_url,
        online: true,
        is_logged_in: isLoggedIn,
        last_seen_at: lastSeenAt
      }

      if (onlineIndex === -1) {
        this.state.onlineUsers = sortContacts([...this.state.onlineUsers, nextOnlineUser])
      } else {
        const nextOnlineUsers = [...this.state.onlineUsers]
        nextOnlineUsers.splice(onlineIndex, 1, nextOnlineUser)
        this.state.onlineUsers = sortContacts(nextOnlineUsers)
      }

      // 如果当前只有占位名称，则再主动拉一次完整在线用户列表补齐头像和昵称。
      if (!contact || nextOnlineUser.user_name === userId) {
        this.requestOnlineUsers()
      }
    } else if (onlineIndex !== -1) {
      this.state.onlineUsers = this.state.onlineUsers.filter((user) => user.user_id !== userId)
    }

    this.state.contacts = sortContacts((this.state.contacts || []).map((item) => {
      if (item.user_id !== userId) {
        return item
      }
      return {
        ...item,
        user_name: item.user_name || userName,
        avatar_url: item.avatar_url || avatarUrl,
        online,
        is_logged_in: isLoggedIn,
        last_seen_at: lastSeenAt
      }
    }))
    this.emit()
  }

  /**
   * 发送 `session.init`，把当前用户展示资料同步到后端 Redis。
   */
  sendSessionInit() {
    try {
      this.sendCommand('session.init', {
        user_id: this.state.userId,
        user_name: this.state.profile.user_name,
        avatar_url: this.state.profile.avatar_url
      }, { allowBeforeReady: true })
    } catch (error) {
      this.state.lastError = error.message
      this.emit()
    }
  }

  /**
   * 统一发送命令，并生成 request_id 便于回包匹配。
   */
  sendCommand(type, data, options = {}) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error('站内信服务尚未连接')
    }

    if (!options.allowBeforeReady && !this.state.sessionReady) {
      throw new Error('站内信会话尚未初始化')
    }

    const requestId = `req-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
    this.socket.send(JSON.stringify({
      type,
      request_id: requestId,
      data
    }))

    return requestId
  }

  /**
   * 记录待响应请求，超时后自动回滚状态。
   */
  trackPendingRequest(requestId, meta) {
    if (typeof window === 'undefined') {
      return
    }

    const timer = window.setTimeout(() => {
      const requestMeta = this.clearPendingRequest(requestId)
      if (!requestMeta) {
        return
      }

      if (requestMeta.type === 'message') {
        this.markMessageStatus(requestMeta.peerId, requestMeta.clientId, 'failed')
      }
      if (requestMeta.type === 'history') {
        const historyState = this.ensureHistoryState(requestMeta.peerId)
        historyState.loading = false
      }

      this.state.lastError = '请求超时，请稍后重试'
      this.emit()
    }, REQUEST_TIMEOUT)

    this.pendingRequests.set(requestId, {
      ...meta,
      timer
    })
  }

  /**
   * 清理指定待响应请求。
   */
  clearPendingRequest(requestId) {
    if (!requestId || !this.pendingRequests.has(requestId)) {
      return null
    }

    const meta = this.pendingRequests.get(requestId)
    if (typeof window !== 'undefined') {
      window.clearTimeout(meta.timer)
    }
    this.pendingRequests.delete(requestId)
    return meta
  }

  /**
   * 清理全部待响应请求。
   */
  clearPendingRequests() {
    this.pendingRequests.forEach((meta) => {
      if (typeof window !== 'undefined') {
        window.clearTimeout(meta.timer)
      }
    })
    this.pendingRequests.clear()
  }

  /**
   * 在断线时统一回滚挂起中的消息与历史请求。
   */
  failPendingRequests(reason) {
    this.pendingRequests.forEach((meta) => {
      if (meta.type === 'message') {
        this.markMessageStatus(meta.peerId, meta.clientId, 'failed')
      }
      if (meta.type === 'history') {
        const historyState = this.ensureHistoryState(meta.peerId)
        historyState.loading = false
      }
      if (typeof window !== 'undefined') {
        window.clearTimeout(meta.timer)
      }
    })

    this.pendingRequests.clear()
    if (reason) {
      this.state.lastError = reason
    }
  }

  /**
   * 按退避策略自动重连。
   */
  scheduleReconnect() {
    if (typeof window === 'undefined') {
      return
    }

    window.clearTimeout(this.reconnectTimer)
    const delay = Math.min(RECONNECT_MAX_DELAY, RECONNECT_BASE_DELAY * Math.max(1, this.reconnectAttempts + 1))
    const nextUserId = this.state.userId
    const nextProfile = { ...this.state.profile }
    this.reconnectAttempts += 1

    this.reconnectTimer = window.setTimeout(() => {
      this.connect(nextUserId, nextProfile)
    }, delay)
  }

  /**
   * 获取指定会话的分页状态，不存在时自动初始化。
   */
  ensureHistoryState(peerId) {
    if (!this.state.historyByPeer[peerId]) {
      this.state.historyByPeer[peerId] = {
        loading: false,
        has_more: true,
        next_offset: 0
      }
    }
    return this.state.historyByPeer[peerId]
  }

  /**
   * 确保当前会话对应联系人已存在，便于从在线用户测试列表直接发起聊天。
   */
  ensureConversationContact(peerId) {
    let contact = this.findContact(peerId)
    if (contact) {
      return contact
    }

    const onlineContact = this.findOnlineUser(peerId)
    contact = normalizeContact(onlineContact || {
      user_id: peerId,
      user_name: peerId
    })
    this.state.contacts = sortContacts([...this.state.contacts, contact])
    return contact
  }

  /**
   * 查找联系人快照。
   */
  findContact(peerId) {
    return (this.state.contacts || []).find((contact) => contact.user_id === peerId)
  }

  /**
   * 查找在线用户快照。
   */
  findOnlineUser(peerId) {
    return (this.state.onlineUsers || []).find((user) => user.user_id === peerId)
  }

  /**
   * 插入或更新消息。
   */
  upsertMessage(peerId, message) {
    const messages = this.state.messagesByPeer[peerId] || []
    const index = messages.findIndex((item) => item.id === message.id || (message.client_id && item.client_id === message.client_id))
    if (index === -1) {
      this.state.messagesByPeer[peerId] = this.mergeUniqueMessages(messages, [message])
      return
    }

    messages.splice(index, 1, {
      ...messages[index],
      ...message
    })
    this.state.messagesByPeer[peerId] = [...messages].sort((left, right) => new Date(left.timestamp) - new Date(right.timestamp))
  }

  /**
   * 用服务端正式消息替换本地临时消息。
   */
  replaceMessage(peerId, clientId, nextMessage) {
    const messages = this.state.messagesByPeer[peerId] || []
    const index = messages.findIndex((item) => item.client_id === clientId || item.id === clientId)
    if (index === -1) {
      this.upsertMessage(peerId, nextMessage)
      return
    }

    messages.splice(index, 1, {
      ...messages[index],
      ...nextMessage
    })
    this.state.messagesByPeer[peerId] = [...messages].sort((left, right) => new Date(left.timestamp) - new Date(right.timestamp))
  }

  /**
   * 更新消息发送状态，如 sending / sent / failed / read。
   */
  markMessageStatus(peerId, clientId, status) {
    const messages = this.state.messagesByPeer[peerId] || []
    const index = messages.findIndex((item) => item.client_id === clientId || item.id === clientId)
    if (index === -1) {
      return
    }

    messages.splice(index, 1, {
      ...messages[index],
      status
    })
    this.state.messagesByPeer[peerId] = [...messages]
  }

  /**
   * 更新会话摘要，保持联系人列表排序和未读状态正确。
   */
  touchConversation(peerId, message, increaseUnread) {
    const contact = this.ensureConversationContact(peerId)
    contact.last_message = message.msg_type === 'text' ? message.content : '[非文本消息]'
    contact.last_msg_type = message.msg_type
    contact.last_timestamp = message.timestamp

    if (increaseUnread) {
      contact.unread_count = Number(contact.unread_count || 0) + 1
    }

    this.state.contacts = sortContacts([...this.state.contacts])
  }

  /**
   * 合并消息列表并按时间排序去重。
   */
  mergeUniqueMessages(primaryMessages, secondaryMessages) {
    const mergedMap = new Map()
    ;[...primaryMessages, ...secondaryMessages].forEach((message) => {
      const normalized = normalizeMessage(message)
      const key = normalized.id || normalized.client_id
      if (!key) {
        return
      }
      mergedMap.set(key, normalized)
    })

    return Array.from(mergedMap.values()).sort((left, right) => new Date(left.timestamp) - new Date(right.timestamp))
  }

  /**
   * 重置运行时状态。
   */
  resetRuntimeState() {
    this.clearPendingRequests()
    if (typeof window !== 'undefined') {
      window.clearTimeout(this.reconnectTimer)
    }
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.socket = null
    this.manualClose = false
    this.state = createState()
    this.emit()
  }

  /**
   * 广播最新状态给订阅组件。
   */
  emit() {
    const snapshot = this.getState()
    this.listeners.forEach((listener) => {
      listener(snapshot)
    })
  }
}

const chatService = new ChatService()

export default chatService
