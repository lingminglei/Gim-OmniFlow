<template>
	<!-- 查看文件详情对话框 -->
	<el-dialog
		class="file-info-dialog"
		title="文件详情222"
		:visible.sync="visible"
		:close-on-click-modal="false"
		width="550px"
		@close="handleDialogClose"
	>
		<img
			class="file-img"
			:src="$file.setFileImg(fileInfo)"
			:title="`${fileInfo.isDir ? '' : '点击预览'}`"
		/>
		<el-form
			class="file-info-form"
			:model="fileInfo"
			ref="fileInfoForm"
			label-width="82px"
			label-position="right"
			label-suffix="："
			size="small"
		>
			<el-form-item label="文件名" prop="fileName">
				<el-input
					:value="$file.getFileNameComplete(fileInfo)"
					readonly
				></el-input>
			</el-form-item>
			<el-form-item
				:label="fileType === 6 ? '原路径' : '路径'"
				prop="filePath"
				v-if="![0, 8].includes(fileType)"
			>
				<el-input
					:value="fileInfo.filePath"
					readonly
					@click.native="handleClickFilePath(fileInfo)"
				></el-input>
			</el-form-item>
			<el-form-item label="类型" prop="fileName">
				<el-input :value="$file.getFileType(fileInfo)" readonly></el-input>
			</el-form-item>
			<el-form-item label="大小" prop="fileSize">
				<el-input
					:value="
						fileInfo.isDir === 0
							? $file.calculateFileSize(fileInfo.fileSize)
							: ''
					"
					readonly
				></el-input>
			</el-form-item>
			<el-form-item
				label="修改日期"
				prop="uploadTime"
				v-if="![7, 8].includes(fileType) && !['Share'].includes(this.routeName)"
			>
				<el-input :value="fileInfo.uploadTime" readonly></el-input>
			</el-form-item>
			<el-form-item label="删除日期" prop="deleteTime" v-if="fileType === 6">
				<el-input :value="fileInfo.deleteTime" readonly></el-input>
			</el-form-item>
			<el-form-item label="分享类型" prop="shareType" v-if="fileType === 8">
				<el-input
					:value="fileInfo.shareType === 1 ? '私密' : '公共'"
					readonly
				></el-input>
			</el-form-item>
			<el-form-item label="分享时间" prop="shareTime" v-if="fileType === 8">
				<el-input :value="fileInfo.shareTime" readonly></el-input>
			</el-form-item>
			<el-form-item
				label="过期时间"
				prop="endTime"
				v-if="fileType === 8"
				class="form-item-end-time"
			>
				<el-input :value="fileInfo.endTime" readonly></el-input>
				<i
					class="status-icon el-icon-warning"
					v-if="$file.getFileShareStatus(fileInfo.endTime)"
				></i>
				<i class="status-icon el-icon-time" v-else></i>
			</el-form-item>
		</el-form>
		<div slot="footer" class="dialog-footer">
			<el-button @click="handleDialogClose">关 闭</el-button>
		</div>
	</el-dialog>
</template>

<script>
import router from '@/router/index.js'

export default {
	name: 'FileDetailInfoDialog',
	data() {
		return {
			visible: false //  对话框是否可见
		}
	},
	computed: {
		// 左侧菜单选中的文件类型
		fileType() {
			return router.currentRoute.query.fileType
				? Number(router.currentRoute.query.fileType)
				: 0
		},
		// 路由名称
		routeName() {
			return router.currentRoute.name
		}
	},
	methods: {
		/**
		 * 查看文件详情对话框 | 对话框关闭的回调
		 * @description 关闭对话框
		 */
		handleDialogClose() {
			this.visible = false
			this.$refs.fileInfoForm.resetFields()
			this.callback('cancel')
		},
		/**
		 * 路径点击事件
		 * @param {object} fileInfo 文件信息
		 */
		handleClickFilePath(fileInfo) {
			router.push({
				query: { filePath: fileInfo.filePath, fileType: 0 }
			})
			this.handleDialogClose()
		}
	}
}
</script>

<style lang="less" scoped>
// --- 样式变量定义（替代被弃用的 Stylus 文件） ---
@PrimaryColor: #409EFF; // 主题蓝
@Warning: #E6A23C;      // 警告黄
@Success: #67C23A;      // 成功绿
@TextColor: #303133;    // 主要文本色
@BorderColor: #DCDFE6;  // 边框颜色

// --- 对话框主体和图片样式 ---
.file-info-dialog {
    /deep/ .el-dialog {
        border-radius: 8px;
        .el-dialog__header {
            border-bottom: 1px solid @BorderColor;
            padding: 15px 20px 10px;
            .el-dialog__title {
                font-weight: 600;
                color: @TextColor;
            }
        }
        .el-dialog__body {
            padding: 20px 30px;
        }
    }
}

.file-img {
    display: block;
    margin: 0 auto 16px auto; // 增加底部间距
    max-width: 120px;
    width: 40%;
    height: auto;
    cursor: pointer; // 提示可点击预览
    transition: transform 0.3s;
    &:hover {
        transform: scale(1.05);
    }
}

// --- 表单样式优化 ---
/deep/ .el-form.file-info-form {
    padding: 0 20px; // 表单内边距，使内容更居中
    .el-form-item {
        margin-bottom: 12px; // 缩小表单项间距
        
        .el-form-item__label {
            color: #909399; // 标签使用辅助文本色
            font-weight: normal;
        }

        .el-form-item__content {
            // 确保输入框内容对齐
            line-height: 20px; 
        }

        .el-input__inner {
            border: none;
            padding: 0;
            height: 20px; // 适应紧凑布局
            line-height: 20px;
            color: @TextColor;
            font-size: 14px;
            background-color: transparent; // 背景透明
        }
        
        // 路径输入框样式美化，提示可点击
        .el-form-item:nth-child(2) .el-input {
            cursor: pointer;
            .el-input__inner {
                color: @PrimaryColor; // 路径文本使用主题色
                text-decoration: underline; // 添加下划线
                &:hover {
                    color: lighten(@PrimaryColor, 10%);
                }
            }
        }

        // 分享过期时间特殊样式
        &.form-item-end-time {
            .el-form-item__content {
                display: flex;
                align-items: center;
                
                .el-input {
                    // 保持输入框宽度，为图标留出空间
                    width: 140px;
                    .el-input__inner {
                        padding-right: 0;
                    }
                }
                
                .status-icon {
                    margin-left: 8px;
                    font-size: 16px; // 调整图标大小
                    &.el-icon-warning {
                        color: @Warning; // 警告色
                    }
                    &.el-icon-time {
                        color: @Success; // 成功色
                    }
                }
            }
        }
    }
}

// --- 底部按钮区 ---
.dialog-footer {
    padding-bottom: 5px;
    .el-button {
        // 确保按钮是主题色，与整体设计一致
        &:hover {
            color: @PrimaryColor;
        }
    }
}
</style>
