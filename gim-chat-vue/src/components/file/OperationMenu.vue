<template>
	<div class="operation-menu-wrapper" :class="'file-type-' + fileType" ref="operationMenuRef">
		<el-button-group class="create-operate-group"
			v-if="(!selectedFiles.length || !isBatchOperation) && fileType === 0">
			<el-dropdown
				class="upload-drop"
				trigger="hover"
				popper-class="docs-operation-dropdown docs-upload-dropdown"
			>
				<el-button size="mini" type="primary" icon="el-icon-upload2" id="uploadFileId">上传<i
						class="el-icon-arrow-down el-icon--right"></i></el-button>
				<el-dropdown-menu slot="dropdown">
					<el-dropdown-item @click.native="openUpload">上传文件</el-dropdown-item>
					<el-dropdown-item @click.native="handleUploadFileBtnClick(2)">上传文件夹</el-dropdown-item>
					<el-dropdown-item @click.native="handleUploadFileBtnClick(3)" title="截图粘贴或拖拽上传"
						:disabled="screenWidth <= 520">拖拽上传</el-dropdown-item>
				</el-dropdown-menu>
			</el-dropdown>

			<el-dropdown
				class="create-drop"
				trigger="hover"
				popper-class="docs-operation-dropdown docs-create-dropdown"
			>
				<el-button size="mini" type="primary" icon="el-icon-plus" id="uploadFileId">新建<i
						class="el-icon-arrow-down el-icon--right"></i></el-button>
				<el-dropdown-menu slot="dropdown">
					<el-dropdown-item @click.native="handleClickAddFolderBtn">
						<div class="img-text-wrapper"><img :src="dirImg" /> 新建文件夹</div>
					</el-dropdown-item>
					<el-dropdown-item divided @click.native="handleCreateFile('docx')">
						<div class="img-text-wrapper"><img :src="wordImg" />Word 文档</div>
					</el-dropdown-item>
					<el-dropdown-item @click.native="handleCreateFile('xlsx')">
						<div class="img-text-wrapper">
							<img :src="excelImg" />Excel 工作表
						</div>
					</el-dropdown-item>
					<el-dropdown-item @click.native="handleCreateFile('pptx')">
						<div class="img-text-wrapper">
							<img :src="pptImg" />PPT 演示文稿
						</div>
					</el-dropdown-item>
				</el-dropdown-menu>
			</el-dropdown>
		</el-button-group>
		<div class="batch-operate-group">
			<el-button-group v-if="isBatchOperation">
				<el-button size="mini" type="primary" v-if="selectedFiles.length" icon="el-icon-delete"
					@click="handleBatchDeleteBtnClick">批量删除</el-button>
				<el-button size="mini" type="primary" v-if="selectedFiles.length && !fileType && fileType !== 6"
					icon="el-icon-rank" @click="handleBatchMoveBtnClick">批量移动</el-button>
				<el-button size="mini" type="primary" v-if="selectedFiles.length && fileType !== 6"
					icon="el-icon-download" @click="handleBatchDownloadBtnClick">批量下载</el-button>
				<el-button size="mini" type="primary" v-if="
					selectedFiles.length && fileType !== 6 && $route.name !== 'Share'
				" icon="el-icon-share" @click="handleBatchShareBtnClick">批量分享</el-button>
			</el-button-group>
		</div>

		<!-- 全局搜索文件 -->
		<el-input v-if="fileType === 0" class="select-file-input" v-model="searchFile.fileName" placeholder="搜索您的文件"
			size="mini" maxlength="255" :clearable="true" @change="handleSearchInputChange"
			@clear="$emit('getTableDataByType')" @keyup.enter.native="handleSearchInputChange(searchFile.fileName)">
			<i slot="prefix" class="el-input__icon el-icon-search" title="点击搜索"
				@click="handleSearchInputChange(searchFile.fileName)"></i>
		</el-input>
		<!-- 批量操作 -->
		<i class="batch-icon el-icon-finished" :class="isBatchOperation ? 'active' : ''"
			:title="isBatchOperation ? '取消批量操作' : '批量操作'" v-if="fileModel === 1 && fileType !== 8"
			@click="handleBatchOperationChange()"></i>
		<i class="refresh-icon el-icon-refresh" title="刷新文件列表" @click="$emit('getTableDataByType')"></i>
		<el-divider direction="vertical"></el-divider>
		<template v-if="screenWidth > 768">
			<!-- 文件展示模式 -->
			<i class="model-icon el-icon-s-fold" :class="{ active: fileGroupLable === 0 }" title="列表模式"
				@click="handleFileDisplayModelChange(0)"></i>
			<i class="model-icon el-icon-s-grid" :class="{ active: fileGroupLable === 1 }" title="网格模式"
				@click="handleFileDisplayModelChange(1)"></i>
			<i class="model-icon el-icon-date" :class="{ active: fileGroupLable === 2 }" title="时间线模式"
				v-if="fileType === 1" @click="handleFileDisplayModelChange(2)"></i>
			<el-divider direction="vertical"></el-divider>
		</template>

		<!-- 操作栏收纳 -->
		<el-popover v-model="operatePopoverVisible" placement="bottom"
			:trigger="screenWidth <= 768 ? 'manual' : 'hover'">
			<i slot="reference" class="setting-icon el-icon-setting"
				@click="operatePopoverVisible = !operatePopoverVisible"></i>
			<!-- 选择表格列 -->
			<SelectColumn></SelectColumn>
			<!-- 文件展示模式 -->
			<div class="change-file-model" v-if="screenWidth <= 768">
				<div class="title">查看模式</div>
				<el-radio-group v-model="fileGroupLable" size="mini" @change="handleFileDisplayModelChange">
					<el-radio-button :label="0">
						<i class="iconfont icon-liebiao1"></i> 列表
					</el-radio-button>
					<el-radio-button :label="1">
						<i class="el-icon-s-grid"></i> 网格
					</el-radio-button>
					<el-radio-button :label="2" v-if="fileType === 1">
						<i class="el-icon-date"></i> 时间线
					</el-radio-button>
				</el-radio-group>
			</div>
			<template v-if="fileGroupLable === 1 || fileGroupLable === 2">
				<el-divider class="split-line"></el-divider>
				<!-- 图标大小调整 -->
				<div class="change-grid-size">
					<div class="title">调整图标大小</div>
					<el-slider v-model="gridSize" :min="20" :max="150" :step="10"
						:format-tooltip="formatTooltip"></el-slider>
				</div>
			</template>
		</el-popover>

		<!-- 多选文件下载，页面隐藏 -->
		<a target="_blank" :href="batchDownloadLink" ref="batchDownloadRef"></a>
	</div>
</template>

<script>
import FileUploader from '@/components/file/box/uploadFile/Box.vue';
import showUploadFileBox from '@/components/file/box/uploadFile/index.js'
import SelectColumn from './SelectColumn.vue'
import { mapState } from 'vuex'
export default {
	name: 'OperationMenu',
	components: { FileUploader },
	props: {
		// 文件类型
		fileType: {
			required: true,
			type: Number
		},
		// 文件路径
		filePath: {
			required: true,
			type: String
		}
	},
	components: {
		SelectColumn
	},
	data() {
		return {
			// 文件搜索数据
			searchFile: {
				fileName: ''
			},
			operatePopoverVisible: false, //  收纳栏是否显示
			fileGroupLable: 0, //  文件展示模式
			dirImg: require('_a/images/file/dir.png'),
			wordImg: require('_a/images/file/file_word.svg'),
			excelImg: require('_a/images/file/file_excel.svg'),
			pptImg: require('_a/images/file/file_ppt.svg')
		}
	},
	computed: {
		...mapState({
			showUploadMask: state => state.uploadFile.showUploadMask
		}),
		// 上传文件组件参数
		uploadFileParams() {
			return {
				filePath: this.filePath,
				isDir: 0
			}
		},
		// 文件查看模式 0 列表模式 1 网格模式 2 时间线模式
		fileModel() {
			return this.$store.getters.fileModel
		},
		// 图标大小
		gridSize: {
			get() {
				return this.$store.getters.gridSize
			},
			set(val) {
				this.$store.commit('changeGridSize', val)
			}
		},
		// 被选中的文件列表
		selectedFiles() {
			return this.$store.state.fileList.selectedFiles
		},
		// 是否批量操作
		isBatchOperation() {
			return this.$store.state.fileList.isBatchOperation
		},
		// 屏幕宽度
		screenWidth() {
			return this.$store.state.common.screenWidth
		},

		// 批量下载文件链接
		batchDownloadLink() {
			return `${this.$config.baseContext
				}/filetransfer/batchDownloadFile?userFileIds=${this.selectedFiles
					.map((item) => item.userFileId)
					.join(',')}`
		}
	},
	watch: {
		// 显示拖拽上传文件遮罩
		showUploadMask() {
			this.handleUploadFileBtnClick(3);
		},
		fileType(newValue, oldValue) {
			console.log('fileType===')
			console.log('newValue=' + newValue)
			console.log('oldValue=' + oldValue)
			if (oldValue === 1 && this.fileModel === 2) {
				this.$store.commit('changeFileModel', 0)
				this.fileGroupLable = 0
			}
		},
		/**
		 * 监听收纳栏状态
		 * @description 打开时，body 添加点击事件的监听
		 */
		operatePopoverVisible(newValue) {
			if (newValue === true) {
				document.body.addEventListener('click', this.closeOperatePopover)
			} else {
				document.body.removeEventListener('click', this.closeOperatePopover)
			}
		}
	},
	mounted() {
		this.fileGroupLable = this.fileModel
	},
	methods: {
		/**
		 * 新建文件夹按钮点击事件
		 * @description 调用新建文件夹服务，并在弹窗确认回调事件中刷新文件列表
		 */
		handleClickAddFolderBtn() {
			this.$openDialog
				.addFolder({
					filePath: this.$route.query.filePath || '/'
				})
				.then((res) => {
					if (res === 'confirm') {
						this.$emit('getTableDataByType')
					}
				})
		},
		/**
		 * 新建 office 文件
		 * @description 调用新建 office 文件服务，并在弹窗确认回调事件中刷新文件列表
		 * @param {string} 文件扩展名 docx xlsx pptx
		 */
		handleCreateFile(extendName) {
			this.$openDialog
				.addFile({
					extendName: extendName
				})
				.then((res) => {
					if (res === 'confirm') {
						this.$emit('getTableDataByType')
					}
				})
		},
		/**
		 * 上传文件
		 */
		async openUpload() {
			console.log('this.$openBox.uploadFile')
			console.log('filePath === ')
			console.log(this.filePath)
			console.log('fileType === ')
			console.log(this.fileType)
			const result = await showUploadFileBox({
				params: {
					headers: { token: 'abc-123' },
					userId: 10001,
					filePath: this.filePath
				},
				uploadWay: 1, // 1=上传文件
				serviceEl: this,
				callType: 1
			})
			console.log('result')
			console.log(result)

			if (result !== 'cancel') {
				console.log('上传结果：', result)
			}
		},

		/**
		 * 批量删除按钮点击事件
		 * @description 区分 删除到回收站中 | 在回收站中彻底删除，调用相应的删除文件接口
		 */
		handleBatchDeleteBtnClick() {
			this.$openDialog
				.deleteFile({
					isBatchOperation: true,
					fileInfo: this.selectedFiles,
					deleteMode: this.fileType === 6 ? 2 : 1 //  删除模式：1-删除到回收站 2-彻底删除
				})
				.then((res) => {
					if (res === 'confirm') {
						this.$emit('getTableDataByType')
						this.$store.dispatch('showStorage')
					}
				})
		},
		/**
		 * 批量移动按钮点击事件
		 */
		handleBatchMoveBtnClick() {
			if (this.selectedFiles.length > 0) {
				this.$openDialog
					.moveFile({
						isBatchOperation: true,
						fileInfo: this.selectedFiles
					})
					.then((res) => {
						if (res === 'confirm') {
							this.$emit('getTableDataByType')
						}
					})
			} else {
				this.$message.warning('请先勾选文件')
			}
		},
		/**
		 * 批量分享按钮点击事件
		 */
		handleBatchShareBtnClick() {
			this.$openDialog.shareFile({
				fileInfo: this.selectedFiles.map((item) => {
					return {
						userFileId: item.userFileId
					}
				})
			})
		},
		/**
		 * 打包批量下载按钮点击事件
		 */
		handleBatchDownloadBtnClick() {
			console.log('打包批量下载文件操作')
			console.log(this.batchDownloadLink)
			this.$refs['batchDownloadRef'].click()
		},
		/**
		 * 搜索输入框搜索事件
		 * @param {string} value 搜索内容
		 */
		handleSearchInputChange(value) {
			if (value === '') {
				this.$emit('getTableDataByType')
			} else {
				this.$emit('getSearchFileList', value)
			}
		},
		/**
		 * 网格模式下，批量操作状态切换
		 */
		handleBatchOperationChange() {
			this.$store.commit('changeIsBatchOperation', !this.isBatchOperation)
		},
		/**
		 * 文件查看模式切换
		 * @param {number} label 0 列表 1 网格 2 时间线
		 */
		handleFileDisplayModelChange(label) {
			this.fileGroupLable = label
			// 关闭右键菜单事件
			this.$openBox.contextMenu.close()
			this.$store.commit('changeFileModel', label)
			this.handleSearchInputChange(this.searchFile.fileName)
			// 关闭收纳栏
			this.operatePopoverVisible = false
		},
		/**
		 * 格式化图标大小显示
		 * @param {number} val 改变后的数值
		 */
		formatTooltip(val) {
			return `${val}px`
		},
		/**
		 * 关闭收纳栏
		 */
		closeOperatePopover(event) {
			if (!event.target.className.includes('setting-icon')) {
				this.operatePopoverVisible = false
			}
		}
	}
}
</script>


<style lang="less" scoped>
// ⚠️ 注意：如果您的项目使用 Less，请确保在项目中定义了以下变量。
// 示例变量 (用于演示效果)
@PrimaryColor: #409EFF; // 主题色，蓝色
@WarningColor: #F56C6C; // 警告色，红色
@SecondaryText: #909399; // 辅助文本色
@BorderColor: #EBEEF5; // 边框颜色

@RegularText: #606266; // 正文文本颜色，Element UI 默认

// --- 基础布局和间距优化 ---
.operation-menu-wrapper {
	padding: 16px 0;
	display: flex;
	justify-content: flex-start;
	align-items: center;
	flex-wrap: wrap;
	gap: 16px 20px;
	border-bottom: 1px solid @BorderColor;

	// --- 核心操作区：上传/新建 (已修改为浅色风格) ---
	.create-operate-group {
		display: flex;

		.upload-drop,
		.create-drop {
			margin-right: 0;
			margin-left: 10px;
		}

		// 确保 Element UI 按钮组样式统一美观
		/deep/ .el-button {
			padding: 8px 18px;
			font-weight: 500;

			// 继承批量操作的浅色样式
			background: lighten(@PrimaryColor, 55%);
			color: @PrimaryColor;
			border: 1px solid lighten(@PrimaryColor, 50%);

			&:hover,
			&:focus {
				background: lighten(@PrimaryColor, 50%); // 保持 hover 浅色
				border-color: lighten(@PrimaryColor, 45%);
			}

			// 确保按钮组之间的边框自然过渡
			&:first-child {
				border-radius: 4px 0 0 4px;
			}

			&:last-child {
				border-radius: 0 4px 4px 0;
			}
		}

		// 覆盖默认的 el-button-group 样式
		&:last-child {
			/deep/ .el-button {
				// 修改连接处边框颜色，使其与主题色浅边框融合
				border-left: 1px solid lighten(@PrimaryColor, 45%);

				&:hover {
					z-index: 1;
				}
			}
		}
	}

	// --- 批量操作区 (保持不变，因为它是您期望的样式) ---
	.batch-operate-group {
		display: flex;
		align-items: center;

		.el-button-group {
			display: flex;
			gap: 8px;

			/deep/ .el-button {
				border-radius: 4px !important;
				padding: 8px 15px;
				font-weight: normal;

				// 批量按钮使用浅背景 + 主题色文本
				background: lighten(@PrimaryColor, 55%);
				color: @PrimaryColor;
				border: 1px solid lighten(@PrimaryColor, 50%);

				&:hover {
					background: lighten(@PrimaryColor, 50%);
				}
			}

			// 批量删除 (通常是第一个) 使用警告色
			/deep/ .el-button:nth-child(1) {
				background: #fcebeb; // 浅红色
				color: @WarningColor;
				border-color: #fcebeb;

				&:hover {
					background: #f6e0e0;
				}
			}
		}
	}

	// --- 搜索框 ---
	.select-file-input {
		width: 250px;
		margin-left: auto;
		margin-right: 16px;

		/deep/ .el-input__inner {
			border-radius: 20px;
			padding-left: 35px !important;
			height: 32px;
		}

		.el-icon-search {
			cursor: pointer;
			font-size: 16px;
			color: @SecondaryText;

			&:hover {
				color: @PrimaryColor;
			}
		}
	}

	// --- 右侧工具图标组 ---
	.batch-icon,
	.model-icon,
	.refresh-icon,
	.setting-icon {
		font-size: 20px;
		cursor: pointer;
		color: @SecondaryText;
		margin-right: 12px;
		transition: color 0.2s;

		&:hover {
			color: @PrimaryColor;
		}

		&:last-of-type {
			margin-right: 0;
		}
	}

	.batch-icon.active,
	.model-icon.active {
		color: @PrimaryColor;
	}

	.el-divider {
		margin: 0 10px;
	}
}

// --- 辅助样式 ---
.operation-menu-wrapper.file-type-6 {
	justify-content: flex-end;

	.create-operate-group {
		display: none;
	}

	.select-file-input {
		margin-left: 0;
	}
}

.img-text-wrapper {
	display: flex;
	align-items: center;

	img {
		margin-right: 8px;
		height: 18px;
	}
}

.split-line {
	margin: 8px 0;
	background-color: @BorderColor;
}

// --- Dropdown 菜单样式优化 (保留) ---
/deep/ .el-dropdown-menu {
	border: 1px solid @BorderColor;
	border-radius: 4px;
	box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
	padding: 6px 0;

	.el-dropdown-menu__item {
		padding: 0 16px;
		line-height: 36px;
		min-width: 140px;
		color: @RegularText;
		transition: background-color 0.2s, color 0.2s;

		&:hover {
			background-color: lighten(@PrimaryColor, 55%);
			color: @PrimaryColor;
		}

		&.el-dropdown-menu__item--divided {
			margin-top: 6px;
			border-top: 1px solid @BorderColor;

			&::before {
				display: none;
			}
		}

		.img-text-wrapper {
			display: flex;
			align-items: center;

			img {
				margin-right: 8px;
				height: 18px;
			}
		}
	}
}

// 针对上传菜单项，添加图标样式（保留）
.upload-drop {
	/deep/ .el-dropdown-menu__item:nth-child(1)::before {
		content: "\e647";
		font-family: 'element-icons' !important;
		margin-right: 8px;
		font-size: 16px;
	}

	.el-dropdown-menu__item:nth-child(2)::before {
		content: "\e643";
		font-family: 'element-icons' !important;
		margin-right: 8px;
		font-size: 16px;
	}

	.el-dropdown-menu__item:nth-child(3)::before {
		content: "\e60d";
		font-family: 'element-icons' !important;
		margin-right: 8px;
		font-size: 16px;
	}
}
</style>

<style lang="less">
.docs-operation-dropdown {
	padding: 8px 0;
	border-radius: 16px;
	border: 1px solid rgba(148, 163, 184, 0.16);
	background: rgba(11, 19, 35, 0.98);
	box-shadow: 0 24px 48px rgba(0, 0, 0, 0.24);
	backdrop-filter: blur(18px);
}

.docs-operation-dropdown[x-placement^='bottom'] {
	margin-top: 10px;
}

.docs-operation-dropdown .popper__arrow,
.docs-operation-dropdown .popper__arrow::after {
	display: none;
}

.docs-operation-dropdown .el-dropdown-menu__item {
	display: flex;
	align-items: center;
	min-width: 176px;
	padding: 0 16px;
	line-height: 40px;
	font-size: 13px;
	font-weight: 500;
	color: #edf4ff;
	background: transparent;
	transition: background-color 0.2s ease, color 0.2s ease;
}

.docs-operation-dropdown .el-dropdown-menu__item:focus,
.docs-operation-dropdown .el-dropdown-menu__item:not(.is-disabled):hover {
	background: rgba(110, 168, 255, 0.12);
	color: #6ea8ff;
}

.docs-operation-dropdown .el-dropdown-menu__item.is-disabled {
	color: rgba(147, 167, 199, 0.45);
	background: transparent;
}

.docs-operation-dropdown .el-dropdown-menu__item--divided {
	margin-top: 6px;
	border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.docs-operation-dropdown .el-dropdown-menu__item--divided::before {
	display: none;
}

.docs-operation-dropdown .img-text-wrapper {
	display: inline-flex;
	align-items: center;
	gap: 8px;
	line-height: 1;
}

.docs-operation-dropdown .img-text-wrapper img {
	width: 18px;
	height: 18px;
	flex: 0 0 18px;
	object-fit: contain;
}

.docs-upload-dropdown .el-dropdown-menu__item::before {
	font-family: 'element-icons' !important;
	font-size: 16px;
	margin-right: 8px;
	color: rgba(147, 167, 199, 0.92);
	transition: color 0.2s ease;
}

.docs-upload-dropdown .el-dropdown-menu__item:nth-child(1)::before {
	content: "\e647";
}

.docs-upload-dropdown .el-dropdown-menu__item:nth-child(2)::before {
	content: "\e643";
}

.docs-upload-dropdown .el-dropdown-menu__item:nth-child(3)::before {
	content: "\e60d";
}

.docs-upload-dropdown .el-dropdown-menu__item:focus::before,
.docs-upload-dropdown .el-dropdown-menu__item:not(.is-disabled):hover::before {
	color: #6ea8ff;
}

.docs-upload-dropdown .el-dropdown-menu__item.is-disabled::before {
	color: rgba(147, 167, 199, 0.35);
}
</style>

<style lang="less" scoped>
@panel-soft: rgba(255, 255, 255, 0.04);
@panel-hover: rgba(110, 168, 255, 0.08);
@border-color: rgba(148, 163, 184, 0.15);
@border-strong: rgba(110, 168, 255, 0.22);
@text-main: #edf4ff;
@text-sub: #93a7c7;
@accent: #6ea8ff;
@danger: #ff7f88;

.operation-menu-wrapper {
	padding: 4px 0;
	gap: 14px;
	border-bottom: none;
}

.create-operate-group,
.batch-operate-group {
	display: flex;
	align-items: center;
}

.create-operate-group {
	gap: 10px;
}

.create-operate-group ::v-deep .el-button,
.batch-operate-group ::v-deep .el-button {
	height: 38px;
	padding: 0 18px;
	border-radius: 14px !important;
	font-size: 13px;
	font-weight: 600;
	border: 1px solid @border-strong;
	box-shadow: none;
	transition: all 0.2s ease;
}

.create-operate-group ::v-deep .el-button {
	background: linear-gradient(145deg, rgba(110, 168, 255, 0.92), rgba(124, 140, 255, 0.8));
	color: #f8fbff;
}

.create-operate-group ::v-deep .el-button:hover,
.create-operate-group ::v-deep .el-button:focus {
	transform: translateY(-1px);
	box-shadow: 0 14px 30px rgba(110, 168, 255, 0.2);
}

.batch-operate-group .el-button-group {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
}

.batch-operate-group ::v-deep .el-button {
	background: @panel-soft;
	color: @text-main;
	border-color: @border-color;
}

.batch-operate-group ::v-deep .el-button:hover,
.batch-operate-group ::v-deep .el-button:focus {
	background: @panel-hover;
	border-color: @border-strong;
	color: @accent;
}

.batch-operate-group ::v-deep .el-button:first-child {
	color: @danger;
	border-color: rgba(255, 127, 136, 0.22);
	background: rgba(255, 127, 136, 0.08);
}

.batch-operate-group ::v-deep .el-button:first-child:hover,
.batch-operate-group ::v-deep .el-button:first-child:focus {
	background: rgba(255, 127, 136, 0.14);
}

.select-file-input {
	width: 280px;
	margin-left: auto;
	margin-right: 0;
}

.select-file-input ::v-deep .el-input__inner {
	height: 40px;
	padding-left: 40px !important;
	border-radius: 16px;
	border: 1px solid @border-color;
	background: rgba(255, 255, 255, 0.04);
	color: @text-main;
}

.select-file-input ::v-deep .el-input__inner:focus {
	border-color: @border-strong;
	box-shadow: 0 0 0 3px rgba(110, 168, 255, 0.08);
}

.select-file-input .el-icon-search {
	color: @text-sub;
}

.select-file-input .el-icon-search:hover {
	color: @accent;
}

.batch-icon,
.model-icon,
.refresh-icon,
.setting-icon {
	width: 38px;
	height: 38px;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	border-radius: 12px;
	background: rgba(255, 255, 255, 0.03);
	border: 1px solid transparent;
	color: @text-sub;
	font-size: 18px;
	transition: all 0.2s ease;
}

.batch-icon:hover,
.model-icon:hover,
.refresh-icon:hover,
.setting-icon:hover,
.batch-icon.active,
.model-icon.active {
	color: @accent;
	background: @panel-hover;
	border-color: @border-strong;
}

.el-divider {
	height: 24px;
	margin: 0 2px;
	background: rgba(148, 163, 184, 0.18);
}

.operation-menu-wrapper.file-type-6 {
	justify-content: flex-end;
}

.operation-menu-wrapper.file-type-6 .create-operate-group {
	display: none;
}

.operation-menu-wrapper.file-type-6 .select-file-input {
	margin-left: 0;
}

.split-line {
	background: rgba(148, 163, 184, 0.14);
}

/deep/ .el-dropdown-menu {
	padding: 8px 0;
	border-radius: 16px;
	border: 1px solid rgba(148, 163, 184, 0.16);
	background: rgba(11, 19, 35, 0.96);
	box-shadow: 0 24px 48px rgba(0, 0, 0, 0.22);
}

/deep/ .el-dropdown-menu__item {
	color: @text-main;
}

/deep/ .el-dropdown-menu__item:hover {
	background: rgba(110, 168, 255, 0.1);
	color: @accent;
}

@media (max-width: 768px) {
	.operation-menu-wrapper {
		gap: 12px;
	}

	.select-file-input {
		width: 100%;
		order: 10;
		margin-left: 0;
	}
}
</style>
