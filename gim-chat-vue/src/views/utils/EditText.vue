<template>
  <div class="typora-style-editor">
    <h2>Typora 风格 (Live Preview 模式) 编辑器</h2>
    
    <textarea ref="markdownEditor"></textarea>
    
    <div class="note">
        <p>💡 **提示：**</p>
        <ul>
            <li>该组件使用了 EasyMDE 库来实现实时预览。</li>
            <li>要激活类似 Typora 的预览效果，请点击编辑器工具栏右上角的 **眼睛图标 (Preview)** 或 **分屏图标 (Side-by-Side)**。</li>
            <li>您也可以点击 **全屏图标 (Fullscreen)** 获得沉浸式编辑体验。</li>
        </ul>
    </div>
  </div>
</template>

<script>
// 1. 导入 EasyMDE 核心库
import EasyMDE from 'easymde';
// 2. 导入 EasyMDE 默认样式
import 'easymde/dist/easymde.min.css';

// 3. 导入 Markdown 解析和代码高亮库 (用于自定义渲染)
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css'; // 导入代码高亮主题样式

export default {
  name: 'TyporaStyleEditor',
  data() {
    return {
      mde: null, // EasyMDE 实例
      markdownContent: `# Vue 2 集成 EasyMDE 编辑器

这是一个基于 EasyMDE 的 Markdown 编辑器组件。

## 主要功能

* 实时预览 (Live Preview)
* 全屏模式 (Fullscreen Mode)
* 支持代码块和高亮

### 代码高亮示例
\`\`\`javascript
function setupEditor() {
  const element = document.getElementById('editor');
  if (element) {
    return new EasyMDE({ element: element });
  }
}

setupEditor();
\`\`\`
`,
    };
  },
  mounted() {
    this.initializeEditor();
  },
  methods: {
    initializeEditor() {
        this.mde = new EasyMDE({
            element: this.$refs.markdownEditor, // 绑定到模板中的 textarea
            initialValue: this.markdownContent, // 设置初始内容
            autofocus: true,
            // 配置工具栏，提供预览、分屏和全屏选项
            toolbar: [
                "bold", "italic", "heading", "|", 
                "quote", "unordered-list", "ordered-list", "|", 
                "link", "image", "table", "horizontal-rule", "|", 
                "preview", "side-by-side", "fullscreen", "|", 
                "guide"
            ],
            // 核心：自定义预览渲染函数，集成 marked 和 highlight.js
            previewRender: (plainText) => {
                return this.customMarkedRender(plainText);
            },
        });
        
        // 监听内容变化，更新 Vue 组件的 data (可选，用于数据双向绑定或保存)
        this.mde.codemirror.on("change", () => {
            this.markdownContent = this.mde.value();
            // 在实际项目中，您可能需要在这里触发一个事件 emit 给父组件
            // this.$emit('change', this.markdownContent);
        });
    },
    
    // 负责将 Markdown 源码转换为带代码高亮的 HTML
    customMarkedRender(markdownText) {
        // 配置 marked 选项，尤其是如何处理代码块
        marked.setOptions({
            gfm: true,
            breaks: true,
            highlight: (code, lang) => {
                const language = hljs.getLanguage(lang) ? lang : 'plaintext';
                try {
                    // 使用 highlight.js 进行语法高亮
                    return hljs.highlight(code, { language }).value;
                } catch (error) {
                    // 如果高亮失败，返回原始内容
                    return code;
                }
            }
        });
        // 执行解析并返回 HTML 字符串
        return marked.parse(markdownText);
    }
  },
  beforeDestroy() {
    // 组件销毁前，确保销毁 EasyMDE 实例，防止内存泄漏
    if (this.mde) {
      this.mde.toTextArea(); // 将编辑器还原为 textarea
      this.mde = null;
    }
  }
};
</script>

<style scoped>
/* 容器和居中 */
.typora-style-editor {
    max-width: 900px;
    margin: 50px auto;
    padding: 20px;
}

/* 提示信息框 */
.note {
    margin-top: 20px;
    padding: 15px;
    border: 1px solid #cce5ff; /* 蓝色边框 */
    background-color: #e6f3ff; /* 浅蓝色背景 */
    border-radius: 4px;
    color: #004085; /* 深蓝色文字 */
}
.note ul {
    margin-top: 10px;
    padding-left: 20px;
    list-style-type: disc;
}

/* 优化 EasyMDE 样式以适应页面 */
/* 如果 EasyMDE 看起来太小或太宽，可以在这里调整 */
/* 例如，设置编辑器的高度 */
.typora-style-editor >>> .CodeMirror {
    min-height: 400px; 
    height: auto; /* 允许内容增长 */
}

/* 隐藏 EasyMDE 默认的 status bar，如果不需要的话 */
/* .typora-style-editor >>> .editor-statusbar { display: none; } */
</style>