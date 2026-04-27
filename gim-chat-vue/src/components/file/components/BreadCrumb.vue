<template>
    <div class="breadcrumb-wrapper tech-breadcrumb-wrapper">
        <div class="title tech-title">当前位置：</div>
        <el-input class="file-path-input tech-input" ref="filePathInputRef" placeholder="请输入路径" v-model="inputFilePath" size="mini"
            :autofocus="true" v-show="isShowInput" @blur="handleInputBlurEnter"
            @change="handleInputBlurEnter"></el-input>
            
        <div class="breadcrumb-box tech-breadcrumb-box" :class="{ 'able-input tech-able-input': fileType === 0 }" v-show="!isShowInput"
            @click.self="handleClickBreadCrumbSelf">
            <el-breadcrumb v-if="![0, 8].includes(fileType) && !['Share'].includes($route.name)"
                separator-class="el-icon-arrow-right tech-separator">
                <el-breadcrumb-item class="tech-breadcrumb-item">{{ fileTypeMap[fileType] }}</el-breadcrumb-item>
            </el-breadcrumb>
            <el-breadcrumb v-else separator-class="el-icon-arrow-right tech-separator">
                <el-breadcrumb-item v-for="(item, index) in breadCrumbList" :key="index" :to="getRouteQuery(item)" class="tech-breadcrumb-item">{{
                    item.name }}</el-breadcrumb-item>
            </el-breadcrumb>
        </div>
    </div>
</template>

<script>
import { getFileListByPath } from '@/api/file'
export default {
    name: 'BreadCrumb',
    props: {
        // 文件类型
        fileType: {
            required: true,
            type: Number
        },
        // 文件路径
        filePath: {
            require: true,
            type: String
        }
    },
    data() {
        return {
            fileTypeMap: {
                1: '全部图片',
                2: '全部文档',
                3: '全部视频',
                4: '全部音乐',
                5: '其他',
                6: '回收站'
            },
            pageData: {
                currentPage: 1,
                pageCount: 10,
                total: 0
            },
            isShowInput: false, //  是否展示路径输入框
            inputFilePath: '' //  路径输入
        }
    },
    computed: {
        /**
         * 面包屑导航栏数组
         */
        breadCrumbList: {
            get() {
                let filePath = this.$route.query.filePath
                let filePathList = filePath ? filePath.split('/') : []
                let res = [] //  返回结果数组
                let _path = [] //  存放祖先路径
                for (let i = 0; i < filePathList.length; i++) {
                    if (filePathList[i]) {
                        _path.push(filePathList[i])
                        res.push({
                            path: _path.join('/'),
                            name: filePathList[i]
                        })
                    } else if (i === 0) {
                        //  根目录
                        filePathList[i] = ''
                        _path.push(filePathList[i])
                        res.push({
                            path: '/',
                            name:
                                this.fileType === 0
                                    ? '全部文件'
                                    : this.fileType === 8
                                        ? '我的分享'
                                        : this.$route.name === 'Share'
                                            ? '全部分享'
                                            : ''
                        })
                    }
                }
                return res
            },
            set() {
                return []
            }
        }
    },
    watch: {
        '$route.query': {
            immediate: true,
            handler(newQuery) {
                console.log('watch 路由跳转')
                console.log(newQuery)
                this.$emit('getTableDataByType')
            }
        }
    },
    methods: {
        /**
         * 点击面包屑导航栏空白处
         */
        handleClickBreadCrumbSelf() {
            if (this.fileType === 0) {
                this.inputFilePath = this.filePath
                this.isShowInput = true
                this.$nextTick(() => {
                    this.$refs.filePathInputRef.focus()
                })
            }
        },
        /**
         * 路径输入框失去焦点或用户按下回车时触发
         */
        handleInputBlurEnter() {
            this.isShowInput = false
            if (this.inputFilePath !== this.filePath) {
                const fixInputFilePath = this.inputFilePath.endsWith('/')
                    ? this.inputFilePath.slice(0, -1)
                    : this.inputFilePath
                this.$router.push({
                    query: { filePath: `${fixInputFilePath}`, fileType: 0 }
                })
            }
        },
        // 获取文件参数
        getRouteQuery(item) {
            let routeName = this.$route.name
            if (routeName === 'Share') {
                // 当前是查看他人分享列表的页面
                return { query: { filePath: item.path } }
            } else if (this.fileType === 8) {
                // 当前是我的已分享列表页面
                return {
                    query: {
                        fileType: 8,
                        filePath: item.path,
                        shareBatchNum:
                            item.path === '/' ? undefined : this.$route.query.shareBatchNum //  当查看的是根目录，批次号置空
                    }
                }
            } else {
                // 网盘页面
                return { query: { filePath: item.path, fileType: 0 } }
            }
        }
    }
}
</script>

<style lang="less" scoped>
/* ================= 赛博朋克主题变量 ================= */
@bg-dark: #0f1219;
@bg-panel: rgba(26, 31, 44, 0.7);
@primary-color: #00f2ff; /* Cyber Cyan - 霓虹青色 */
@accent-color: #7d2ae8;  /* Neon Purple */
@text-main: #e2e8f0; // 浅色主文本
@text-sub: #94a3b8;  // 辅助文本
@border-color: rgba(255, 255, 255, 0.08); // 用于主要边框
@hover-bg: rgba(0, 242, 255, 0.1); // 悬停背景

.breadcrumb-wrapper.tech-breadcrumb-wrapper {
    padding: 5px 0; // 调整 padding
    height: 40px; // 调整高度
    line-height: 30px;
    display: flex;
    align-items: center;
    background-color: @bg-dark;
    color: @text-sub;
    border-bottom: 1px solid @border-color; // 增加底部细线
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);

    .title.tech-title {
        height: 36px;
        line-height: 30px;
        color: @text-main;
        font-weight: 500;
        margin-left: 10px;
        padding-left: 5px;
    }
    
    // 路径输入框样式
    .file-path-input.tech-input {
        flex: 1;
        font-size: 14px;
        margin-right: 10px;

        ::v-deep .el-input__inner {
            height: 30px;
            line-height: 30px;
            background-color: @bg-panel;
            border: 1px solid @border-color;
            color: @primary-color;
            box-shadow: 0 0 5px rgba(0, 242, 255, 0.3);
            transition: all 0.3s;
            
            &:focus {
                border-color: @primary-color;
                box-shadow: 0 0 10px @primary-color;
            }
        }
    }

    .breadcrumb-box.tech-breadcrumb-box {
        padding: 0 8px;
        flex: 1;
        display: flex;
        align-items: center;
        margin-right: 10px;
        
        &.able-input.tech-able-input {
            cursor: pointer;
            border: 1px dashed transparent; // 默认透明虚线
            transition: all 0.2s;
            
            &:hover {
                background: @hover-bg;
                border: 1px dashed @primary-color; // 悬停时显示霓虹虚线边框
                box-shadow: 0 0 5px rgba(0, 242, 255, 0.5);
            }
        }
        
        // El-Breadcrumb 整体样式
        ::v-deep .el-breadcrumb {
            height: 30px;
            line-height: 30px;
            
            .el-breadcrumb__separator {
                font-weight: bold;
                color: @accent-color; // 分隔符使用紫色霓虹
            }
            
            .el-breadcrumb__item {
                .el-breadcrumb__inner {
                    color: @text-sub; // 默认文本颜色
                    transition: all 0.2s;

                    &.is-link {
                        color: @primary-color; // 链接颜色
                        font-weight: 500;
                        text-shadow: 0 0 5px rgba(0, 242, 255, 0.3);

                        &:hover {
                            color: @primary-color;
                            text-shadow: 0 0 10px @primary-color; // 悬停光效增强
                        }
                    }
                }
                
                // 最后一个元素，非链接
                &:last-child .el-breadcrumb__inner {
                    color: @text-main;
                    font-weight: bold;
                }
            }
        }
    }
}
</style>

<style lang="less" scoped>
@panel-soft: rgba(255, 255, 255, 0.03);
@panel-hover: rgba(110, 168, 255, 0.08);
@border-color: rgba(148, 163, 184, 0.14);
@border-strong: rgba(110, 168, 255, 0.2);
@text-main: #edf4ff;
@text-sub: #9cb0cf;
@text-muted: #6f809e;
@accent: #6ea8ff;

.breadcrumb-wrapper.tech-breadcrumb-wrapper {
	height: auto;
	padding: 10px 14px;
	border-radius: 12px;
	background: rgba(255, 255, 255, 0.03);
	border: 1px solid @border-color;
	box-shadow: none;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .title.tech-title {
	margin-left: 0;
	margin-right: 14px;
	font-size: 13px;
	font-weight: 600;
	color: @text-muted;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .file-path-input.tech-input {
	margin-right: 0;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .file-path-input.tech-input ::v-deep .el-input__inner {
	height: 36px;
	line-height: 36px;
	border-radius: 14px;
	background: rgba(255, 255, 255, 0.04);
	border-color: @border-color;
	box-shadow: none;
	color: @text-main;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .file-path-input.tech-input ::v-deep .el-input__inner:focus {
	border-color: @border-strong;
	box-shadow: 0 0 0 3px rgba(110, 168, 255, 0.08);
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box {
	padding: 0 12px;
	margin-right: 0;
	min-height: 36px;
	border-radius: 14px;
	background: @panel-soft;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box.able-input.tech-able-input:hover {
	background: @panel-hover;
	border-color: @border-strong;
	box-shadow: none;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box ::v-deep .el-breadcrumb {
	line-height: 36px;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box ::v-deep .el-breadcrumb__separator {
	color: @text-muted;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box ::v-deep .el-breadcrumb__inner {
	color: @text-sub;
	text-shadow: none;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box ::v-deep .el-breadcrumb__inner.is-link {
	color: @accent;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box ::v-deep .el-breadcrumb__inner.is-link:hover {
	color: @text-main;
}

.breadcrumb-wrapper.tech-breadcrumb-wrapper .breadcrumb-box.tech-breadcrumb-box ::v-deep .el-breadcrumb__item:last-child .el-breadcrumb__inner {
	color: @text-main;
}

@media (max-width: 768px) {
	.breadcrumb-wrapper.tech-breadcrumb-wrapper {
		padding: 8px 12px;
	}

	.breadcrumb-wrapper.tech-breadcrumb-wrapper .title.tech-title {
		display: none;
	}
}
</style>
