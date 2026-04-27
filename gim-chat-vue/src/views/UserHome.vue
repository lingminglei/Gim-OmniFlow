<template>
  <div class="user-dashboard-wrapper">
    <header class="dashboard-header user-info-card">
      <div class="user-avatar">
        <img :src="userInfo.avatarUrl" style="width:82px;height:82px;" alt="用户头像" />
      </div>
      <div class="user-details">
        <h1 class="user-name">✨ {{ userInfo.userName }}</h1>
        <p class="user-intro">{{ userInfo.userIntroduction }}</p>
        <div class="meta-info">
          <span class="meta-item">🌐 IP: {{ userInfo.serverIp }}</span>
          <span class="meta-item">⏱️ 最近登录: {{ userInfo.lastOnlineTime }}</span>
          <span class="meta-item">🗓️ 注册天数: {{ userInfo.datesCnt }} 天</span>
        </div>
      </div>
    </header>

    <div class="stat-grid">
      <div v-for="stat in stats" :key="stat.title" class="stat-card" :style="{ '--bg-color': stat.bgColor }">
        <button v-if="stat.title === '当前余额'" class="recharge-btn" @click="handleRecharge">充值</button>

        <span class="stat-icon">{{ stat.icon }}</span>
        <div class="stat-info">
          <p class="stat-title">{{ stat.title }}</p>
          <h2 class="stat-value">{{ stat.value }}{{ stat.unit }}</h2>
        </div>
      </div>
    </div>

    <div class="content-split-grid">
      <div class="announcement-section">
        <div class="section-header">
          <h3>📢 系统公告</h3>
        </div>

        <div class="announcement-list">
          <div v-for="item in announcements" :key="item.id" class="announcement-item">
            <span :class="['status-dot', `status-${item.status}`]"></span>
            <div class="announcement-content">
              <p class="text">{{ item.text }}</p>
              <span class="date">{{ item.date }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="side-panel">
        <div class="model-section">
          <div class="section-header">
            <h3>🧠 接入模型</h3>
            <button class="small-tag" @click="viewModelDetails">更多模型</button>
          </div>

          <div class="model-grid">
            <div v-for="model in models" :key="model.name" class="model-card">
              <div class="model-card__icon">
                <span class="model-icon">{{ model.icon }}</span>
              </div>
              <div class="model-card__content">
                <h4 class="model-name">{{ model.name }}</h4>
                <p class="model-advantage">{{ model.advantage }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {getUserRanksInfo} from '@/api/user'
export default {
  name: 'UserDashboard',

  data() {
    return {
      // 用户信息
      userInfo: {
        avatarUrl: 'https://mjzjcdn.heycross.com/240601/u/ddwo6zwjgu80/b59855eb-9b0c-4291-9be2-56237c23b85f.jpg',
        userName: 'AI_PowerUser_V5',
        userIntroduction: '一名热爱数据分析和知识管理的忠实用户。专注于提升效率。',
        serverIp: '192.168.1.100',
        lastOnlineTime: new Date().toLocaleString(),
        datesCnt: 480,
      },
      stats: [
        // 账户数据
        { title: '当前余额', value: '140', icon: '💰', bgColor: '#166534',unit: 'Pts' },
        { title: '历史消耗', value: '$17.16', icon: '📊', bgColor: '#1e3a8a' },

        // 使用统计
        { title: '请求次数', value: '1792', icon: '🚀', bgColor: '#92400e' },
        { title: '统计次数', value: '2', icon: '⚡', bgColor: '#991b1b' },

        // 资源消耗
        { title: '统计额度', value: '$0.01', icon: '💎', bgColor: '#86198f' },
        { title: '统计Tokens', value: '1,357', icon: '🧩', bgColor: '#0f766e' },

        // 性能指标
        { title: '平均RPM', value: '0.001', icon: '⏱️', bgColor: '#374151' },
        { title: '平均TPM', value: '0.905', icon: '📈', bgColor: '#a16207' },
      ],
      // 模型数据 (新增 type 和 typeText 字段)
      models: [
        {
          name: 'GPT-3.5 Turbo',
          icon: '💡',
          type: 'llm',
          typeText: '大语言模型',
          advantage: '通用语言理解和代码生成，响应速度快，成本效益高。',
        },
        {
          name: 'LLaMA-3 8B',
          icon: '🧠',
          type: 'embedding',
          typeText: '知识库向量',
          advantage: '专为本地知识库检索优化，保障数据隐私和安全性。',
        },
        {
          name: 'DALL·E 3',
          icon: '🎨',
          type: 'image',
          typeText: '图像生成',
          advantage: '高质量图像生成和编辑，适合设计和创意工作流。',
        },
        {
          name: 'BERT Base',
          icon: '🔍',
          type: 'search',
          typeText: '语义检索',
          advantage: '深度理解用户查询意图，提供高度相关的搜索结果和问答服务。',
        },
        {
          name: 'Stable Diffusion XL',
          icon: '🖼️',
          type: 'image',
          typeText: '图像生成',
          advantage: '开源且灵活的图像生成模型，支持精细化风格定制和训练。',
        },
        {
          name: 'Claude 3.7 Sonnet',
          icon: '🪄',
          type: 'llm',
          typeText: '推理助手',
          advantage: '兼顾长文本推理与指令跟随，适合复杂问答、文案润色和结构化分析。',
        },
        {
          name: 'Gemini 1.5 Pro',
          icon: '🌌',
          type: 'llm',
          typeText: '多模态理解',
          advantage: '支持大上下文输入与多模态内容理解，适合长文档总结和跨媒体推理。',
        },
        {
          name: 'DeepSeek-V3',
          icon: '⚙️',
          type: 'llm',
          typeText: '高性价比',
          advantage: '在通用任务与代码任务之间取得平衡，响应稳定，适合高频业务调用。',
        },
        {
          name: 'Whisper Large V3',
          icon: '🎙️',
          type: 'audio',
          typeText: '语音识别',
          advantage: '高精度语音转写与多语言识别表现稳定，适合会议纪要和音频整理。',
        },
        {
          name: 'Qwen2.5-72B',
          icon: '🧭',
          type: 'llm',
          typeText: '知识推理',
          advantage: '适用于中文复杂问答、知识整理与工作流协作场景，输出更贴近业务语境。',
        }
      ],

      // 操作日志数据 (初始)
      activityLogs: [
        { time: '11:00 AM', message: '使用 **Claude 3 Opus** 进行了 15 次复杂文本总结。' }, // 新增
        { time: '10:45 AM', message: '成功创建了新的知识库：**【营销创意素材】**。' }, // 新增
        { time: '10:30 AM', message: '在【项目文档库】新增了 5 份文件。' },
        { time: '10:15 AM', message: '更新了个人介绍和头像。' },
        { time: '9:45 AM', message: '分享了知识库【技术笔记】给同事。' },
        { time: '9:30 AM', message: '修改了知识库【技术笔记】的访问权限，设置为只读。' }, // 新增
        { time: '9:00 AM', message: '完成了本周活跃度任务。' },
        { time: '8:30 AM', message: '使用 **DALL·E 3** 生成了 3 张海报草稿。' },
        { time: '8:00 AM', message: '登录时使用了 **二步验证**。' }, // 新增
      ],

      // 日志加载状态 (用于支持多数据加载)
      logLoading: false,
      hasMoreLogs: true,
      announcements: [
        { id: 1, status: 'success', text: '新功能发布：接入 Claude 3.5 接口...', date: '2026-03-10' },
        { id: 2, status: 'warning', text: '维护预告：3月15日凌晨进行例行升级...', date: '2026-03-09' },
        { id: 3, status: 'success', text: '充值限时优惠：活动开启...', date: '2026-03-08' },
        { id: 4, status: 'error', text: '安全警示：请及时更换 API Key...', date: '2026-03-07' },
        { id: 5, status: 'success', text: '知识库功能已全面升级...', date: '2026-03-05' },
        {
          id: 6,
          status: 'success',
          text: '【性能提升】后端检索架构完成重构，向量知识库检索速度提升约 40%，延时更低。',
          date: '2026-03-01 09:12'
        },
        {
          id: 7,
          status: 'error',
          text: '【服务异常】部分地区访问延迟较高，我们正在与 CDN 供应商协同进行节点路由优化。',
          date: '2026-02-28 22:30'
        },
        {
          id: 8,
          status: 'success',
          text: '【新增模型】已正式支持 DeepSeek-V3 接入，具备极高性价比，现已在模型配置中开放。',
          date: '2026-02-25 14:05'
        }
      ]
    };
  },

  created() {
    console.log('UserDashboard component created.');
  },

  mounted() {
    // 确保显示最新登录时间
    this.userInfo.lastOnlineTime = new Date().toLocaleString();
    console.log('UserDashboard component mounted.');

    this.getUserRanksInfo();
  },

  methods: {

    async getUserRanksInfo(){

      const resData = await getUserRanksInfo();

      console.log('sxxxxxxxxxxxxxxxxx')
      console.log('RESData',resData)

      if(resData.data.code === 200){
        this.userInfo = {...resData.data.data}

        this.stats[0].value = this.userInfo.balance
      }
      
    },
    handleRecharge() {
      this.$router.push({
        path: '/pay'
      });
    },
    viewModelDetails() {
      alert('跳转至全部模型配置页面...');
      // 实际应用中：this.$router.push('/settings/models');
    },

    /**
     * ❗ 新增方法: 查看完整日志记录
     */
    viewLogDetails() {
      alert('跳转至完整操作日志页面...');
      // 实际应用中：this.$router.push('/user/logs');
    },
    // 模拟异步加载更多日志
    loadMoreLogs() {
      if (this.logLoading || !this.hasMoreLogs) {
        return;
      }

      this.logLoading = true;
      console.log('开始加载更多日志...');

      // 模拟 API 请求延迟
      setTimeout(() => {
        const newLogs = [
          { time: '前天', message: '首次上传了【个人简历 V2.0】文件。' },
          { time: '前天', message: '将 **GPT-3.5 Turbo** 设置为默认模型。' },
          { time: '前天', message: '修改了密码。' },
        ];

        // 追加新数据
        this.activityLogs = [...this.activityLogs, ...newLogs];
        this.logLoading = false;

        // 模拟加载完所有数据
        this.hasMoreLogs = false;

        console.log('日志加载完成。');

      }, 1000); // 1秒延迟
    }
  },
};
</script>

<style scoped>
.user-avatar {
  width: 80px;
  height: 80px;
}

/* --- 基础配置 --- */
.user-dashboard-wrapper {
  padding: 40px 60px;
  background-color: #0f172a;
  color: #e2e8f0;
  font-family: 'Segoe UI', Roboto, Helvetica, sans-serif;
}

/* --- 头部和统计网格样式 --- */
.dashboard-header {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  gap: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
  margin-bottom: 40px;
}

.stat-card {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  /* 让内容在垂直方向拉开 */
  transition: transform 0.3s ease, border-color 0.3s ease;
  position: relative;
  overflow: hidden;
}

/* 添加一个底部发光条，取代原来的粗边框 */
.stat-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--bg-color), transparent);
  opacity: 0.5;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 255, 255, 0.2);
}

/* 顶部增加微弱的渐变光带，增强科技感 */
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, var(--bg-color), transparent);
}

.stat-icon {
  font-size: 20px;
  margin-bottom: 8px;
  display: block;
}

.stat-title {
  font-size: 12px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0;
}

.stat-value {
  font-size: 28px;
  /* 增大数值字号，占据视觉重点 */
  font-weight: 700;
  color: #f8fafc;
  margin: 8px 0 0 0;
  font-family: 'Inter', sans-serif;
  /* 使用更现代的数字字体 */
}

/* --- 内容分割网格 --- */
.content-split-grid {
  --dashboard-panel-height: clamp(420px, calc(var(--shell-available-height, 70vh) - 140px), 720px);
  display: grid;
  grid-template-columns: 2fr 1fr;
  /* 左侧公告占据2/3 */
  gap: 24px;
  align-items: stretch;
}

.announcement-section,
.model-section {
  background: #1e293b;
  border: 1px solid #334155;
  padding: 24px;
  display: flex;
  flex-direction: column;
  height: var(--dashboard-panel-height);
  min-height: 0;
  max-height: var(--dashboard-panel-height);
  overflow: hidden;
}

.model-section {
  border-radius: 16px;
}

.log-section {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);

  /* ❗ 设置相对定位，以便内部元素布局 */
  position: relative;
  min-height: 0;
  max-height: 600px;

  /* ❗ 使用 Flex 布局垂直排列内容 */
  display: flex;
  flex-direction: column;
}

.model-section h3,
.log-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #cbd5e1;
  margin-top: 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #334155;
  padding-bottom: 10px;
  flex-shrink: 0;
}

.model-card {
  display: flex;
  gap: 14px;
  align-items: start;
  min-height: 104px;
  background: linear-gradient(145deg, #1e293b, #0f172a);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  padding: 16px;
  position: relative;
  transition: all 0.3s ease;
}

/* 增加科技感光效 */
.model-card:hover {
  border: 1px solid rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.2);
}

.model-card__icon {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(59, 130, 246, 0.18), rgba(14, 165, 233, 0.1));
  border: 1px solid rgba(96, 165, 250, 0.2);
}

.model-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  min-width: 0;
}

.model-name {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
}

/* --- 优化后的操作日志样式 (时间轴风格) --- */
.activity-logs {
  list-style: none;
  padding: 0;
  margin: 0;
}

.activity-logs li {
  display: flex;
  align-items: flex-start;
  padding: 12px 0 12px 20px;
  position: relative;
}

/* 日志项之间的点状连接线 */
.activity-logs li:not(:last-child)::before {
  content: '';
  position: absolute;
  top: 0;
  left: 3px;
  height: 100%;
  width: 1px;
  background-color: #475569;
  z-index: 0;
}

/* 日志点的指示器 */
.log-indicator {
  position: absolute;
  left: 0;
  top: 18px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3b82f6;
  z-index: 1;
  border: 2px solid #1e293b;
}

.log-time {
  font-size: 13px;
  color: #94a3b8;
  width: 100px;
  flex-shrink: 0;
}

.log-message {
  font-size: 14px;
  color: #e2e8f0;
  line-height: 1.4;
  border-bottom: 1px dotted #334155;
  flex-grow: 1;
  padding-bottom: 12px;
}

.activity-logs li:last-child .log-message {
  border-bottom: none;
}

/* --- 加载更多按钮样式 --- */
.load-more-button {
  width: 100%;
  padding: 10px 15px;
  margin-top: 20px;
  background-color: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: background-color 0.2s, opacity 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
}

.load-more-button:hover:not(:disabled) {
  background-color: #2563eb;
}

.load-more-button:disabled {
  background-color: #475569;
  cursor: not-allowed;
  opacity: 0.7;
}

/* 加载动画 (Spinner) */
.loading-spinner {
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top: 3px solid #ffffff;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}


/* --- 新增的详情按钮样式 --- */
.detail-button {
  width: 100%;
  padding: 10px 15px;
  margin-top: 20px;
  /* 使用稍暗的颜色作为区分 */
  background-color: #475569;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: background-color 0.2s;
  text-align: center;
}

.detail-button:hover {
  background-color: #64748b;
}

/* 保持 load-more-button 和 detail-button 都有 100% 宽度 */
.load-more-button {
  /* ... (原样式不变) */
  margin-top: 20px;
  /* 确保它在日志列表下方有间距 */
}

/* 确保日志区在 load-more-button 下方有额外的间距来放置 detail-button */
.log-section .detail-button {
  margin-top: 10px;
  /* 调整与上方 "加载更多" 按钮的间距 */
}

.log-list-wrapper {
  /* ❗ 启用滚动功能 */
  overflow-y: auto;

  /* ❗ 自动填充可用高度 */
  flex-grow: 1;

  /* ❗ 设置一个最大高度，确保滚动条出现 */
  max-height: 500px;
  /* 根据实际需求调整此值 */

  /* ❗ 增加内边距以避免滚动条紧贴内容 */
  padding-right: 15px;
}

.log-list-wrapper::-webkit-scrollbar {
  width: 8px;
}

.log-list-wrapper::-webkit-scrollbar-thumb {
  background-color: #475569;
  border-radius: 4px;
}

.log-list-wrapper::-webkit-scrollbar-track {
  background-color: #1e293b;
}

.user-details {
  display: flex;
  /* 启用 Flex 布局 */
  flex-direction: column;
  /* 垂直排列子元素 */
  gap: 8px;
  /* 增加 h1, p, meta-info 之间的垂直间距 */
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
  /* 清除默认 margin */
}

.user-intro {
  font-size: 15px;
  color: #94a3b8;
  margin: 0 0 10px 0;
  /* 增加简介和下方元信息之间的距离 */
}

/* 2. 优化 meta-info 的布局和元素间距 */
.meta-info {
  /* ❗ 确保元信息项在一行内，并使用间距 */
  display: flex;
  flex-wrap: wrap;
  /* 防止在窄屏上溢出 */
  gap: 20px;
  /* ❗ 关键：增加项目之间的水平间距，使其不紧贴 */
  padding-top: 5px;
  /* 在元信息上方增加一点垂直间距 */
  border-top: 1px solid #334155;
  /* 添加一条分割线，区分简介和元信息 */
  padding-bottom: 5px;
}

.meta-item {
  font-size: 13px;
  color: #cbd5e1;
  /* 增加右侧间距以确保视觉分离，虽然有 gap，但有时辅助 margin 更有用 */
  /* margin-right: 20px; */
}

/* 充值按钮样式 */
.recharge-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  background: #16a34a;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.recharge-btn:hover {
  background: #15803d;
}

.announcement-section {
  border-radius: 12px;
}

.side-panel {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #334155;
  padding-bottom: 15px;
  flex-shrink: 0;
}

.announcement-item {
  display: flex;
  gap: 12px;
  padding: 20px 0;
  border-bottom: 1px solid #334155;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

/* 颜色定义 */
.status-success {
  background-color: #10b981;
}

.status-warning {
  background-color: #f59e0b;
}

.status-error {
  background-color: red;
}

.announcement-content .text {
  margin: 0 0 5px 0;
  line-height: 1.6;
}

.announcement-content .date {
  font-size: 12px;
  color: #64748b;
}

.small-tag {
  background: #334155;
  border: 1px solid #475569;
  color: #cbd5e1;
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 12px;
  cursor: pointer;
}

.announcement-list,
.model-grid {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  margin-top: 15px;
  padding-right: 12px;
  scrollbar-gutter: stable;
}

.model-grid {
  gap: 16px;
}

.announcement-list {
  gap: 0;
}

.announcement-list .announcement-item:first-child {
  padding-top: 0;
}

.announcement-list .announcement-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.model-icon {
  font-size: 28px;
  line-height: 1;
}

.model-name {
  font-size: 15px;
  font-weight: 600;
  color: #f8fafc;
  margin: 0;
}

.model-advantage {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  /* 限制两行 */
  -webkit-box-orient: vertical;
  overflow: hidden;
}


/* 滚动条美化 */
.announcement-list::-webkit-scrollbar {
  width: 6px;
}

.model-grid::-webkit-scrollbar {
  width: 6px;
}

.announcement-list::-webkit-scrollbar-thumb {
  background: #475569;
  border-radius: 3px;
}

.model-grid::-webkit-scrollbar-thumb {
  background: #475569;
  border-radius: 3px;
}

.announcement-list::-webkit-scrollbar-track,
.model-grid::-webkit-scrollbar-track {
  background: transparent;
}
</style>
