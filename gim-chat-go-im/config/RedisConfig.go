package config

import (
	"context"
	"log"
	"sync"
	"time"

	"github.com/redis/go-redis/v9"
)

var (
	redisClient *redis.Client
	ctx         = context.Background()
	RedisOnce   sync.Once
)

// Redis配置（可改为从 .env 或 yaml 文件读取）
const (
	redisAddr     = "47.120.60.61:6379"
	redisPassword = "123456"
	redisDB       = 0
)

// 初始化 Redis（仅执行一次）
func InitRedis() {
	log.Println("初始化 Redis...")
	RedisOnce.Do(func() {
		redisClient = redis.NewClient(&redis.Options{
			Addr:         redisAddr,
			Password:     redisPassword,
			DB:           redisDB,
			ReadTimeout:  5 * time.Second,
			WriteTimeout: 5 * time.Second,
		})

		if _, err := redisClient.Ping(ctx).Result(); err != nil {
			log.Fatalf("❌ Redis 连接失败: %v", err)
		}

		log.Println("✅ Redis 初始化成功")
	})
}

// 获取 Redis 客户端实例
func GetRedis() *redis.Client {
	if redisClient == nil {
		log.Fatal("Redis 未初始化，请先调用 InitRedis()")
	}
	return redisClient
}

// 获取上下文（统一）
func GetRedisCtx() context.Context {
	return ctx
}
