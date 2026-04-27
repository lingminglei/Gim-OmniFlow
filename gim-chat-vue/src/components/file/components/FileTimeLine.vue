<template>
    <div class="image-timeline-wrapper tech-timeline-wrapper">
        <div class="radio tech-radio-group">
            排序：
            <el-radio-group v-model="reverse">
                <el-radio :label="true">倒序</el-radio>
                <el-radio :label="false">正序</el-radio>
            </el-radio-group>
        </div>
        <el-timeline
            class="image-timeline-list tech-timeline-list"
            :reverse="reverse"
            v-if="imageTimelineData.length"
        >
            <el-timeline-item
                class="image-timeline-item tech-timeline-item"
                v-for="(item, index) in imageTimelineData"
                :key="index"
                :timestamp="item.uploadDate"
                color="#6ea8ff"
                placement="top"
            >
                <ul class="image-list tech-image-list">
                    <li
                        class="image-item tech-image-item"
                        v-for="(image, imageIndex) in item.imageList"
                        :key="`${index}-${imageIndex}`"
                        :style="`width: ${gridSize + 40}px; `"
                        @click="$file.handleImgPreview(imageIndex, {}, item.imageList)"
                        @contextmenu.prevent="handleContextMenu(item, imageIndex, $event)"
                    >
                        <img
                            class="image"
                            :src="$file.getMinImgStream(image)"
                            :alt="$file.getFileNameComplete(image)"
                            :style="`width: ${gridSize}px; height: ${gridSize}px;`"
                        />
                        <div
                            class="image-name tech-image-name"
                            v-html="$file.getFileNameComplete(image, true)"
                        ></div>
                    </li>
                </ul>
            </el-timeline-item>
        </el-timeline>
    </div>
</template>

<script>
export default {
    name: 'FileTimeLine',
    props: {
        fileList: {
            required: true,
            type: Array
        }
    },
    data() {
        return {
            reverse: true
        }
    },
    computed: {
        imageTimelineData() {
            let res = []
            let uploadTimeSet = new Set(
                this.fileList.map((item) => item.uploadTime.split(' ')[0])
            )
            let uploadDate = [...uploadTimeSet]
            uploadDate.forEach((element) => {
                res.push({
                    uploadDate: element,
                    imageList: this.fileList.filter(
                        (item) => item.uploadTime.split(' ')[0] === element
                    )
                })
            })
            return res
        },
        gridSize() {
            return this.$store.getters.gridSize
        },
        screenWidth() {
            return this.$store.state.common.screenWidth
        }
    },
    methods: {
        handleContextMenu(item, index, event) {
            event.cancelBubble = true
            if (this.screenWidth > 768) {
                this.selectedFile = item
                if (!this.isBatchOperation) {
                    event.preventDefault()
                    this.$openBox
                        .contextMenu({
                            selectedFile: item,
                            domEvent: event
                        })
                        .then((res) => {
                            this.selectedFile = {}
                            if (res === 'confirm') {
                                this.$emit('getTableDataByType')
                                this.$store.dispatch('showStorage')
                            }
                        })
                }
            }
        }
    }
}
</script>

<style lang="less" scoped>
@panel-bg: linear-gradient(180deg, rgba(18, 28, 47, 0.88), rgba(11, 18, 33, 0.86));
@surface: rgba(255, 255, 255, 0.04);
@surface-hover: rgba(110, 168, 255, 0.08);
@border-color: rgba(148, 163, 184, 0.16);
@border-strong: rgba(110, 168, 255, 0.22);
@text-main: #edf4ff;
@text-sub: #9cb0cf;
@text-muted: #6f809e;
@accent: #6ea8ff;

.setScrollbar(@width, @track-color, @thumb-color) {
  &::-webkit-scrollbar {
    width: @width;
    height: @width;
  }

  &::-webkit-scrollbar-track {
    background: @track-color;
  }

  &::-webkit-scrollbar-thumb {
    background: @thumb-color;
    border-radius: @width / 2;
  }
}

.image-timeline-wrapper.tech-timeline-wrapper {
    min-height: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 18px;
    border-radius: 24px;
    background: @panel-bg;
    border: 1px solid @border-color;
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.04),
        0 18px 36px rgba(0, 0, 0, 0.18);
    color: @text-main;
    overflow: hidden;
}

.radio.tech-radio-group {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    border-radius: 16px;
    background: @surface;
    border: 1px solid rgba(255, 255, 255, 0.05);
    color: @text-sub;
    font-size: 13px;
    margin-bottom: 16px;

    ::v-deep .el-radio {
        color: @text-sub;
    }

    ::v-deep .el-radio__input.is-checked .el-radio__inner {
        background: @accent;
        border-color: @accent;
        box-shadow: 0 0 0 4px rgba(110, 168, 255, 0.12);
    }

    ::v-deep .el-radio__input.is-checked + .el-radio__label {
        color: @text-main;
    }

    ::v-deep .el-radio__inner {
        background-color: rgba(255, 255, 255, 0.04);
        border-color: rgba(148, 163, 184, 0.26);
    }
}

.image-timeline-list.tech-timeline-list {
    flex: 1;
    overflow: auto;
    margin: 0;
    padding: 0 6px 0 4px;
    .setScrollbar(6px, transparent, rgba(148, 163, 184, 0.28));

    ::v-deep .el-timeline-item__tail {
        border-left: 2px solid rgba(148, 163, 184, 0.14);
    }

    ::v-deep .el-timeline-item__node {
        box-shadow: 0 0 0 6px rgba(110, 168, 255, 0.12);
    }
}

.image-timeline-item.tech-timeline-item {
    ::v-deep .el-timeline-item__timestamp {
        color: @text-main;
        font-size: 15px;
        font-weight: 600;
        padding-bottom: 12px;
    }

    .image-list.tech-image-list {
        display: flex;
        flex-wrap: wrap;
        gap: 14px;
        list-style: none;
        padding: 0;
        margin: 0;
    }

    .image-item.tech-image-item {
        margin: 0;
        padding: 12px;
        text-align: center;
        cursor: pointer;
        background: @surface;
        border: 1px solid rgba(255, 255, 255, 0.06);
        border-radius: 20px;
        transition: all 0.24s ease;

        img.image {
            display: block;
            margin: 0 auto;
            border-radius: 14px;
            object-fit: cover;
            background: rgba(255, 255, 255, 0.03);
        }

        &:hover {
            background: @surface-hover;
            border-color: @border-strong;
            box-shadow: 0 18px 36px rgba(0, 0, 0, 0.18);
            transform: translateY(-2px);
        }

        .image-name.tech-image-name {
            margin-top: 12px;
            min-height: 40px;
            line-height: 20px;
            font-size: 13px;
            color: @text-main;
            word-break: break-all;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;

            ::v-deep .keyword {
                color: #ff8a8a;
                font-weight: 600;
            }
        }
    }
}

@media (max-width: 768px) {
    .image-timeline-wrapper.tech-timeline-wrapper {
        padding: 14px;
        border-radius: 20px;
    }
}
</style>
