package broadcast

import (
	"context"

	"go-ws-server/internal/chat/model"
)

// Broadcaster 抽象了站内信模块的广播能力。
// 单机模式下可使用内存实现，多实例部署时可平滑替换为 Redis Pub/Sub 实现。
type Broadcaster interface {
	// Publish 发布一条站内信广播事件。
	Publish(ctx context.Context, envelope model.BroadcastEnvelope) error
	// Subscribe 订阅广播事件，并返回取消订阅函数。
	Subscribe(ctx context.Context) (<-chan model.BroadcastEnvelope, func(), error)
}
