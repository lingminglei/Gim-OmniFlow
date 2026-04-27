package service

import (
	"context"
	"errors"
	"fmt"
	"slices"
	"strings"
	"sync/atomic"
	"time"

	"go-ws-server/internal/chat/model"
	"go-ws-server/internal/chat/repository"
)

var messageSequence uint64

// ChatService 负责承接站内信的核心业务规则。
// 它不依赖 MySQL，所有在线状态、登录状态、消息历史与会话摘要全部由 Redis 提供。
type ChatService struct {
	messageRepo  repository.MessageRepository
	presenceRepo repository.PresenceRepository
}

// NewChatService 创建站内信服务层对象。
func NewChatService(messageRepo repository.MessageRepository, presenceRepo repository.PresenceRepository) *ChatService {
	return &ChatService{
		messageRepo:  messageRepo,
		presenceRepo: presenceRepo,
	}
}

// MarkOnline 在首个连接建立时把用户标记为在线。
// 这里不做用户真实性校验，只维护 Redis 中的在线状态与基础占位资料。
func (s *ChatService) MarkOnline(ctx context.Context, userID string) (model.UserPresence, error) {
	normalizedUserID := strings.TrimSpace(userID)
	if normalizedUserID == "" {
		return model.UserPresence{}, errors.New("missing user id")
	}

	now := time.Now().UTC()
	return s.presenceRepo.UpsertProfileAndOnline(ctx, model.UserPresence{
		UserID:      normalizedUserID,
		UserName:    normalizedUserID,
		Online:      true,
		IsLoggedIn:  true,
		ConnectedAt: &now,
		LastSeenAt:  &now,
	})
}

// InitializeSession 接收前端上报的展示资料，并同步到 Redis。
// 这里只校验 userId 是否与握手参数一致，不做账号存在性校验。
func (s *ChatService) InitializeSession(ctx context.Context, userID string, request model.SessionInitRequest) (model.UserPresence, error) {
	normalizedUserID := strings.TrimSpace(userID)
	if normalizedUserID == "" {
		return model.UserPresence{}, errors.New("missing user id")
	}

	requestUserID := strings.TrimSpace(request.UserID)
	if requestUserID != "" && requestUserID != normalizedUserID {
		return model.UserPresence{}, errors.New("session user id mismatch")
	}

	now := time.Now().UTC()
	return s.presenceRepo.UpsertProfileAndOnline(ctx, model.UserPresence{
		UserID:     normalizedUserID,
		UserName:   fallback(strings.TrimSpace(request.UserName), normalizedUserID),
		AvatarURL:  strings.TrimSpace(request.AvatarURL),
		Online:     true,
		IsLoggedIn: true,
		LastSeenAt: &now,
	})
}

// MarkOffline 在最后一个连接断开时把用户标记为离线，并返回最新的状态快照。
func (s *ChatService) MarkOffline(ctx context.Context, userID string) (model.UserPresence, error) {
	normalizedUserID := strings.TrimSpace(userID)
	if normalizedUserID == "" {
		return model.UserPresence{}, errors.New("missing user id")
	}

	lastSeenAt := time.Now().UTC()
	if err := s.presenceRepo.SetOffline(ctx, normalizedUserID, lastSeenAt); err != nil {
		return model.UserPresence{}, err
	}

	return s.presenceRepo.GetPresence(ctx, normalizedUserID)
}

// ListConversations 聚合当前用户自己的会话摘要与 Redis 中缓存的用户状态，返回前端会话列表。
func (s *ChatService) ListConversations(ctx context.Context, userID string) ([]model.ConversationSummary, error) {
	normalizedUserID := strings.TrimSpace(userID)
	if normalizedUserID == "" {
		return []model.ConversationSummary{}, nil
	}

	storedSummaries, err := s.messageRepo.ListConversationSummaries(ctx, normalizedUserID)
	if err != nil {
		return nil, err
	}

	result := make([]model.ConversationSummary, 0, len(storedSummaries))
	for _, storedSummary := range storedSummaries {
		presence, err := s.presenceRepo.GetPresence(ctx, storedSummary.PeerID)
		if err != nil {
			return nil, err
		}
		result = append(result, buildConversationSummary(storedSummary, presence))
	}

	sortConversationSummaries(result)
	return result, nil
}

// ListOnlineUsers 返回所有当前在线的 WebSocket 用户，并补齐当前用户视角下的会话摘要信息。
func (s *ChatService) ListOnlineUsers(ctx context.Context, currentUserID string) ([]model.ConversationSummary, error) {
	onlineUsers, err := s.presenceRepo.ListOnlineUsers(ctx)
	if err != nil {
		return nil, err
	}

	storedSummaries, err := s.messageRepo.ListConversationSummaries(ctx, strings.TrimSpace(currentUserID))
	if err != nil {
		return nil, err
	}

	summaryMap := make(map[string]model.ConversationState, len(storedSummaries))
	for _, summary := range storedSummaries {
		summaryMap[summary.PeerID] = summary
	}

	result := make([]model.ConversationSummary, 0, len(onlineUsers))
	for _, presence := range onlineUsers {
		if presence.UserID == "" || presence.UserID == strings.TrimSpace(currentUserID) {
			continue
		}
		result = append(result, buildConversationSummary(summaryMap[presence.UserID], presence))
	}

	sortConversationSummaries(result)
	return result, nil
}

// LoadConversationHistory 分页加载指定会话的历史消息。
func (s *ChatService) LoadConversationHistory(ctx context.Context, userID string, request model.LoadHistoryRequest) (model.ConversationHistoryPayload, error) {
	if strings.TrimSpace(request.PeerID) == "" {
		return model.ConversationHistoryPayload{}, errors.New("missing peer id")
	}

	limit := request.Limit
	if limit <= 0 {
		limit = model.DefaultHistoryPageSize
	}
	if limit > model.MaxHistoryPageSize {
		limit = model.MaxHistoryPageSize
	}

	messages, nextOffset, hasMore, err := s.messageRepo.ListMessages(ctx, userID, request.PeerID, request.Offset, limit)
	if err != nil {
		return model.ConversationHistoryPayload{}, err
	}

	return model.ConversationHistoryPayload{
		PeerID:     request.PeerID,
		Messages:   messages,
		NextOffset: nextOffset,
		HasMore:    hasMore,
	}, nil
}

// SendMessage 负责校验消息参数、生成消息 ID，并把消息写入 Redis。
func (s *ChatService) SendMessage(ctx context.Context, senderID string, request model.SendMessageRequest) (model.ChatMessage, error) {
	normalizedSenderID := strings.TrimSpace(senderID)
	receiverID := strings.TrimSpace(request.ReceiverID)
	content := strings.TrimSpace(request.Content)

	if normalizedSenderID == "" {
		return model.ChatMessage{}, errors.New("missing sender id")
	}
	if receiverID == "" {
		return model.ChatMessage{}, errors.New("missing receiver id")
	}
	if content == "" {
		return model.ChatMessage{}, errors.New("message content cannot be empty")
	}

	messageType := strings.TrimSpace(request.MsgType)
	if messageType == "" {
		messageType = model.DefaultMessageType
	}

	message := model.ChatMessage{
		ID:         generateMessageID(normalizedSenderID),
		ClientID:   strings.TrimSpace(request.ClientID),
		SenderID:   normalizedSenderID,
		ReceiverID: receiverID,
		Content:    content,
		MsgType:    messageType,
		Timestamp:  time.Now().UTC(),
		Status:     model.MessageStatusSent,
	}

	if err := s.messageRepo.AppendMessage(ctx, message); err != nil {
		return model.ChatMessage{}, err
	}

	return message, nil
}

// MarkConversationRead 将当前用户视角下的指定会话标记为已读，并返回已读回执。
func (s *ChatService) MarkConversationRead(ctx context.Context, userID string, request model.MarkReadRequest) (model.ConversationReadPayload, error) {
	normalizedUserID := strings.TrimSpace(userID)
	peerID := strings.TrimSpace(request.PeerID)
	if normalizedUserID == "" {
		return model.ConversationReadPayload{}, errors.New("missing user id")
	}
	if peerID == "" {
		return model.ConversationReadPayload{}, errors.New("missing peer id")
	}

	if err := s.messageRepo.MarkConversationRead(ctx, normalizedUserID, peerID); err != nil {
		return model.ConversationReadPayload{}, err
	}

	return model.ConversationReadPayload{
		PeerID:   peerID,
		ReaderID: normalizedUserID,
		ReadAt:   time.Now().UTC(),
	}, nil
}

// generateMessageID 生成全局唯一消息 ID，避免并发发送时冲突。
func generateMessageID(senderID string) string {
	sequence := atomic.AddUint64(&messageSequence, 1)
	return fmt.Sprintf("%s-%d-%d", senderID, time.Now().UnixNano(), sequence)
}

// sortTime 统一计算会话排序时间，优先使用最后消息时间。
func sortTime(summary model.ConversationSummary) time.Time {
	if summary.LastTimestamp != nil {
		return *summary.LastTimestamp
	}
	if summary.LastSeenAt != nil {
		return *summary.LastSeenAt
	}
	if summary.ConnectedAt != nil {
		return *summary.ConnectedAt
	}
	return time.Time{}
}

// fallback 在主字段为空时回退到默认值。
func fallback(value, defaultValue string) string {
	if strings.TrimSpace(value) == "" {
		return defaultValue
	}
	return value
}

// buildConversationSummary 统一组装会话列表和在线用户列表需要的展示结构。
func buildConversationSummary(storedSummary model.ConversationState, presence model.UserPresence) model.ConversationSummary {
	userID := fallback(presence.UserID, storedSummary.PeerID)
	userName := fallback(presence.UserName, userID)

	summary := model.ConversationSummary{
		UserID:      userID,
		UserName:    userName,
		AvatarURL:   presence.AvatarURL,
		Online:      presence.Online,
		IsLoggedIn:  presence.IsLoggedIn,
		ConnectedAt: presence.ConnectedAt,
		LastSeenAt:  presence.LastSeenAt,
		UnreadCount: storedSummary.UnreadCount,
		LastMessage: storedSummary.LastMessage,
		LastMsgType: storedSummary.LastMsgType,
	}

	if !storedSummary.LastTimestamp.IsZero() {
		lastTimestamp := storedSummary.LastTimestamp
		summary.LastTimestamp = &lastTimestamp
	}

	return summary
}

// sortConversationSummaries 按在线状态、最近时间、用户名统一排序，保证前端展示一致。
func sortConversationSummaries(result []model.ConversationSummary) {
	slices.SortFunc(result, func(left, right model.ConversationSummary) int {
		if left.Online != right.Online {
			if left.Online {
				return -1
			}
			return 1
		}

		leftTime := sortTime(left)
		rightTime := sortTime(right)
		if !leftTime.Equal(rightTime) {
			if leftTime.After(rightTime) {
				return -1
			}
			return 1
		}

		return strings.Compare(left.UserName, right.UserName)
	})
}
