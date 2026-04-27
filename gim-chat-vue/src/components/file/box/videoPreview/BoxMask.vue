<template>
  <transition name="el-zoom-in-top">
    <div class="video-preview-wrapper" v-show="visible">
      <div class="top-bar">
        <div class="file-info">
          <span class="file-name" :title="`${activeFileObj.fileName}.${activeFileObj.extendName}`">
            {{ activeFileObj.fileName }}.{{ activeFileObj.extendName }}
          </span>
          <span class="file-size">{{ $file.calculateFileSize(activeFileObj.fileSize) }}</span>
        </div>

        <div class="action-group">
          <a class="action-btn" :href="$file.getDownloadFilePath(activeFileObj)" target="_blank">
            <i class="el-icon-download" title="下载"></i>
          </a>
          <div class="action-btn" @click="isFoldVideoList = !isFoldVideoList">
            <i :class="isFoldVideoList ? 'el-icon-s-fold' : 'el-icon-s-unfold'"
              :title="isFoldVideoList ? '展开列表' : '折叠列表'"></i>
          </div>
          <div class="action-btn close-btn" @click="handleClosePreview">
            <i class="el-icon-close" title="关闭"></i>
          </div>
        </div>
      </div>

      <div class="main-content">
        <div class="player-container">
          <video-player :key="activeIndex" class="video-player-instance" ref="VideoPlayer" :options="activeVideoSource" :playsinline="true"
            :preload="true" :autoplay="true" :loop="true" :muted="true" v-if="visible">
          </video-player>
        </div>

        <el-collapse-transition>
          <div class="playlist-sidebar" v-show="!isFoldVideoList">
            <div class="list-header">
              <span>播放列表</span>
              <span class="list-count">({{ videoList.length }})</span>
            </div>
            <ul class="video-list">
              <li class="video-item" v-for="(item, index) in videoList" :key="index"
                :class="{ 'is-active': activeIndex === index }" @click="activeIndex = index">

                <div class="item-icon">
                  <i class="el-icon-video-play" v-if="activeIndex === index"></i>
                  <i class="el-icon-video-camera" v-else></i>
                </div>

                <div class="item-info">
                  <div class="name" :title="`${item.fileName}.${item.extendName}`">
                    {{ item.fileName }}.{{ item.extendName }}
                  </div>
                  <div class="size">{{ $file.calculateFileSize(item.fileSize) }}</div>
                </div>
              </li>
            </ul>
          </div>
        </el-collapse-transition>
      </div>
    </div>
  </transition>
</template>

<script>
// import VideoPlayer from './VideoPlayer.vue'
import store from '@/store/index.js'

export default {
  name: 'VideoPreview',
  components: {
    // VideoPlayer
  },
  data() {
    return {
      visible: false,
      activeIndex: 0,
      isFoldVideoList: false,
      customOptions: {
        autoplay: true,
        muted: true,
        fluid: true, // 建议开启，让video.js自适应容器大小

        techOrder: ['html5'],
        poster: 'https://vjs.zencdn.net/v/oceans.png',  // 预览图
        controls: true,
        controlBar: {
          children: ['playToggle', 'volumePanel', 'timeDivider', 'progressControl', 'fullscreenToggle'],
          align: 'center'
        },
      }
    }
  },
  computed: {
    activeFileObj() {
      return this.videoList.length ? this.videoList[this.activeIndex] : {}
    },
    activeVideoSource() {
      console.log('this.activeIndex=', this.activeIndex)
      console.log('activeVideoSource==', this.videoList)

      console.log('type=', `video/${this.videoList[this.activeIndex].extendName}`)

      console.log('src=', this.videoList[this.activeIndex].fileUrl)

      // 增加非空校验防止报错
      if (!this.videoList || !this.videoList[this.activeIndex]) return {}

      return {
        techOrder: ['html5'],
        sources: [
          {
            type: `video/${this.videoList[this.activeIndex].extendName}`,
            src: this.videoList[this.activeIndex].fileUrl
          },
        ],
        // poster: 'https://vjs.zencdn.net/v/oceans.png',
        controls: true,
        controls: true,
        controlBar: {
          children: ['playToggle', 'volumePanel', 'timeDivider', 'progressControl', 'fullscreenToggle'],
          align: 'center'
        },
      }
    },
    screenWidth() {
      return store.state.common.screenWidth
    }
  },
  watch: {
    visible(newValue) {
      if (newValue) {
        // 确保 activeIndex 不越界
        this.activeIndex = typeof this.defaultIndex === 'number' ? this.defaultIndex : 0
      }
    },
    screenWidth(newValue) {
      if (newValue <= 768) {
        this.isFoldVideoList = true
      }
    },

    // ⭐ 核心修复：监听 activeIndex 或 activeVideoSource 变化
    activeVideoSource(newSource) {
      if (this.$refs.VideoPlayer) {
        // 获取 video.js 播放器实例
        const player = this.$refs.VideoPlayer.player || this.$refs.VideoPlayer; // 确保获取到正确的 player 实例

        // 确保实例存在且有新的播放源
        if (player && newSource && newSource.sources && newSource.sources.length > 0) {
          // 1. 设置新的播放源 (src/type)
          player.src(newSource.sources);

          // 2. 更新海报 (poster)
          if (newSource.poster) {
            player.poster(newSource.poster);
          }

          // 3. 重新加载并播放
          player.load();
          player.play();
        }
      }
    },
  },
  mounted() {
    console.log('activeVideoSource=-xxxxx', this.activeVideoSource)
    if (this.screenWidth <= 768) {
      this.isFoldVideoList = true
    }
  },
  methods: {
    handleClosePreview() {
      this.visible = false
      this.callback && this.callback('cancel')
    }
  }
}
</script>

<style lang="less" scoped>
/* 定义本地变量，替代外部引入 */
@primary-color: #409EFF; // 主题色
@bg-overlay: rgba(20, 20, 20, 0.98); // 整体遮罩背景
@bg-sidebar: #1f1f1f; // 侧边栏背景
@text-main: #ffffff; // 主要文字
@text-sub: #909399; // 次要文字
@border-color: rgba(255, 255, 255, 0.1); // 边框颜色

.video-preview-wrapper {
  background: @bg-overlay;
  backdrop-filter: blur(10px);
  /* 毛玻璃效果 */
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  display: flex;
  flex-direction: column;

  /* 顶部栏样式 */
  .top-bar {
    height: 60px;
    /* 增加高度 */
    background: rgba(0, 0, 0, 0.3);
    border-bottom: 1px solid @border-color;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24px;
    flex-shrink: 0;
    /* 防止被压缩 */

    .file-info {
      display: flex;
      align-items: baseline;
      overflow: hidden;

      .file-name {
        font-size: 18px;
        color: @text-main;
        font-weight: 500;
        margin-right: 12px;
        max-width: 500px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .file-size {
        font-size: 12px;
        color: @text-sub;
      }
    }

    .action-group {
      display: flex;
      align-items: center;

      .action-btn {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        /* 圆形按钮 */
        display: flex;
        justify-content: center;
        align-items: center;
        margin-left: 12px;
        cursor: pointer;
        transition: all 0.3s;
        text-decoration: none; // 针对a标签

        i {
          font-size: 20px;
          color: #ddd;
        }

        &:hover {
          background: rgba(255, 255, 255, 0.15);

          i {
            color: #fff;
          }
        }

        /* 关闭按钮特殊样式 */
        &.close-btn:hover {
          background: #f56c6c;

          i {
            color: #fff;
          }
        }
      }
    }
  }

  /* 底部主体内容 */
  .main-content {
    flex: 1;
    /* 占据剩余高度 */
    display: flex;
    overflow: hidden;
    /* 防止出现双滚动条 */

    .player-container {
      flex: 1;
      height: 100%;
      background: #000;
      display: flex;
      justify-content: center;
      align-items: center;
      position: relative;

      .video-player-instance {
        width: 100%;
        height: 100%;

        /* 深度选择器确保 video.js 填满容器 */
        /deep/ .video-js {
          width: 100%;
          height: 100%;

          // /* --- 新增代码开始：修复 Poster 重复问题 --- */
          // .vjs-poster {
          //   background-size: cover;
          //   /* 让图片保持比例充满容器 (或者用 contain 显示全图) */
          //   background-repeat: no-repeat;
          //   /* 禁止背景图片重复 */
          //   background-position: center;
          //   /* 图片居中显示 */
          // }
          /* --- 修复 Poster 重复和自定义占位符 --- */
          .vjs-poster {
            background-color: #1a1a1a; /* 比纯黑更柔和的背景色 */
            background-image: none !important; /* 确保没有残留的背景图 */
            background-size: cover;
            background-repeat: no-repeat;
            background-position: center;
            display: flex; /* 启用 Flexbox */
            justify-content: center;
            align-items: center;
            
            /* 使用伪元素添加一个巨大的视频图标 */
            &::before {
              content: "\e61d"; /* Element UI 视频图标 (el-icon-video-camera 的 Unicode) */
              font-family: 'element-icons' !important; /* 指定 Element UI 字体 */
              font-size: 150px; /* 超大图标 */
              color: rgba(255, 255, 255, 0.2); /* 半透明的白色，沉浸式效果 */
              line-height: 1;
              transition: color 0.3s;
            }
            
            /* 鼠标悬停时，图标颜色稍微亮一点 */
            &:hover::before {
                color: rgba(255, 255, 255, 0.4);
            }
          }
        }
      }
    }

    /* 播放列表侧边栏 */
    .playlist-sidebar {
      width: 300px;
      background: @bg-sidebar;
      border-left: 1px solid @border-color;
      display: flex;
      flex-direction: column;
      color: @text-main;

      .list-header {
        height: 50px;
        line-height: 50px;
        padding: 0 16px;
        border-bottom: 1px solid @border-color;
        font-size: 14px;
        font-weight: bold;
        letter-spacing: 1px;

        .list-count {
          font-weight: normal;
          color: @text-sub;
          font-size: 12px;
          margin-left: 4px;
        }
      }

      .video-list {
        flex: 1;
        overflow-y: auto;
        padding: 10px;
        list-style: none;
        margin: 0;

        /* 自定义滚动条样式 (替代 mixin) */
        &::-webkit-scrollbar {
          width: 6px;
        }

        &::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.2);
          border-radius: 3px;
        }

        &::-webkit-scrollbar-track {
          background: transparent;
        }

        .video-item {
          display: flex;
          align-items: center;
          padding: 12px;
          margin-bottom: 8px;
          border-radius: 6px;
          cursor: pointer;
          transition: background 0.2s ease;
          border: 1px solid transparent;
          /* 预留边框位置避免抖动 */

          &:hover {
            background: rgba(255, 255, 255, 0.08);

            .name {
              color: #fff;
            }
          }

          /* 选中状态 */
          &.is-active {
            background: rgba(@primary-color, 0.15);
            border-color: rgba(@primary-color, 0.3);

            .item-icon i {
              color: @primary-color;
            }

            .item-info .name {
              color: @primary-color;
              font-weight: 500;
            }
          }

          .item-icon {
            margin-right: 10px;

            i {
              font-size: 16px;
              color: @text-sub;
            }
          }

          .item-info {
            flex: 1;
            overflow: hidden;

            .name {
              font-size: 13px;
              line-height: 1.4;
              color: #ccc;
              margin-bottom: 4px;
              /* 单行省略 */
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }

            .size {
              font-size: 12px;
              color: @text-sub;
            }
          }
        }
      }
    }
  }
}
</style>