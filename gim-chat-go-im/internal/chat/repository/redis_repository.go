package repository

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strconv"
	"strings"
	"time"

	"go-ws-server/config"
	"go-ws-server/internal/chat/model"

	"github.com/redis/go-redis/v9"
)

// RedisRepository 是站内信模块当前唯一的状态中心实现。
// 在线状态、登录状态、消息历史、会话摘要全部统一收口到 Redis。
type RedisRepository struct {
	client *redis.Client
}

// NewRedisRepository 创建 Redis 仓储实现。
func NewRedisRepository() *RedisRepository {
	return &RedisRepository{
		client: config.GetRedis(),
	}
}

// AppendMessage 将消息正文写入 Redis，并同步更新双方的会话摘要与索引。
func (r *RedisRepository) AppendMessage(ctx context.Context, message model.ChatMessage) error {
	conversationKey := buildConversationKey(message.SenderID, message.ReceiverID)

	// 先读取双方现有摘要，避免把未读数和最后消息状态覆盖掉。
	senderState, err := r.getConversationState(ctx, message.SenderID, message.ReceiverID)
	if err != nil {
		return err
	}

	receiverState, err := r.getConversationState(ctx, message.ReceiverID, message.SenderID)
	if err != nil {
		return err
	}

	senderState.PeerID = message.ReceiverID
	senderState.LastMessage = message.Content
	senderState.LastMsgType = message.MsgType
	senderState.LastTimestamp = message.Timestamp

	receiverState.PeerID = message.SenderID
	receiverState.LastMessage = message.Content
	receiverState.LastMsgType = message.MsgType
	receiverState.LastTimestamp = message.Timestamp
	receiverState.UnreadCount++

	encodedMessage, err := json.Marshal(message)
	if err != nil {
		return err
	}

	pipe := r.client.TxPipeline()

	// 消息正文统一进入双人会话 List，并在写入后立即裁剪到最近 200 条。
	pipe.RPush(ctx, conversationKey, encodedMessage)
	pipe.LTrim(ctx, conversationKey, -model.MaxConversationMessages, -1)

	if err := putConversationState(ctx, pipe, message.SenderID, senderState); err != nil {
		return err
	}
	if err := putConversationState(ctx, pipe, message.ReceiverID, receiverState); err != nil {
		return err
	}

	score := float64(message.Timestamp.UnixMilli())
	pipe.ZAdd(ctx, conversationIndexKey(message.SenderID), redis.Z{Score: score, Member: message.ReceiverID})
	pipe.ZAdd(ctx, conversationIndexKey(message.ReceiverID), redis.Z{Score: score, Member: message.SenderID})

	_, err = pipe.Exec(ctx)
	return err
}

// ListMessages 按“从最近消息往前翻页”的方式加载历史记录。
func (r *RedisRepository) ListMessages(ctx context.Context, userID, peerID string, offset, limit int) ([]model.ChatMessage, int, bool, error) {
	if limit <= 0 {
		limit = model.DefaultHistoryPageSize
	}
	if offset < 0 {
		offset = 0
	}

	key := buildConversationKey(userID, peerID)
	total, err := r.client.LLen(ctx, key).Result()
	if err != nil {
		return nil, 0, false, err
	}
	if total == 0 {
		return []model.ChatMessage{}, 0, false, nil
	}

	end := total - 1 - int64(offset)
	if end < 0 {
		return []model.ChatMessage{}, offset, false, nil
	}

	start := end - int64(limit) + 1
	if start < 0 {
		start = 0
	}

	rawMessages, err := r.client.LRange(ctx, key, start, end).Result()
	if err != nil {
		return nil, 0, false, err
	}

	messages := make([]model.ChatMessage, 0, len(rawMessages))
	for _, rawMessage := range rawMessages {
		var message model.ChatMessage
		if err := json.Unmarshal([]byte(rawMessage), &message); err != nil {
			return nil, 0, false, err
		}
		messages = append(messages, message)
	}

	nextOffset := offset + len(messages)
	hasMore := start > 0
	return messages, nextOffset, hasMore, nil
}

// ListConversationSummaries 按用户自己的会话索引顺序读取全部摘要。
func (r *RedisRepository) ListConversationSummaries(ctx context.Context, userID string) ([]model.ConversationState, error) {
	peerIDs, err := r.client.ZRevRange(ctx, conversationIndexKey(userID), 0, -1).Result()
	if err != nil {
		return nil, err
	}
	if len(peerIDs) == 0 {
		return []model.ConversationState{}, nil
	}

	pipe := r.client.Pipeline()
	summaryCommands := make(map[string]*redis.MapStringStringCmd, len(peerIDs))
	for _, peerID := range peerIDs {
		summaryCommands[peerID] = pipe.HGetAll(ctx, conversationSummaryKey(userID, peerID))
	}

	if _, err := pipe.Exec(ctx); err != nil && !errors.Is(err, redis.Nil) {
		return nil, err
	}

	result := make([]model.ConversationState, 0, len(peerIDs))
	for _, peerID := range peerIDs {
		fields := summaryCommands[peerID].Val()
		if len(fields) == 0 {
			continue
		}
		result = append(result, mapToConversationState(peerID, fields))
	}

	return result, nil
}

// MarkConversationRead 将当前用户视角下某个会话的未读数清零。
func (r *RedisRepository) MarkConversationRead(ctx context.Context, userID, peerID string) error {
	state, err := r.getConversationState(ctx, userID, peerID)
	if err != nil {
		return err
	}

	state.PeerID = peerID
	state.UnreadCount = 0

	pipe := r.client.TxPipeline()
	if err := putConversationState(ctx, pipe, userID, state); err != nil {
		return err
	}

	_, err = pipe.Exec(ctx)
	return err
}

// UpsertProfileAndOnline 写入用户资料并把当前用户标记为在线。
func (r *RedisRepository) UpsertProfileAndOnline(ctx context.Context, presence model.UserPresence) (model.UserPresence, error) {
	userID := strings.TrimSpace(presence.UserID)
	if userID == "" {
		return model.UserPresence{}, errors.New("missing user id")
	}

	existing, err := r.GetPresence(ctx, userID)
	if err != nil {
		return model.UserPresence{}, err
	}

	now := time.Now().UTC()
	merged := existing
	merged.UserID = userID
	merged.UserName = fallbackString(strings.TrimSpace(presence.UserName), fallbackString(existing.UserName, userID))
	merged.AvatarURL = fallbackString(strings.TrimSpace(presence.AvatarURL), existing.AvatarURL)
	merged.Online = true
	merged.IsLoggedIn = true

	if presence.ConnectedAt != nil {
		connectedAt := presence.ConnectedAt.UTC()
		merged.ConnectedAt = &connectedAt
	} else if merged.ConnectedAt == nil {
		connectedAt := now
		merged.ConnectedAt = &connectedAt
	}

	if presence.LastSeenAt != nil {
		lastSeenAt := presence.LastSeenAt.UTC()
		merged.LastSeenAt = &lastSeenAt
	} else {
		lastSeenAt := now
		merged.LastSeenAt = &lastSeenAt
	}

	pipe := r.client.TxPipeline()
	pipe.HSet(ctx, presenceKey(userID), presenceToRedisMap(merged))
	pipe.SAdd(ctx, onlineUsersKey(), userID)

	_, err = pipe.Exec(ctx)
	return merged, err
}

// SetOffline 将用户标记为离线，并从在线集合中移除。
func (r *RedisRepository) SetOffline(ctx context.Context, userID string, lastSeenAt time.Time) error {
	normalizedUserID := strings.TrimSpace(userID)
	if normalizedUserID == "" {
		return errors.New("missing user id")
	}

	existing, err := r.GetPresence(ctx, normalizedUserID)
	if err != nil {
		return err
	}

	existing.UserID = normalizedUserID
	existing.UserName = fallbackString(existing.UserName, normalizedUserID)
	existing.Online = false
	existing.IsLoggedIn = false
	lastSeenAt = lastSeenAt.UTC()
	existing.LastSeenAt = &lastSeenAt

	pipe := r.client.TxPipeline()
	pipe.HSet(ctx, presenceKey(normalizedUserID), presenceToRedisMap(existing))
	pipe.SRem(ctx, onlineUsersKey(), normalizedUserID)

	_, err = pipe.Exec(ctx)
	return err
}

// GetPresence 读取单个用户的状态快照。
func (r *RedisRepository) GetPresence(ctx context.Context, userID string) (model.UserPresence, error) {
	normalizedUserID := strings.TrimSpace(userID)
	if normalizedUserID == "" {
		return model.UserPresence{}, errors.New("missing user id")
	}

	fields, err := r.client.HGetAll(ctx, presenceKey(normalizedUserID)).Result()
	if err != nil {
		return model.UserPresence{}, err
	}
	if len(fields) == 0 {
		return model.UserPresence{
			UserID:   normalizedUserID,
			UserName: normalizedUserID,
		}, nil
	}

	return mapToPresence(normalizedUserID, fields), nil
}

// ListOnlineUsers 批量读取当前在线用户的状态快照。
func (r *RedisRepository) ListOnlineUsers(ctx context.Context) ([]model.UserPresence, error) {
	userIDs, err := r.client.SMembers(ctx, onlineUsersKey()).Result()
	if err != nil {
		return nil, err
	}
	if len(userIDs) == 0 {
		return []model.UserPresence{}, nil
	}

	sort.Strings(userIDs)

	pipe := r.client.Pipeline()
	presenceCommands := make(map[string]*redis.MapStringStringCmd, len(userIDs))
	for _, userID := range userIDs {
		presenceCommands[userID] = pipe.HGetAll(ctx, presenceKey(userID))
	}

	if _, err := pipe.Exec(ctx); err != nil && !errors.Is(err, redis.Nil) {
		return nil, err
	}

	result := make([]model.UserPresence, 0, len(userIDs))
	for _, userID := range userIDs {
		fields := presenceCommands[userID].Val()
		if len(fields) == 0 {
			result = append(result, model.UserPresence{
				UserID:     userID,
				UserName:   userID,
				Online:     true,
				IsLoggedIn: true,
			})
			continue
		}

		presence := mapToPresence(userID, fields)
		if !presence.Online {
			continue
		}
		result = append(result, presence)
	}

	sort.SliceStable(result, func(left, right int) bool {
		leftTime := presenceSortTime(result[left])
		rightTime := presenceSortTime(result[right])
		if !leftTime.Equal(rightTime) {
			return leftTime.After(rightTime)
		}
		return strings.Compare(result[left].UserName, result[right].UserName) < 0
	})

	return result, nil
}

// getConversationState 读取单个会话摘要，不存在时返回默认值。
func (r *RedisRepository) getConversationState(ctx context.Context, userID, peerID string) (model.ConversationState, error) {
	fields, err := r.client.HGetAll(ctx, conversationSummaryKey(userID, peerID)).Result()
	if err != nil {
		return model.ConversationState{}, err
	}
	if len(fields) == 0 {
		return model.ConversationState{PeerID: peerID}, nil
	}

	return mapToConversationState(peerID, fields), nil
}

// putConversationState 将摘要以 Hash 形式写入 Redis，便于后续迁移到 MySQL。
func putConversationState(ctx context.Context, pipe redis.Pipeliner, userID string, state model.ConversationState) error {
	if strings.TrimSpace(state.PeerID) == "" {
		return errors.New("missing peer id")
	}

	pipe.HSet(ctx, conversationSummaryKey(userID, state.PeerID), map[string]interface{}{
		"peer_id":        state.PeerID,
		"last_message":   state.LastMessage,
		"last_msg_type":  fallbackString(state.LastMsgType, model.DefaultMessageType),
		"last_timestamp": timeToString(&state.LastTimestamp),
		"unread_count":   state.UnreadCount,
	})
	return nil
}

// mapToConversationState 把 Redis Hash 字段还原成领域模型。
func mapToConversationState(peerID string, fields map[string]string) model.ConversationState {
	state := model.ConversationState{
		PeerID:      fallbackString(fields["peer_id"], peerID),
		LastMessage: fields["last_message"],
		LastMsgType: fallbackString(fields["last_msg_type"], model.DefaultMessageType),
		UnreadCount: parseRedisInt(fields["unread_count"]),
	}

	if rawTime := fields["last_timestamp"]; rawTime != "" {
		if parsedTime, err := time.Parse(time.RFC3339Nano, rawTime); err == nil {
			state.LastTimestamp = parsedTime
		}
	}

	return state
}

// mapToPresence 把 Redis Hash 字段还原成用户状态快照。
func mapToPresence(userID string, fields map[string]string) model.UserPresence {
	presence := model.UserPresence{
		UserID:     fallbackString(fields["user_id"], userID),
		UserName:   fallbackString(fields["user_name"], userID),
		AvatarURL:  fields["avatar_url"],
		Online:     parseRedisBool(fields["online"]),
		IsLoggedIn: parseRedisBool(fields["is_logged_in"]),
	}

	if rawConnectedAt := fields["connected_at"]; rawConnectedAt != "" {
		if parsedTime, err := time.Parse(time.RFC3339Nano, rawConnectedAt); err == nil {
			connectedAt := parsedTime
			presence.ConnectedAt = &connectedAt
		}
	}

	if rawLastSeenAt := fields["last_seen_at"]; rawLastSeenAt != "" {
		if parsedTime, err := time.Parse(time.RFC3339Nano, rawLastSeenAt); err == nil {
			lastSeenAt := parsedTime
			presence.LastSeenAt = &lastSeenAt
		}
	}

	return presence
}

// presenceToRedisMap 把用户状态快照转换为适合 Redis Hash 写入的字段集合。
func presenceToRedisMap(presence model.UserPresence) map[string]interface{} {
	return map[string]interface{}{
		"user_id":      presence.UserID,
		"user_name":    fallbackString(presence.UserName, presence.UserID),
		"avatar_url":   presence.AvatarURL,
		"online":       boolToRedisValue(presence.Online),
		"is_logged_in": boolToRedisValue(presence.IsLoggedIn),
		"connected_at": timeToString(presence.ConnectedAt),
		"last_seen_at": timeToString(presence.LastSeenAt),
	}
}

// presenceSortTime 统一在线用户列表的排序时间基准。
func presenceSortTime(presence model.UserPresence) time.Time {
	if presence.ConnectedAt != nil {
		return *presence.ConnectedAt
	}
	if presence.LastSeenAt != nil {
		return *presence.LastSeenAt
	}
	return time.Time{}
}

// buildConversationKey 生成双人会话的唯一 List Key。
func buildConversationKey(userA, userB string) string {
	users := []string{userA, userB}
	sort.Strings(users)
	return fmt.Sprintf("im:conversation:%s:%s", users[0], users[1])
}

// conversationIndexKey 生成某个用户自己的会话索引 ZSet Key。
func conversationIndexKey(userID string) string {
	return fmt.Sprintf("im:conversation:index:%s", userID)
}

// conversationSummaryKey 生成某个用户视角下单个会话摘要的 Hash Key。
func conversationSummaryKey(userID, peerID string) string {
	return fmt.Sprintf("im:conversation:summary:%s:%s", userID, peerID)
}

// onlineUsersKey 生成在线用户集合 Key。
func onlineUsersKey() string {
	return "im:online_users"
}

// presenceKey 生成用户状态快照 Key。
func presenceKey(userID string) string {
	return fmt.Sprintf("im:presence:%s", userID)
}

// boolToRedisValue 将布尔值转成 Redis 中统一保存的 0/1 字符串。
func boolToRedisValue(value bool) string {
	if value {
		return "1"
	}
	return "0"
}

// parseRedisBool 将 Redis 中的 0/1 或 true/false 文本恢复为布尔值。
func parseRedisBool(value string) bool {
	return value == "1" || strings.EqualFold(value, "true")
}

// parseRedisInt 将 Redis 文本字段恢复为整数，解析失败时返回 0。
func parseRedisInt(value string) int {
	parsedValue, err := strconv.Atoi(strings.TrimSpace(value))
	if err != nil {
		return 0
	}
	return parsedValue
}

// timeToString 将时间指针转换成 Redis 中统一存储的 RFC3339Nano 字符串。
func timeToString(value *time.Time) string {
	if value == nil || value.IsZero() {
		return ""
	}
	return value.UTC().Format(time.RFC3339Nano)
}

// fallbackString 在主字段为空时回退到默认值。
func fallbackString(value, defaultValue string) string {
	if strings.TrimSpace(value) == "" {
		return defaultValue
	}
	return value
}
