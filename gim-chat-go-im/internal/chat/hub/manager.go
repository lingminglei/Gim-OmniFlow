package hub

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"strings"
	"sync"
	"time"

	"go-ws-server/internal/chat/broadcast"
	"go-ws-server/internal/chat/model"
	chatservice "go-ws-server/internal/chat/service"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

// Manager 是站内信接入层的核心协调者。
// 它负责管理连接池、处理心跳与断线、分发业务事件，并把单机广播抽象为可扩展接口。
type Manager struct {
	clients     map[string]map[string]*Client
	mu          sync.RWMutex
	service     *chatservice.ChatService
	broadcaster broadcast.Broadcaster
	upgrader    websocket.Upgrader
}

// NewManager 创建站内信接入层管理器。
func NewManager(service *chatservice.ChatService, broadcaster broadcast.Broadcaster) *Manager {
	manager := &Manager{
		clients:     make(map[string]map[string]*Client),
		service:     service,
		broadcaster: broadcaster,
		upgrader: websocket.Upgrader{
			CheckOrigin: func(r *http.Request) bool {
				return true
			},
		},
	}

	// 单机默认订阅内存广播通道，后续切换到 Redis Pub/Sub 时只需要替换 Broadcaster 实现。
	if broadcaster != nil {
		events, _, err := broadcaster.Subscribe(context.Background())
		if err != nil {
			log.Printf("failed to subscribe broadcaster: %v", err)
		} else {
			go manager.consumeBroadcasts(events)
		}
	}

	return manager
}

// ServeWS 处理站内信 WebSocket 握手，并把连接注册到内存连接池。
// 这里只要求前端带 userId，不做账号真实性校验。
func (m *Manager) ServeWS(c *gin.Context) {
	userID := strings.TrimSpace(c.Query("userId"))
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "missing userId"})
		return
	}

	conn, err := m.upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("websocket upgrade failed, user=%s, err=%v", userID, err)
		return
	}

	client := newClient(userID, conn, m)
	m.register(client)

	go client.writePump()
	go client.readPump()
}

// register 将新连接放入连接池。
// 同一用户允许多个标签页同时在线，只有首个连接建立时才会写入 Redis 在线状态。
func (m *Manager) register(client *Client) {
	firstConnection := false

	m.mu.Lock()
	userClients, exists := m.clients[client.userID]
	if !exists {
		userClients = make(map[string]*Client)
		m.clients[client.userID] = userClients
		firstConnection = true
	}
	userClients[client.connectionID] = client
	m.mu.Unlock()

	if !firstConnection {
		return
	}

	presence, err := m.service.MarkOnline(context.Background(), client.userID)
	if err != nil {
		log.Printf("failed to mark user online, user=%s, err=%v", client.userID, err)
		return
	}

	m.publishPresenceChange(presence)
}

// unregister 将失效连接移出连接池。
// 只有最后一个连接断开时，才会把用户标记为离线并广播下线事件。
func (m *Manager) unregister(client *Client) {
	lastConnection := false

	m.mu.Lock()
	userClients, exists := m.clients[client.userID]
	if exists {
		delete(userClients, client.connectionID)
		if len(userClients) == 0 {
			delete(m.clients, client.userID)
			lastConnection = true
		}
	}
	m.mu.Unlock()

	client.close()

	if !lastConnection {
		return
	}

	presence, err := m.service.MarkOffline(context.Background(), client.userID)
	if err != nil {
		log.Printf("failed to mark user offline, user=%s, err=%v", client.userID, err)
		return
	}

	m.publishPresenceChange(presence)
}

// handleClientMessage 根据命令类型分发到不同的业务处理逻辑。
func (m *Manager) handleClientMessage(client *Client, payload []byte) {
	var command model.ClientCommand
	if err := json.Unmarshal(payload, &command); err != nil {
		m.sendError(client, "", "bad_request", "invalid command payload")
		return
	}

	// 除了 ping 和 session.init 之外，其余命令都要求当前连接先完成会话初始化。
	if command.Type != model.CommandPing && command.Type != model.CommandSessionInit && !client.isInitialized() {
		m.sendError(client, command.RequestID, "session_not_ready", "session.init is required before other commands")
		return
	}

	switch command.Type {
	case model.CommandPing:
		m.sendToClient(client, model.ServerEvent{
			Type:      model.EventPong,
			RequestID: command.RequestID,
			Data: map[string]time.Time{
				"timestamp": time.Now().UTC(),
			},
		})

	case model.CommandSessionInit:
		var request model.SessionInitRequest
		if err := decodeData(command.Data, &request); err != nil {
			m.sendError(client, command.RequestID, "bad_request", "invalid session.init payload")
			return
		}

		presence, err := m.service.InitializeSession(context.Background(), client.userID, request)
		if err != nil {
			m.sendError(client, command.RequestID, "bad_request", err.Error())
			return
		}

		client.markInitialized()
		m.sendToClient(client, model.ServerEvent{
			Type:      model.EventSessionReady,
			RequestID: command.RequestID,
			Data: model.SessionReadyPayload{
				UserID:     presence.UserID,
				UserName:   presence.UserName,
				AvatarURL:  presence.AvatarURL,
				Online:     presence.Online,
				IsLoggedIn: presence.IsLoggedIn,
			},
		})
		m.pushConversationListToClient(client, "")
		m.pushOnlineUsersToClient(client, "")
		m.publishPresenceChange(presence)

	case model.CommandConversationList:
		m.pushConversationListToClient(client, command.RequestID)

	case model.CommandOnlineUsersList:
		m.pushOnlineUsersToClient(client, command.RequestID)

	case model.CommandConversationHistory:
		var request model.LoadHistoryRequest
		if err := decodeData(command.Data, &request); err != nil {
			m.sendError(client, command.RequestID, "bad_request", "invalid history request")
			return
		}

		history, err := m.service.LoadConversationHistory(context.Background(), client.userID, request)
		if err != nil {
			m.sendError(client, command.RequestID, "bad_request", err.Error())
			return
		}

		m.sendToClient(client, model.ServerEvent{
			Type:      model.EventConversationHistory,
			RequestID: command.RequestID,
			Data:      history,
		})

	case model.CommandMessageSend:
		var request model.SendMessageRequest
		if err := decodeData(command.Data, &request); err != nil {
			m.sendError(client, command.RequestID, "bad_request", "invalid send message request")
			return
		}

		message, err := m.service.SendMessage(context.Background(), client.userID, request)
		if err != nil {
			m.sendError(client, command.RequestID, "bad_request", err.Error())
			return
		}

		m.sendToClient(client, model.ServerEvent{
			Type:      model.EventMessageSent,
			RequestID: command.RequestID,
			Data: model.MessageEnvelope{
				ClientID: request.ClientID,
				Message:  message,
			},
		})
		m.pushConversationListToUser(client.userID)
		m.publishToUser(message.ReceiverID, model.ServerEvent{
			Type: model.EventMessageNew,
			Data: model.MessageEnvelope{
				Message: message,
			},
		})

	case model.CommandConversationRead:
		var request model.MarkReadRequest
		if err := decodeData(command.Data, &request); err != nil {
			m.sendError(client, command.RequestID, "bad_request", "invalid mark read request")
			return
		}

		payload, err := m.service.MarkConversationRead(context.Background(), client.userID, request)
		if err != nil {
			m.sendError(client, command.RequestID, "bad_request", err.Error())
			return
		}

		m.sendToClient(client, model.ServerEvent{
			Type:      model.EventConversationRead,
			RequestID: command.RequestID,
			Data:      payload,
		})
		m.pushConversationListToUser(client.userID)

		if payload.PeerID != "" && payload.PeerID != client.userID {
			m.publishToUser(payload.PeerID, model.ServerEvent{
				Type: model.EventConversationRead,
				Data: model.ConversationReadPayload{
					PeerID:   client.userID,
					ReaderID: client.userID,
					ReadAt:   payload.ReadAt,
				},
			})
		}

	default:
		m.sendError(client, command.RequestID, "bad_request", "unknown command type")
	}
}

// pushConversationListToClient 将最新会话摘要返回给发起请求的连接。
func (m *Manager) pushConversationListToClient(client *Client, requestID string) {
	conversations, err := m.service.ListConversations(context.Background(), client.userID)
	if err != nil {
		m.sendError(client, requestID, "internal_error", "failed to load conversation list")
		return
	}

	m.sendToClient(client, model.ServerEvent{
		Type:      model.EventConversationList,
		RequestID: requestID,
		Data: model.ConversationListPayload{
			Conversations: conversations,
		},
	})
}

// pushConversationListToUser 将最新会话摘要同步给某个用户的全部已初始化连接。
func (m *Manager) pushConversationListToUser(userID string) {
	conversations, err := m.service.ListConversations(context.Background(), userID)
	if err != nil {
		log.Printf("failed to load conversation list, user=%s, err=%v", userID, err)
		return
	}

	m.sendToUser(userID, model.ServerEvent{
		Type: model.EventConversationList,
		Data: model.ConversationListPayload{
			Conversations: conversations,
		},
	})
}

// pushOnlineUsersToClient 将在线用户列表返回给发起请求的连接。
func (m *Manager) pushOnlineUsersToClient(client *Client, requestID string) {
	users, err := m.service.ListOnlineUsers(context.Background(), client.userID)
	if err != nil {
		m.sendError(client, requestID, "internal_error", "failed to load online users")
		return
	}

	m.sendToClient(client, model.ServerEvent{
		Type:      model.EventOnlineUsersList,
		RequestID: requestID,
		Data: model.OnlineUsersPayload{
			Users: users,
		},
	})
}

// pushOnlineUsersToUser 将在线用户列表同步给某个用户的全部已初始化连接。
func (m *Manager) pushOnlineUsersToUser(userID string) {
	users, err := m.service.ListOnlineUsers(context.Background(), userID)
	if err != nil {
		log.Printf("failed to load online users, user=%s, err=%v", userID, err)
		return
	}

	m.sendToUser(userID, model.ServerEvent{
		Type: model.EventOnlineUsersList,
		Data: model.OnlineUsersPayload{
			Users: users,
		},
	})
}

// broadcastOnlineUsers 在在线状态变化后，向所有在线用户同步最新在线列表。
func (m *Manager) broadcastOnlineUsers() {
	for _, userID := range m.snapshotUserIDs() {
		m.pushOnlineUsersToUser(userID)
	}
}

// consumeBroadcasts 消费广播层输出的事件，并路由到当前实例上的本地连接。
func (m *Manager) consumeBroadcasts(events <-chan model.BroadcastEnvelope) {
	for event := range events {
		m.handleBroadcast(event)
	}
}

// handleBroadcast 根据广播目标把事件分发给指定用户或所有用户。
func (m *Manager) handleBroadcast(envelope model.BroadcastEnvelope) {
	if envelope.TargetUserID != "" {
		m.sendToUser(envelope.TargetUserID, envelope.Event)
		if envelope.Event.Type == model.EventMessageNew {
			m.pushConversationListToUser(envelope.TargetUserID)
		}
		return
	}

	m.broadcastEvent(envelope.Event)
	if envelope.Event.Type == model.EventPresenceChanged {
		m.broadcastOnlineUsers()
	}
}

// publishPresenceChange 发布在线状态变化事件。
func (m *Manager) publishPresenceChange(presence model.UserPresence) {
	lastSeenAt := time.Now().UTC()
	if presence.LastSeenAt != nil {
		lastSeenAt = presence.LastSeenAt.UTC()
	}

	m.publishEnvelope(model.BroadcastEnvelope{
		Event: model.ServerEvent{
			Type: model.EventPresenceChanged,
			Data: model.PresencePayload{
				UserID:     presence.UserID,
				UserName:   presence.UserName,
				AvatarURL:  presence.AvatarURL,
				Online:     presence.Online,
				IsLoggedIn: presence.IsLoggedIn,
				LastSeenAt: lastSeenAt,
			},
		},
	})
}

// publishToUser 发布定向用户事件，单机下会回落为当前实例内存分发。
func (m *Manager) publishToUser(userID string, event model.ServerEvent) {
	m.publishEnvelope(model.BroadcastEnvelope{
		TargetUserID: userID,
		Event:        event,
	})
}

// publishEnvelope 统一调用广播接口；若广播接口不可用，则直接在当前实例本地分发。
func (m *Manager) publishEnvelope(envelope model.BroadcastEnvelope) {
	if m.broadcaster == nil {
		m.handleBroadcast(envelope)
		return
	}

	if err := m.broadcaster.Publish(context.Background(), envelope); err != nil {
		log.Printf("failed to publish envelope, err=%v", err)
		m.handleBroadcast(envelope)
	}
}

// sendToClient 将事件发送给单条连接。
func (m *Manager) sendToClient(client *Client, event model.ServerEvent) {
	if client == nil {
		return
	}
	if err := client.push(event); err != nil {
		log.Printf("failed to push event to client, user=%s, type=%s, err=%v", client.userID, event.Type, err)
	}
}

// sendToUser 将事件发送给某个用户的全部已初始化连接。
func (m *Manager) sendToUser(userID string, event model.ServerEvent) {
	for _, client := range m.snapshotUserClients(userID) {
		if !client.isInitialized() && !allowEventBeforeInit(event.Type) {
			continue
		}
		if err := client.push(event); err != nil {
			log.Printf("failed to push event, user=%s, type=%s, err=%v", userID, event.Type, err)
		}
	}
}

// broadcastEvent 将事件广播给当前实例上的全部已初始化连接。
func (m *Manager) broadcastEvent(event model.ServerEvent) {
	for _, client := range m.snapshotClients() {
		if !client.isInitialized() && !allowEventBeforeInit(event.Type) {
			continue
		}
		if err := client.push(event); err != nil {
			log.Printf("failed to broadcast event, user=%s, type=%s, err=%v", client.userID, event.Type, err)
		}
	}
}

// sendError 统一封装错误事件返回格式。
func (m *Manager) sendError(client *Client, requestID, code, message string) {
	m.sendToClient(client, model.ServerEvent{
		Type:      model.EventError,
		RequestID: requestID,
		Error: &model.ErrorPayload{
			Code:    code,
			Message: message,
		},
	})
}

// snapshotClients 返回当前实例上的全部连接快照，避免遍历过程长时间持锁。
func (m *Manager) snapshotClients() []*Client {
	m.mu.RLock()
	defer m.mu.RUnlock()

	result := make([]*Client, 0)
	for _, userClients := range m.clients {
		for _, client := range userClients {
			result = append(result, client)
		}
	}
	return result
}

// snapshotUserClients 返回某个用户的全部连接快照。
func (m *Manager) snapshotUserClients(userID string) []*Client {
	m.mu.RLock()
	defer m.mu.RUnlock()

	userClients, exists := m.clients[userID]
	if !exists {
		return []*Client{}
	}

	result := make([]*Client, 0, len(userClients))
	for _, client := range userClients {
		result = append(result, client)
	}
	return result
}

// snapshotUserIDs 返回当前实例上所有在线用户 ID。
func (m *Manager) snapshotUserIDs() []string {
	m.mu.RLock()
	defer m.mu.RUnlock()

	result := make([]string, 0, len(m.clients))
	for userID := range m.clients {
		result = append(result, userID)
	}
	return result
}

// allowEventBeforeInit 定义哪些事件允许在 session.init 前发给客户端。
func allowEventBeforeInit(eventType string) bool {
	switch eventType {
	case model.EventSessionReady, model.EventError, model.EventPong:
		return true
	default:
		return false
	}
}

// decodeData 将命令 data 字段解码到具体请求结构。
func decodeData(raw json.RawMessage, target interface{}) error {
	if len(raw) == 0 {
		return errors.New("missing data payload")
	}
	return json.Unmarshal(raw, target)
}
