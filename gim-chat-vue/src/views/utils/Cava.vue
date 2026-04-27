<template>
    <div class="ice-wrapper">
        <div class="ice-toolbar">
            <div class="btn-group">
                <el-button-group>
                    <el-button type="primary" icon="el-icon-arrow-left" @click="goBack()">返回</el-button>
                    <el-button @click="addNode('text')" style="margin-left: 10px;">添加节点</el-button>

                    <el-button @click="saveState()" style="margin-left: 10px;">保存草稿</el-button>

                    <el-button @click="clearData()" style="margin-left: 10px;">清空画布</el-button>

                    <el-button @click="saveCanvas()" icon="el-icon-upload" style="margin-left: 10px;">保存版本</el-button>

                    <el-dropdown split-button type="default" style="margin-left: 10px;">
                        历史版本
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item v-for="version in versionHistory" :key="version.id"
                                @click.native="openVersion(version.id)">
                                {{ version.name }}
                            </el-dropdown-item>
                            <el-dropdown-item v-if="versionHistory.length === 0" disabled>暂无历史版本</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </el-button-group>
            </div>
        </div>

        <div class="ice-canvas" ref="canvas" @mousedown="onCanvasMouseDown" @wheel.prevent="onWheel">
            <svg class="ice-edges-layer">
                <path v-for="edge in edges" :key="edge.id" :d="getEdgePath(edge)" fill="none" class="edge-background" />
                <path v-for="edge in edges" :key="'main-' + edge.id" :d="getEdgePath(edge)" fill="none"
                    class="edge-foreground" />
            </svg>
            <!-- :class="{ 'is-selected': selectedNodeId === node.id }"  -->
            <div v-for="node in nodes" :key="node.id" class="ice-node"
                :class="{ 'is-selected': selectedNodeIds.includes(node.id) }" :style="getNodeStyle(node)"
                @mousedown.stop="onNodeMouseDown(node, $event)">
                <!-- <div class="ice-node-header">
                    <div class="ice-node-title">{{ node.title || node.id }} -- {{node.status}}</div>
                    <div class="ice-node-actions">
                        <span title="切换为文本" @click.stop="changeNodeType(node, 'text')"
                            :class="{ active: node.contentType === 'text' }">📝</span>
                        <span title="切换为图片" @click.stop="changeNodeType(node, 'image')"
                            :class="{ active: node.contentType === 'image' }">🖼️</span>
                        <span title="切换为视频" @click.stop="changeNodeType(node, 'video')"
                            :class="{ active: node.contentType === 'video' }">🎬</span>
                        <span class="delete-btn" @click.stop="deleteNode(node.id)">✕</span>
                    </div>
                </div> -->

                <div v-if="node.contentType === 'text'" class="ice-node-content">
                    <div v-if="node.status === 'processing'" class="ice-node-loading-overlay">
                            <p>AI 任务处理中...</p>
                    </div>
                    <textarea v-else v-model="node.content" class="ice-node-textarea" placeholder="输入文本..." @mousedown.stop />
                </div>
                <div v-else class="ice-node-contentBody">

                    <div class="inSiderDiv">
                        <div v-if="node.status === 'processing'" class="ice-node-loading-overlay">
                            <p>AI 任务处理中...</p>
                        </div>

                        <textarea v-if="node.contentType === 'text'" v-model="node.content" class="ice-node-textarea"
                            placeholder="输入文本..." @mousedown.stop />

                        <div v-else-if="node.contentType === 'image'" class="ice-media-box">
                            <img v-if="node.src" :src="node.src" draggable="false" />
                            <div v-else class="placeholder">请输入图片 URL</div>
                            <input class="url-input" v-model="node.src" placeholder="图片地址 https://..."
                                @mousedown.stop />
                        </div>

                        <div v-else-if="node.contentType === 'video'" class="ice-media-box">
                            <video v-if="node.src" :src="node.src" controls muted></video>
                            <div v-else class="placeholder">请输入视频 URL</div>
                            <input class="url-input" v-model="node.src" placeholder="视频地址 mp4/webm..."
                                @mousedown.stop />
                        </div>
                    </div>


                </div>

                <!-- <div class="ice-node-footer">
                    <span class="tag">{{ node.contentType }}</span>
                    <button class="add-child-btn" @click.stop="openAddChildModal(node)">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="4">
                            <path d="M12 5v14M5 12h14" />
                        </svg>
                    </button>
                </div> -->

                <div class="ice-resize-handle" @mousedown.stop="onNodeResizeMouseDown(node, $event)"></div>
            </div>

            <div class="floating-toolbar">
                <el-button-group class="vertical-group">

                    <el-button :class="{ 'is-active': currentTool === 'select' }" @click="setTool('select')"
                        title="选择 (Select)">
                        <i class="el-icon-rank"></i> </el-button>

                    <el-button :class="{ 'is-active': currentTool === 'pan' }" @click="setTool('pan')" title="平移 (Pan)">
                        <i class="el-icon-hand-up"></i>
                    </el-button>

                    <el-button :class="{ 'is-active': currentTool === 'text' }" @click="setTool('text')"
                        title="文本 (Text)">
                        <i class="el-icon-edit-outline"></i>
                    </el-button>

                </el-button-group>

            </div>

            <!-- <div class="floating-oval-card">
                <div class="oval-content">
                    <i class="el-icon-magic-stick"></i>
                    <span>Sora 灵感探测器：准备就绪</span>
                    <el-button type="text" @click="isFloatingVisible = false">关闭</el-button>
                </div>
            </div> -->

            <div v-if="isLoading" class="sora-capsule-panel" :style="getModalStyle">
                <div class="capsule-inner">
                    <div class="type-selector-bar">
                        <div class="modal-type-switcher">
                            <button :class="{ active: newChildType === 'storyboardText' }"
                                @click="newChildType = 'storyboardText'">分镜 📝</button>
                            <button :class="{ active: newChildType === 'text' }" @click="newChildType = 'text'">文本
                                📝</button>
                            <button :class="{ active: newChildType === 'image' }" @click="newChildType = 'image'">图片
                                🖼️</button>
                            <button :class="{ active: newChildType === 'video' }" @click="newChildType = 'video'">视频
                                🎬</button>
                        </div>
                    </div>

                    <div class="input-section">
                        <textarea v-model="newChildText" class="capsule-textarea" placeholder="在此输入视频脚本或创意描述..."
                            rows="2"></textarea>
                    </div>

                    <div class="capsule-footer">
                        <button @click="isLoading = false" class="btn-cancel-text">取消</button>
                        <button @click="deleteNodes" class="btn-cancel-text">删除节点</button>
                        <button @click="confirmAddChild" class="btn-generate-shiny">
                            <span>立即生成</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div v-if="isModalVisible" class="modal-content-floating" :style="getModalStyle">
            <h3>为节点 "{{ pendingParentNode.title || pendingParentNode.id }}" 添加子节点</h3>
            <p>请选择类型并输入内容：</p>

            <div class="modal-type-switcher">
                <button :class="{ active: newChildType === 'storyboardText' }"
                    @click="newChildType = 'storyboardText'">分镜提示词 📝</button>

                <button :class="{ active: newChildType === 'text' }" @click="newChildType = 'text'">文本 📝</button>

                <button :class="{ active: newChildType === 'image' }" @click="newChildType = 'image'">图片 🖼️</button>

                <button :class="{ active: newChildType === 'video' }" @click="newChildType = 'video'">视频 🎬</button>
            </div>

            <el-form ref="form" :model="form" label-width="80px" :label-position="labelPosition">
                <el-form-item label="分片镜头数" v-if="newChildType === 'storyboardText'">
                    <el-input-number v-model="form.videoLength" @change="handleChange" :min="1" :max="10"
                        label="分片镜头数"></el-input-number>
                </el-form-item>
                <el-form-item label="模型名称">
                    <el-select v-model="form.modelType" placeholder="请选择模型名称">
                        <el-option v-for="item in modelOptions" :key="item.value" :label="item.label"
                            :value="item.value">
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="行文风格">
                    <el-select v-model="form.modelStyle" placeholder="请选择风格">
                        <el-option v-for="item in styleOptions" :key="item.value" :label="item.label"
                            :value="item.value">
                        </el-option>
                    </el-select>
                </el-form-item>
            </el-form>

            <textarea v-model="newChildText" placeholder="请输入提示词信息..." rows="4" autofocus></textarea>

            <div class="modal-actions">
                <button @click="closeModal" class="btn-cancel">取消</button>
                <button @click="confirmAddChild" class="btn-confirm">确认添加</button>
            </div>
        </div>

    </div>
</template>

<script>
import {
    generatorImage, queryResultTextToImage,
     generatorVideo, queryVideoResult2,
     generatorSlicePrompt
} from '@/api/ai'
import { saveCanvas } from '@/api/canvas'
let idCounter = 3; // 从 3 开始，因为初始 nodes 中 n1 和 n2 已占用
const genId = (prefix = 'n') => {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
};

export default {
    name: 'InfiniteCanvasMedia',
    data() {
        return {

            isLoading: false,

            currentTool: '',
            roleOptions: [
                { label: '产品经理', value: 'product_manager', icon: 'el-icon-s-custom' },
                { label: '前端开发', value: 'frontend_dev', icon: 'el-icon-monitor' },
                { label: 'UI 设计师', value: 'ui_designer', icon: 'el-icon-picture-outline' },
                { label: '后端工程师', value: 'backend_engineer', icon: 'el-icon-setting' },
                // ... 更多角色
            ],
            isSidebarOpen: true, // 控制侧边栏的显示/隐藏状态 (可选)
            versionHistory: [
                { id: 1, name: 'V1.0 - 初始布局', timestamp: '2025-12-15 10:00:00' },
                { id: 2, name: 'V1.1 - 添加了角色节点', timestamp: '2025-12-15 11:30:00' }
            ],
            selectedRole: '', // 存储当前选中的角色 value
            taskId: '',
            labelPosition: 'left',
            form: {
                modelType: '',
                modelStyle: '',
                videoLength: 0
            },
            styleOptions: [
                {
                    label: '写实风格',
                    value: '写实风格',
                }, {
                    label: '动漫风格',
                    value: '动漫风格',
                }
            ],
            //模型下拉框
            modelOptions: [
                {
                    value: 'gpt-4.1',
                    label: 'GPT-4.1（默认推荐）'
                },
                {
                    value: 'gpt-4o',
                    label: 'GPT-4o'
                }
            ],
            modelType: '',//模型选择
            //=============================
            // --- 多选 & 框选状态 (新增/修改) ---
            selectedNodeIds: [],         // 存储所有选中节点的 ID

            // --- 交互状态 (修改 groupDragOrigin) ---
            draggingNode: null,
            nodeDragStart: { x: 0, y: 0 },
            // 存储被拖拽组内所有节点的初始位置 { id: {x, y} }
            groupDragOrigin: {},
            //=============================
            // 悬浮输入框状态
            globalSearchText: 'pav', // 示例文本，模仿图片中的输入

            // 新增：Modal 状态管理
            isModalVisible: false,
            pendingParentNode: null, // 暂存的父节点对象
            newChildType: 'text',// 新子节点类型
            newChildText: '', // 新子节点文本内容
            newChildSrc: '',// 新子节点 URL (用于图片/视频)
            modalPos: { x: 0, y: 0 },// 弹框在**画布坐标系**中的位置 (新增加)

            // 视图变换
            offsetX: 0,
            offsetY: 0,
            scale: 1,

            // 交互状态
            isPanning: false,
            panStart: { x: 0, y: 0 },
            panOrigin: { x: 0, y: 0 },

            draggingNode: null,
            nodeDragStart: { x: 0, y: 0 },
            nodeOrigin: { x: 0, y: 0 },

            resizingNode: null,
            resizeStart: { x: 0, y: 0 },
            resizeOriginSize: { w: 0, h: 0 },

            selectedNodeId: null,

            // 数据模型
            nodes: [
                {
                    id: genId('n'),
                    x: 0, y: 0, width: 230, height: 180,
                    title: '欢迎使用',
                    contentType: 'text', // text | image | video
                    content: '初始节点',
                    src: '',
                    status: 'ready', // 新增: 节点状态 (ready, processing, success, failed)
                    timer: null       // 新增: 存储查询定时器 ID
                },
                {
                    id: genId('n'),
                    x: 300, y: 0, width: 230, height: 180,
                    title: '欢迎使用',
                    contentType: 'image', // text | image | video
                    content: '初始节点',
                    src: 'https://gips1.baidu.com/it/u=1658389554,617110073&fm=3028&app=3028&f=JPEG&fmt=auto?w=1280&h=960',
                    status: 'ready', // 新增: 节点状态 (ready, processing, success, failed)
                    timer: null       // 新增: 存储查询定时器 ID
                },
                
            ],
            edges: [],
            // 新增: 存储正在处理的 AI 任务的节点引用
            processingNode: null,
            // 新增: 存储查询定时器ID的Map
            queryTimers: new Map(),
        };
    },
    mounted() {
        // 绑定全局事件，防止鼠标拖出画布区域导致状态卡死
        window.addEventListener('mousemove', this.onWindowMouseMove);
        window.addEventListener('mouseup', this.onWindowMouseUp);
        // 增加点击画布外部关闭弹窗的监听 (重要)
        window.addEventListener('mousedown', this.onWindowMouseDownCloseModal);

        //组件加载时尝试加载本地数据
        this.loadState();
    },
    computed: {
        /**
         * 计算浮动弹框的样式 (屏幕坐标)
         */
        getModalStyle() {
            if (!this.isModalVisible) return {};
            const screenPos = this.worldToScreen(this.modalPos.x, this.modalPos.y);
            return {
                transform: `translate(${screenPos.x}px, ${screenPos.y}px)`,
            };
        }
    },
    beforeDestroy() {
        window.removeEventListener('mousemove', this.onWindowMouseMove);
        window.removeEventListener('mouseup', this.onWindowMouseUp);
        window.removeEventListener('mousedown', this.onWindowMouseDownCloseModal);

        this.nodes.forEach(node => {
            if (node.timer) {
                this.stopQueryTimer(node);
            }
        });
        console.log('组件销毁，所有 AI 任务定时器已停止。');
    },
    methods: {
        /**
         * 删除节点
         */
        deleteNodes() {

            if (this.selectedNodeIds.length <= 0) {
                this.$message.error('【删除节点操作】：所选节点不能为空！')
                return;
            }

            this.nodes = this.nodes
                .filter(node => !this.selectedNodeIds.includes(node.id)) // 筛选出选中的节点

        },
        saveCanvas() {
            console.log('保存画布数据...');
            // 1. 准备要保存的数据结构，去除不可序列化的 timer
            const serializableNodes = this.nodes.map(node => {
                // 使用解构赋值创建一个新对象，不包含 timer
                const { timer, ...rest } = node;
                return rest;
            });

            const state = {
                nodes: serializableNodes, // 使用去除 timer 的节点列表
                edges: this.edges,
                offsetX: this.offsetX,
                offsetY: this.offsetY,
                scale: this.scale,
            };

            const param = {
                projectName: "版本A",
                canvasStateJson: JSON.stringify(state)
            }

            saveCanvas(param).then(res => {
                if (res && res.data.code === 200 && res.data.data) {
                    this.$message.success('保存成功！');
                } else {
                    this.$message.error('保存失败！');
                }
            })

        },
        /**
         * 提示用户确认清空画布
         */
        clearData() {
            this.$confirm('此操作将清空所有节点、连线，并删除本地保存的数据，是否继续?', '警告', {
                confirmButtonText: '确定清空',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.clearCanvas();
                this.$message.success('画布已清空并删除本地数据。');
            }).catch(() => {
                this.$message.error('已取消清空操作。');
            });
        },
        /**
         * 清空画布数据和视图状态
         */
        clearCanvas() {
            // 1. 停止所有正在运行的 AI 任务定时器
            this.nodes.forEach(node => {
                if (node.timer) {
                    this.stopQueryTimer(node);
                }
            });

            // 2. 重置数据模型
            this.nodes = [];
            this.edges = [];
            this.selectedNodeId = null;
            this.queryTimers.clear();

            // 3. 重置视图状态
            this.offsetX = 0;
            this.offsetY = 0;
            this.scale = 1;

            // 4. 清除本地存储
            try {
                localStorage.removeItem('infiniteCanvasState');
            } catch (error) {
                console.error('Error removing state from localStorage:', error);
            }

            // 5. 重新添加一个初始节点，让画布不至于完全空白
            this.$nextTick(() => {
                this.nodes.push({
                    id: genId('n'),
                    x: 0, y: 0, width: 100, height: 200,
                    title: '欢迎使用',
                    contentType: 'text',
                    content: '初始节点',
                    src: '',
                    status: 'ready',
                    timer: null
                })
            });
        },
        /**
         * 将当前画布状态（节点、连线、视图）保存到 LocalStorage
         * 【优化点】在保存时，明确排除节点对象上的 'timer' 引用。
         */
        saveState() {
            // 1. 准备要保存的数据结构，去除不可序列化的 timer
            const serializableNodes = this.nodes.map(node => {
                // 使用解构赋值创建一个新对象，不包含 timer
                const { timer, ...rest } = node;
                return rest;
            });

            const state = {
                nodes: serializableNodes, // 使用去除 timer 的节点列表
                edges: this.edges,
                offsetX: this.offsetX,
                offsetY: this.offsetY,
                scale: this.scale,
            };

            try {
                localStorage.setItem('infiniteCanvasState', JSON.stringify(state));
                this.$message.success('画布状态已成功保存到本地！');
            } catch (error) {
                console.error('Error saving state to localStorage:', error);
                this.$message.error('保存失败，请检查浏览器设置。');
            }
        },
        /**
         * 从 LocalStorage 加载画布状态
         * 【优化点】
         * 1. 简化加载时的初始节点判断。
         * 2. 恢复进行中任务时，优先使用 taskType 字段。
         */
        loadState() {
            try {
                const savedState = localStorage.getItem('infiniteCanvasState');
                if (savedState) {
                    const state = JSON.parse(savedState);

                    // 1. 直接用保存的状态覆盖当前数据
                    this.nodes = state.nodes || [];
                    this.edges = state.edges || [];
                    this.offsetX = state.offsetX || 0;
                    this.offsetY = state.offsetY || 0;
                    this.scale = state.scale || 1;

                    // 2. 重新启动任务检查
                    this.nodes.forEach(node => {
                        // 必须重新添加响应式字段并清除旧引用
                        node.timer = null;

                        if (node.status === 'processing' && node.taskId) {
                            // 优先使用 node.taskType 字段（如果已添加）
                            let taskType = node.taskType || (node.contentType === 'video' ? 'video' : 'image');

                            this.startQueryTimer(node, node.taskId, taskType);
                        }
                    });

                    this.$message.success('画布状态已从本地加载。');

                } else {
                    // 如果没有保存记录且当前画布为空（仅在 mount 时调用 loadState 保证）
                    if (this.nodes.length === 0) {
                        // 确保 ID 计数器也被正确初始化
                        this.addNode('text');
                        this.$message.info('未找到本地保存数据，已创建初始节点。');
                    }
                }
            } catch (error) {
                console.error('Error loading state from localStorage:', error);
                this.$message.error('加载失败，可能数据格式有误。');
            }
        },
        goBack() {
            this.$router.push('/group');
        },
        /**
         * 弹框确认操作
         */
        confirmAddChild() {

            console.log('selectedNodeIds=', this.selectedNodeIds)

            const parentId = this.selectedNodeIds[this.selectedNodeIds.length - 1]

            const parent = this.nodes.filter(item => item.id === parentId)[0]

            const type = this.newChildType

            console.log('parent=', parent)

            console.log('type=', type)

            if (this.selectedNodeIds.length <= 0) {
                this.$message.error('所选节点数据不能为空！')
                return;
            }

            // const parent = this.pendingParentNode;
            if (!parent || !this.newChildText.trim()) {
                this.$message.warning('请输入提示词信息');
                return;
            }

            // 1. 创建基础子节点对象
            let child = {
                id: genId('n'),
                x: parent.x + parent.width + 100,
                y: parent.y + parent.height / 2 - 75, // 新节点垂直居中对齐
                width: 230,
                height: 180,
                title: '子节点',
                contentType: this.newChildType,
                content: this.newChildText, // 提示词作为内容
                src: '',
                status: 'ready', // 默认状态
                timer: null
            };
            console.log('newChildType==', this.newChildType)

            // 2. 判断是否为 AI 操作
            if (this.newChildType.startsWith('image')) {

                /**
                 * 前置判断：所选节点，必须全部为图片类型数据
                 */
                console.log('parent==', parent)
                let isOk = true
                // for (let i = 0; i < this.selectedNodeIds.length; i++) {
                //     const nodeId = this.selectedNodeIds[i]

                //     const nodes = this.nodes.filter(item => item.id === nodeId)

                //     for (let j = 0; j < nodes.length; j++) {
                //         if (nodes[j].contentType !== 'image') {
                //             isOk = false;

                //             this.$message.error('【生图操作】：所选节点必须全部为图片类型')
                //             return;

                //         }
                //     }

                // }

                child.title = `AI 任务 (${this.newChildType})`;
                child.status = 'processing';
                child.contentType = 'image'; // 初始仍显示提示词

                // 启动 AI 任务
                this.generatorImage(child, this.newChildType);

                this.nodes.push(child);
                for (let i = 0; i < this.selectedNodeIds.length; i++) {
                    const parentId = this.selectedNodeIds[i]

                    // 3. 添加节点和连线
                    this.edges.push({
                        id: genId('e'),
                        fromId: parentId,
                        toId: child.id
                    });
                }

            } else if (this.newChildType === 'video') {
                /**
                * 前置判断：所选节点，必须全部为图片类型数据
                */
                console.log('parent==', parent)
                let isOk = true
                if (this.selectedNodeIds.length > 1 || (parent.contentType !== 'image' && parent.contentType !== 'text')) {
                    this.$message.error('【生视频操作】：所选节点只能为一个，并且必须为图片|文本节点')
                    return;
                }

                // === 新增：视频生成逻辑 ===
                child.title = `AI 视频`;
                child.status = 'processing';
                child.contentType = 'video';
                // 调用视频生成方法
                this.generatorImageToVieo(child, this.newChildType, parent);

                this.nodes.push(child);
                for (let i = 0; i < this.selectedNodeIds.length; i++) {
                    const parentId = this.selectedNodeIds[i]

                    // 3. 添加节点和连线
                    this.edges.push({
                        id: genId('e'),
                        fromId: parentId,
                        toId: child.id
                    });
                }

            } else if (this.newChildType === 'storyboardText') {
                /**
                 * 前置判断：所选节点，必须只有一个，并且只有一个文本类型节点
                 */
                console.log('parent==', parent)
                // if (this.selectedNodeIds.length > 1 || parent.contentType !== 'text') {

                //     this.$message.error('【分镜操作】：所选节点必须为有且只有一个文本类型节点数据')
                //     return;
                // }

                let nodeSplices = []

                let nodeInfo = { ... this.form }

                let currentXOffset = parent.x + parent.width + 200;
                const newNodeY = parent.y;

                // 定义新节点之间的垂直间距
                const horizontalSpacing = 30;
                const newNodeHeight = 200;
                const newNodeWidth = 300;

                for (let i = 0; i < 3; i++) {
                    // 计算当前新节点的 Y 坐标
                    // 逻辑：从父节点的 Y 坐标开始 + 累加的偏移量
                    const nodes = {
                        id: genId('n'),
                        // X 坐标：使用累加的偏移量
                        x: currentXOffset,
                        // Y 坐标：保持与父节点顶部对齐
                        y: newNodeY,
                        width: newNodeWidth,
                        height: newNodeHeight,
                        title: '分片镜头-' + (i + 1),
                        contentType: 'text',
                        status: 'processing'
                    };

                    currentXOffset = currentXOffset + newNodeWidth + horizontalSpacing;

                    nodeSplices.push(nodes);

                    this.nodes.push(nodes)

                    this.edges.push({
                        id: genId('e'),
                        fromId: parent.id,
                        toId: nodes.id
                    });


                }

                this.generatorSpliceVideo(nodeSplices, nodeInfo)

            } else {
                // 普通媒体节点
                child.title = '新节点 - ' + this.newChildType;
                child.src = '';
                child.contentType = 'text';
                console.log('child==',child)

                this.nodes.push(child);
                this.edges.push({
                    id: genId('e'),
                    fromId: parent.id,
                    toId: child.id
                });

            }

            this.closeModal(); // 完成操作后关闭弹窗
        },
        /**
         * 生成分片镜头
         */
        generatorSpliceVideo(nodes, nodeInfo) {

            const param = {
                // 使用节点中的提示词作为 prompt
                userPrompt: this.newChildText,
                // 传递模型和风格，如果接口需要
                videoLength: 3,
                videoStyle: this.form.modelStyle
            };

            nodes.forEach(node => {
                // 确保节点处于加载状态
                node.status = 'processing';
            });
            generatorSlicePrompt(param).then(res => {
                if (res && res.data.code === 200 && res.data.data) {

                    if (res && res.data.code === 200 && res.data.data) {
                        for (let i = 0; i < nodes.length; i++) {
                            let nodesChild = nodes[i]
                            nodesChild.content = res.data.data[i]

                            nodes[i].status = 'success';
                            nodes[i].contentType = 'text';
                            nodes[i].title = `分片镜头: ${i + 1}`;
                        }
                    }

                } else {
                    // 任务启动失败
                    nodes.forEach(node => {
                        // 确保节点处于加载状态
                        node.status = 'failed';
                        node.title = `任务启动失败`;
                    });
                    this.$message.error('AI 任务启动失败: ' + (res.data.msg || '未知错误'));
                }
            }).catch(err => {
                console.error('Generator Image API Error:', err);
                nodes.forEach(node => {
                    // 确保节点处于加载状态
                    node.status = 'failed';
                    node.title = `网络错误`;
                });
                this.$message.error('AI 任务网络错误');
            });
        },
        /**
         * 文生图接口/图生图 (启动任务)
         * @param {Object} node - 正在处理的节点对象
         */
        generatorImage(node, newChildType) {

            console.log('generatorImage==多选文件')

            const selectedNodeSrcs = this.nodes
                .filter(node => this.selectedNodeIds.includes(node.id)) // 筛选出选中的节点
                .map(node => node.src); // 提取每个选中节点的 src 字段

            console.log('Selected Nodes src:', selectedNodeSrcs);

            const selectedNodeSrcsString = selectedNodeSrcs.join(',');

            console.log('Selected Nodes src String:', selectedNodeSrcsString);

            const param = {
                // 使用节点中的提示词作为 prompt
                prompt: node.content,
                // 传递模型和风格，如果接口需要
                modelType: this.form.modelType,
                modelStyle: this.form.modelStyle,
                filePath: selectedNodeSrcs && selectedNodeSrcs.length > 0
                    ? selectedNodeSrcsString
                    : null
            };

            console.log('param==', param)


            // 确保节点处于加载状态
            node.status = 'processing';
            node.title = `生成中...`;

            // 假设文生图接口返回一个 taskId
            generatorImage(param).then(res => {
                if (res && res.data.code === 200 && res.data.data) {
                    const taskId = res.data.data;
                    node.taskId = taskId; // 存储 taskId 到节点上

                    console.log('taskId=', taskId)

                    // 启动定时查询任务
                    this.startQueryTimer(node, taskId);

                } else {
                    // 任务启动失败
                    node.status = 'failed';
                    node.title = '任务启动失败';
                    this.$message.error('AI 任务启动失败: ' + (res.data.msg || '未知错误'));
                }
            }).catch(err => {
                console.error('Generator Image API Error:', err);
                node.status = 'failed';
                node.title = '网络错误';
                this.$message.error('AI 任务网络错误');
            });
        },
        /**
         * 图生视频接口/图生视频（启动任务）
         */
        generatorImageToVieo(node, newChildType, parent) {
            const param = {
                prompt: node.content,
                modelType: this.form.modelType,
                modelStyle: this.form.modelStyle,
                // 获取父节点的图片作为输入（如果是图生视频）
                filePath: parent && parent.src
                    ? parent.src
                    : null
            };

            // 1. 设置节点状态
            node.status = 'processing';
            node.title = '视频生成中...';
            // 确保内容类型暂时为 text，以便显示加载动画或提示词，成功后再切为 video

            // 2. 调用生成接口
            generatorVideo(param).then(res => {
                if (res && res.data.code === 200 && res.data.data) {
                    const taskId = res.data.data;
                    node.taskId = taskId;

                    // 3. 启动定时查询任务 (注意第三个参数传 'video')
                    this.startQueryTimer(node, taskId, 'video');
                } else {
                    node.status = 'failed';
                    node.title = '任务启动失败';
                    this.$message.error('视频生成任务启动失败');
                }
            }).catch(err => {
                console.error('Generator Video API Error:', err);
                node.status = 'failed';
                node.title = '网络错误';
            });
        },
        /**
         * 查询图片 (轮询任务状态)
         * @param {Object} node - 正在处理的节点对象
         * @param {string} taskId - 任务ID
         */
        queryGeneratoImage(node, taskId) {
            const param = { taskId };

            queryResultTextToImage(param).then(res => {
                if (res && res.data.code === 200 && res.data.data) {
                    console.log('queryGeneratoImage=', res)
                    const state = res.data.data;


                    if (state === '0') {
                        console.log(`节点 ${node.id} 排队中`);
                        node.title = '排队中 (0)...';
                    } else if (state === '1') {
                        console.log(`节点 ${node.id} 生成中`);
                        node.title = '生成中 (1)...';
                    } else if (state === '3') {
                        // 失败状态
                        node.status = 'failed';
                        node.title = '生成失败';
                        this.stopQueryTimer(node);
                        this.$message.error(`节点 ${node.id} AI 任务失败`);

                    } else if (state === '-1') {
                        console.log('节点数据已完成')
                        this.stopQueryTimer(node);
                    } else {
                        // 成功状态 (假设 state 包含了文件 URL 或 fileUrl 字段)
                        const fileUrl = state;

                        node.status = 'success';
                        node.title = '生成成功';
                        node.contentType = 'image'; // 假设文生图结果是图片
                        node.src = fileUrl; // 更新图片 URL

                        this.stopQueryTimer(node);
                        this.$message.success(`节点 ${node.id} 图片生成成功！`);
                    }
                } else {
                    // 查询接口返回错误，视为失败
                    node.status = 'failed';
                    node.title = '查询失败';
                    this.stopQueryTimer(node);
                    this.$message.error('AI 任务查询失败');
                }
            }).catch(err => {
                console.error('Query API Error:', err);
                node.status = 'failed';
                node.title = '查询网络错误';
                this.stopQueryTimer(node);
            });
        },
        /**
         * 查询视频 (轮询任务状态)
         */
        queryGeneratoImageVideo(node, taskId) {
            const param = { taskId };

            queryVideoResult2(param).then(res => {
                if (res && res.data.code === 200 && res.data.data) {
                    const resDatas = res.data.data;
                    // 注意：这里根据你提供的逻辑，result 为字符串 '1' 或 '-1'

                    if (resDatas.result === '1') {
                        // === 成功 ===
                        const videoUrl = resDatas.filePath;

                        // 1. 更新节点状态
                        node.status = 'success';
                        node.title = '视频生成成功';

                        // 2. 关键：切换节点类型为 video，并赋值 src
                        node.contentType = 'video';
                        node.src = videoUrl;

                        // 3. 停止轮询
                        this.stopQueryTimer(node);
                        this.$message.success(`节点 ${node.id} 视频生成完成！`);

                    } else if (resDatas.result === '-1') {
                        // === 失败 ===
                        node.status = 'failed';
                        node.title = '视频生成失败';

                        this.stopQueryTimer(node);
                        this.$message.error(`节点 ${node.id} 视频生成失败`);

                    } else {
                        // === 生成中 (假设其他状态码都是生成中) ===
                        console.log(`节点 ${node.id} 视频处理中...`);
                        // 可以选择性更新 title，例如显示进度（如果有进度字段）
                        // node.title = `生成中...`; 
                    }

                } else {
                    // 接口返回异常
                    node.status = 'failed';
                    this.stopQueryTimer(node);
                }
            }).catch(err => {
                console.error('Query Video API Error:', err);
                node.status = 'failed';
                this.stopQueryTimer(node);
            });
        },
        /**
         * 启动定时器
         * @param {Object} node - 节点对象
         * @param {string} taskId - 任务ID
         * @param {string} taskType - 任务类型 'image' | 'video' (默认 image)
         */
        startQueryTimer(node, taskId, taskType = 'image') {
            // 先清除旧的（如果有）
            this.stopQueryTimer(node);

            // 每 3 秒查询一次
            const timer = setInterval(() => {
                if (taskType === 'video') {
                    this.queryGeneratoImageVideo(node, taskId);
                } else {
                    this.queryGeneratoImage(node, taskId);
                }
            }, 3000);

            // 存储定时器 ID 到节点和 Map
            node.timer = timer;
            this.queryTimers.set(node.id, timer);
        },
        // 新增: 停止并清理定时器
        stopQueryTimer(node) {
            if (node.timer) {
                clearInterval(node.timer);
                node.timer = null;
                this.queryTimers.delete(node.id);
            }
        },
        onWindowMouseDownCloseModal(e) {
            if (!this.isModalVisible) return;

            const modal = this.$el.querySelector('.modal-content-floating');

            // 1. 检查点击是否发生在 Modal 内部
            if (modal && modal.contains(e.target)) {
                return; // 点击在 Modal 内部，不关闭
            }

            // 2. 新增检查：检查点击是否发生在 Element UI 的下拉列表 (Popper) 内部
            // Element UI 的下拉组件通常有一个共同的 class 或属性，例如 'el-select-dropdown'
            // 我们可以向上遍历 DOM 树检查父元素是否包含这个 class。
            let clickedPopper = false;
            let target = e.target;
            while (target && target.nodeName !== 'BODY') {
                // 假设 Element UI 下拉框的 class 为 el-popper 或 el-select-dropdown
                // 具体的类名可能因 Element UI 版本而异，请检查你的实际 DOM
                if (target.classList.contains('el-select-dropdown') || target.classList.contains('el-popper')) {
                    clickedPopper = true;
                    break;
                }
                target = target.parentNode;
            }

            if (clickedPopper) {
                return; // 点击在 Element UI 下拉框内部，不关闭
            }

            // 如果既不在 Modal 内部，也不在 Element UI 下拉框内部，则关闭
            this.closeModal();
        },
        /**
         * Enter 确认
         */
        handleFloatingInputEnter() {
            if (this.globalSearchText.trim()) {
                alert(`触发操作：${this.globalSearchText} (此处应执行搜索或生成逻辑)`);
            }
        },
        // 添加子节点-打开弹窗 (已修改定位计算)
        openAddChildModal(parent) {
            this.pendingParentNode = parent;
            this.newChildType = parent.contentType; // 默认继承父节点类型
            this.newChildText = '';
            this.newChildSrc = '';

            if (parent.contentType === 'text') {
                this.newChildText = parent.text;
            }

            this.isModalVisible = true;

            // --- 新增：计算弹框应该出现的位置 (世界坐标) ---
            // 目标位置: 父节点右侧 20px 处
            this.modalPos = {
                x: parent.x + parent.width + 20,
                y: parent.y // 与父节点顶部对齐
            };

            // 聚焦输入框 (确保弹窗已渲染)
            this.$nextTick(() => {
                const input = this.$el.querySelector('.modal-content-floating textarea, .modal-content-floating input');
                if (input) input.focus();
            });
        },
        /**
         * 关闭弹框
         */
        closeModal() {
            this.isModalVisible = false;
            this.pendingParentNode = null;
            this.newChildText = '';
            this.newChildSrc = '';
            this.modalPos = { x: 0, y: 0 }; // 清空位置
        },
        // --- 坐标系统核心 ---
        worldToScreen(x, y) {
            return {
                x: (x + this.offsetX) * this.scale,
                y: (y + this.offsetY) * this.scale
            };
        },
        screenToWorld(x, y) {
            return {
                x: x / this.scale - this.offsetX,
                y: y / this.scale - this.offsetY
            };
        },

        // --- 渲染辅助 ---
        getNodeStyle(node) {
            const pos = this.worldToScreen(node.x, node.y);
            return {
                transform: `translate(${pos.x}px, ${pos.y}px)`, // 使用 transform 性能更好
                width: node.width * this.scale + 'px',
                height: node.height * this.scale + 'px',
                zIndex: this.selectedNodeId === node.id ? 10 : 1
            };
        },
        getEdgePath(edge) {
            const from = this.nodes.find(n => n.id === edge.fromId);
            const to = this.nodes.find(n => n.id === edge.toId);
            if (!from || !to) return '';

            // 保持连线始终使用世界坐标系进行计算，然后转换到屏幕坐标
            const startWorldX = from.x + from.width;
            const startWorldY = from.y + from.height / 2;
            const endWorldX = to.x;
            const endWorldY = to.y + to.height / 2;

            const start = this.worldToScreen(startWorldX, startWorldY);
            const end = this.worldToScreen(endWorldX, endWorldY);

            const dist = Math.abs(end.x - start.x) * 0.5;
            const cp1 = { x: start.x + dist, y: start.y }; // 控制点1
            const cp2 = { x: end.x - dist, y: end.y };     // 控制点2

            return `M ${start.x} ${start.y} C ${cp1.x} ${cp1.y} ${cp2.x} ${cp2.y} ${end.x} ${end.y}`;
        },
        //修改2
        // --- 交互事件 ---
        onCanvasMouseDown(e) {
            if (e.button !== 0) return; // 只响应左键
            this.isPanning = true;
            this.panStart = { x: e.clientX, y: e.clientY };
            this.panOrigin = { x: this.offsetX, y: this.offsetY };
            this.selectedNodeId = null; // 点击空白取消选中
        },
        onNodeMouseDown(node, e) {

            this.isLoading = true;

            const isMultiSelect = e.ctrlKey || e.metaKey; // 按住Ctrl键时为多选

            if (isMultiSelect) {
                // 如果节点已经被选中，则取消选中
                if (this.selectedNodeIds.includes(node.id)) {
                    this.selectedNodeIds = this.selectedNodeIds.filter(id => id !== node.id);
                } else {
                    // 否则，选中该节点
                    this.selectedNodeIds.push(node.id);
                }
            } else {
                // 没有按住Ctrl键时，只选中当前节点
                this.selectedNodeIds = [node.id];

            }

            this.draggingNode = node;
            this.selectedNodeId = node.id;
            this.nodeDragStart = { x: e.clientX, y: e.clientY };
            this.nodeOrigin = { x: node.x, y: node.y };
            this.closeModal(); // 开始拖拽节点时关闭弹窗
        },
        clearSelectedNodes() {
            this.selectedNodeIds = [];
        },

        onNodeResizeMouseDown(node, e) {
            this.resizingNode = node;
            this.resizeStart = { x: e.clientX, y: e.clientY };
            this.resizeOriginSize = { w: node.width, h: node.height };
            this.closeModal(); // 开始调整大小时关闭弹窗
        },

        // --- 全局鼠标移动 (Window级) ---
        onWindowMouseMove(e) {
            // 1. 拖拽节点
            if (this.draggingNode) {
                const dx = (e.clientX - this.nodeDragStart.x) / this.scale;
                const dy = (e.clientY - this.nodeDragStart.y) / this.scale;
                this.draggingNode.x = this.nodeOrigin.x + dx;
                this.draggingNode.y = this.nodeOrigin.y + dy;
                return;
            }
            // 2. 调整大小
            if (this.resizingNode) {
                const dx = (e.clientX - this.resizeStart.x) / this.scale;
                const dy = (e.clientY - this.resizeStart.y) / this.scale;
                /***
                 * 设置节点框：伸缩上下限
                 */
                this.resizingNode.width = Math.max(180, this.resizeOriginSize.w + dx);
                this.resizingNode.height = Math.max(120, this.resizeOriginSize.h + dy);
                return;
            }
            // 3. 拖拽画布
            if (this.isPanning) {
                const dx = (e.clientX - this.panStart.x) / this.scale;
                const dy = (e.clientY - this.panStart.y) / this.scale;
                this.offsetX = this.panOrigin.x + dx;
                this.offsetY = this.panOrigin.y + dy;
            }
        },
        //修改3
        onWindowMouseUp(e) {
            this.isPanning = false;
            this.draggingNode = null;
            this.resizingNode = null;
        },
        onWheel(e) {
            // 缩放步长
            const zoomIntensity = 0.1;
            const delta = e.deltaY > 0 ? -zoomIntensity : zoomIntensity;

            // 设置缩放上限 5.0 (500%)，下限 0.2 (20%)，防止节点在屏幕上过小
            const MIN_SCALE = 0.6;
            const MAX_SCALE = 5;

            const newScale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, this.scale + delta));

            // 计算鼠标在世界坐标系的位置，确保缩放以鼠标为中心
            const rect = this.$refs.canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const mouseY = e.clientY - rect.top;

            const worldPos = this.screenToWorld(mouseX, mouseY);

            // 更新缩放
            this.scale = newScale;

            // 修正 Offset 使得世界坐标下的鼠标位置保持不变
            this.offsetX = mouseX / this.scale - worldPos.x;
            this.offsetY = mouseY / this.scale - worldPos.y;
        },

        // --- 业务逻辑 ---
        addNode(type = 'text') {
            // 在视口中心生成
            const center = this.screenToWorld(this.$refs.canvas.clientWidth / 2, this.$refs.canvas.clientHeight / 2);
            this.nodes.push({
                id: genId('n'),
                x: center.x - 100,
                y: center.y - 75,
                width: 200,
                height: 150,
                title: '新节点',
                contentType: type,
                content: '',
                src: '',
                // ======== 新增/更新字段 ========
                status: 'ready',
                timer: null
                // ============================
            });
        },

        changeNodeType(node, type) {
            node.contentType = type;
        },

        deleteNode(id) {
            const nodeToDelete = this.nodes.find(n => n.id === id);
            if (nodeToDelete) {
                this.stopQueryTimer(nodeToDelete); // <-- 关键：停止定时器
            }
            this.nodes = this.nodes.filter(n => n.id !== id);
            this.edges = this.edges.filter(e => e.fromId !== id && e.toId !== id);
        }
    }
};
</script>
<style scoped>
/* 基础容器 */
.ice-wrapper {
    display: flex;
    flex-direction: column;
    height: 100vh;
    width: 100vw;
    background: #0f172a;
    color: #e2e8f0;
    overflow: hidden;
    font-family: 'Segoe UI', sans-serif;
}

.ice-toolbar {
    height: 56px;
    background: #101620;
    /* 保持深灰蓝背景 */
    border-bottom: 1px solid #334155;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    padding: 0 20px;
    justify-content: flex-start;
    z-index: 20;
}

/* 按钮组容器 */
.btn-group {
    /* 使用 gap 替代 margin-right */
    display: flex;
    gap: 12px;
}

.btn-group button {
    /* 核心样式：使用略微深一点的蓝色作为默认色，为悬停做准备 */
    background: #1d4ed8;
    border: none;
    color: white;
    padding: 8px 16px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    transition: background 0.3s ease, transform 0.1s ease, box-shadow 0.3s ease;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4), 0 0 10px rgba(56, 189, 248, 0.15);
    /* 增加微妙的科技光感 */
}

.btn-group button:hover {
    background: #3b82f6;
    box-shadow: 0 0 5px rgba(59, 130, 246, 0.8), 0 4px 8px rgba(0, 0, 0, 0.5);
    transform: translateY(-1px);
}

.btn-group button:active {
    background: #1e40af;
    box-shadow: none;
    transform: translateY(1px);
}


/* ==================================================================  */
/* 画布 */
.ice-canvas {
    flex: 1;
    position: relative;
    overflow: hidden;
    background: radial-gradient(#334155 1.5px, transparent 1.5px);
    background-size: 18px 18px;
}

/* 确保 SVG 连线层的 z-index 低于节点 */
.ice-canvas .ice-edges-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;
}

/* ==================================================================  */

/* 连线 */
.ice-edges-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;
}

.ice-edges-layer path {
    shape-rendering: geometricPrecision;
    transition: stroke-width 0.2s, stroke 0.2s, opacity 0.2s;
    /* 添加平滑过渡 */
}

/* 🚀 背景层（光晕/深度线） */
.ice-edges-layer .edge-background {
    stroke: #2563eb;
    /* 深蓝色 */
    stroke-width: 4px;
    /* 较粗 */
    opacity: 0.5;
    /* 半透明，作为光晕或深度 */
    /* 移除虚线效果，使用实线 */
    stroke-dasharray: none;
}

/* 🚀 前景层（主线/电流线） */
.ice-edges-layer .edge-foreground {
    stroke: #38bdf8;
    /* 亮青蓝色 */
    stroke-width: 1.8px;
    /* 较细 */
    opacity: 1;
    /* 完全不透明 */
    /* 可选：添加动画实现“电流”效果 */

    /* 核心动画设置：创建线段和间隙 */
    stroke-dasharray: 20, 200;
    /* 短线段 (20) 和长间隙 (200)，突出“流动点” */

    /* 应用快速动画：0.8秒循环一次，匀速 */
    animation: fast-flow 0.8s linear infinite;
}

/* 连线动画定义 */
@keyframes fast-flow {

    /* 偏移量从 0 移动到 -220，让虚线向前移动一个单位 */
    100% {
        stroke-dashoffset: -220;
    }
}

/* 假设您有逻辑在节点悬停时给节点添加一个 .is-hovered class */
.ice-node.is-hovered~.ice-edges-layer .edge-background {
    /* 悬停时加粗光晕 */
    stroke-width: 5px;
    stroke: #60a5fa;
    opacity: 0.7;
}

.ice-node.is-hovered~.ice-edges-layer .edge-foreground {
    /* 悬停时主线变亮 */
    stroke: #a5f3fc;
}

.ice-node {
    position: absolute;
    top: 0;
    left: 0;
    /* 节点主体背景颜色 */
    background: #1e293b;
    border: 1px solid #334155;
    /* 默认边框 */
    border-radius: 12px;
    /* 默认阴影 */
    /* box-shadow: 0 6px 15px rgba(0, 0, 0, 0.5); */
    display: flex;
    flex-direction: column;
    user-select: none;
    transform-origin: 0 0;
    min-width: 220px;
    /* 稍微增加宽度 */
    min-height: 120px;
    z-index: 10;
    /* 确保节点在连线上方 */
    transition: box-shadow 0.2s ease, border-color 0.2s ease;

    /* 🚀 默认持续微弱阴影和光圈 */
    border-color: #38bdf8;
    /* 亮青蓝色边框 */
    /* 核心：三层阴影营造光圈和深度感 */
    box-shadow:
        0 0 0 3px rgba(56, 189, 248, 0.3),
        /* 内层发光 */
        0 0 15px rgba(56, 189, 248, 0.5),
        /* 外层光晕 */
        0 8px 20px rgba(0, 0, 0, 0.8);
    /* 底部深度阴影 */
    filter: drop-shadow(0 0 4px rgba(56, 189, 248, 0.6));
    /* 进一步增强光晕 */

    padding: 0px;
    margin: 0px;
}

.ice-node.is-selected {
    /* 1. 颜色突变：使用高亮电光蓝色边框 */
    border-color: #4b5563;
    /* 边框恢复深色，让光晕效果更明显 */

    /* 2. 视觉特效：放大 + 抬升 */
    transform: scale(1.02) translateY(-2px);
    z-index: 20;
    /* 选中时提升层级 */

    /* 3. 强力光圈：使用高饱和度紫色作为选中强调色 */
    box-shadow:
        0 0 0 3px rgba(129, 140, 248, 0.5),
        /* 内圈紫色发光 */
        0 0 25px rgba(167, 139, 250, 1),
        /* 强烈的紫色外层光晕 */
        0 12px 30px rgba(0, 0, 0, 1);
    /* 更深的底部阴影 */

    /* 4. 滤镜光晕增强 */
    filter: drop-shadow(0 0 8px rgba(129, 140, 248, 0.9));
}

.ice-node-header {
    height: 38px;
    /* 略微增加高度，更具分量 */
    /* 🚀 增加微弱的渐变背景，提升层次感 */
    background: linear-gradient(to right, #2c3e50, #2c3e50 90%, #3a5068);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 15px 0 12px;
    /* 调整内边距 */
    cursor: grab;
    border-radius: 12px 12px 0 0;
    /* 使用深色边框，增强分割线效果 */
    border-bottom: 1px solid #4a627a;
}

.inSiderDiv {
    height: 100%;
    background: #1e293b;
    /* 稍微提亮一点 */
    border-radius: 12px;
    color: #f1f5f9;
    /* 增加一个非常内敛的深色阴影，让它浮起来 */
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3);
    margin: 0px;
    padding: 0px;
}

.ice-node-header:active {
    cursor: grabbing;
    background: #334a60;
    /* 点击时背景略微变深 */
}


.ice-node-title {
    font-weight: 800;
    font-size: 13px;
    color: #e0f2f1;
    letter-spacing: 1px;
    text-transform: uppercase;

    text-shadow:
        0 0 2px rgba(125, 211, 252, 0.8),
        0 0 8px rgba(56, 189, 248, 0.5);
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
}

/* 操作图标容器 */
.ice-node-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    /* 使用 gap 控制间距 */
}

/* 操作图标样式 */
.ice-node-actions span {
    cursor: pointer;
    font-size: 16px;
    opacity: 0.7;
    /* 默认略微透明 */
    transition: opacity 0.2s, transform 0.1s;
}

.ice-node-actions span:hover,
.ice-node-actions span.active {
    opacity: 1;
    color: #38bdf8;
    transform: scale(1.1);
    /* 悬停时微小放大 */
}


.delete-btn {
    color: #ef4444;
}

.ice-node-contentBody {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: relative;
    background: #0f172a;

    /* 🚀 核心优化：使用内发光的 box-shadow 模拟彩色边框 */
    box-shadow:
        /* 内部发光 (inset)，模拟边框线 */
        inset 0 0 0 2px rgba(13, 160, 223, 0.5),
        /* 青蓝色线 */
        /* 增加一个微弱的内发光光晕 */
        inset 0 0 5px rgba(56, 189, 248, 0.2);

    border-radius: 12px;

    padding: 0px;
}

/* 节点内容 */
.ice-node-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: relative;
    background: #0f172a;

    padding: 20px;
    /* 添加一点内边距 */

    /* 🚀 核心优化：使用内发光的 box-shadow 模拟彩色边框 */
    box-shadow:
        /* 内部发光 (inset)，模拟边框线 */
        inset 0 0 0 2px rgba(13, 160, 223, 0.5),
        /* 青蓝色线 */
        /* 增加一个微弱的内发光光晕 */
        inset 0 0 5px rgba(56, 189, 248, 0.2);

    border-radius: 12px;
}

/* 文本模式 */
.ice-node-textarea {
    /* --- 1. 基础布局 --- */
    width: 100%;
    height: 100%;
    box-sizing: border-box;
    /* 关键：确保 padding 不会撑大盒子 */
    background: transparent;
    border: none;
    resize: none;
    outline: none;

    text-align: center;
    padding: 10px;
    color: #e2e8f0;
    font-family: 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
    font-size: 15px;
    /* 稍微调大字号 */
    font-weight: 500;
    line-height: 1.8;
    letter-spacing: 1px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
    scrollbar-width: thin;
    scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
}

.ice-node-textarea::-webkit-scrollbar {
    width: 4px;
}

.ice-node-textarea::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 4px;
}

.ice-node-textarea::-webkit-scrollbar-track {
    background: transparent;
}

/* 媒体模式 (Image/Video) */
.ice-media-box {
    /* 保持垂直方向的 flex 布局 */
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    position: relative;
    /* 保持水平和垂直居中，用于 placeholder */
    align-items: center;
    justify-content: center;
    background: #000;
    overflow: hidden;
    box-sizing: border-box;

}

.ice-media-box img,
.ice-media-box video {

    flex-grow: 1;
    width: 100%;
    height: 100%;
    object-fit: contain;
    /* 保持比例 */
    display: block;
}

.ice-media-box .placeholder {
    color: #64748b;
    font-size: 14px;
    padding: 20px;
    text-align: center;
}

.url-input {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    background: rgba(0, 0, 0, 0.8);
    border: none;
    color: white;
    padding: 4px 8px;
    font-size: 12px;
    opacity: 0;
    height: auto;
    min-height: 30px;
    transition: opacity 0.2s;
    box-sizing: border-box;
}

/* 媒体为空，或者鼠标悬停时显示输入框 */
.ice-media-box:hover .url-input,
.ice-media-box .placeholder~.url-input {
    opacity: 1;
}

.ice-node-footer {
    height: 30px;
    /* 略微增加高度 */
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 12px;
    background: #101620;
    /* 比主体背景 (#1e293b) 更深的颜色，增加底部厚重感 */
    border-top: 1px solid #334155;
    border-radius: 0 0 12px 12px;
}

.tag {
    font-size: 10px;
    color: #a0aec0;
    /* 略微柔和的白色 */
    text-transform: uppercase;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 4px;
    letter-spacing: 1px;
}

.add-child-btn {
    background: none;
    border: none;
    color: #38bdf8;
    cursor: pointer;
    display: flex;
    align-items: center;
    /* 🚀 按钮圆形光圈效果 */
    width: 35px;
    height: 35px;
    justify-content: center;
    border-radius: 50%;
    transition: background 0.2s, box-shadow 0.2s;
}

.add-child-btn:hover {
    color: #a5f3fc;
    background: rgba(56, 189, 248, 0.1);
    box-shadow: 0 0 8px rgba(56, 189, 248, 0.5);
    /* 悬停时出现光晕 */
}

/* 缩放手柄 */
.ice-resize-handle {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 15px;
    height: 15px;
    cursor: nwse-resize;
    background: transparent;
    z-index: 20;
}

/* 增加一个可视的小三角 */
.ice-resize-handle::after {
    content: '';
    position: absolute;
    right: 3px;
    bottom: 3px;
    border-right: 2px solid #38bdf8;
    /* 使用强调色 */
    border-bottom: 2px solid #38bdf8;
    width: 6px;
    height: 6px;
}
</style>


<style scoped>
/* 局部浮动 Modal 样式（美化版） */
.modal-content-floating {
    position: absolute;
    top: 0;
    left: 0;
    transform-origin: 0 0;
    background: radial-gradient(circle at top left, rgba(56, 189, 248, 0.18), transparent 55%),
        radial-gradient(circle at bottom right, rgba(129, 140, 248, 0.2), transparent 55%),
        rgba(15, 23, 42, 0.96);
    backdrop-filter: blur(10px);

    border: 1px solid rgba(148, 163, 184, 0.6);
    border-radius: 14px;
    padding: 18px 16px 14px;
    box-shadow:
        0 18px 45px rgba(0, 0, 0, 0.65),
        0 0 0 1px rgba(15, 23, 42, 0.9);

    width: 340px;
    color: #e2e8f0;
    z-index: 999;
    pointer-events: auto;

    display: flex;
    flex-direction: column;
    gap: 10px;
}

/* 标题行：标题 + 关闭空间 */
.modal-content-floating h3 {
    margin: 0;
    padding-bottom: 8px;
    font-size: 15px;
    font-weight: 600;
    color: #e5e7eb;
    border-bottom: 1px solid rgba(51, 65, 85, 0.9);
    display: flex;
    align-items: center;
    gap: 6px;
}

.modal-content-floating h3::before {
    content: '';
    width: 8px;
    height: 8px;
    border-radius: 999px;
    background: radial-gradient(circle, #38bdf8, #6366f1);
    box-shadow: 0 0 10px rgba(56, 189, 248, 0.9);
}

/* 小说明文字 */
.modal-content-floating p {
    margin: 0;
    font-size: 12px;
    color: #9ca3af;
}

/* Element 表单在弹窗内的整体压缩一点间距 */
.modal-content-floating .el-form {
    margin-top: 4px;
}

.modal-content-floating .el-form-item {
    margin-bottom: 10px;
}

.modal-content-floating .el-form-item__label {
    color: #cbd5f5;
    font-size: 12px;
}

/* 选择框样式微调 */
.modal-content-floating .el-input__inner {
    background-color: rgba(15, 23, 42, 0.9);
    border-color: rgba(75, 85, 99, 0.9);
    color: #e5e7eb;
}

.modal-content-floating .el-input__inner::placeholder {
    color: #6b7280;
}

.modal-type-switcher button {
    border: 1px solid rgba(64, 158, 255, 0.15);
    color: #409EFF;
    /* 文字直接使用主题色，但亮度稍低 */

    padding: 6px 14px;
    border-radius: 999px;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s ease;
}

.modal-type-switcher button:hover {
    background: rgba(64, 158, 255, 0.15);
    border-color: rgba(64, 158, 255, 0.3);
}

.modal-type-switcher button.active {
    background: #409EFF;
    color: #fff;
    border-color: #409EFF;
}

/* 输入区域（textarea / input）统一样式 */
.modal-content-floating textarea,
.modal-content-floating input {
    width: 100%;
    padding: 9px 10px;
    margin-top: 6px;
    border: 1px solid rgba(71, 85, 105, 0.9);
    border-radius: 8px;
    background: rgba(15, 23, 42, 0.95);
    color: #e5e7eb;
    resize: vertical;
    min-height: 70px;
    box-sizing: border-box;
    font-size: 13px;
    outline: none;
    transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.modal-content-floating textarea::placeholder,
.modal-content-floating input::placeholder {
    color: #6b7280;
}

.modal-content-floating textarea:focus,
.modal-content-floating input:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.6);
    background: rgba(15, 23, 42, 1);
}

/* 底部按钮区域 */
.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 6px;
}

.modal-actions button {
    padding: 7px 14px;
    border: none;
    border-radius: 999px;
    cursor: pointer;
    font-weight: 600;
    font-size: 12px;
    line-height: 1;
    transition: all 0.18s ease;
}

/* 取消按钮 */
.btn-cancel {
    background: rgba(31, 41, 55, 0.95);
    color: #e5e7eb;
}

.btn-cancel:hover {
    background: rgba(55, 65, 81, 0.95);
}

/* 确认按钮 */
.btn-confirm {
    background: linear-gradient(135deg, #2563eb, #4f46e5);
    color: #f9fafb;
    box-shadow: 0 8px 18px rgba(37, 99, 235, 0.45);
}

.btn-confirm:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 22px rgba(59, 130, 246, 0.6);
}

/* 禁用状态 */
.btn-confirm:disabled {
    background: rgba(75, 85, 99, 0.8);
    color: #9ca3af;
    box-shadow: none;
    cursor: not-allowed;
    transform: none;
}
</style>

<style>
::v-deep .el-form-item__label {
    color: #66b1ff;
}

.ice-node-loading-overlay {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;

    /* 基础背景色：保持深色基底 */
    background-color: #2c3340;

    /* 核心：使用线性渐变创建高亮脉冲光带 */
    background-image:
        linear-gradient(90deg,
            transparent 0%,
            rgba(77, 219, 255, 0.5) 30%,
            rgba(77, 219, 255, 0.15) 60%,
            transparent 100%);

    /* 关键 1: 扩大背景尺寸，确保光带够长，且行程足够隐藏跳跃 */
    background-size: 200% 100%;
    /* background-repeat: no-repeat; */
    background-repeat: repeat-x;

    /* 关键 2: 动画速度放慢 (4s)，使用 linear 实现缓慢匀速滑动 */
    animation:
        switch-slow-flow 4s linear infinite,
        /* 主光带缓慢滑动 */
        pulse-opacity 3s ease-in-out infinite alternate;
    /* 整体呼吸效果 */

    color: #fff;
    font-size: 14px;
    z-index: 5;
    border-radius: 8px;
    overflow: hidden;

    box-shadow: inset 0 0 15px rgba(0, 0, 0, 0.3);
}

/* 动画 A：主光带缓慢、连续地从左到右滑动 */
@keyframes switch-slow-flow {
    0% {
        /* 起点：光带在左侧外部 */
        background-position: -200% 0;
    }

    100% {
        /* 终点：光带在右侧外部 */
        background-position: 200% 0;
    }
}

/* 动画 B：整体罩层的细微呼吸效果 (保持不变) */
@keyframes pulse-opacity {
    0% {
        opacity: 1;
    }

    100% {
        opacity: 0.95;
    }
}


/* 文本样式保持不变 (沿用高级感样式) */
.ice-node-loading-overlay p {
    z-index: 10;
    position: relative;
    background-color: transparent;
    border: none;
    padding: 0;
    border-radius: 0;
    color: rgba(255, 255, 255, 0.9);
    font-size: 16px;
    font-weight: 500;
    opacity: 0.6;
    letter-spacing: 0.5px;
    text-shadow:
        0 0 5px rgba(0, 0, 0, 0.6),
        0 0 10px rgba(0, 120, 255, 0.3);
}

.ice-node-loading-overlay p::before {
    content: '';
    position: absolute;
    top: -5px;
    left: -10px;
    right: -10px;
    bottom: -5px;
    border-radius: 6px;
    z-index: -1;
    backdrop-filter: blur(5px);
    -webkit-backdrop-filter: blur(5px);
}


.floating-toolbar {
    /* 使用绝对定位，固定在画布内的某个位置 */
    position: absolute;
    top: 50%;
    /* 垂直居中 */
    left: 20px;
    /* 距离左侧 20px */
    transform: translateY(-50%);
    /* 垂直居中的微调 */
    z-index: 1000;
    /* 确保它在所有画布元素之上 */
    background-color: rgba(255, 255, 255, 0.3);
}

/* 覆盖 Element UI 的默认样式以实现垂直堆叠和图片中的外观 */
.vertical-group {
    display: flex;
    /* 启用 Flexbox */
    flex-direction: column;
    /* 垂直排列 */
    border-radius: 12px;
    /* 外部圆角 */
    overflow: hidden;
    /* 确保子元素圆角生效 */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    background-color: rgba(255, 255, 255, 0.3);
}

.vertical-group .el-button {
    /* 统一按钮尺寸 */
    width: 50px;
    height: 50px;
    padding: 0;
    margin: 0;
    /* 消除默认 margin */
    border: none;
    /* background-color: white;  */
    color: #606266;
    /* 默认图标颜色 */
    font-size: 24px;
    /* 图标大小 */
    display: flex;
    justify-content: center;
    align-items: center;
}

/* 鼠标悬停和激活状态 */
.vertical-group .el-button:hover,
.vertical-group .el-button.is-active {
    background-color: #ecf5ff;
    /* 浅蓝色背景 */
    color: #409EFF;
    /* 蓝色图标 */
}

/* 模拟图片中选中工具的外观（使用更大的蓝色背景，并保持堆叠）*/
.vertical-group .el-button.is-active {
    background-color: #DDE8F4;
    color: #303133;
}
</style>

<style>
/* 核心容器：胶囊悬浮感 */
.sora-capsule-panel {
    background-color: rgba(255, 255, 255, 0.3);

    position: fixed;
    z-index: 10000;
    width: 480px;
    /* 略微加宽 */
    bottom: 40px;
    left: 50%;
    transform: translateX(-50%);

    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(20px) saturate(180%);
    -webkit-backdrop-filter: blur(20px) saturate(180%);
    border-radius: 32px;
    /* 优雅的圆角 */
    padding: 16px;
    border: 1px solid rgba(255, 255, 255, 0.4);
    box-shadow:
        0 20px 40px rgba(0, 0, 0, 0.1),
        0 0 0 1px rgba(0, 0, 0, 0.02);
    border: 1px solid rgba(255, 255, 255, 0.2);

    box-shadow: 0 0 20px rgba(64, 158, 255, 0.6);
}

.capsule-inner {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

/* 分段切换器样式 */
.type-selector-bar {
    display: flex;
    justify-content: center;
}

.modal-type-switcher {
    display: flex;
    background: rgba(0, 0, 0, 0.05);
    padding: 4px;
    border-radius: 14px;
    width: 100%;
}

.modal-type-switcher button {
    flex: 1;
    border: none;
    background: transparent;
    padding: 8px 0;
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    cursor: pointer;
    border-radius: 10px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-type-switcher button.active {
    background: #fff;
    color: #409EFF;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

/* 输入框：无界感设计 */
.input-section {
    position: relative;
}

.capsule-textarea {
    width: 100%;
    box-sizing: border-box;
    border: 1px solid rgba(0, 0, 0, 0.04);
    border-radius: 16px;
    padding: 14px;
    font-size: 15px;
    line-height: 1.5;
    background: rgba(255, 255, 255, 0.5);
    color: #303133;
    outline: none;
    resize: none;
    transition: all 0.2s;
}

.capsule-textarea:focus {
    background: #fff;
    border-color: rgba(64, 158, 255, 0.3);
    box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
}

.capsule-textarea::placeholder {
    color: #909399;
}

/* 底部按钮 */
.capsule-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 2px;
}

.btn-cancel-text {
    background: transparent;
    border: none;
    color: #909399;
    cursor: pointer;
    font-size: 14px;
    padding: 8px 16px;
    border-radius: 10px;
    transition: background 0.2s;
}

.btn-cancel-text:hover {
    color: #606266;
    background: rgba(0, 0, 0, 0.05);
}

.btn-generate-shiny {
    display: flex;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #409EFF, #303133);
    /* 科技感深色渐变或品牌色 */
    color: white;
    border: none;
    padding: 10px 24px;
    border-radius: 20px;
    cursor: pointer;
    font-weight: 600;
    font-size: 14px;
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.25);
    transition: all 0.3s;
}

.btn-generate-shiny:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 25px rgba(64, 158, 255, 0.35);
    filter: brightness(1.1);
}

.btn-generate-shiny:active {
    transform: translateY(0);
}

.btn-generate-shiny i {
    font-size: 16px;
}
</style>