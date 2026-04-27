<template>
    <div class="toolkit-wrapper">
        <div class="toolkit-header">
            <div class="header-content">
                <h1>效能工具箱 <span class="badge">PRO</span></h1>
                <p class="subtitle">集成 AI 创作、流程管理与多媒体处理的一站式工作台</p>
            </div>

            <div class="search-box">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input type="text" v-model="searchQuery" placeholder="搜索工具..." />
            </div>
        </div>

        <div class="tools-grid">
            <div v-for="tool in filteredTools" :key="tool.id" class="tool-card" @click="navigateTo(tool)">
                <div class="card-glow"></div>

                <div class="card-header">
                    <div class="icon-wrapper" :style="{ background: tool.bgGradient }">
                        <span class="emoji">{{ tool.icon }}</span>
                    </div>
                    <div class="arrow-icon">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="2">
                            <path d="M5 12h14M12 5l7 7-7 7" />
                        </svg>
                    </div>
                </div>

                <div class="card-body">
                    <h3>{{ tool.name }}</h3>
                    <p>{{ tool.desc }}</p>
                </div>

                <div class="card-footer">
                    <span class="tag" v-for="tag in tool.tags" :key="tag">{{ tag }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'EfficiencyToolkit',
    data() {
        return {
            searchQuery: '',
            // 模拟工具数据
            tools: [
                {
                    id: 1,
                    name: '🎬 视界引擎', // 推荐名称
                    desc: '基于画布节点流程，智能生成结构化视频和动态演示。', // 推荐描述
                    icon: '✨', // 推荐图标：✨ (闪光) 或 🎬 (场记板)
                    bgGradient: 'linear-gradient(135deg, #a78bfa, #7c3aed)', // 使用紫色系渐变，与蓝色区分
                    route: '/aiVideoCava',
                    tags: ['AIGC', '视频渲染', '动态叙事']
                },
                {
                    id: 2, // 建议 ID 递增
                    name: '📝 AI 故事流', // 调整：突出故事策划和AI辅助
                    desc: '基于文本分镜头、剧本大纲或提示词，一键生成结构化视频脚本和预览。', // 调整：强调输入是“分镜头/剧本”
                    icon: '💡', // 调整：使用灯泡/想法图标，代表创意和策划
                    bgGradient: 'linear-gradient(135deg, #f472b6, #db2777)', // 使用粉色/红色系渐变，与蓝色/紫色区分
                    route: '/aiVideoCreate',
                    tags: ['AIGC', '剧本策划', '分镜脚本', '叙事'] // 调整：聚焦叙事和策划
                },
                {
                    id: 3,
                    name: '知识库管理',
                    desc: '集中管理您的多媒体素材、提示词模板与历史项目资源。',
                    icon: '🗂️',
                    bgGradient: 'linear-gradient(135deg, #fbbf24, #d97706)',
                    route: '/knowledge',
                    tags: ['资源', '存储']
                },
                {
                    id: 4,
                    name: '热榜信息',
                    desc: "聚合展示微博、抖音、知乎、网易等主流平台的实时热榜前十数据。提供快速浏览、趋势分析和历史热度对比功能，助您掌握舆情脉搏。",
                    icon: '📊',
                    bgGradient: "linear-gradient(135deg, #FF6B6B, #F8E81C)",
                    route: '/newList',
                    tags: ["热点", "排行榜", "趋势", "实时"],
                }
            ]
        }
    },
    computed: {
        filteredTools() {
            if (!this.searchQuery) return this.tools;
            const query = this.searchQuery.toLowerCase();
            return this.tools.filter(tool =>
                tool.name.toLowerCase().includes(query) ||
                tool.desc.toLowerCase().includes(query)
            );
        }
    },
    methods: {
        navigateTo(tool) {
            console.log(`跳转到工具: ${tool.name}`, tool.route);
            // 实际跳转逻辑，根据你的路由配置调整
            this.$router.push(tool.route);

        }
    }
}
</script>

<style scoped>
/* 基础容器：深色背景 + 网格纹理 */
.toolkit-wrapper {
    min-height: 0;
    width: 100%;
    background-color: #0f172a;
    background-size: 30px 30px;
    color: #e2e8f0;
    font-family: 'Segoe UI', Roboto, Helvetica, sans-serif;
    padding: 40px 60px;
    box-sizing: border-box;
}

/* 顶部 Header */
.toolkit-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 50px;
    padding-bottom: 20px;
    border-bottom: 1px solid rgba(51, 65, 85, 0.5);
}

.header-content h1 {
    font-size: 32px;
    font-weight: 700;
    margin: 0 0 8px 0;
    display: flex;
    align-items: center;
    gap: 12px;
    background: linear-gradient(to right, #fff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.badge {
    font-size: 12px;
    background: #3b82f6;
    color: white;
    padding: 2px 6px;
    border-radius: 4px;
    -webkit-text-fill-color: white;
    /* 重置渐变 */
    letter-spacing: 1px;
}

.subtitle {
    margin: 0;
    color: #94a3b8;
    font-size: 14px;
}

/* 搜索框 */
.search-box {
    position: relative;
    width: 300px;
}

.search-box svg {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: #64748b;
}

.search-box input {
    width: 100%;
    padding: 10px 10px 10px 40px;
    background: rgba(30, 41, 59, 0.8);
    border: 1px solid #334155;
    border-radius: 8px;
    color: white;
    font-size: 14px;
    outline: none;
    transition: all 0.3s;
}

.search-box input:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
    background: rgba(30, 41, 59, 1);
}

/* 网格布局 */
.tools-grid {
    display: grid;
    /* 响应式 Grid：最小宽度 320px，自动填充 */
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
}

/* 卡片样式 */
.tool-card {
    position: relative;
    background: #1e293b;
    /* Slate 800 */
    border: 1px solid #334155;
    border-radius: 16px;
    padding: 24px;
    cursor: pointer;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: column;
}

/* 悬停效果：上浮 + 边框高亮 + 阴影 */
.tool-card:hover {
    transform: translateY(-5px);
    border-color: #60a5fa;
    box-shadow:
        0 10px 30px -10px rgba(0, 0, 0, 0.5),
        0 0 0 1px rgba(96, 165, 250, 0.3);
    /* 模拟内发光 */
}

/* 卡片内部发光 (装饰) */
.card-glow {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle at top right,
            rgba(59, 130, 246, 0.1),
            transparent 40%);
    opacity: 0;
    transition: opacity 0.3s;
    pointer-events: none;
}

.tool-card:hover .card-glow {
    opacity: 1;
}

/* 卡片头部 */
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
}

.icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    transition: transform 0.3s;
}

.tool-card:hover .icon-wrapper {
    transform: scale(1.1) rotate(5deg);
}

.arrow-icon {
    color: #475569;
    transition: all 0.3s;
    opacity: 0;
    transform: translateX(-10px);
}

.tool-card:hover .arrow-icon {
    opacity: 1;
    color: #60a5fa;
    transform: translateX(0);
}

/* 卡片文本 */
.card-body h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    font-weight: 600;
    color: #f1f5f9;
}

.card-body p {
    margin: 0;
    font-size: 14px;
    color: #94a3b8;
    line-height: 1.5;
    /* 限制为2行 */
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* 底部标签 */
.card-footer {
    margin-top: auto;
    /* 推到底部 */
    padding-top: 20px;
    display: flex;
    gap: 8px;
}

.tag {
    font-size: 12px;
    color: #64748b;
    background: rgba(30, 41, 59, 0.5);
    border: 1px solid #334155;
    padding: 4px 10px;
    border-radius: 100px;
    transition: all 0.3s;
}

.tool-card:hover .tag {
    border-color: #475569;
    color: #cbd5e1;
    background: rgba(59, 130, 246, 0.1);
}
</style>
