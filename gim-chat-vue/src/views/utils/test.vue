<template>
  <div class="chat-view">

    <!-- 视频采集区域 -->
    <section class="video-box">
      <video ref="video" autoplay muted playsinline />
      <canvas ref="canvas" style="display:none" />

      <div class="actions">
        <el-button type="primary" @click="start">开始</el-button>
        <el-button type="danger" @click="stop">停止</el-button>
      </div>
    </section>
  </div>
</template>


<script>
import WebSocketService from '@/plugins/ws';
export default {
  data() {
    return {
      // WebRTC / 视频
      stream: null,
      videoEl: null,
      canvasEl: null,
      ctx: null,

      // WebSocket
      ws: null,
      wsReady: false,

      // 定时器
      captureTimer: null
    }
  },

  computed: {
  },

  mounted() {
    this.videoEl = this.$refs.video
    this.canvasEl = this.$refs.canvas
    this.ctx = this.canvasEl.getContext('2d')
  },

  methods: {
    /* ================= 摄像头 ================= */
    async startCamera() {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: 640,
          height: 480,
          frameRate: { ideal: 10, max: 15 }
        },
        audio: false
      })

      this.videoEl.srcObject = this.stream
    },

    stopCamera() {
      if (!this.stream) return
      this.stream.getTracks().forEach(t => t.stop())
      this.stream = null
    },

    /* ================= WebSocket ================= */
    initWS() {
      return new Promise((resolve, reject) => {
        this.ws = new WebSocket('ws://localhost:9876/video')
        this.ws.binaryType = 'arraybuffer'

        this.ws.onopen = () => {
          this.wsReady = true
          resolve()
        }

        this.ws.onerror = err => {
          this.wsReady = false
          reject(err)
        }

        this.ws.onclose = () => {
          this.wsReady = false
        }
      })
    },

    closeWS() {
      if (this.ws) {
        this.ws.close()
        this.ws = null
      }
    },

    /* ================= 抽帧 ================= */
    startCapture() {
      this.canvasEl.width = 640
      this.canvasEl.height = 480

      this.captureTimer = setInterval(() => {
        if (!this.wsReady) return
        if (this.ws.bufferedAmount > 2 * 1024 * 1024) return

        this.ctx.drawImage(
          this.videoEl,
          0,
          0,
          this.canvasEl.width,
          this.canvasEl.height
        )

        this.canvasEl.toBlob(blob => {
          blob && this.ws.send(blob)
        }, 'image/jpeg', 0.6)
      }, 100)
    },

    stopCapture() {
      if (this.captureTimer) {
        clearInterval(this.captureTimer)
        this.captureTimer = null
      }
    },

    /* ================= 总控 ================= */
    async start() {
      try {
        await this.startCamera()
        await this.initWS()

        this.startCapture()
      } catch (e) {
        console.error('启动失败', e)
        this.stop()
      }
    },

    stop() {
      this.stopCapture()
      this.stopCamera()
      this.closeWS()
    }
  },

  beforeDestroy() {
    this.stop()
  }
}

</script>

<style scoped></style>