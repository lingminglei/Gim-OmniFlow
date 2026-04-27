<template>
    <div class="container tech-container">
        <div class="header">
            <h2 class="neon-title">
                <el-button icon="el-icon-arrow-left" @click="goBack()" round></el-button> 
                AI 视频制作 <span class="sub-text"></span></h2>
            <p class="sub-text">通过AI技术，只需输入视频标题，即可快速生成高清画质视频，支持自定义片段，实时预览效果。</p>
        </div>

        <div class="content">
            <el-row :gutter="20">
                <el-col :span="7">
                    <el-card class="tech-card">
                        <div slot="header" class="clearfix">
                            <i class="el-icon-edit-outline tech-icon" /><span>提示词设置 </span>
                        </div>
                        <div class="form-container">
                            <el-form :model="formData" ref="form" label-width="100px" class="tech-form"
                                :label-position="labelPosition">
                                <el-form-item label="视频标题" prop="title">
                                    <el-input v-model="formData.title" placeholder="输入视频标题"
                                        class="tech-input"></el-input>
                                </el-form-item>
                                <el-form-item label="视频描述" prop="userPrompt">
                                    <el-input type="textarea" v-model="formData.userPrompt" placeholder="请输入视频描述"
                                        rows="4" class="tech-input"></el-input>
                                </el-form-item>
                                <el-form-item label="视频风格" prop="videoStyle">
                                    <el-select v-model="formData.videoStyle" placeholder="写实风格" style="width: 100%"
                                        class="tech-select">
                                        <el-option label="写实风格" value="写实风格"></el-option>
                                        <el-option label="卡通风格" value="卡通风格"></el-option>
                                        <el-option label="未来感风格" value="未来感风格"></el-option>
                                    </el-select>
                                </el-form-item>
                                <el-form-item label="视频长度" prop="videoLength">
                                    <el-select v-model="formData.videoLength" placeholder="短片 (10s)" style="width: 100%"
                                        class="tech-select">
                                        <el-option label="短片 (10s)" value="10"></el-option>
                                        <el-option label="中等长度 (15s)" value="15"></el-option>
                                        <el-option label="长片 (30s)" value="30"></el-option>
                                    </el-select>
                                </el-form-item>
                            </el-form>
                        </div>
                    </el-card>

                    <el-card class="tech-card" style="margin-top: 20px;">
                        <div slot="header" class="clearfix">
                            <i class="el-icon-paperclip tech-icon" /><span>文件上传</span>
                        </div>
                        <el-upload drag action="https://jsonplaceholder.typicode.com/posts/" multiple
                            class="tech-upload-area">
                            <div class="upload-content">
                                <i class="el-icon-upload"></i>
                                <div class="el-upload__text">将文件拖到此处，或<em class="neon-text-primary">点击上传</em></div>
                            </div>
                        </el-upload>
                    </el-card>

                    <el-card class="tech-card" style="margin-top: 20px;">
                        <div slot="header" class="clearfix">
                            <i class="el-icon-s-tools tech-icon" /><span>操作控制</span>
                        </div>
                        <div>
                            <el-button type="primary" class="tech-button primary-btn" @click.native="getVideoPrompt">
                                <i class="el-icon-cpu"></i> 生成提示词
                            </el-button>
                        </div>
                        <div><el-button class="tech-button primary-btn" @click.native="saveFormData">保存草稿</el-button>
                        </div>
                        <div><el-button class="tech-button primary-btn" @click.native="clearData">清空内容</el-button>
                        </div>
                        <div>
                            <el-button type="primary" class="tech-button primary-btn" @click.native="generateVideo">
                                <i class="el-icon-cpu"></i> 生成视频
                            </el-button>
                        </div>
                    </el-card>

                </el-col>

                <el-col :span="17">
                    <el-card class="tech-card">
                        <div slot="header" class="clearfix">
                            <i class="el-icon-view tech-icon" /><span>视频预览 </span>
                        </div>
                        <div class="loaded-time-display" v-if="videoLoading"
                            :class="{ 'is-loading': videoLoading, 'is-complete': !videoLoading && videoLoadedTime }">
                            <template>
                                ⏳视频数据加载中...已查询:{{ queryCnt }}次，耗时: <span class="time-value loading-text">{{
                                    videoElapsedTime }}</span>
                                <i class="el-icon-loading loading-icon-spin"></i>
                            </template>
                        </div>
                        <el-row>
                            <div class="preview-box" v-loading="loading" element-loading-text="拼命加载中"
                                element-loading-spinner="el-icon-loading"
                                element-loading-background="rgba(0, 0, 0, 0.8)">
                                <video-player ref="videoPlayer" :key="videoKey" class="video-player"
                                    :options="playerOptions" :playsinline="true" :preload="true" :autoplay="true"
                                    :loop="true" :muted="true"></video-player>
                            </div>
                        </el-row>

                    </el-card>

                    <el-card class="tech-card" style="margin-top: 20px;">
                        <div class="loaded-time-display"
                            :class="{ 'is-loading': loading, 'is-complete': !loading && loadedTime }" v-if="loading">
                            <template>
                                ⏳分片提示词数据加载中...耗时: <span class="time-value loading-text">{{ elapsedTime }}</span>
                                <i class="el-icon-loading loading-icon-spin"></i>
                            </template>
                        </div>

                        <div slot="header" class="clearfix">
                            <i class="el-icon-s-operation tech-icon" />分片信息<span></span>
                        </div>
                        <el-timeline class="tech-timeline" v-loading="loading" element-loading-text="拼命加载中"
                            element-loading-spinner="el-icon-loading" element-loading-background="rgba(0, 0, 0, 0.8)">
                            <el-timeline-item v-for="(item, index) in timelineData" :key="index"
                                :timestamp="getSegmentTimestamp(item)" placement="top">
                                <el-card class="timeline-card">
                                    <el-button type="text" class="copy-button" icon="el-icon-document-copy"
                                        @click="copySegmentData(item)">
                                        复制
                                    </el-button>
                                    <span class="segment-title">{{ item.segmentTitle }}</span>
                                    <p class="segment-description-long">{{ item.segmentDescription }}</p>

                                    <div class="segment-detail-divider"></div>

                                    <div class="segment-details-grid">
                                        <p class="segment-param">
                                            <span class="param-label">关键字:</span>
                                            <span class="param-value neon-text-accent">{{ item.segmentKeywords }}</span>
                                        </p>
                                        <p class="segment-param">
                                            <span class="param-label">风格:</span>
                                            <span class="param-value">{{ item.shardingStyle }}</span>
                                        </p>
                                        <p class="segment-param">
                                            <span class="param-label">构图:</span>
                                            <span class="param-value">{{ item.fragmentedComposition }}</span>
                                        </p>
                                        <p class="segment-param">
                                            <span class="param-label">光影:</span>
                                            <span class="param-value">{{ item.fragmentedLightAndShadow }}</span>
                                        </p>
                                        <p class="segment-param">
                                            <span class="param-label">运镜:</span>
                                            <span class="param-value">{{ item.fragmentedCameraMovement }}</span>
                                        </p>
                                    </div>
                                </el-card>
                            </el-timeline-item>
                        </el-timeline>
                    </el-card>

                    <el-card class="tech-card" style="margin-top: 20px;">
                        <div slot="header" class="clearfix">
                            <i class="el-icon-folder-checked tech-icon" /><span>最近完成 (RECENT LOG)</span>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>
    </div>
</template>

<script>
import { createPrompt, generatorVideo, queryVideoResult, generatorVideoPro } from '@/api/ai'
// JavaScript 逻辑保持不变
export default {
    data() {
        return {
            queryCnt: 0,//轮询函数执行次数
            loading: false,     // 控制 loading 状态
            loadedTime: '',     // 存储加载完成时间
            // 实时加载时长相关
            startTime: 0,       // 请求开始时的时间戳
            elapsedTime: '0.000s', // 实时显示加载了多久
            timerInterval: null, // 用于存储 setInterval 的引用

            videoLoading: false,
            videoLoadedTime: '',
            videoStartTime: 0,       // 请求开始时的时间戳
            videoElapsedTime: '0.000s', // 实时显示加载了多久
            videoTimerInterval: null,

            startTime: null,
            taskId: null,
            labelPosition: 'top',
            formData: {
                title: '美女跳舞',
                userPrompt: '美女跳舞',
                videoStyle: '写实风格',
                videoLength: '15'
            },
            videoKey: 0,  // 用来强制重新渲染视频播放器
            playerOptions: {
                techOrder: ['html5'],
                sources: [
                    {
                        type: 'video/mp4',
                        // src: 'https://vjs.zencdn.net/v/oceans.mp4',  // 视频源
                        // src: 'https://tempfile.aiquickdraw.com/f/aaebe9df5e951608796487abbaebd7a9/4481152c-38db-465e-87ab-ebb45dfe36cc.mp4'
                        // src: 'https://tempfile.aiquickdraw.com/r/5407877d-1beb-450f-9f98-d250134c32fa_watermarked.mp4'
                        // src: 'http://127.0.0.1:9876/upload/20251201/8a0a4fb8-30a2-4249-a1e8-f26f80bab4fe.mp4',
                        src: 'http://127.0.0.1:9876/upload/20251203/32e1f79b588ddb5b8f600054b5a322b5.mp4'

                    },
                ],
                // poster: 'https://vjs.zencdn.net/v/oceans.png',  // 预览图
                controls: true,
                controlBar: {
                    children: ['playToggle', 'volumePanel', 'timeDivider', 'progressControl', 'fullscreenToggle'],
                    align: 'center'
                },
            },
            // 模拟的时间线数据
            // 更新后的分片信息数据
            timelineData: [
                {
                    segmentTitle: '片段 1',
                    beginTime: '0', // 模拟 API 返回的字符串秒数
                    endTime: '5',   // 模拟 API 返回的字符串秒数
                    segmentDescription: '分片描述信息',
                    segmentKeywords: '分片关键字1,分片关键字2',
                    shardingStyle: '超现实写实风格', // 新增字段
                    fragmentedComposition: '分片构图信息', // 新增字段
                    fragmentedLightAndShadow: '分片光影信息', // 新增字段
                    fragmentedCameraMovement: '分片运镜信息', // 新增字段
                },
            ]
        };
    },
    methods: {
        // 返回上一步
        goBack() {
            this.$router.push('/group');
        },
        startVideoTimer() {
            console.log('startVideoTimer===')
            this.clearTimer();
            this.videoStartTime = Date.now();
            this.videoTimerInterval = setInterval(() => {
                if (this.videoLoading) {
                    const duration = Date.now() - this.videoStartTime;
                    this.videoElapsedTime = (duration / 1000).toFixed(3) + 's';
                } else {
                    this.clearTimer();
                }
            }, 100);
        },
        /**
         * 生成视频
         */
        async generateVideo() {

            try {
                this.videoLoading = true;
                this.videoElapsedTime = '0.000s';
                this.videoLoadedTime = '';
                this.queryCnt = 1

                this.startVideoTimer();
                const param = {
                    segmentInfoList: this.timelineData
                }


                const respData = await generatorVideoPro(param);

                console.log('respData', respData)

                if (respData.data.code == 200 && respData.data.data) {

                    this.taskId = respData.data.data;

                    console.log('this.taskId =', this.taskId)

                    // 启动定时任务，每隔 30 秒查询一次视频状态
                    this.startPollingTask();

                    this.$message({
                        message: '操作成功！视频生成任务已开始！',
                        type: 'success',
                        duration: 3000
                    });
                } else {
                    this.$message({
                        message: '操作失败，请稍后进行重试！',
                        type: 'error',
                        duration: 3000
                    });
                }

            } catch (error) {
                console.error('Error in getVideoPrompt:', error);
                this.$message({
                    message: `❌ 视频生成失败！错误信息: ${error.message || '未知错误'}`,
                    type: 'error',
                    duration: 5000
                });
            }
        },
        /**
         * 启动轮询任务
         */
        startPollingTask() {
            const that = this
            console.log('taskId=begin', that.taskId)
            // 设置轮询间隔（30秒）
            this.taskInterval = setInterval(async () => {
                console.log('taskId=end', that.taskId)
                this.queryCnt = this.queryCnt + 1
                const param = {
                    taskId: that.taskId
                }
                console.log('param=', param)
                const respData = await queryVideoResult(param);

                console.log('[startPollingTask]respData=', respData)

                if (respData.data.code == 200 && respData.data.data) {

                    const resDatas = respData.data.data

                    if (resDatas.result === '-1') {

                        this.$message.error(resDatas.message)

                        clearInterval(this.taskInterval);  // 停止定时任务

                    } else if (resDatas.result === '1') {
                        console.log('视频url:', resDatas.filePath)

                        this.videoKey += 1;  // 改变 key，强制重新渲染视频播放器
                        this.$set(this.playerOptions.sources, 0, {
                            type: 'video/mp4',
                            src: resDatas.filePath
                        });

                        console.log('图片视频url:', this.playerOptions.sources[0].src)

                        clearInterval(this.taskInterval);  // 停止定时任务

                        this.$message({
                            message: '视频生成完成！',
                            type: 'success',
                            duration: 3000
                        });
                    }



                }
                // else {
                //   const resState = respData.message

                //   if (resState === "2") {
                //     // 如果任务失败，停止轮询
                //     clearInterval(this.taskInterval);
                //     this.$message.error("任务生成失败")
                //   } else if (resState === "3") {
                //     // 如果任务失败，停止轮询
                //     clearInterval(this.taskInterval);
                //     log.info("操作失败，任务创建成功但生成失败")
                //     this.$message.error("任务创建成功但生成失败")
                //   } else if (resState === "-1") {
                //     // 如果任务失败，停止轮询
                //     clearInterval(this.taskInterval);
                //     log.info("操作失败，请稍后重试！")
                //     this.$message.error("操作失败，请稍后重试！")
                //   }
                // }

                // 超过10分钟仍没有结果，停止轮询
                if (!this.startTime) {
                    this.startTime = new Date().getTime();
                }
                const currentTime = new Date().getTime();
                if (currentTime - this.startTime > 10 * 60 * 1000) {  // 10分钟
                    clearInterval(this.taskInterval);
                    this.$message({
                        message: '操作失败，视频生成超时，已停止查询结果。',
                        type: 'error',
                        duration: 3000
                    });
                }
            }, 1000 * 8);
        },

        /**
         * 停止轮询任务
         */
        stopPollingTask() {
            if (this.taskInterval) {
                clearInterval(this.taskInterval);

                this.clearTimer()

                this.videoLoading = false;
            }
        },
        /**
         * 保存表单数据到浏览器的 localStorage
         */
        saveFormData() {
            const token = this.$store.getters['user/token'];
            localStorage.setItem('formData=' + token, JSON.stringify(this.formData));
            this.$message({
                message: '数据已保存！',
                type: 'success',
                duration: 3000
            });
        },
        /**
         * 加载保存的表单数据
         */
        loadFormData() {
            // 从 localStorage 获取保存的表单数据
            const token = this.$store.getters['user/token'];
            const savedData = localStorage.getItem('formData=' + token);
            if (savedData) {
                this.formData = JSON.parse(savedData);  // 解析并加载数据
            }
        },
        /**
         * 清空内容
         */
        clearData() {
            this.formData = {
                title: '',
                userPrompt: '',
                videoStyle: '',
                videoLength: ''
            }

            const token = this.$store.getters['user/token'];
            localStorage.removeItem('formData=' + token);
        },
        // 清除计时器
        clearTimer() {
            if (this.timerInterval) {
                clearInterval(this.timerInterval);
                this.timerInterval = null;
            }

            if (this.videoTimerInterval) {
                clearInterval(this.videoTimerInterval);
                this.videoTimerInterval = null;
            }
        },

        // 实时更新已加载时间
        startTimer() {
            this.clearTimer(); // 先清除旧的计时器
            this.startTime = Date.now(); // 记录开始时间戳

            this.timerInterval = setInterval(() => {
                if (this.loading) {
                    const duration = Date.now() - this.startTime;
                    // 将毫秒转换为秒，并保留三位小数
                    this.elapsedTime = (duration / 1000).toFixed(3) + 's';
                } else {
                    // 如果 loading 结束了，就停止计时器
                    this.clearTimer();
                }
            }, 100); // 每 100 毫秒更新一次
        },
        /**
         * 获取AI分片提示词
         */
        async getVideoPrompt() {
            console.log('getVideoPrompt')

            // 1. 设置 loading 状态和开始计时
            this.loading = true;
            this.elapsedTime = '0.000s';
            this.loadedTime = '';
            // 假设 startTimer, clearTimer, setLoadedTime 方法已定义在 methods 中
            this.startTimer();


            let param = {
                ...this.formData
            }

            try {
                const resData = await createPrompt(param);

                console.log('resData=', resData)

                if (resData.data.code == 200 && resData.data.data && resData.data.data.length > 0) {

                    this.timelineData = resData.data.data

                    this.$message({
                        message: '提示词已生成，分片信息已更新！',
                        type: 'success',
                        duration: 3000
                    });

                } else {
                    this.$message({
                        message: '提示词生成失败或数据为空。',
                        type: 'error',
                        duration: 3000
                    });
                }
            } catch (error) {
                console.error('Error in getVideoPrompt:', error);
                this.$message({
                    message: `❌ 提示词生成失败！错误信息: ${error.message || '未知错误'}`,
                    type: 'error',
                    duration: 5000
                });
            } finally {
                // 3. 无论成功或失败，都在 finally 中停止 loading 状态
                this.loading = false;
                // clearTimer 会在 loading 变为 false 时自动在 startTimer 内部的 setInterval 中执行
                console.log('getVideoPrompt: Request finished');
            }



        },
        /**
        * 复制指定分片的数据到剪贴板
        * @param {Object} item - 分片数据对象
        */
        copySegmentData(item) {
            // 1. 格式化文本
            const copyText = [
                `分片标题：${item.segmentTitle}`,
                `详细的画面描述：${item.segmentDescription}`,
                `关键字：${item.segmentKeywords}`,
                `风格：${item.shardingStyle}`,
                `构图描述：${item.fragmentedComposition}`,
                `光影描述：${item.fragmentedLightAndShadow}`,
                `运镜描述：${item.fragmentedCameraMovement}`,
            ].join('\n'); // 使用换行符分隔

            // 2. 复制到剪贴板
            if (navigator.clipboard) {
                navigator.clipboard.writeText(copyText).then(() => {
                    // 3. 复制成功提示
                    this.$message({
                        message: '分片信息已成功复制到剪贴板！',
                        type: 'success',
                        duration: 2000
                    });
                }).catch(err => {
                    // 复制失败提示
                    this.$message({
                        message: `复制失败：${err}`,
                        type: 'error',
                        duration: 3000
                    });
                });
            } else {
                // 兼容性提示（旧版浏览器）
                this.$message({
                    message: '您的浏览器不支持 navigator.clipboard，请手动复制。',
                    type: 'warning',
                    duration: 4000
                });
            }
        },
        /**
         * 将秒数转换为 MM:SS 格式
         * @param {string | number} totalSeconds - 总秒数
         * @returns {string} 格式化后的时间字符串 (MM:SS)
        */
        formatSecondsToTime(totalSeconds) {
            const seconds = parseInt(totalSeconds);
            if (isNaN(seconds) || seconds < 0) {
                return '00:00';
            }

            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = seconds % 60;

            // 确保是两位数
            const formattedMinutes = String(minutes).padStart(2, '0');
            const formattedSeconds = String(remainingSeconds).padStart(2, '0');

            return `${formattedMinutes}:${formattedSeconds}`;
        },

        /**
         * 根据分片的起始和结束时间，生成时间戳字符串
         * @param {Object} item - 分片数据对象 (包含 beginTime 和 endTime)
         * @returns {string} 格式化的时间范围字符串 (MM:SS - MM:SS)
         */
        getSegmentTimestamp(item) {
            const startTime = this.formatSecondsToTime(item.beginTime || 0);
            const endTime = this.formatSecondsToTime(item.endTime || 0);
            return `${startTime} - ${endTime}`;
        },
    },
    mounted() {
        this.loadFormData()
    },
    beforeDestroy() {
        // 在组件销毁时清除定时任务，避免内存泄漏
        this.stopPollingTask();

        this.clearTimer();
    }
};
</script>

<style scoped lang="less">
.upload-content {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.loaded-time-display {
    padding: 10px 15px;
    border-radius: 4px;
    margin-bottom: 20px;
    font-size: 14px;
    text-align: center;
    font-weight: bold;
}

/* 正在加载时的样式 */
.loaded-time-display.is-loading {
    color: #909399;
}

/* 加载完成时的样式 */
.loaded-time-display.is-complete {
    color: #67c23a;
}

.time-value {
    font-family: 'Consolas', 'Monospace';
    /* 使用等宽字体，更像计时器 */
    font-size: 1.1em;
    padding: 0 5px;
}

.loading-text {
    color: #409eff;
    /* Primary color for loading time */
}

.complete-text {
    color: #67c23a;
    /* Success color for final time */
}


/* ================= 赛博朋克主题变量 ================= */
@bg-dark: #0f1219;
@bg-panel: rgba(26, 31, 44, 0.7);
@primary-color: #00f2ff;
/* Cyber Cyan */
@accent-color: #7d2ae8;
/* Neon Purple */
@text-main: #e2e8f0;
@text-sub: #94a3b8;
@border-color: rgba(255, 255, 255, 0.08);

/* ================= 全局容器和字体 ================= */
.tech-container {
    background-color: @bg-dark;
    color: @text-main;
    min-height: 0;
    padding: 30px;
    font-family: 'Inter', 'Courier New', monospace;
}

/* ================= 头部样式 ================= */
.header {
    padding-bottom: 20px;
    border-bottom: 1px solid @border-color;
    margin-bottom: 20px;
}

.neon-title {
    color: @primary-color;
    text-shadow: 0 0 15px rgba(0, 242, 255, 0.6);
    font-size: 32px;
    font-weight: 800;
    letter-spacing: 2px;

    .sub-text {
        font-size: 16px;
        color: @text-sub;
        margin-left: 10px;
        font-weight: normal;
    }
}

.sub-text {
    color: @text-sub;
    font-size: 14px;
}

/* ================= 卡片 (el-card) 样式重写 ================= */
.tech-card {
    background: @bg-panel !important;
    backdrop-filter: blur(10px);
    border: 1px solid @border-color;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
    color: @text-main;

    ::v-deep .el-card__header {
        border-bottom: 1px solid @border-color;
        padding: 18px 20px;
        color: @text-main;
        font-size: 16px;
    }

    span {
        font-weight: 600;
        color: @text-main;
        margin-left: 10px;
    }
}

.tech-icon {
    color: @primary-color !important;
    text-shadow: 0 0 8px rgba(0, 242, 255, 0.4);
    font-size: 20px;
}

/* ================= 表单 (Form) 样式重写 ================= */
.form-container {
    padding: 10px 0;
}

::v-deep .el-form-item__label {
    color: @text-sub !important;
    font-size: 13px;
    letter-spacing: 1px;
}

/* 输入框和文本域 */
.tech-input,
.tech-select {

    ::v-deep .el-input__inner,
    ::v-deep .el-textarea__inner {
        background-color: rgba(0, 0, 0, 0.3) !important;
        border: 1px solid @border-color !important;
        color: @text-main !important;
        border-radius: 4px;
        transition: all 0.3s;

        &::placeholder {
            color: rgba(255, 255, 255, 0.3);
        }

        &:focus {
            border-color: @primary-color !important;
            box-shadow: 0 0 8px rgba(0, 242, 255, 0.4);
        }
    }
}

/* Select 下拉菜单覆盖 */
::v-deep .el-select-dropdown {
    background-color: @bg-dark !important;
    border: 1px solid @border-color !important;

    .el-select-dropdown__item {
        color: @text-main;

        &:hover {
            background-color: rgba(0, 242, 255, 0.1) !important;
            color: @primary-color;
        }

        &.selected {
            color: @primary-color;
            font-weight: bold;
            background-color: rgba(0, 242, 255, 0.2) !important;
        }
    }
}

/* ================= 文件上传 (Upload) 样式重写 ================= */
.tech-upload-area {
    width: 100%;
    margin-top: 10px;

    ::v-deep .el-upload-dragger {
        background: rgba(0, 0, 0, 0.3) !important;
        border: 1px dashed @border-color !important;
        color: @text-sub;
        width: 100%;
        /* 撑满卡片 */
        height: 120px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        transition: all 0.3s;

        &:hover {
            border-color: @primary-color !important;
            box-shadow: 0 0 10px rgba(0, 242, 255, 0.3);
        }

        .el-icon-upload {
            color: @primary-color;
            font-size: 40px;
            margin-bottom: 10px;
        }
    }
}

.neon-text-primary {
    color: @primary-color;
    text-shadow: 0 0 5px rgba(0, 242, 255, 0.3);
}

.neon-text-accent {
    color: @accent-color;
    text-shadow: 0 0 5px rgba(125, 42, 232, 0.3);
}

/* ================= 按钮 (Button) 样式重写 ================= */
.tech-button {
    width: 100%;
    margin-top: 10px !important;
    border-radius: 4px;
    transition: all 0.3s;

    &.primary-btn {
        // 主按钮：霓虹渐变
        background: linear-gradient(90deg, @primary-color, @accent-color) !important;
        border: none !important;
        color: #fff !important;
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);

        &:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 20px rgba(0, 242, 255, 0.6);
        }
    }

    &.secondary-btn {
        // 次要按钮：透明边框科技感
        background-color: transparent !important;
        border: 1px solid @border-color !important;
        color: @text-sub !important;

        &:hover {
            border-color: @accent-color !important;
            color: @accent-color !important;
            background-color: rgba(125, 42, 232, 0.1) !important;
        }
    }
}

/* ================= 视频预览 (Video Preview) ================= */
.preview-box {
    margin-top: 20px;
    background: #000;
    border: 2px solid @primary-color;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 0 25px rgba(0, 242, 255, 0.4);
    /* 确保视频播放器适应容器 */
    width: 100%;
    height: 100%;

    // aspect-ratio: 16/9;
    /* 保持常见的视频比例 */
}

.video-player {
    width: 100%;
    height: 100%;
    object-fit: cover;

    // 覆盖 video.js 默认样式
    ::v-deep .video-js {
        font-family: 'Courier New', monospace;
    }

    ::v-deep .vjs-poster {
        background-color: #000;
        background-size: cover !important; // 铺满容器
        background-position: center center !important;
        background-repeat: no-repeat !important; // 禁止平铺多张
    }

    ::v-deep .vjs-control-bar {
        background-color: rgba(0, 0, 0, 0.8) !important;
    }
}


/* ================= 时间线 (Timeline) 样式重写 ================= */
.tech-timeline {
    padding: 20px 10px;

    ::v-deep .el-timeline-item {
        padding-bottom: 25px;
    }

    // 时间线节点
    ::v-deep .el-timeline-item__node {
        background-color: @primary-color;
        box-shadow: 0 0 8px @primary-color;
        width: 12px;
        height: 12px;
        border: 1px solid @bg-dark;
    }

    // 时间线连接线
    ::v-deep .el-timeline-item__tail {
        border-left: 1px dashed @border-color;
    }

    // 时间戳
    ::v-deep .el-timeline-item__timestamp {
        color: @text-sub;
        font-family: 'Consolas', monospace;
        font-size: 12px;
    }
}

.timeline-card {
    position: relative;
    /* 确保子元素可以相对于它定位 */
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;

    // 复制按钮的样式
    .copy-button {
        position: absolute;
        top: 10px;
        right: 10px;
        color: @accent-color !important;
        /* Neon Purple */
        font-size: 14px;
        font-weight: 600;
        z-index: 10;
        padding: 5px 10px;
        border-radius: 4px;
        background-color: rgba(125, 42, 232, 0.1);
        /* 轻微背景色 */
        transition: all 0.2s;

        &:hover {
            color: @primary-color !important;
            /* 悬停时切换为 Primary Neon Cyan */
            background-color: rgba(0, 242, 255, 0.15);
            text-shadow: 0 0 5px rgba(0, 242, 255, 0.5);
        }

        i {
            margin-right: 5px;
        }
    }

    // 核心标题
    .segment-title {
        font-weight: 800;
        font-size: 16px;
        color: @primary-color; // 突出显示标题
        text-shadow: 0 0 5px rgba(0, 242, 255, 0.2);
        display: block;
        margin-bottom: 8px;
        padding-bottom: 5px;
    }

    // 针对长文本的描述优化
    .segment-description-long {
        color: @text-main;
        font-size: 13px;
        line-height: 1.6; // 增加行高，提升长文本阅读舒适度
        margin: 0 0 15px 0; // 底部留出空间
        // 确保长文本不会溢出（虽然卡片本身会自适应，但防止意外）
        word-break: break-word;
    }

    // 分隔线，用于区分描述和详细参数
    .segment-detail-divider {
        height: 1px;
        background: linear-gradient(90deg, rgba(0, 242, 255, 0.3), rgba(0, 242, 255, 0), rgba(0, 242, 255, 0.3));
        margin: 10px 0 15px 0;
    }

    .segment-details-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr); // 两列布局
        gap: 8px 20px; // 行间距和列间距
        padding-top: 5px;
    }

    // 关键参数使用 Flex 或 Grid 布局
    .segment-param {
        font-size: 12px;
        color: @text-sub; // 参数标签颜色
        margin: 0; // 清除默认边距

        .param-label {
            font-weight: normal;
            margin-right: 5px;
        }

        .param-value {
            color: @text-main; // 参数值使用主文本色
            font-weight: bold;
            display: inline;
        }
    }

    // .segment-segmentKeywords span,
    // .segment-progress span {
    //   font-weight: bold;
    // }
}

@media (max-width: 1200px) {
    .preview-box {
        aspect-ratio: unset;
        height: 350px;
    }
}

@media (max-width: 992px) {
    .tech-container {
        padding: 10px;
    }

    .el-col {
        margin-bottom: 20px;
    }
}
</style>
