<template>
  <div>
    <uploader ref="uploader" :options="options" @file-added="handleFilesAdded"
              @file-success="handleSuccess" @file-error="handleError" :autoStart="false">
      <uploader-btn :attrs="attrs" ref="uploadBtn">
        <el-button type="primary" icon="el-icon-upload" @click="handleFilesAdded">上传文件</el-button>
      </uploader-btn>
    </uploader>
  </div>
</template>

<script>
import SparkMD5 from 'spark-md5';

export default {
  name: 'FileUploader',
  data() {
    return {
      options: {
        target: '/api/filetransfer/uploadfile',
        chunkSize: 1024 * 1024,
        fileParameterName: 'file',
        maxChunkRetries: 3,
        testChunks: true,
        headers: { token: '123' },
        checkChunkUploadedByResponse: (chunk, message) => {
          try {
            const data = JSON.parse(message).data || {};
            if (data.skipUpload) return true;
            return (data.uploaded || []).includes(chunk.offset + 1);
          } catch {
            return false;
          }
        },
        query() {}
      },
      attrs: {
        accept: '*'
      }
    };
  },
  computed: {
    uploaderInstance() {
      return this.$refs.uploader.uploader;
    }
  },
  methods: {
    handleFilesAdded(files) {
      console.log("处理文件上传：")
      console.log(files)
      if (!files || !files.length) return;
      this.computeMD5(files[0]); // 只支持单文件上传
    },
    computeMD5(file) {
      const reader = new FileReader();
      const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
      const chunkSize = 1 * 1024 * 1024;
      const chunks = Math.ceil(file.size / chunkSize);
      let currentChunk = 0;
      const spark = new SparkMD5.ArrayBuffer();

      file.pause();

      reader.onload = (e) => {
        spark.append(e.target.result);
        currentChunk++;
        if (currentChunk < chunks) {
          loadNext();
        } else {
          const md5 = spark.end();
          this.afterMD5(md5, file);
        }
      };

      reader.onerror = () => {
        this.$emit('error', '读取文件失败');
        file.cancel();
      };

      const loadNext = () => {
        const start = currentChunk * chunkSize;
        const end = Math.min(start + chunkSize, file.size);
        reader.readAsArrayBuffer(blobSlice.call(file.file, start, end));
      };

      loadNext();
    },
    afterMD5(md5, file) {
      file.uniqueIdentifier = md5;
      file.resume();
    },
    handleSuccess(rootFile, file, response) {
      try {
        const res = JSON.parse(response);
        if (res.code === 200 && res.data.uploaded.at(-1) === res.data.uploaded.length) {
          this.$emit('success', {
            fileName: file.name,
            fileSize: file.size,
            response: res
          });
        } else {
          this.$emit('error', res.message || '上传异常');
        }
      } catch (e) {
        this.$emit('error', '解析响应失败');
      }
    },
    handleError(rootFile, file, response) {
      this.$emit('error', response || '上传失败');
    },
    cancelUpload() {
      this.uploaderInstance.cancel();
      this.$emit('cancel');
    }
  }
};
</script>
