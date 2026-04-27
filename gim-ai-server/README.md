# 🚀 智能协作一体化平台 (Smart-Collab-AI)

   本项目是一款集 **实时社交 (IM)**、**云端协作 (Drive)** 与 **多模态 AI (Intelligence)** 于一体的综合性平台。通过自研 AI 网关与高并发后端架构，实现从“内容产生”到“智能沉淀”的全链路打通。

------

## 🏗️ 核心架构与技术亮点

### 1. 统一 AI 服务网关 (AI-Gateway)

- **统一入口**：所有 AI 业务接口收口于网关层，支持统一权限校验、流量配额、日志审计。
- **多模型引擎适配**：利用 **工厂模式** 动态构建模型对象，完美兼容 ChatGPT, Gemini, DeepSeek, Kimi 等主流大模型，支持秒级切换。
- **高扩展性**：基于 **策略模式** 实现不同模型的 Prompt 差异化处理与响应格式化。

### 2. 多模态 AI 工作流 (Visual Workflow)

- **画布化操作**：支持通过可视化画布节点，编排复杂任务流。
- **全场景生成**：支持 **文生图、图生图、图生视频、文生视频**，满足多媒体创作需求。
- **异步解耦**：引入 **Kafka MQ** 对 AI 长耗时操作进行异步化处理，提升系统吞吐量。

### 3. 高并发与实时通信

- **多人音视频 (SFU 架构)**：基于 **Go 语言构建 SFU (Selective Forwarding Unit)** 转发服务器，通过 **WebRTC** 实现低延迟的多人在线视频通话。
- **高性能 IM**：Go 后台驱动的实时消息系统，支持万级消息并发。

### 4. 智能知识库与 RAG 增强

- **个人/企业知识库**：支持上传 PDF, Word, 图片等，自动进行文本抽取与向量化存入数据库。
- **RAG 强化对话**：对话时可动态挂载知识库，让 AI 具备业务“私有大脑”，大幅降低幻觉。

------

## 🛠️ 功能模块全景

| **模块**           | **核心功能**                       | **技术实现**                     |
| ------------------ | ---------------------------------- | -------------------------------- |
| **智能网盘**       | 文档存储、在线预览、权限控制       | SpringBoot + MySQL + OSS         |
| **AI 助手**        | 流式对话、多模型切换、附件解析     | LangChain4j + Spring AI          |
| **实时热榜**       | 聚合微博、抖音、知乎、网易热搜前十 | 爬虫引擎 + Redis 缓存 + 趋势分析 |
| **在线音视频通话** | 1v1、多人视频通话、屏幕共享        | Go + WebRTC                      |
| **工作流画布**     | 文/图/视频 跨模态生成流            | Vue-Flow / Vue/Axios             |
| **任务调度**       | XXL-Job                            | 分布式定时任务                   |
| **对象存储**       | Tencent COS                        | 图片/视频文件海量存储            |

## 📂 项目目录结构

```js
gim-chat/
├── src/main/java/org/gim/
│   ├── config/                   # 全局配置 (Sa-Token鉴权、MybatisPlus、限流器)
│   │   ├── batch/                # Xxl-job定时任务
│   │   └── limit/                # 自研限流：滑动窗口算法保障系统稳定性
│   ├── controller/               # 接口层
│   │   ├── agent/                # 知识库管理与向量文件关联
│   │   ├── ai/                   # AI 多模态生成 (文生视频、图像处理)
│   │   ├── canvas/               # AI 画布数据流转控制
│   │   └── file/                 # 网盘系统：上传下载、分片续传、回收站
│   ├── entity/                   # 数据模型 
│   ├── kafka/                    # 异步任务解耦中心
│   │   ├── config/               # 消费者配置 (消息分发处理)
│   │   ├── Handler/              # 任务处理器 
│   │   └── Sender/               # 消息生产者统一封装
│   ├── service/                  # 业务逻辑层
│   │   ├── knowledge/            # RAG 知识库核心逻辑 (文档解析、向量化)
│   │   └── file/                 # 文件存储与网盘业务实现
│   ├── thirdService/             # 第三方服务集成
│   │   ├── AiGateWay/            # 统一AI 网关：基于策略模式适配 ComfyUI, Sora 等
│   │   ├── aiServiceAPI/         # 模型工厂：LangChain4j 适配 OpenAI, Gemini, DeepSeek
│   │   ├── knowledgeService/     # 向量数据库交互 (Qdrant 存储与检索)
│   │   └── ws/                   # 实时通信：WebRTC 信令转发与 WebSocket 消息
│   └── utils/                    # 工具类 (Redis Lua 状态机脚本执行、Jwt、OCR)
├── src/main/resources/           # 配置资源
│   ├── ai-models-config.yml      # AI 模型动态参数配置
│   ├── application.yml           # 主配置文件
│   └── comfy_workflows/          # ComfyUI，可视化画布工作流 JSON 定义
├── pom.xml                       # Maven 项目依赖
└── README.md                     # 项目说明文档
```

## 🧪 技术栈清单

- **后端 (Java/Go)**: `SpringBoot`, `LangChain4j`, `Spring AI`, `Go (WebRTC/SFU)`
- **中间件**: `MySQL 8.0`, `Redis` , `Kafka`
- **前端**: `Vue.js`, `JavaScript`, `Canvas/Flow` 可视化组件
- **AI 接入**: OpenAI, Gemini, DeepSeek, Kimi

## Docker Compose

This repository now includes a root Docker setup for:

- `gim-ai-service`
- `gim-app-service`
- `gim-gateway-service`

Start all three services with:

```bash
docker compose up -d --build
```

Useful commands:

```bash
docker compose logs -f gim-gateway-service
docker compose down
```

Notes:

- The compose file only starts the three Java services.
- External dependencies are not included: `Nacos`, `MySQL`, `Redis`, `RocketMQ`, `MinIO`, and `Qdrant`.
- Override dependency addresses and credentials by copying `.env.example` to `.env`.
- `gim-app-service` persists runtime files under `./docker-data/app-file`, `./docker-data/ai-video`, and `./docker-data/qdrant-cache`.
