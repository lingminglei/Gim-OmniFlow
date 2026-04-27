<!-- 现在弃用：直接整合到 DocsView 中 -->
<template>
	<div class="file-list-wrapper">
		<!-- 操作按钮 -->
		<el-header height="auto">
			<OperationMenu :fileType="fileType" :filePath="filePath" @getSearchFileList="getSearchFileList"
				@getTableDataByType="getTableDataByType"></OperationMenu>
		</el-header>
		<div class="middle-wrapper">
			<!-- 面包屑导航栏 -->
			<BreadCrumb class="breadcrumb" :fileType="fileType" :filePath="filePath"
				@getTableDataByType="getTableDataByType"></BreadCrumb>
		</div>
		<!-- 文件列表-表格模式 -->
		<FileTable :fileType="fileType" :filePath="filePath" :fileList="fileList" :loading.sync="loading"
			v-if="fileModel === 0" @getTableDataByType="getTableDataByType" @click.native.right="handleClickRight">
		</FileTable>
		<!-- 文件列表-网格模式 -->
		<FileGrid :fileType="fileType" :filePath="filePath" :fileList="fileList" :loading="loading"
			v-if="fileModel === 1" @getTableDataByType="getTableDataByType" @click.native.right="handleClickRight">
		</FileGrid>
		<!-- 图片-时间线模式 -->
		<FileTimeLine class="image-model" :fileList="fileList" :loading.sync="loading"
			v-if="fileModel === 2 && fileType === 1" @getTableDataByType="getTableDataByType"
			@click.native.right="handleClickRight"></FileTimeLine>
		<div class="pagination-wrapper">
			<div class="current-page-count">当前页{{ fileList.length }}条</div>
			<!-- 回收站不展示分页组件 -->
			<el-pagination :current-page="pageData.currentPage" :page-size="pageData.pageCount" :total="pageData.total"
				:page-sizes="[10, 50, 100, 200]" :layout="screenWidth <= 768
					? 'total, prev, next, jumper'
					: 'sizes, total, prev, pager, next'
					" @current-change="handleCurrentChange" @size-change="handleSizeChange" v-if="fileType !== 6">
			</el-pagination>
		</div>
	</div>
</template>

<script>
import FileTable from '@/components/file/FileTable.vue'
import FileGrid from './components/FileGrid.vue'
import FileTimeLine from './components/FileTimeLine.vue'
import OperationMenu from '@/components/file/OperationMenu.vue'
import BreadCrumb from '@/components/file/components/BreadCrumb.vue'
import {
	getFileListByPath,
	getFileListByType,
	getRecoveryFile,
	getMyShareFileList,
	searchFile
} from '@/api/file.js'

export default {
	name: 'FileList',
	components: {
		FileTable,
		OperationMenu,
		FileGrid,
		BreadCrumb,
		FileTimeLine
	},
	props: {
		refreshData: {
			type: String,
			required: true,
			default: '0'
		}
	},
	data() {
		return {
			fileNameSearch: '',
			loading: true, //  表格数据-loading
			fileList: [], //  表格数据-文件列表
			// 分页数据
			pageData: {
				currentPage: 1,
				pageCount: 10,
				total: 0
			}
		}
	},
	computed: {
		// 左侧菜单选中的文件类型
		fileType() {
			return this.$route.query.fileType ? Number(this.$route.query.fileType) : 0
		},
		// 当前所在路径
		filePath() {
			return this.$route.query.filePath ? this.$route.query.filePath : '/'
		},
		// 文件查看模式 0列表模式 1网格模式 2 时间线模式
		fileModel() {
			return this.$store.getters.fileModel
		},
		// 屏幕宽度
		screenWidth() {
			return this.$store.state.common.screenWidth
		}
	},
	watch: {
		//lml
		filePath() {
			// 当左侧菜单选择“全部”或“我的分享”，文件路径发生变化时，再重新获取文件列表
			if (this.$route.name === 'File' && [0, 8].includes(this.fileType)) {
				this.setPageCount()
				this.getTableDataByType()
			}
		},
		//lml
		fileType() {
			if (this.$route.name === 'File') {
				this.setPageCount()
				this.getTableDataByType()
			}
		},
		// 监听文件查看模式
		fileModel() {
			this.setPageCount()
		},
		refreshData(newVal) {
			// 每当 message 变化，执行逻辑
			console.log('父组件传值变化:', newVal)

			this.showFileList()
		}
	},
	created() {
		this.setPageCount()
		//lml
		this.getTableDataByType()
	},
	methods: {

		/**
		 * 文件展示区域的空白处右键事件
		 * @param {Document} event 右键事件对象
		 * //lml
		 */
		handleClickRight(event) {
			console.log('文件展示区域的空白处右键事件:')
			event.preventDefault()
			// 只有在全部页面才可以进行以下操作
			if (![6, 8].includes(this.fileType)) {
				this.$openBox
					.contextMenu({
						selectedFile: undefined,
						domEvent: event,
						serviceEl: this
					})
					.then((res) => {
						if (res === 'confirm') {
							this.getTableDataByType() //  刷新文件列表
							// this.$store.dispatch('showStorage') //  刷新存储容量
						}
					})
			}
		},
		/**
		 * 表格数据获取相关事件 | 调整分页大小
		 */
		setPageCount() {
			this.pageData.currentPage = 1
			if (this.fileModel === 0) {
				this.pageData.pageCount = 50
			}
			if (this.fileModel === 1) {
				this.pageData.pageCount = 100
			}
		},
		/**
		 * 表格数据获取相关事件 | 获取文件列表数据
		 * lml
		 */
		getTableDataByType() {
			this.loading = true
			console.log('getTableDataByType===')
			// 分类型
			if (Number(this.fileType)) {
				switch (Number(this.fileType)) {
					case 6: {
						this.showFileRecovery() //  回收站
						break
					}
					case 8: {
						this.showMyShareFile() //  我的分享
						break
					}
					default: {
						this.showFileList()
						break
					}
				}
			} else {
				// 全部文件
				this.showFileList()
			}
			// this.$store.dispatch('showStorage')
		},
		/**
		 * 表格数据获取相关事件 | 获取当前路径下的文件列表
		 */
		showFileList() {
			console.log('showFileList==')
			let data = {
				fileType: this.fileType,
				filePath: this.filePath,
				currentPage: this.pageData.currentPage,
				pageCount: this.pageData.pageCount
			}
			if (this.refreshData) {
				data.fileType = this.refreshData
			}
			getFileListByPath(data).then((res) => {
				if (res && res.data && res.data.code === 200) {
					this.fileList = res.data.data.records
					this.pageData.total = Number(res.data.data.total)
					this.loading = false
				} else {
					this.$message.error(res.data.message)

					this.loading = false
				}
			})
		},
		/**
		 * 表格数据获取相关事件 | 分页组件 | 当前页码改变
		 * 
		 * //lml
		 */
		handleCurrentChange(currentPage) {
			this.pageData.currentPage = currentPage
			this.getTableDataByType()
		},
		/**
		 * 表格数据获取相关事件 | 分页组件 | 页大小改变时
		 * //lml
		 */
		handleSizeChange(pageCount) {
			this.pageData.pageCount = pageCount
			this.getTableDataByType()
		},
		/**
		 * 表格数据获取相关事件 | 获取回收站文件列表
		 */
		showFileRecovery() {
			console.log('showFileRecovery==')
			getRecoveryFile().then((res) => {
				if (res.success) {
					this.fileList = res.dataList
					this.loading = false
				} else {
					this.$message.error(res.message)
				}
			})
		},
		/**
		 * 表格数据获取相关事件 | 获取我的分享列表
		 */
		showMyShareFile() {
			console.log('showMyShareFile==')
			let data = {
				shareFilePath: this.filePath,
				shareBatchNum: this.$route.query.shareBatchNum,
				currentPage: this.pageData.currentPage,
				pageCount: this.pageData.pageCount
			}
			getMyShareFileList(data).then((res) => {
				if (res.success) {
					this.fileList = res.dataList
					this.pageData.total = Number(res.total)
					this.loading = false
				} else {
					this.$message.error(res.message)
				}
			})
		},
		/**
		 * 表格数据获取相关事件 | 根据文件类型展示文件列表
		 */
		showFileListByType() {
			console.log('showFileListByType==')
			//  分类型
			let data = {
				fileType: this.fileType,
				currentPage: this.pageData.currentPage,
				pageCount: this.pageData.pageCount
			}
			// 模拟数据
			const res = {
				"success": true,
				"code": 0,
				"message": "成功",
				"data": null,
				"dataList": [
					{
						"fileId": null,
						"timeStampName": null,
						"fileUrl": null,
						"fileSize": null,
						"storageType": null,
						"pointCount": null,
						"identifier": null,
						"userFileId": "1896819986112012288",
						"userId": 1,
						"fileName": "node_modules",
						"filePath": "/",
						"extendName": "",
						"isDir": 1,
						"uploadTime": "2025-03-04 15:08:20",
						"deleteFlag": 0,
						"deleteTime": null,
						"deleteBatchNum": null,
						"imageWidth": null,
						"imageHeight": null
					},
					{
						"fileId": null,
						"timeStampName": null,
						"fileUrl": null,
						"fileSize": null,
						"storageType": null,
						"pointCount": null,
						"identifier": null,
						"userFileId": "1902567581638225920",
						"userId": 1,
						"fileName": "文件1",
						"filePath": "/",
						"extendName": "",
						"isDir": 1,
						"uploadTime": "2025-03-20 11:47:14",
						"deleteFlag": 0,
						"deleteTime": null,
						"deleteBatchNum": null,
						"imageWidth": null,
						"imageHeight": null
					},
					{
						"fileId": null,
						"timeStampName": null,
						"fileUrl": null,
						"fileSize": null,
						"storageType": null,
						"pointCount": null,
						"identifier": null,
						"userFileId": "1902567638034837504",
						"userId": 1,
						"fileName": "文件及2",
						"filePath": "/",
						"extendName": "",
						"isDir": 1,
						"uploadTime": "2025-03-20 11:47:27",
						"deleteFlag": 0,
						"deleteTime": null,
						"deleteBatchNum": null,
						"imageWidth": null,
						"imageHeight": null
					},
					{
						"fileId": "1902571481250418688",
						"timeStampName": null,
						"fileUrl": "upload/20250320/633424203495cc0eb540a00de0ccd515.docx",
						"fileSize": 10082,
						"storageType": 0,
						"pointCount": null,
						"identifier": "633424203495cc0eb540a00de0ccd515",
						"userFileId": "1902571481602740224",
						"userId": 1,
						"fileName": "12",
						"filePath": "/",
						"extendName": "docx",
						"isDir": 0,
						"uploadTime": "2025-03-20 12:02:43",
						"deleteFlag": 0,
						"deleteTime": null,
						"deleteBatchNum": null,
						"imageWidth": null,
						"imageHeight": null
					},
					{
						"fileId": "1924646103659642880",
						"timeStampName": null,
						"fileUrl": "upload/20250520/89e81c27cf89c246870f0db520a9b34f.mp3",
						"fileSize": 160111,
						"storageType": 0,
						"pointCount": null,
						"identifier": "89e81c27cf89c246870f0db520a9b34f",
						"userFileId": "1924646103990992896",
						"userId": 1,
						"fileName": "13",
						"filePath": "/",
						"extendName": "mp3",
						"isDir": 0,
						"uploadTime": "2025-05-20 09:59:24",
						"deleteFlag": 0,
						"deleteTime": null,
						"deleteBatchNum": null,
						"imageWidth": null,
						"imageHeight": null
					},
				],
				"total": 14
			}
			console.log('res=====')
			console.log(res)
			if (res.success) {
				this.fileList = res.dataList
				this.pageData.total = Number(res.total)
				this.loading = false
			} else {
				this.$message.error(res.message)
			}
			// getFileListByType(data).then((res) => {
			// 	if (res.success) {
			// 		this.fileList = res.dataList
			// 		this.pageData.total = Number(res.total)
			// 		this.loading = false
			// 	} else {
			// 		this.$message.error(res.message)
			// 	}
			// })
		},

		/**
		 * 获取搜索文件结果列表
		 * @param {string} fileName 文件名称
		 */
		getSearchFileList(fileName) {
			this.loading = true
			searchFile({
				currentPage: this.pageData.currentPage,
				pageCount: this.pageData.pageCount,
				fileName: fileName
			}).then((res) => {
				this.loading = false
				if (res.success) {
					this.fileList = res.dataList.map((item) => {
						return {
							...item,
							highlightFields: item.highLight.fileName[0]
						}
					})
					this.pageData.total = res.data.totalHits
				} else {
					this.$message.error(res.message)
				}
			})
		}
	}
}
</script>

<style lang="less" scoped>
// 定义全局变量
@BorderBase: #e6e6e6; // 假设边框颜色
@RegularText: #333; // 假设常规文本颜色

.file-list-wrapper {
  // 深度选择器，用于覆盖第三方组件样式
  // Vue 组件作用域样式可能不需要此选择器
  // >>> .el-header {
  //   padding: 0;
  // }

  .middle-wrapper {
    margin-bottom: 8px;
  }

  .pagination-wrapper {
    position: relative;
    border-top: 1px solid @BorderBase;
    height: 44px;
    line-height: 44px;
    text-align: center;

    .current-page-count {
      position: absolute;
      left: 16px;
      height: 32px;
      line-height: 32px;
      font-size: 13px;
      color: @RegularText;
    }
  }
}
</style>
