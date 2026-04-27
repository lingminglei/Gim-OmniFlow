<template>
	<transition name="el-zoom-in-top">
		<div class="audio-preview-wrapper" v-show="visible">
			<img class="audio-background" :src="musicImgUrl" alt="背景图" />
			<!-- 右上角操作 -->
			<div class="operate-box">
				<el-tooltip class="operate-tip" effect="dark" placement="bottom">
					<div slot="content" style="line-height: 2">
						操作提示: <br />
						1. 按 Esc 键可退出查看；<br />
						2. 支持键盘控制：<br />
						&nbsp;&nbsp;空格 - 暂停/播放<br />
						&nbsp;&nbsp;左方向键 - 播放上一个<br />
						&nbsp;&nbsp;右方向键 - 播放下一个<br />
						&nbsp;&nbsp;上方向键 - 音量调大<br />
						&nbsp;&nbsp;下方向键 - 音量减小<br />
					</div>
					<i class="tip-icon el-icon-s-opportunity"></i>
				</el-tooltip>
				<i
					class="close-icon el-icon-close"
					title="关闭（Escape）"
					@click="handleClosePreview"
				></i>
			</div>
			<audio
				ref="audioRef"
				:src="activeFileObj.fileUrl"
				controls
				style="position: fixed; top: 0; left: 0; display: none"
				@loadedmetadata="handleLoadedmetadata"
				@timeupdate="handleTimeUpdate"
				@ended="handleChangeAudioIndex('next')"
			></audio>
			<div class="audio-list-wrapper">
				<!-- 音频列表 -->
				<ul class="audio-list">
					<li class="audio-list-header">
						<span class="name">音频名称</span>
						<span class="audio-size">大小</span>
						<span class="path">路径</span>
					</li>
					<div class="audio-list-body">
						<li
							class="audio-item"
							v-for="(item, index) in audioList"
							:key="index"
							:class="[activeIndex === index ? 'active' : '']"
							:title="isPlay ? '暂停' : '播放'"
							@click="handleChangeAudioIndex('manual', index)"
						>
							<span class="name">
								<span class="sequence" v-show="activeIndex !== index">
									{{ index + 1 }}
								</span>
								<img
									class="wave"
									:src="activePlayIcon"
									alt="波浪动图"
									v-show="activeIndex === index && isPlay"
								/>
								<i
									class="no-wave el-icon-s-data"
									v-show="activeIndex === index && !isPlay"
								></i>
								<span class="text"
									>{{ item.fileName }}.{{ item.extendName }}</span
								>
							</span>
							<i
								class="play-icon iconfont icon-icon-7"
								v-show="activeIndex === index && !isPlay"
							></i>
							<i
								class="pause-icon iconfont icon-icon-3"
								v-show="activeIndex === index && isPlay"
							></i>
							<a
								class="download"
								:href="$file.getDownloadFilePath(item)"
								target="_blank"
								title="下载"
							>
								<i class="download-icon el-icon-download"></i>
							</a>
							<i
								class="share-icon el-icon-share"
								title="分享"
								@click.stop="
									$openDialog.shareFile({
										fileInfo: [
											{
												userFileId: item.userFileId
											}
										]
									})
								"
							></i>
							<span class="audio-size">{{
								$file.calculateFileSize(item.fileSize)
							}}</span>
							<span class="path">{{ item.filePath }}</span>
						</li>
					</div>
				</ul>
				<!-- 歌曲图片和歌词 -->
				<div class="img-and-lyrics">
					<img class="audio-img" :src="musicImgUrl" alt="歌曲图片" />
					<div class="audio-name">
						{{ activeFileObj.fileName }}.{{ activeFileObj.extendName }}
					</div>
					<div class="album-artist" v-show="audioInfo.artist">
						歌手：{{ audioInfo.artist }}
					</div>
					<div class="album-name" v-if="audioInfo.album">
						专辑：{{ audioInfo.album }}
					</div>
					<ul
						class="lyrics-list"
						ref="lyricsListRef"
						:class="{ one: lyricsList.length === 1 }"
						v-if="lyricsList.length"
					>
						<li
							class="lyrics-item"
							ref="lyricsLineRef"
							v-for="(item, index) in lyricsList"
							:key="index"
							:class="{
								active: currentLyricsLineIndex === index
							}"
							@click="handleChangeProgress(transferTimeToSeconds(item.time))"
						>
							{{ item.text }}
						</li>
					</ul>
				</div>
			</div>
			<!-- 底部音乐控件 -->
			<div class="control-wrapper">
				<div class="control-left">
					<i
						class="operate-icon iconfont icon-shangyishou"
						title="上一个（按左方向键）"
						@click="handleChangeAudioIndex('pre')"
					></i>
					<i
						class="operate-icon play-icon iconfont icon-icon-7"
						v-show="!isPlay"
						title="播放（按空格键）"
						@click="handleClickPlayIcon"
					></i>
					<i
						class="operate-icon pause-icon iconfont icon-icon-3"
						v-show="isPlay"
						title="暂停（按空格键）"
						@click="handleClickPauseIcon"
					></i>
					<i
						class="operate-icon iconfont icon-xiayishou"
						title="下一个（按右方向键）"
						@click="handleChangeAudioIndex('next')"
					></i>
					<el-slider
						class="progress-bar control-item"
						v-model="currentTime"
						:step="progressStep"
						:max="audioInfo.duration"
						:format-tooltip="(val) => transferSecondsToTime(val)"
						@mousedown.native="isDrop = true"
						@mouseup.native="isDrop = false"
						@change="handleChangeProgress"
					></el-slider>
					<span class="time control-item"
						>{{ transferSecondsToTime(currentTime) }} /
						{{ transferSecondsToTime(audioInfo.duration) }}</span
					>
				</div>
				<div class="control-right">
					<i
						class="operate-icon cycle-type iconfont"
						:class="cycleTypeMap[String(cycleType)].icon"
						:title="cycleTypeMap[String(cycleType)].text"
						@click="handleChangeCycleType"
					></i>
					<a
						class="operate-icon download-link"
						:href="$file.getDownloadFilePath(activeFileObj)"
						target="_blank"
						title="下载"
					>
						<i class="download-icon el-icon-download"></i>
					</a>
					<i
						class="operate-icon share-icon el-icon-share"
						title="分享"
						@click.stop="
							$openDialog.shareFile({
								fileInfo: [
									{
										userFileId: item.userFileId
									}
								]
							})
						"
					></i>
					<i
						class="operate-icon volume-icon control-item iconfont"
						:class="volume === 0 ? 'icon-jingyin01' : 'icon-yinliang101'"
						@click="handleClickVolumeIcon"
					></i>
					<el-slider
						class="volume-bar control-item"
						v-model="volume"
						:step="0.01"
						:max="1"
						:format-tooltip="(val) => Math.floor(val * 100)"
						height="100px"
						title="可按上下方向键调节音量"
						@input="handleChangeVolumeBar"
					></el-slider>
				</div>
			</div>
		</div>
	</transition>
</template>

<script>
import { getFileDetail } from '@/api/file.js'
import * as Base64 from 'js-base64'

export default {
	name: 'AudioPreview',
	data() {
		return {
			visible: false, //  音频预览组件是否可见
			activeIndex: 0, //  当前打开的音频索引
			activePlayIcon: require('_a/images/audio/wave.gif'),
			cycleType: 1, //  音频播放的循环模式
			// 音频循环模式和图标对应的 Map
			cycleTypeMap: {
				1: {
					icon: 'icon-xunhuanbofang',
					text: '列表循环'
				},
				2: {
					icon: 'icon-danquxunhuan1',
					text: '单曲循环'
				},
				3: {
					icon: 'icon-suijibofang1',
					text: '随机播放'
				}
			},
			isPlay: false, //  是否正在播放
			currentTime: 0, //  当前播放的秒
			isDrop: false, //  是否正在拖拽播放进度滑块
			volume: 0, //  音量
			audioInfo: {}, //  音频信息
			lyricsList: [], //  歌词列表
			currentLyricsLineIndex: 0 //  当前高亮的歌词行索引，从 0 开始
		}
	},
	computed: {
		// 当前显示的文件信息
		activeFileObj() {
			const res = this.audioList.length ? this.audioList[this.activeIndex] : {}
			return res
		},
		// 隐藏的 audio 标签
		audioElement() {
			return this.$refs.audioRef
		},
		// 歌曲封面
		musicImgUrl() {
			return this.audioInfo.albumImage
				? `data:image/jpeg;base64,${this.audioInfo.albumImage}`
				: require('_a/images/file/file_music.png')
		},
		// 播放进度条步长
		progressStep() {
			return this.audioInfo.duration / 100
		}
	},
	watch: {
		// 监听音频预览组件状态
		visible(newValue) {
			if (newValue) {
				this.activeIndex = this.defaultIndex
				this.getFileDetailData()
				// 添加键盘相关事件
				document.addEventListener('keyup', this.handleAddKeyupEvent)
			} else {
				// 移除键盘相关事件
				document.removeEventListener('keyup', this.handleAddKeyupEvent)
			}
		},
		// 监听当前索引变化
		activeIndex() {
			this.getFileDetailData()
		}
	},
	methods: {
		/**
		 * DOM 绑定 Esc 键、左方向键、右方向键的键盘按下事件
		 * @param {event} event 事件
		 */
		handleAddKeyupEvent(event) {
			switch (event.code) {
				// 关闭预览
				case 'Escape': {
					this.handleClosePreview()
					break
				}
				// 切换到上一个
				case 'ArrowLeft': {
					this.handleChangeAudioIndex('pre')
					break
				}
				// 切换到下一个
				case 'ArrowRight': {
					this.handleChangeAudioIndex('next')
					break
				}
				// 音量调大
				case 'ArrowUp': {
					this.volume = this.volume === 1 ? 1 : this.volume + 0.1
					this.volume = Number(this.volume.toFixed(1))
					this.handleChangeVolumeBar(this.volume)
					break
				}
				// 音量调小
				case 'ArrowDown': {
					this.volume = this.volume === 0 ? 0 : this.volume - 0.1
					this.volume = Number(this.volume.toFixed(1))
					this.handleChangeVolumeBar(this.volume)
					break
				}
				// 暂停/播放
				case 'Space': {
					this.handleChangeAudioIndex('manual', this.activeIndex)
					break
				}
			}
		},
		/**
		 * 获取文件信息
		 */
		getFileDetailData() {
			this.handleClickPauseIcon()
			this.loading = true
			getFileDetail({ userFileId: this.activeFileObj.userFileId })
				.then((res) => {
					this.loading = false
					if (res.success) {
						this.audioInfo = {
							...res.data.music,
							duration: res.data.music.trackLength
						}
						// Base64 解码为 lrc 格式的歌词文件
						let lyricsStr = Base64.decode(this.audioInfo.lyrics)
						if (lyricsStr.includes('[offset:0]')) {
							// 有歌词，从标志位 [offset:0] 下一行开始截取
							lyricsStr = lyricsStr.split('[offset:0]\n')[1]
						}
						this.lyricsList = lyricsStr
							.split('\n')
							.map((item) => {
								const line = item.split('[')[1].split(']')
								return {
									time: line[0], //  当前行歌词开始播放的秒数
									text: line[1] //  当前歌词文本
								}
							})
							.filter((item) => item.text !== '')
						this.lyricsList = this.lyricsList.map((item, index) => {
							return {
								...item,
								// 当前行歌词起始秒数
								startSeconds: this.transferTimeToSeconds(item.time),
								// 当前行歌词结束秒数
								endSeconds:
									index < this.lyricsList.length - 1
										? this.transferTimeToSeconds(
												this.lyricsList[index + 1].time
										  )
										: this.audioInfo.duration
							}
						})
						// 当切换完歌曲时，歌词重新滚动到顶部
						this.$refs.lyricsListRef.scrollTo({
							top: 0,
							behavior: 'smooth'
						})
						this.currentLyricsLineIndex = 0
					}
				})
				.catch(() => {
					this.loading = false
				})
		},
		/**
		 * 获取播放器参数
		 */
		handleLoadedmetadata(event) {
			const audioDom = event.target
			this.volume = audioDom.volume || 0.5
			this.currentTime = audioDom.currentTime
			this.handleClickPlayIcon()
		},
		/**
		 * 将秒转化为时分秒
		 * @param {number} duration 总秒数
		 */
		transferSecondsToTime(duration) {
			const hour = Math.floor(duration / 3600)
			const minutes = Math.floor(duration / 60)
			const seconds = Math.ceil(duration % 60)
			return `${hour < 10 ? `0${hour}` : hour}:${
				minutes < 10 ? `0${minutes}` : minutes
			}:${seconds < 10 ? `0${seconds}` : seconds}`
		},
		/**
		 * 将分秒转化为秒
		 * @param {string} time 分秒，格式 00:00
		 */
		transferTimeToSeconds(time) {
			const timeList = time.split('.')[0].split(':')
			return Number(timeList[1]) + Number(timeList[0]) * 60
		},
		/**
		 * 当前播放时间改变时触发
		 */
		handleTimeUpdate(event) {
			// 如果正在拖拽进度滑块，函数结束，不计算当前时间
			if (this.isDrop) return
			this.currentTime = event.target.currentTime
			if (this.lyricsList.length) {
				// 遍历歌词，当前秒对应的歌词整行添加高亮效果
				this.lyricsList.forEach((item, index) => {
					if (
						item.startSeconds <= this.currentTime &&
						this.currentTime < item.endSeconds &&
						this.currentLyricsLineIndex !== index
					) {
						// 确定高亮歌词行索引
						this.currentLyricsLineIndex = index
						// 使高亮歌词行永远保持在第二行
						if (this.currentLyricsLineIndex > 2) {
							// 平滑滚动
							this.$refs.lyricsListRef.scrollTo({
								top: this.$refs.lyricsLineRef[index].clientHeight * (index - 2),
								behavior: 'smooth'
							})
						}
					}
				})
			}
		},
		/**
		 * 拖动播放进度滑块触发
		 */
		handleChangeProgress(progress) {
			this.audioElement.currentTime = progress
			this.isDrop = false
		},
		/**
		 * 切换循环播放类型
		 */
		handleChangeCycleType() {
			if (this.cycleType === 3) {
				this.cycleType = 1
			} else if (this.cycleType >= 1) {
				this.cycleType++
			}
		},
		/**
		 * 点击播放图标触发
		 * @description 开始播放音频
		 */
		handleClickPlayIcon() {
			this.isPlay = true
			this.audioElement.play()
		},
		/**
		 * 点击暂停图标触发
		 * @description 暂停音频
		 */
		handleClickPauseIcon() {
			this.isPlay = false
			this.audioElement.pause()
		},
		/**
		 * 切换、暂停或播放歌曲
		 * @param {string} type pre - 上一首 | next - 下一首 | manual 手动切换
		 * @param {number} index 手动切换的音频索引，从 0 开始
		 */
		handleChangeAudioIndex(type, index) {
			// 如果当前手动切换
			if (type === 'manual') {
				if (this.activeIndex === index) {
					if (this.isPlay) {
						this.handleClickPauseIcon()
					} else {
						this.handleClickPlayIcon()
					}
				} else {
					this.activeIndex = index
				}
			} else {
				this.handleClickPauseIcon()
				// 判断当前循环播放类型
				switch (this.cycleType) {
					case 3: {
						let activeIndex = 0
						do {
							activeIndex =
								Math.floor(Math.random() * (this.audioList.length - 1)) + 1
						} while (this.activeIndex === activeIndex)
						this.activeIndex = activeIndex
						break
					}
					default: {
						if (type === 'pre') {
							if (this.activeIndex === 0) {
								this.activeIndex = this.audioList.length - 1
							} else {
								this.activeIndex--
							}
						} else if (type === 'next') {
							if (this.activeIndex === this.audioList.length - 1) {
								this.activeIndex = 0
							} else {
								this.activeIndex++
							}
						}
						break
					}
				}
			}
		},
		/**
		 * 点击音量图标
		 */
		handleClickVolumeIcon() {
			this.volume = this.volume === 0 ? 0.5 : 0
			this.handleChangeVolumeBar(this.volume)
		},
		/**
		 * 音量滑块改变时触发
		 */
		handleChangeVolumeBar(volume) {
			this.audioElement.volume = Number(volume.toFixed(1))
		},
		// 关闭音频预览
		handleClosePreview() {
			this.visible = false
			this.callback('cancel')
		}
	}
}
</script>

<style lang="less" scoped>
/* ====== 主题变量（可按自己全局主题调整）====== */
@PrimaryText: #14161a;         // 背景主色（比原来的 #303133 更偏音乐播放器风格）
@BorderBase: #dcdfe6;
@Warning: #e6a23c;             // 进度条 / 高亮橙色（保留原始语义）
@TextPrimary: #f5f5f5;         // 主文字
@TextSecondary: #a1a6b3;       // 次要文字
@CardBg: rgba(18, 22, 34, 0.96);
@CardBorder: rgba(255, 255, 255, 0.06);
@BlurBg: rgba(10, 12, 20, 0.85);

/* ====== 外层整体区域 ====== */
.audio-preview-wrapper {
  background: radial-gradient(circle at top left, #26293a 0, #14161a 40%, #050608 100%);
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  z-index: 100;
  color: @TextPrimary;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;

  .audio-background {
    position: fixed;
    top: -40%;
    left: -10%;
    width: 120vw;
    height: auto;
    filter: blur(80px);
    opacity: 0.55;
    z-index: -1;
    transform: scale(1.1);
  }

  /* 右上角操作按钮区域 */
  .operate-box {
    position: fixed;
    top: 18px;
    right: 32px;
    display: flex;
    align-items: center;
    font-size: 14px;
    color: @TextSecondary;

    .tip-icon,
    .close-icon {
      margin-left: 16px;
      cursor: pointer;
      transition: color 0.25s ease, transform 0.25s ease, text-shadow 0.25s ease;

      &:hover {
        color: @Warning;
        transform: translateY(-1px);
        text-shadow: 0 0 8px fade(@Warning, 50%);
      }
    }

    .tip-icon {
      font-size: 24px;
    }

    .close-icon {
      font-size: 28px;
    }
  }

  /* ====== 中部内容区域：列表 + 封面 / 歌词 ====== */
  .audio-list-wrapper {
    margin: 72px auto 0;
    width: 88%;
    height: calc(100vh - 160px);
    padding: 24px 26px 0;
    display: flex;
    justify-content: space-between;
    box-sizing: border-box;
    border-radius: 24px 24px 18px 18px;
    background: @BlurBg;
    border: 1px solid @CardBorder;
    box-shadow:
      0 18px 45px rgba(0, 0, 0, 0.75),
      0 0 0 1px rgba(255, 255, 255, 0.02);
    backdrop-filter: blur(24px);

    /* 左侧音频列表 */
    .audio-list {
      flex: 1;
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      color: @TextPrimary;

      .audio-list-header,
      .audio-item {
        border-radius: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 56px;
        cursor: pointer;
        padding: 0 16px;
        font-size: 13px;
        transition:
          background 0.2s ease,
          transform 0.15s ease,
          box-shadow 0.2s ease,
          color 0.2s ease;
      }

      .audio-list-header {
        margin-bottom: 4px;
        background: rgba(255, 255, 255, 0.02);
        border: 1px solid rgba(255, 255, 255, 0.04);
        color: @TextSecondary;
        padding-right: 24px;
        font-size: 12px;
        letter-spacing: 0.04em;

        .name {
          flex: 1;
          padding-left: 18px;
        }
      }

      .audio-list-body {
        height: calc(100% - 56px);
        overflow: auto;
        padding-right: 6px;

        /* 自定义滚动条，类似网易云窄滚动条 */
        &::-webkit-scrollbar {
          width: 6px;
        }

        &::-webkit-scrollbar-track {
          background: transparent;
        }

        &::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.15);
          border-radius: 3px;
        }

        &::-webkit-scrollbar-thumb:hover {
          background: rgba(255, 255, 255, 0.28);
        }
      }

      .audio-item {
        background: transparent;
        color: @TextSecondary;
        margin-bottom: 4px;

        &:hover {
          background: rgba(255, 255, 255, 0.04);
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(0, 0, 0, 0.45);
          color: @TextPrimary;
        }

        &.active {
          background: radial-gradient(circle at left, fade(@Warning, 35%) 0, rgba(255, 255, 255, 0.06) 32%, transparent 70%);
          color: @Warning;
          box-shadow:
            0 0 16px fade(@Warning, 24%),
            0 10px 28px rgba(0, 0, 0, 0.6);
        }

        .name {
          flex: 1;
          display: flex;
          align-items: center;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;

          .sequence {
            display: inline-block;
            margin-right: 8px;
            width: 18px;
            text-align: center;
            font-size: 12px;
            opacity: 0.7;
          }

          .wave {
            margin-right: 8px;
            width: 14px;
            height: 14px;
          }

          .no-wave {
            margin-right: 8px;
            font-size: 16px;
          }

          .text {
            max-width: 260px;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
          }
        }

        .play-icon,
        .pause-icon,
        .download-icon,
        .share-icon {
          margin-right: 12px;
          font-size: 20px;
          cursor: pointer;
          transition: color 0.25s ease, transform 0.15s ease;
          opacity: 0.88;

          &:hover {
            color: @Warning;
            transform: translateY(-1px);
          }
        }

        .download {
          color: inherit;
          text-decoration: none;

          &:hover {
            color: @Warning;
          }
        }

        .audio-size {
          width: 110px;
          padding-right: 18px;
          text-align: right;
          font-variant-numeric: tabular-nums;
        }

        .path {
          min-width: 120px;
          font-size: 12px;
          color: @TextSecondary;
          opacity: 0.85;
        }
      }
    }

    /* 右侧封面 + 歌词区域 */
    .img-and-lyrics {
      padding: 6px 0 0 24px;
      width: 360px;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      box-sizing: border-box;
      border-left: 1px solid rgba(255, 255, 255, 0.04);

      .audio-img {
        margin-bottom: 18px;
        width: 180px;
        height: 180px;
        border-radius: 16px;
        object-fit: cover;
        box-shadow:
          0 18px 45px rgba(0, 0, 0, 0.9),
          0 0 0 1px rgba(255, 255, 255, 0.04);
      }

      .audio-name {
        margin-bottom: 6px;
        font-size: 18px;
        line-height: 1.8;
        font-weight: 600;
        letter-spacing: 0.02em;
      }

      .album-artist,
      .album-name {
        margin-bottom: 4px;
        font-size: 13px;
        color: @TextSecondary;
      }

      .lyrics-list {
        width: 100%;
        flex: 1;
        margin-top: 12px;
        padding: 8px 6px 6px;
        overflow: auto;
        list-style: none;
        box-sizing: border-box;
        border-radius: 12px;
        background: rgba(0, 0, 0, 0.18);
        position: relative;

        /* 渐隐遮罩，保持中间区域歌词最清晰 */
        -webkit-mask-image: linear-gradient(
          180deg,
          rgba(255, 255, 255, 0) 0,
          rgba(255, 255, 255, 0.6) 15%,
          #fff 25%,
          #fff 75%,
          rgba(255, 255, 255, 0.6) 85%,
          rgba(255, 255, 255, 0) 100%
        );

        &::-webkit-scrollbar {
          width: 4px;
        }

        &::-webkit-scrollbar-track {
          background: transparent;
        }

        &::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.18);
          border-radius: 3px;
        }

        &.one {
          .lyrics-item {
            margin-top: 40px;
          }
        }

        .lyrics-item {
          line-height: 32px;
          cursor: pointer;
          font-size: 13px;
          color: @TextSecondary;
          transition: color 0.25s ease, transform 0.18s ease, text-shadow 0.25s ease;

          &:not(.active):hover {
            color: #ffffff;
            transform: translateY(-1px);
          }

          &.active {
            color: @Warning;
            font-size: 14px;
            text-shadow: 0 0 12px fade(@Warning, 40%);
          }
        }
      }
    }
  }

  /* ====== 底部控制区域 ====== */
  .control-wrapper {
    margin: 0 auto;
    width: 88%;
    height: 120px;
    padding: 18px 24px 26px;
    box-sizing: border-box;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    color: @TextSecondary;

    .control-left {
      flex: 1;
      height: 100%;
      display: flex;
      align-items: center;
      text-align: center;
      padding-left: 4px;

      .operate-icon {
        margin-right: 18px;
        font-size: 34px;
        cursor: pointer;
        transition: color 0.25s ease, transform 0.2s ease, text-shadow 0.25s ease;

        &:hover {
          color: @Warning;
          transform: translateY(-1px) scale(1.03);
          text-shadow: 0 0 10px fade(@Warning, 35%);
        }
      }

      .play-icon {
        font-size: 40px;
      }

      .progress-bar {
        margin-right: 18px;
        flex: 1;

        /* 深度选择 ElementUI Slider */
        ::v-deep .el-slider__runway {
          height: 2px;
          background: rgba(255, 255, 255, 0.08);
          border-radius: 999px;

          .el-slider__button-wrapper {
            top: -16px;

            .el-slider__button {
              border: none;
              width: 12px;
              height: 12px;
              box-shadow: 0 0 0 4px rgba(230, 162, 60, 0.18);
            }
          }

          .el-slider__bar {
            height: 100%;
            background: linear-gradient(90deg, @Warning 0%, #ffd08a 60%, #ffffff 100%);
          }
        }
      }

      .time {
        min-width: 155px;
        text-align: right;
        font-variant-numeric: tabular-nums;
      }
    }

    .control-right {
      width: 340px;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      font-size: 28px;

      .operate-icon {
        margin-right: 16px;
        cursor: pointer;
        transition: color 0.25s ease, transform 0.18s ease;

        &:hover {
          color: @Warning;
          transform: translateY(-1px);
        }

        &.download-link {
          font-size: 26px;
          color: inherit;
          text-decoration: none;

          .download-icon {
            font-size: 24px;
          }

          &:hover {
            .download-icon {
              color: @Warning;
            }
          }
        }
      }

      .cycle-type {
        font-size: 24px;
      }

      .share-icon {
        font-size: 24px;
      }

      .volume-icon {
        margin-right: 8px;
        font-size: 24px;
      }

      .volume-bar {
        width: 110px;

        ::v-deep .el-slider__runway {
          height: 2px;
          background: rgba(255, 255, 255, 0.12);
          border-radius: 999px;

          .el-slider__button-wrapper {
            top: -18px;

            .el-slider__button {
              border: none;
              width: 10px;
              height: 10px;
            }
          }

          .el-slider__bar {
            height: 100%;
            background: @Warning;
          }
        }
      }
    }
  }
}
</style>
