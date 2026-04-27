package repository

import (
	"context"

	"go-ws-server/internal/chat/model"
)

type MessageRepository interface {
	// AppendMessage 将一条消息写入 Redis，并同步更新双方的会话摘要与会话索引。
	AppendMessage(ctx context.Context, message model.ChatMessage) error
	// ListMessages 按偏移量倒序分页加载某个会话的历史消息。
	ListMessages(ctx context.Context, userID, peerID string, offset, limit int) ([]model.ChatMessage, int, bool, error)
	// ListConversationSummaries 按最近活跃时间返回当前用户的全部会话摘要。
	ListConversationSummaries(ctx context.Context, userID string) ([]model.ConversationState, error)
	// MarkConversationRead 将某个会话的未读数清零。
	MarkConversationRead(ctx context.Context, userID, peerID string) error
}
