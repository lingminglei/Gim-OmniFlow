<template>
	<div class="upload-file-wrapper">
		<uploader class="uploader-app" ref="uploader" :options="options" :autoStart="false"
			:fileStatusText="fileStatusText" @files-added="handleFilesAdded" @file-success="handleFileSuccess"
			@file-error="handleFileError" @dragleave="hideUploadMask">
			<uploader-unsupport />
			<uploader-btn class="select-file-btn" :attrs="attrs" ref="uploadBtn">选择文件</uploader-btn>
			<uploader-btn class="select-file-btn" :attrs="attrs" :directory="true"
				ref="uploadDirBtn">选择目录</uploader-btn>
			<uploader-drop class="drop-box" id="dropBox" @paste.native="handlePaste" v-show="dropBoxShow">
				<div class="paste-img-wrapper" v-show="pasteImg.src">
					<div class="paste-name">{{ pasteImg.name }}</div>
					<img class="paste-img" :src="pasteImg.src" :alt="pasteImg.name" v-if="pasteImg.src" />
				</div>
				<span class="text" v-show="!pasteImg.src">截图粘贴或将文件拖拽至此区域上传</span>
				<i class="upload-icon el-icon-upload" v-show="pasteImg.src" @click="handleUploadPasteImg">上传图片</i>
				<i class="delete-icon el-icon-delete" v-show="pasteImg.src" @click="handleDeletePasteImg">删除图片</i>
				<i class="close-icon el-icon-circle-close" @click="dropBoxShow = false">关闭</i>
			</uploader-drop>
			<uploader-list v-show="panelShow">
				<template v-slot:default="props">
					<div class="file-panel">
						<div class="file-title">
							<span class="title-span">上传列表<span class="count">（{{ props.fileList.length }}）</span></span>
							<div class="operate">
								<el-button type="text" :title="collapse ? '展开' : '折叠'"
									:icon="collapse ? 'el-icon-full-screen' : 'el-icon-minus'"
									@click="collapse = !collapse" />
								<el-button @click="handleClosePanel" type="text" title="关闭" icon="el-icon-close" />
							</div>
						</div>
						<el-collapse-transition>
							<ul class="file-list" v-show="!collapse">
								<li class="file-item" v-for="file in props.fileList" :key="file.id"
									:class="{ 'custom-status-item': uploadStatus[file.id] !== '' }">
									<uploader-file ref="fileItem" :file="file" :list="true" />
									<span class="custom-status">{{ uploadStatus[file.id] }}</span>
								</li>
								<div class="no-file" v-if="!props.fileList.length">
									<i class="icon-empty-file"></i> 暂无待上传文件
								</div>
							</ul>
						</el-collapse-transition>
					</div>
				</template>
			</uploader-list>
		</uploader>
	</div>
</template>

<script>
import SparkMD5 from 'spark-md5'
import { getToken,getTokenType} from '@/utils/auth'

export default {
	data() {
		return {
			params: {},          // ✅ 外部传入的参数
			uploadWay: 0,        // ✅ 外部传入上传方式
			serviceEl: null,     // ✅ 外部组件
			callType: null,      // ✅ 外部调用方式
			callback: () => { },  // ✅ 外部回调函数
			uploadStatus: {},
			options: {
				target: '/api/filetransfer/uploadfile',
				chunkSize: 1024 * 1024,
				fileParameterName: 'file',
				maxChunkRetries: 3,
				testChunks: true,
				checkChunkUploadedByResponse: function (chunk, message) {
					let objMessage = JSON.parse(message)
				   if (objMessage.code === 200) {
						let data = objMessage.data
						if (data.skipUpload) return true
						return (data.uploaded || []).indexOf(chunk.offset + 1) >= 0
					} else {
						console.log('响应参数：')
						console.log(objMessage)
						console.log(objMessage.message)
						return true
					}
				},
				headers: {
					token: getTokenType()+ ' '+getToken(),
				},
				query() { }
			},
			fileStatusText: {
				success: '上传成功',
				error: 'error',
				uploading: '上传中',
				paused: '暂停中',
				waiting: '等待中'
			},
			attrs: {
				accept: '*'
			},
			panelShow: false,
			collapse: false,
			dropBoxShow: false,
			pasteImg: { src: '', name: '' },
			pasteImgObj: null,
			filesLength: 0
		}
	},
	mounted() {
		this.$nextTick(() => {
			this.handlePrepareUpload()
		})
	},
	computed: {
		uploaderInstance() {
			return this.$refs.uploader?.uploader || null
		}
	},
	methods: {
		// 隐藏拖拽上传遮罩
		hideUploadMask(e) {
			e.stopPropagation()
			e.preventDefault()
			this.dropBoxShow = false
		},
		handlePrepareUpload() {
			this.options.headers.token = getTokenType()+ ' '+getToken()

			switch (this.uploadWay) {
				case 1:
					this.$refs.uploadBtn?.$el?.click()
					break
				case 2:
					this.$refs.uploadDirBtn?.$el?.click()
					break
				case 3:
					this.pasteImg = { src: '', name: '' }
					this.pasteImgObj = null
					this.dropBoxShow = true
					break
			}
		},
		handleUploadPasteImg() {
			this.uploaderInstance?.addFile(this.pasteImgObj)
		},
		handleDeletePasteImg() {
			this.pasteImg = { src: '', name: '' }
			this.pasteImgObj = null
		},
		handlePaste(event) {
			const items = (event.clipboardData || window.clipboardData).items
			const imgObj = items?.[0]?.getAsFile()
			if (!imgObj) {
				this.$message.error('粘贴内容非图片')
				return
			}
			this.pasteImgObj = new File([imgObj], `upload_${Date.now()}.png`, { type: imgObj.type })
			const reader = new FileReader()
			reader.onload = (e) => {
				this.pasteImg.src = e.target.result
				this.pasteImg.name = this.pasteImgObj.name
			}
			reader.readAsDataURL(this.pasteImgObj)
		},
		handleFilesAdded(files) {
			this.filesLength += files.length
			files.forEach(file => {
				this.dropBoxShow = false
				this.panelShow = true
				this.collapse = false
				this.computeMD5(file)
			})
		},
		computeMD5(file) {
			const reader = new FileReader()
			const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice
			let currentChunk = 0
			const chunkSize = 1024 * 1024
			const chunks = Math.ceil(file.size / chunkSize)
			const spark = new SparkMD5.ArrayBuffer()
			this.uploadStatus[file.id] = '计算MD5'
			file.pause()

			const loadNext = () => {
				const start = currentChunk * chunkSize
				const end = Math.min(start + chunkSize, file.size)
				reader.readAsArrayBuffer(blobSlice.call(file.file, start, end))
			}

			reader.onload = (e) => {
				spark.append(e.target.result)
				if (++currentChunk < chunks) {
					loadNext()
					this.uploadStatus[file.id] = `校验MD5 ${(currentChunk / chunks * 100).toFixed(0)}%`
					this.uploadStatus = { ...this.uploadStatus }
				} else {
					this.calculateFileMD5End(spark.end(), file)
				}
			}

			reader.onerror = () => {
				this.$notify({ title: '错误', message: `文件读取失败`, type: 'error' })
				file.cancel()
			}

			loadNext()
		},
		calculateFileMD5End(md5, file) {
			Object.assign(this.uploaderInstance.opts, {
				query: { ...this.params }
			})
			file.uniqueIdentifier = md5
			file.resume()
			this.uploadStatus[file.id] = ''
			this.uploadStatus = { ...this.uploadStatus }
		},
		handleFileSuccess(rootFile, file, response) {
			console.log("文件上传成功：")
			console.log(response)
			let result = {}
			try { result = JSON.parse(response) } catch { }
			console.log('响应参数：')
			console.log(result)
			if (!result.code === 200) {
				this.uploadStatus[file.id] = '上传失败'
				this.$message.error(result.message || '上传失败')
				return
			}
			if (this.filesLength === 1) {
				this.$message.success('上传完毕')
				if (this.callType === 1) {
					this.serviceEl?.$emit?.('getTableDataByType')
				} else {
					this.serviceEl?.getTableDataByType?.()
				}
				//计算文件上传 内存
				// this.serviceEl?.$store?.dispatch('showStorage')
				this.callback(true)
			}
			this.filesLength--
		},
		handleFileError(rootFile, file, response) {
			this.$message({ message: response, type: 'error' })
		},
		handleClosePanel() {
			this.uploaderInstance?.cancel()
			this.panelShow = false
			this.callback('cancel')
		}
	}
}
</script>

<style lang="stylus" scoped>

.upload-file-wrapper {
  position: fixed;
  z-index: 100;
  right: 16px;
  bottom: 16px;

  .drop-box {
    position: fixed;
    z-index: 19;
    top: 0;
    left: 0;
    border: 5px dashed #8091a5 !important;
    background: #ffffffd9;
    color: #8091a5 !important;
    text-align: center;
    box-sizing: border-box;
    height: 100%;
    line-height: 100%;
    width: 100%;

    .text {
      position: absolute;
      top: 50%;
      left: 50%;
      width: 100%;
      transform: translate(-50%, -50%);
      font-size: 30px;
    }

    .upload-icon {
      position: absolute;
      right: 176px;
      top: 16px;
      cursor: pointer;

      &:hover {
        color: $Primary;
      }
    }

    .delete-icon {
      position: absolute;
      right: 80px;
      top: 16px;
      cursor: pointer;

      &:hover {
        color: $Danger;
      }
    }

    .close-icon {
      position: absolute;
      right: 16px;
      top: 16px;
      cursor: pointer;

      &:hover {
        color: $Success;
      }
    }

    .paste-img-wrapper {
      width: 100%;
      height: 100%;
    }

    .paste-img {
      margin-top: 16px;
      max-width: 90%;
      max-height: 80%;
    }

    .paste-name {
      height: 24px;
      line-height: 24px;
      font-size: 18px;
      color: $PrimaryText;
    }
  }

  .uploader-app {
    width: 560px;
  }

  .file-panel {
    background-color: #fff;
    border: 1px solid #e2e2e2;
    border-radius: 7px 7px 0 0;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);

    .file-title {
      display: flex;
      height: 40px;
      line-height: 40px;
      padding: 0 15px;
      border-bottom: 1px solid #ddd;

      .title-span {
        padding-left: 0;
        margin-bottom: 0;
        font-size: 16px;

        .count {
          color: $SecondaryText;
        }
      }

      .operate {
        flex: 1;
        text-align: right;

        >>> .el-button--text {
          color: $PrimaryText;

          i[class^=el-icon-] {
            font-weight: 600;
          }

          &:hover {
            .el-icon-full-screen, .el-icon-minus {
              color: $Success;
            }

            .el-icon-close {
              color: $Danger;
            }
          }
        }
      }
    }

    .file-list {
      position: relative;
      height: 240px;
      overflow-x: hidden;
      overflow-y: auto;
      background-color: #fff;
      font-size: 12px;
      list-style: none;
      setScrollbar(6px, #EBEEF5, #C0C4CC);

      .file-item {
        position: relative;
        background-color: #fff;

        >>> .uploader-file {
          height: 40px;
          line-height: 40px;

          .uploader-file-progress {
            border: 1px solid $Success;
            border-right: none;
            border-left: none;
            background: #e1f3d8;
          }

          .uploader-file-name {
            width: 44%;
          }

          .uploader-file-size {
            width: 16%;
          }

          .uploader-file-meta {
            display: none;
          }

          .uploader-file-status {
            width: 30%;
            text-indent: 0;
          }

          .uploader-file-actions>span {
            margin-top: 12px;
          }
        }

        >>> .uploader-file[status='success'] {
          .uploader-file-progress {
            border: none;
          }
        }
      }

      .file-item.custom-status-item {
        >>> .uploader-file-status {
          visibility: hidden;
        }

        .custom-status {
          position: absolute;
          top: 0;
          right: 10%;
          width: 24%;
          height: 40px;
          line-height: 40px;
        }
      }
    }

    &.collapse {
      .file-title {
        background-color: #E7ECF2;
      }
    }
  }

  .no-file {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 16px;
  }

  /deep/.uploader-file-icon {
    display: none;
  }

  /deep/.uploader-file-actions > span {
    margin-right: 6px;
  }
}

/* 隐藏上传按钮 */
.select-file-btn {
  display: none;
}
</style>
