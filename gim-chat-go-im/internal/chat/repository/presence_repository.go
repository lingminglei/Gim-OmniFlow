package repository

import (
	"context"
	"time"

	"go-ws-server/internal/chat/model"
)

// PresenceRepository 定义用户在线状态与登录状态的存储接口。
// 当前由 Redis 实现，后续如需切换存储介质，只需要替换实现层。
type PresenceRepository interface {
	// UpsertProfileAndOnline 写入用户资料并把当前用户标记为在线/已登录。
	UpsertProfileAndOnline(ctx context.Context, presence model.UserPresence) (model.UserPresence, error)
	// SetOffline 将用户标记为离线，并保留最后活跃时间与展示资料。
	SetOffline(ctx context.Context, userID string, lastSeenAt time.Time) error
	// GetPresence 查询单个用户的状态快照。
	GetPresence(ctx context.Context, userID string) (model.UserPresence, error)
	// ListOnlineUsers 返回当前所有在线用户的状态快照。
	ListOnlineUsers(ctx context.Context) ([]model.UserPresence, error)
}
