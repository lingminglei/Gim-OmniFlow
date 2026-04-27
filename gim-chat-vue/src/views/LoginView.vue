<template>
    <div class="login-view-container">
        <div class="tech-bg-overlay"></div>

        <div class="login-card-wrapper tech-dialog">
            <div class="dialog-content-wrapper">
                <h2 class="login-title">用户登录</h2>
                <p class="login-subtitle">欢迎连接 Gim-Chat 智能终端</p>

                <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="tech-form">

                    <el-form-item prop="phone">
                        <el-input v-model="loginForm.phone" placeholder="请输入手机号" prefix-icon="el-icon-mobile-phone"
                            class="tech-input"></el-input>
                    </el-form-item>

                    <el-form-item prop="passWord">
                        <el-input v-model="loginForm.passWord" type="password" show-password placeholder="请输入密码"
                            prefix-icon="el-icon-lock" class="tech-input"></el-input>
                    </el-form-item>

                    <el-form-item prop="captcha">
                        <div class="captcha-item">
                            <el-input v-model="loginForm.captcha" placeholder="请输入验证码" prefix-icon="el-icon-key"
                                class="tech-input captcha-input"></el-input>
                            <div class="captcha-img-box" @click="refreshCaptcha">
                                <img :src="captchaUrl" alt="验证码" class="captcha-img" v-if="captchaUrl" />
                                <div v-else class="captcha-placeholder">点击获取验证码</div>
                            </div>
                        </div>

                    </el-form-item>

                    <el-form-item class="login-action-item">
                        <el-button type="primary" @click="submitLogin" class="tech-btn block-btn primary"
                            :loading="isLoading">
                            {{ isLoading ? '连接中...' : '登 录' }} <i class="el-icon-right"></i>
                        </el-button>
                    </el-form-item>

                    <div class="extra-links">
                        <el-button type="text" class="register-btn" @click="goToRegister">
                            还没有账号？去注册 <i class="el-icon-arrow-right"></i>
                        </el-button>
                    </div>
                </el-form>
            </div>
        </div>
    </div>
</template>

<script>
import { handleLogin, getPicturCode } from '@/api/user' // 确保路径正确
import WebSocketService from '@/plugins/ws'; // 确保路径正确
import Vue from 'vue'

// 假设您有一个获取验证码图片的 API
const CAPTCHA_API_BASE = '/api/getCaptchaImage?t='

export default {
    name: 'LoginView',
    data() {
        return {
            isLoading: false, // 登录加载状态
            captchaUrl: '', // 验证码图片URL
            loginForm: {
                phone: "",
                passWord: "",
                captcha: "", // 验证码字段
            },
            loginRules: {
                phone: [
                    { required: true, message: "请输入手机号", trigger: "blur" },
                    {
                        pattern: /^1[3-9]\d{9}$/,
                        message: "请输入合法的手机号",
                        trigger: "blur",
                    },
                ],
                passWord: [{ required: true, message: "请输入密码", trigger: "blur" }],
                captcha: [
                    { required: true, message: "请输入验证码", trigger: "blur" },
                    { min: 4, max: 6, message: "验证码长度不符", trigger: "blur" } // 假设长度是4-6位
                ]
            },
        };
    },
    mounted() {
    },
    methods: {
        // 刷新验证码图片
        async refreshCaptcha() {
            const param = {
                ...this.loginForm
            }

            const resData = await getPicturCode(param)

            if (resData && resData.data && resData.data.code === 200) {

                console.log('resData.data.data.captcha', resData.data.data)

                this.captchaUrl = 'data:image/png;base64,' + resData.data.data.img


                console.log('this.captchaUrl', this.captchaUrl)
            }else{

                this.$message.error(resData.data.message)
            }

        },
        // 跳转到注册页面
        goToRegister() {
            this.$router.push({ name: 'register' });
        },
        async submitLogin() {
            const that = this
            this.$refs.loginForm.validate(async (valid) => {
                if (valid) {
                    this.isLoading = true; // 开始加载
                    const param = { ...this.loginForm }
                    try {
                        // ⚠️ 假设 handleLogin 返回的数据结构为 { data: { code: 200, data: { token: '...', userInfo: {} }, message: '...' } }
                        const res = await handleLogin(param);
                        const data = res.data.data;
                        if (res.data.code === 200 && data.token) {
                            const token = data.token;
                            const user = data.userInfo;
                            that.$store.dispatch('user/login', {
                                token,
                                userInfo: user
                            });

                             console.log('checkTokenBegin====',token)
                            that.$message.success('连接成功，欢迎回来');
                            // 登录成功后跳转到首页
                            this.$router.push({ name: 'index' });
                        } else {
                            that.$message.error(res.data.message || '登录失败');
                            this.refreshCaptcha(); // 登录失败，刷新验证码
                        }
                    } catch (e) {
                        console.error(e);
                        that.$message.error('网络错误或服务异常');
                        this.refreshCaptcha(); // 发生错误，刷新验证码
                    } finally {
                        this.isLoading = false; // 结束加载
                    }
                }
            });
        },
    },
};
</script>

<style lang="less" scoped>
/* ================= 变量定义 (从 App.vue 复制) ================= */
@bg-dark: #0f1219;
@primary-color: #00f2ff;
/* 赛博青色 */
@accent-color: #7d2ae8;
/* 霓虹紫 */
@text-sub: #94a3b8;
@text-main: #e2e8f0;

/* 动画颜色定义 */
@glow-color-A: #00f2ff;
@glow-color-B: #7d2ae8;

/* 🚀 关键帧动画：光晕呼吸 */
@keyframes tech-glow-pulse {
    0% {
        box-shadow: 0 0 15px fade(@glow-color-A, 50%), 0 0 30px fade(@glow-color-A, 30%);
    }

    50% {
        box-shadow: 0 0 25px fade(@glow-color-B, 70%), 0 0 45px fade(@glow-color-B, 50%);
    }

    100% {
        box-shadow: 0 0 15px fade(@glow-color-A, 50%), 0 0 30px fade(@glow-color-A, 30%);
    }
}


/* ================= 页面布局 (居中) ================= */
.login-view-container {
    display: flex;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */
    min-height: 100vh;
    background-color: @bg-dark;
    position: relative;
    overflow: hidden;
    z-index: 1;
}

/* 背景纹理 */
.tech-bg-overlay {
    position: absolute;
    width: 100%;
    height: 100%;
    background:
        radial-gradient(circle at 10% 80%, rgba(125, 42, 232, 0.1) 0%, transparent 30%),
        radial-gradient(circle at 90% 20%, rgba(0, 242, 255, 0.08) 0%, transparent 30%);
    z-index: 0;
}

/* 登录卡片样式 */
.login-card-wrapper {
    width: 520px;
    padding-bottom: 20px;
    background: rgba(26, 29, 38, 0.95);
    backdrop-filter: blur(5px);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 12px;
    z-index: 10;

    /* 应用光晕动画 */
    animation: tech-glow-pulse 4s ease-in-out infinite alternate;
    box-shadow: 0 0 40px rgba(0, 0, 0, 0.8);
}

.dialog-header {
    padding: 20px 30px 15px;
    position: relative;

    .dialog-title-label {
        color: @primary-color;
        font-size: 14px;
        letter-spacing: 2px;
        font-family: 'Consolas', monospace;
        text-align: center;
        margin-bottom: 10px;
    }

    .header-deco-line {
        height: 1px;
        width: 80%;
        margin: 0 auto;
        background: linear-gradient(90deg, transparent, @primary-color, transparent);
        box-shadow: 0 0 5px @primary-color;
    }
}

.dialog-content-wrapper {
    padding: 30px;
    padding-top: 10px;
    text-align: center;

    .login-title {
        font-size: 28px;
        color: #fff;
        margin-bottom: 8px;
        font-weight: 700;
        text-shadow: 0 0 5px rgba(0, 242, 255, 0.4);
    }

    .login-subtitle {
        color: @text-sub;
        font-size: 14px;
        margin-bottom: 40px;
    }
}

/* ================= Element UI 样式覆盖 (美化) ================= */
.tech-btn {
    &.primary {
        background: linear-gradient(135deg, @primary-color, #006eff);
        box-shadow: 0 4px 15px rgba(0, 242, 255, 0.5);
        font-weight: 700;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

        &:hover {
            transform: scale(1.02);
            box-shadow: 0 0 25px rgba(0, 242, 255, 0.8), 0 0 10px rgba(125, 42, 232, 0.5);
            background: linear-gradient(135deg, #00f2ff, #0099ff);
        }

        &:active {
            transform: scale(0.98);
            box-shadow: 0 0 5px rgba(0, 242, 255, 0.9);
        }
    }

    &.block-btn {
        width: 100%;
        height: 48px;
        font-size: 18px;
        letter-spacing: 3px;
    }
}

.tech-form {
    .el-form-item {
        margin-bottom: 25px;
    }

    .el-input__inner {
        background: rgba(0, 0, 0, 0.4);
        border: 1px solid rgba(0, 242, 255, 0.2);
        color: @text-main;
        height: 48px;
        border-radius: 4px;
        font-size: 16px;

        &:focus {
            border-color: @primary-color;
            box-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        }
    }

    .el-input__icon {
        color: @primary-color;
        font-size: 18px;
    }

    /* 验证码容器样式 */
    .captcha-item {
        display: flex;
        align-items: flex-start;

        .captcha-input {
            flex: 1;
            margin-right: 15px;
        }

        .captcha-img-box {
            height: 48px;
            width: 100px;
            border-radius: 4px;
            overflow: hidden;
            cursor: pointer;
            border: 1px solid rgba(0, 242, 255, 0.3);
            transition: all 0.3s;
            background: rgba(0, 0, 0, 0.6);
            display: flex;
            justify-content: center;
            align-items: center;

            &:hover {
                box-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
            }

            .captcha-img {
                width: 100%;
                height: 100%;
                display: block;
                object-fit: contain;
            }

            .captcha-placeholder {
                color: @text-sub;
                font-size: 12px;
            }
        }
    }
}

/* 底部额外链接样式 (去注册) */
.extra-links {
    text-align: right;
    margin-top: 15px;

    .register-btn {
        color: @text-sub;
        font-size: 14px;
        padding: 5px 10px;
        border-radius: 4px;
        transition: color 0.3s, background 0.3s;

        &:hover {
            color: @primary-color;
            background: rgba(0, 242, 255, 0.05);
        }
    }
}
</style>