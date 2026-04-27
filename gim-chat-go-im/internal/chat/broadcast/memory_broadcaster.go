package broadcast

import (
	"context"
	"sync"
	"sync/atomic"

	"go-ws-server/internal/chat/model"
)

// MemoryBroadcaster 是单机模式下的默认广播实现。
// 它使用进程内 channel 分发事件，后续替换成 Redis Pub/Sub 时无需改动业务层。
type MemoryBroadcaster struct {
	mu          sync.RWMutex
	sequence    uint64
	subscribers map[uint64]chan model.BroadcastEnvelope
}

// NewMemoryBroadcaster 创建内存广播器。
func NewMemoryBroadcaster() *MemoryBroadcaster {
	return &MemoryBroadcaster{
		subscribers: make(map[uint64]chan model.BroadcastEnvelope),
	}
}

// Publish 将事件广播给当前所有订阅者。
func (b *MemoryBroadcaster) Publish(ctx context.Context, envelope model.BroadcastEnvelope) error {
	for _, subscriber := range b.snapshotSubscribers() {
		select {
		case subscriber <- envelope:
		case <-ctx.Done():
			return ctx.Err()
		}
	}
	return nil
}

// Subscribe 注册一个新的订阅者，并在上下文结束时自动清理。
func (b *MemoryBroadcaster) Subscribe(ctx context.Context) (<-chan model.BroadcastEnvelope, func(), error) {
	subscriberID := atomic.AddUint64(&b.sequence, 1)
	subscriber := make(chan model.BroadcastEnvelope, 128)

	b.mu.Lock()
	b.subscribers[subscriberID] = subscriber
	b.mu.Unlock()

	var once sync.Once
	cancel := func() {
		once.Do(func() {
			b.mu.Lock()
			delete(b.subscribers, subscriberID)
			close(subscriber)
			b.mu.Unlock()
		})
	}

	go func() {
		<-ctx.Done()
		cancel()
	}()

	return subscriber, cancel, nil
}

// snapshotSubscribers 返回订阅者快照，避免长时间持锁。
func (b *MemoryBroadcaster) snapshotSubscribers() []chan model.BroadcastEnvelope {
	b.mu.RLock()
	defer b.mu.RUnlock()

	result := make([]chan model.BroadcastEnvelope, 0, len(b.subscribers))
	for _, subscriber := range b.subscribers {
		result = append(result, subscriber)
	}
	return result
}
