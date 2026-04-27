package hub

import (
	"encoding/json"
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"go-ws-server/internal/chat/model"

	"github.com/gorilla/websocket"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 64 * 1024
	sendBufferSize = 64
)

var connectionSequence uint64

// Client 表示单个浏览器标签页建立的一条 WebSocket 连接。
type Client struct {
	connectionID string
	userID       string
	conn         *websocket.Conn
	manager      *Manager
	send         chan []byte
	initialized  bool
	closed       bool
	mu           sync.RWMutex
}

// newClient 为单个用户创建站内信 WebSocket 会话对象。
func newClient(userID string, conn *websocket.Conn, manager *Manager) *Client {
	return &Client{
		connectionID: generateConnectionID(userID),
		userID:       userID,
		conn:         conn,
		manager:      manager,
		send:         make(chan []byte, sendBufferSize),
	}
}

// readPump 负责接收浏览器发来的命令，并在断连时触发清理。
func (c *Client) readPump() {
	defer c.manager.unregister(c)

	c.conn.SetReadLimit(maxMessageSize)
	c.conn.SetReadDeadline(time.Now().Add(pongWait))
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, payload, err := c.conn.ReadMessage()
		if err != nil {
			return
		}
		c.manager.handleClientMessage(c, payload)
	}
}

// writePump 负责事件推送和心跳保活。
func (c *Client) writePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		_ = c.conn.Close()
	}()

	for {
		select {
		case payload, ok := <-c.send:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				_ = c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			writer, err := c.conn.NextWriter(websocket.TextMessage)
			if err != nil {
				return
			}
			if _, err := writer.Write(payload); err != nil {
				_ = writer.Close()
				return
			}
			if err := writer.Close(); err != nil {
				return
			}

		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// push 将服务端事件序列化后放入发送队列。
func (c *Client) push(event model.ServerEvent) error {
	payload, err := json.Marshal(event)
	if err != nil {
		return err
	}

	c.mu.RLock()
	defer c.mu.RUnlock()
	if c.closed {
		return websocket.ErrCloseSent
	}

	select {
	case c.send <- payload:
		return nil
	default:
		return websocket.ErrCloseSent
	}
}

// markInitialized 标记当前连接已经完成 session.init。
func (c *Client) markInitialized() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.initialized = true
}

// isInitialized 判断当前连接是否已经完成 session.init。
func (c *Client) isInitialized() bool {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.initialized
}

// close 安全关闭当前客户端发送通道，避免重复 close 导致 panic。
func (c *Client) close() {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.closed {
		return
	}
	c.closed = true
	close(c.send)
}

// generateConnectionID 为每条连接生成唯一 ID，便于同一用户多标签页并存。
func generateConnectionID(userID string) string {
	sequence := atomic.AddUint64(&connectionSequence, 1)
	return fmt.Sprintf("%s-%d-%d", userID, time.Now().UnixNano(), sequence)
}
