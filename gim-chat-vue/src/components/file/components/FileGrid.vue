<template>
    <div class="file-grid-wrapper tech-grid-wrapper">
        <ul
            class="file-list tech-file-list"
            v-loading="loading"
            element-loading-text="文件加载中..."
        >
            <li
                class="file-item tech-file-item"
                v-for="(item, index) in fileListSorted"
                :key="index"
                :title="$file.getFileNameComplete(item)"
                :style="`width: ${gridSize + 40}px; `"
                :class="item.userFileId === selectedFile.userFileId ? 'tech-active-item' : ''"
                @click="$file.handleFileNameClick(item, index, fileListSorted)"
                @contextmenu.prevent="handleContextMenu(item, index, $event)"
            >
                <video
                    :style="`height:${gridSize}px; width:${gridSize}px`"
                    v-if="$file.isVideoFile(item)"
                    :src="$file.setFileImg(item)"
                ></video>
                <el-image
                    class="file-img"
                    :src="$file.setFileImg(item)"
                    :style="`width: ${gridSize}px; height: ${gridSize}px;`"
                    fit="cover"
                    v-else
                />
                <div
                    class="file-name tech-file-name"
                    v-html="$file.getFileNameComplete(item, true)"
                ></div>
                <i
                    class="file-operate el-icon-more tech-file-operate"
                    :class="`operate-more-${index}`"
                    v-if="screenWidth <= 768"
                    @click.stop="handleClickMore(item, $event)"
                ></i>
                <div
                    class="file-checked-wrapper tech-checked-wrapper"
                    :class="{ 'tech-checked': item.checked }"
                    v-show="isBatchOperation"
                    @click.stop.self="item.checked = !item.checked"
                >
                    <el-checkbox
                        class="file-checked tech-checkbox"
                        v-model="item.checked"
                        @click.stop="item.checked = !item.checked"
                    ></el-checkbox>
                </div>
            </li>
        </ul>
        <ul class="right-menu-list tech-right-menu-list">
            <li class="right-menu-item tech-menu-item">
                <i class="el-icon-folder-opened"></i> 打开
            </li>
            <li class="right-menu-item tech-menu-item">
                <i class="el-icon-download"></i> 下载
            </li>
        </ul>
    </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
    name: 'FileGrid',
    props: {
        fileType: {
            required: true,
            type: Number
        },
        filePath: {
            required: true,
            type: String
        },
        fileList: Array,
        loading: Boolean
    },
    data() {
        return {
            fileListSorted: [],
            officeFileType: ['ppt', 'pptx', 'doc', 'docx', 'xls', 'xlsx'],
            selectedFile: {}
        }
    },
    computed: {
        ...mapGetters(['gridSize']),
        isBatchOperation() {
            return this.$store.state.fileList.isBatchOperation
        },
        selectedFileList() {
            let res = this.fileListSorted.filter((item) => item.checked)
            return res
        },
        screenWidth() {
            return this.$store.state.common.screenWidth
        }
    },
    watch: {
        fileList(newValue) {
            this.fileListSorted = [...newValue]
                .sort((pre, next) => {
                    return next.isDir - pre.isDir
                })
                .map((item) => {
                    return {
                        ...item,
                        checked: false
                    }
                })
        },
        selectedFileList(newValue) {
            this.$store.commit('changeSelectedFiles', newValue)
            this.$store.commit('changeIsBatchOperation', newValue.length !== 0)
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
        },
        handleClickMore(item, event) {
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

.file-grid-wrapper.tech-grid-wrapper {
    min-height: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 16px 0 0;
    border-radius: 24px;
    background: @panel-bg;
    border: 1px solid @border-color;
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.04),
        0 18px 36px rgba(0, 0, 0, 0.18);
    overflow: hidden;
    color: @text-main;
}

.file-grid-wrapper.tech-grid-wrapper ::v-deep .el-loading-mask {
    background: rgba(7, 12, 24, 0.72) !important;
    backdrop-filter: blur(6px);
}

.file-list.tech-file-list {
    flex: 1;
    overflow: auto;
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    align-content: flex-start;
    gap: 14px;
    list-style: none;
    padding: 4px 18px 18px;
    margin: 0;
    .setScrollbar(6px, transparent, rgba(148, 163, 184, 0.28));
}

.file-item.tech-file-item {
    margin: 0;
    position: relative;
    padding: 14px 12px 12px;
    text-align: center;
    cursor: pointer;
    background: @surface;
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 20px;
    transition: all 0.24s ease;

    video,
    .file-img {
        display: block;
        margin: 0 auto;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.03);
        object-fit: cover;
    }

    &:hover {
        background: @surface-hover;
        border-color: @border-strong;
        box-shadow: 0 18px 36px rgba(0, 0, 0, 0.18);
        transform: translateY(-2px);
    }

    &.tech-active-item {
        background: rgba(110, 168, 255, 0.12);
        border-color: @border-strong;
        box-shadow: 0 18px 36px rgba(0, 0, 0, 0.2);
    }

    .file-name.tech-file-name {
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

    .file-operate.tech-file-operate {
        position: absolute;
        top: 10px;
        right: 10px;
        width: 30px;
        height: 30px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: 10px;
        background: rgba(255, 255, 255, 0.05);
        color: @text-sub;
        transition: all 0.2s ease;

        &:hover {
            color: @accent;
            background: rgba(110, 168, 255, 0.1);
        }
    }

    .file-checked-wrapper.tech-checked-wrapper {
        position: absolute;
        inset: 0;
        z-index: 2;
        background: rgba(7, 12, 24, 0.48);
        backdrop-filter: blur(2px);
        border-radius: 20px;

        .file-checked.tech-checkbox {
            position: absolute;
            top: 12px;
            left: 12px;

            ::v-deep .el-checkbox__inner {
                background: rgba(255, 255, 255, 0.04);
                border-color: rgba(148, 163, 184, 0.22);
            }

            ::v-deep .el-checkbox__input.is-checked .el-checkbox__inner,
            ::v-deep .el-checkbox__input.is-indeterminate .el-checkbox__inner {
                background: @accent;
                border-color: @accent;
            }
        }
    }

    .file-checked-wrapper.tech-checked {
        background: rgba(7, 12, 24, 0.2);
    }
}

.right-menu-list.tech-right-menu-list,
.unzip-list {
    background: rgba(11, 19, 35, 0.96);
    border: 1px solid rgba(110, 168, 255, 0.22);
    border-radius: 14px;
    box-shadow: 0 24px 48px rgba(0, 0, 0, 0.24);
    color: @text-main;
    padding: 6px 0;
}

.right-menu-list.tech-right-menu-list {
    position: fixed;
    display: none;
    flex-direction: column;
    min-width: 160px;
    z-index: 1000;
}

.tech-menu-item {
    height: 38px;
    line-height: 38px;
    padding: 0 16px;
    cursor: pointer;
    font-size: 14px;
    color: @text-main;
    transition: all 0.2s ease;

    &:hover {
        background: rgba(110, 168, 255, 0.1);
        color: @accent;
    }

    i {
        margin-right: 8px;
        color: @text-sub;
    }
}

.unzip-menu-item {
    position: relative;
}

.unzip-menu-item:hover .unzip-list {
    display: block;
}

.unzip-menu-item .unzip-list {
    position: absolute;
    top: 0;
    left: 100%;
    display: none;
    min-width: 180px;
}

@media (max-width: 768px) {
    .file-grid-wrapper.tech-grid-wrapper {
        padding-top: 12px;
        border-radius: 20px;
    }

    .file-list.tech-file-list {
        padding: 4px 14px 14px;
    }
}
</style>
