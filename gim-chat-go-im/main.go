package main

import (
	"go-ws-server/config"
	"go-ws-server/internal/chat/broadcast"
	"go-ws-server/internal/chat/hub"
	"go-ws-server/internal/chat/repository"
	chatservice "go-ws-server/internal/chat/service"

	"github.com/gin-gonic/gin"
)

func main() {
	// 初始化站内信模块依赖的 Redis 状态中心。
	config.InitRedis()

	// 站内信的消息、在线状态与广播能力全部收口到 Redis/内存实现。
	redisRepository := repository.NewRedisRepository()
	memoryBroadcaster := broadcast.NewMemoryBroadcaster()
	chatService := chatservice.NewChatService(redisRepository, redisRepository)
	manager := hub.NewManager(chatService, memoryBroadcaster)

	r := gin.Default()
	r.GET("/healthz", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok"})
	})

	// 站内信只保留新的专用 WebSocket 入口。
	r.GET("/chat/ws", manager.ServeWS)

	r.Run(":9999")
}
