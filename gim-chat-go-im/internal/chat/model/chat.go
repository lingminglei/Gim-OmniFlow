package model

import (
	"encoding/json"
	"time"
)

const (
	// CommandSessionInit 用于建立连接后同步当前用户的展示资料。
	CommandSessionInit = "session.init"
	// CommandPing 用于客户端主动保活。
	CommandPing = "ping"
	// CommandConversationList 用于拉取会话列表。
	CommandConversationList = "conversation.list"
	// CommandConversationHistory 用于分页拉取会话历史。
	CommandConversationHistory = "conversation.history"
	// CommandConversationRead 用于将某个会话标记为已读。
	CommandConversationRead = "conversation.read"
	// CommandMessageSend 用于发送一条消息。
	CommandMessageSend = "message.send"
	// CommandOnlineUsersList 用于拉取当前在线用户列表。
	CommandOnlineUsersList = "online.users.list"
)

const (
	// EventSessionReady 表示当前连接已经完成站内信会话初始化。
	EventSessionReady = "session.ready"
	// EventPong 是对 ping 的响应。
	EventPong = "pong"
	// EventConversationList 推送会话列表。
	EventConversationList = "conversation.list"
	// EventConversationHistory 推送会话历史。
	EventConversationHistory = "conversation.history"
	// EventConversationRead 推送已读回执。
	EventConversationRead = "conversation.read"
	// EventMessageNew 推送新的站内信消息。
	EventMessageNew = "message.new"
	// EventMessageSent 推送发送确认。
	EventMessageSent = "message.sent"
	// EventPresenceChanged 推送在线状态变化。
	EventPresenceChanged = "presence.changed"
	// EventOnlineUsersList 推送在线用户列表。
	EventOnlineUsersList = "online.users.list"
	// EventError 推送统一错误格式。
	EventError = "error"
	// DefaultHistoryPageSize 是默认历史消息分页大小。
	DefaultHistoryPageSize = 20
	// MaxHistoryPageSize 是单次历史拉取的最大条数。
	MaxHistoryPageSize = 50
	// MaxConversationMessages 是 Redis 中每个会话保留的最大消息数。
	MaxConversationMessages = 200
	// DefaultMessageType 是默认消息类型。
	DefaultMessageType = "text"
	// MessageStatusSending 表示消息正在发送中。
	MessageStatusSending = "sending"
	// MessageStatusSent 表示消息已被服务端接受。
	MessageStatusSent = "sent"
	// MessageStatusDelivered 预留给后续送达态扩展。
	MessageStatusDelivered = "delivered"
	// MessageStatusRead 表示消息已被对端读取。
	MessageStatusRead = "read"
	// MessageStatusFailed 表示消息发送失败。
	MessageStatusFailed = "failed"
)

// ClientCommand 表示浏览器发往站内信服务端的统一命令格式。
type ClientCommand struct {
	Type      string          `json:"type"`
	RequestID string          `json:"request_id,omitempty"`
	Data      json.RawMessage `json:"data,omitempty"`
}

// ServerEvent 表示服务端发往浏览器的统一事件格式。
type ServerEvent struct {
	Type      string        `json:"type"`
	RequestID string        `json:"request_id,omitempty"`
	Data      interface{}   `json:"data,omitempty"`
	Error     *ErrorPayload `json:"error,omitempty"`
}

// ErrorPayload 是站内信统一错误结构。
type ErrorPayload struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

// ChatMessage 是站内信消息体。
// 这里固定保留 sender_id、receiver_id、content、msg_type、timestamp 五个核心字段。
type ChatMessage struct {
	ID         string    `json:"id"`
	ClientID   string    `json:"client_id,omitempty"`
	SenderID   string    `json:"sender_id"`
	ReceiverID string    `json:"receiver_id"`
	Content    string    `json:"content"`
	MsgType    string    `json:"msg_type"`
	Timestamp  time.Time `json:"timestamp"`
	Status     string    `json:"status,omitempty"`
}

// ConversationState 表示某个用户视角下的单个会话摘要状态。
type ConversationState struct {
	PeerID        string    `json:"peer_id"`
	LastMessage   string    `json:"last_message"`
	LastMsgType   string    `json:"last_msg_type"`
	LastTimestamp time.Time `json:"last_timestamp"`
	UnreadCount   int       `json:"unread_count"`
}

// UserPresence 表示 Redis 中缓存的用户在线状态与展示资料。
type UserPresence struct {
	UserID      string     `json:"user_id"`
	UserName    string     `json:"user_name"`
	AvatarURL   string     `json:"avatar_url,omitempty"`
	Online      bool       `json:"online"`
	IsLoggedIn  bool       `json:"is_logged_in"`
	ConnectedAt *time.Time `json:"connected_at,omitempty"`
	LastSeenAt  *time.Time `json:"last_seen_at,omitempty"`
}

// ConversationSummary 是前端联系人列表与在线用户列表共用的展示模型。
type ConversationSummary struct {
	UserID        string     `json:"user_id"`
	UserName      string     `json:"user_name"`
	AvatarURL     string     `json:"avatar_url,omitempty"`
	Online        bool       `json:"online"`
	IsLoggedIn    bool       `json:"is_logged_in"`
	ConnectedAt   *time.Time `json:"connected_at,omitempty"`
	LastSeenAt    *time.Time `json:"last_seen_at,omitempty"`
	LastMessage   string     `json:"last_message,omitempty"`
	LastMsgType   string     `json:"last_msg_type,omitempty"`
	LastTimestamp *time.Time `json:"last_timestamp,omitempty"`
	UnreadCount   int        `json:"unread_count"`
}

// SessionInitRequest 是连接建立后由前端上报的展示资料。
type SessionInitRequest struct {
	UserID    string `json:"user_id,omitempty"`
	UserName  string `json:"user_name,omitempty"`
	AvatarURL string `json:"avatar_url,omitempty"`
}

// SessionReadyPayload 表示当前连接已经完成资料同步，可继续拉取列表和发送消息。
type SessionReadyPayload struct {
	UserID     string `json:"user_id"`
	UserName   string `json:"user_name"`
	AvatarURL  string `json:"avatar_url,omitempty"`
	Online     bool   `json:"online"`
	IsLoggedIn bool   `json:"is_logged_in"`
}

// ConversationListPayload 是会话列表事件的数据结构。
type ConversationListPayload struct {
	Conversations []ConversationSummary `json:"conversations"`
}

// OnlineUsersPayload 是在线用户列表事件的数据结构。
type OnlineUsersPayload struct {
	Users []ConversationSummary `json:"users"`
}

// ConversationHistoryPayload 是会话历史分页响应的数据结构。
type ConversationHistoryPayload struct {
	PeerID     string        `json:"peer_id"`
	Messages   []ChatMessage `json:"messages"`
	NextOffset int           `json:"next_offset"`
	HasMore    bool          `json:"has_more"`
}

// MessageEnvelope 将消息和前端的 client_id 一并返回，便于前端替换临时消息。
type MessageEnvelope struct {
	ClientID string      `json:"client_id,omitempty"`
	Message  ChatMessage `json:"message"`
}

// PresencePayload 用于广播某个用户的在线状态变化。
type PresencePayload struct {
	UserID     string    `json:"user_id"`
	UserName   string    `json:"user_name"`
	AvatarURL  string    `json:"avatar_url,omitempty"`
	Online     bool      `json:"online"`
	IsLoggedIn bool      `json:"is_logged_in"`
	LastSeenAt time.Time `json:"last_seen_at"`
}

// ConversationReadPayload 用于同步会话已读信息。
type ConversationReadPayload struct {
	PeerID   string    `json:"peer_id"`
	ReaderID string    `json:"reader_id"`
	ReadAt   time.Time `json:"read_at"`
}

// SendMessageRequest 是发送消息请求。
type SendMessageRequest struct {
	ClientID   string `json:"client_id,omitempty"`
	ReceiverID string `json:"receiver_id"`
	Content    string `json:"content"`
	MsgType    string `json:"msg_type"`
}

// LoadHistoryRequest 是历史消息分页请求。
type LoadHistoryRequest struct {
	PeerID string `json:"peer_id"`
	Offset int    `json:"offset"`
	Limit  int    `json:"limit"`
}

// MarkReadRequest 是会话已读请求。
type MarkReadRequest struct {
	PeerID string `json:"peer_id"`
}

// BroadcastEnvelope 是广播层统一分发的事件载体。
// TargetUserID 为空时表示广播给当前实例上的所有在线用户。
type BroadcastEnvelope struct {
	TargetUserID string      `json:"target_user_id,omitempty"`
	Event        ServerEvent `json:"event"`
}
