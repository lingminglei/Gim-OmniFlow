<template>
	<div class="file-table-wrapper">
		<el-table
			class="file-table"
			:class="['file-type-' + fileType, routeName === 'Share' ? 'share' : '']"
			ref="multipleTable"
			fit
			v-loading="loading"
			element-loading-text="文件加载中..."
			tooltip-effect="dark"
			:data="fileList"
			:highlight-current-row="true"
			@selection-change="handleSelectRow"
			@sort-change="handleSortChange"
			@row-contextmenu="handleContextMenu"
		>
			<el-table-column type="selection" key="selection" width="56" align="center" v-if="fileType !== 8"></el-table-column>
			<el-table-column
				label
				prop="isDir"
				key="isDir"
				:width="screenWidth <= 768 ? 40 : 56"
				align="center"
				class-name="file-icon-column"
			>
				<template slot-scope="scope">
					<video
						style="width: 30px; max-height: 30px; cursor: pointer"
						v-if="$file.isVideoFile(scope.row)"
						:src="$file.setFileImg(scope.row)"
					></video>
					<img
						:src="$file.setFileImg(scope.row)"
						:title="`${scope.row.isDir ? '' : '点击预览'}`"
						style="width: 30px; max-height: 30px; cursor: pointer"
						@click="$file.handleFileNameClick(scope.row, scope.$index, sortedFileList)"
						v-else
					/>
				</template>
			</el-table-column>
			<el-table-column prop="fileName" key="fileName" :sort-by="['isDir', 'fileName']" sortable show-overflow-tooltip>
				<template slot="header">
					<span>文件名</span>
				</template>
				<template slot-scope="scope">
					<span @click="$file.handleFileNameClick(scope.row, scope.$index, sortedFileList)">
						<span
							class="file-name"
							style="cursor: pointer"
							:title="`${scope.row.isDir ? '' : '点击预览'}`"
							v-html="$file.getFileNameComplete(scope.row, true)"
						></span>
						<div class="file-info" v-if="screenWidth <= 768">
							{{ scope.row.uploadTime }}
							<span class="file-size">
								{{ scope.row.isDir === 0 ? $file.calculateFileSize(scope.row.fileSize) : '' }}
							</span>
						</div>
					</span>
				</template>
			</el-table-column>
			<el-table-column
				:label="fileType === 6 ? '原路径' : '路径'"
				prop="filePath"
				key="filePath"
				show-overflow-tooltip
				v-if="![0, 8].includes(Number($route.query.fileType)) && routeName !== 'Share' && screenWidth > 768"
			>
				<template slot-scope="scope">
					<span
						style="cursor: pointer"
						title="点击跳转"
						@click="
							$router.push({
								query: { filePath: scope.row.filePath, fileType: 0 }
							})
						"
					>{{ scope.row.filePath }}</span>
				</template>
			</el-table-column>
			<el-table-column
				label="类型"
				width="80"
				prop="extendName"
				key="extendName"
				:sort-by="['isDir', 'extendName']"
				sortable
				show-overflow-tooltip
				v-if="selectedColumnList.includes('extendName') && screenWidth > 768"
			>
				<template slot-scope="scope">
					<span>{{ $file.getFileType(scope.row) }}</span>
				</template>
			</el-table-column>
			<el-table-column
				label="大小"
				width="100"
				prop="fileSize"
				key="fileSize"
				:sort-by="['isDir', 'fileSize']"
				sortable
				align="right"
				v-if="selectedColumnList.includes('fileSize') && screenWidth > 768"
			>
				<template slot-scope="scope">
					{{ scope.row.isDir === 0 ? $file.calculateFileSize(scope.row.fileSize) : '' }}
				</template>
			</el-table-column>
			<el-table-column
				label="修改日期"
				prop="uploadTime"
				key="uploadTime"
				width="160"
				:sort-by="['isDir', 'uploadTime']"
				sortable
				align="center"
				v-if="
					selectedColumnList.includes('uploadTime') &&
					![7, 8].includes(fileType) &&
					!['Share'].includes(this.routeName) &&
					screenWidth > 768
				"
			></el-table-column>
			<el-table-column
				label="删除日期"
				prop="deleteTime"
				key="deleteTime"
				width="160"
				:sort-by="['isDir', 'deleteTime']"
				sortable
				align="center"
				v-if="fileType === 6 && selectedColumnList.includes('deleteTime') && screenWidth > 768"
			></el-table-column>
			<el-table-column
				label="分享类型"
				prop="shareType"
				key="shareType"
				width="100"
				align="center"
				v-if="fileType === 8 && screenWidth > 768"
			>
				<template slot-scope="scope">
					{{ scope.row.shareType === 1 ? '私密' : '公开' }}
				</template>
			</el-table-column>
			<el-table-column
				label="分享时间"
				prop="shareTime"
				key="shareTime"
				width="160"
				:sort-by="['isDir', 'shareTime']"
				show-overflow-tooltip
				sortable
				align="center"
				v-if="fileType === 8 && screenWidth > 768"
			></el-table-column>
			<el-table-column
				label="过期时间"
				prop="endTime"
				key="endTime"
				width="190"
				:sort-by="['isDir', 'endTime']"
				show-overflow-tooltip
				sortable
				align="center"
				v-if="fileType === 8 && screenWidth > 768"
			>
				<template slot-scope="scope">
					<div>
						<i class="el-icon-warning" v-if="$file.getFileShareStatus(scope.row.endTime)"></i>
						<i class="el-icon-time" v-else></i>
						{{ scope.row.endTime }}
					</div>
				</template>
			</el-table-column>
			<el-table-column label="" key="operation" width="48" v-if="screenWidth <= 768">
				<template slot-scope="scope">
					<i class="file-operate el-icon-more" :class="`operate-more-${scope.$index}`" @click="handleClickMore(scope.row, $event)"></i>
				</template>
			</el-table-column>
		</el-table>
	</div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
	name: 'FileTable',
	props: {
		fileType: {
			required: true,
			type: Number
		},
		filePath: {
			required: true,
			type: String
		},
		fileList: {
			required: true,
			type: Array
		},
		loading: {
			required: true,
			type: Boolean
		}
	},
	data() {
		return {
			officeFileType: ['ppt', 'pptx', 'doc', 'docx', 'xls', 'xlsx'],
			sortedFileList: []
		}
	},
	computed: {
		...mapGetters(['selectedColumnList']),
		routeName() {
			return this.$route.name
		},
		screenWidth() {
			return this.$store.state.common.screenWidth
		}
	},
	watch: {
		filePath() {
			this.clearSelectedTable()
			this.$refs.multipleTable.clearSort()
		},
		fileType() {
			this.clearSelectedTable()
			this.$refs.multipleTable.clearSort()
		},
		fileList() {
			this.clearSelectedTable()
			this.$refs.multipleTable.clearSort()
			this.sortedFileList = this.fileList
		}
	},
	methods: {
		handleSortChange() {
			this.sortedFileList = this.$refs.multipleTable.tableData
		},
		handleContextMenu(row, column, event) {
			event.cancelBubble = true
			if (this.screenWidth > 768) {
				event.preventDefault()
				this.$refs.multipleTable.setCurrentRow(row)
				this.$openBox
					.contextMenu({
						selectedFile: row,
						domEvent: event
					})
					.then((res) => {
						this.$refs.multipleTable.setCurrentRow()
						if (res === 'confirm') {
							this.$emit('getTableDataByType')
						}
					})
			}
		},
		clearSelectedTable() {
			this.$refs.multipleTable.clearSelection()
		},
		handleSelectRow(selection) {
			this.$store.commit('changeSelectedFiles', selection)
			this.$store.commit('changeIsBatchOperation', selection.length !== 0)
		},
		handleClickMore(row, event) {
			this.$refs.multipleTable.setCurrentRow(row)
			this.$openBox
				.contextMenu({
					selectedFile: row,
					domEvent: event
				})
				.then((res) => {
					this.$refs.multipleTable.setCurrentRow()
					if (res === 'confirm') {
						this.$emit('getTableDataByType')
					}
				})
		}
	}
}
</script>

<style lang="less" scoped>
@panel-bg: linear-gradient(180deg, rgba(18, 28, 47, 0.88), rgba(11, 18, 33, 0.86));
@border-color: rgba(148, 163, 184, 0.16);
@text-main: #edf4ff;
@text-sub: #9cb0cf;
@text-muted: #6f809e;
@accent: #6ea8ff;

.file-table-wrapper {
	min-height: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	padding: 18px 20px;
	border-radius: 24px;
	background: @panel-bg;
	border: 1px solid @border-color;
	box-shadow:
		inset 0 1px 0 rgba(255, 255, 255, 0.04),
		0 18px 36px rgba(0, 0, 0, 0.18);
	overflow: hidden;
}

.file-table {
	flex: 1;
	min-height: 0;
	width: 100%;
}

.file-name {
	color: @text-main;
	font-weight: 500;
}

.file-info {
	margin-top: 4px;
	font-size: 12px;
	color: @text-muted;
}

.file-size {
	margin-left: 8px;
	font-family: 'Consolas', 'Courier New', monospace;
}

.file-operate {
	width: 30px;
	height: 30px;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	border-radius: 10px;
	background: rgba(255, 255, 255, 0.05);
	color: @text-sub;
	transition: all 0.2s ease;
}

.file-operate:hover {
	color: @accent;
	background: rgba(110, 168, 255, 0.1);
}

.file-table-wrapper ::v-deep .el-loading-mask {
	background: rgba(7, 12, 24, 0.72) !important;
	backdrop-filter: blur(6px);
}

.file-table-wrapper ::v-deep .el-table,
.file-table-wrapper ::v-deep .el-table__expanded-cell,
.file-table-wrapper ::v-deep .el-table tr,
.file-table-wrapper ::v-deep .el-table th,
.file-table-wrapper ::v-deep .el-table td {
	background: transparent !important;
}

.file-table-wrapper ::v-deep .el-table {
	color: @text-main;
}

.file-table-wrapper ::v-deep .el-table::before {
	background: rgba(148, 163, 184, 0.12);
}

.file-table-wrapper ::v-deep .el-table th.el-table__cell {
	height: 52px;
	color: @text-muted;
	font-size: 12px;
	font-weight: 600;
	border-bottom-color: rgba(148, 163, 184, 0.14);
	background: linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.01)) !important;
}

.file-table-wrapper ::v-deep .el-table td.el-table__cell {
	border-bottom-color: rgba(148, 163, 184, 0.08);
}

.file-table-wrapper ::v-deep .el-table .cell {
	color: @text-main;
}

.file-table-wrapper ::v-deep .el-table .file-icon-column .cell {
	display: flex;
	align-items: center;
	justify-content: center;
}

.file-table-wrapper ::v-deep .el-table--enable-row-hover .el-table__body tr:hover > td {
	background: rgba(110, 168, 255, 0.06) !important;
}

.file-table-wrapper ::v-deep .el-table__body tr.current-row > td {
	background: rgba(110, 168, 255, 0.08) !important;
}

.file-table-wrapper ::v-deep .el-checkbox__inner {
	background: rgba(255, 255, 255, 0.04);
	border-color: rgba(148, 163, 184, 0.22);
}

.file-table-wrapper ::v-deep .el-checkbox__inner:hover {
	border-color: @accent;
}

.file-table-wrapper ::v-deep .el-checkbox__input.is-checked .el-checkbox__inner,
.file-table-wrapper ::v-deep .el-checkbox__input.is-indeterminate .el-checkbox__inner {
	background: @accent;
	border-color: @accent;
}

.file-table-wrapper ::v-deep .el-table__empty-block {
	background: transparent;
}

@media (max-width: 768px) {
	.file-table-wrapper {
		padding: 14px;
		border-radius: 20px;
	}
}
</style>
