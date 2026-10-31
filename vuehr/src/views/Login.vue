<template>
    <main class="login-page">
        <section class="login-story" aria-label="微人事介绍">
            <div class="login-brand"><span>微</span><strong>微人事</strong><small>人力资源管理平台</small></div>
            <div class="story-content">
                <div class="story-kicker">有序工作，从这里开始</div>
                <h1>让每一份人才，<br />都被认真对待。</h1>
                <p>连接员工与组织，让繁杂的人事事务<br />回归简单、清晰与高效。</p>
                <div class="story-features">
                    <div>
                        <el-icon><User /></el-icon
                        ><span><strong>员工档案</strong><small>完整记录每一段成长</small></span>
                    </div>
                    <div>
                        <el-icon><OfficeBuilding /></el-icon
                        ><span><strong>组织协作</strong><small>让团队信息井然有序</small></span>
                    </div>
                    <div>
                        <el-icon><Wallet /></el-icon
                        ><span><strong>薪资管理</strong><small>从日常事务到清晰管理</small></span>
                    </div>
                </div>
            </div>
            <div class="story-footer">微人事 · 为更好的工作体验而设计</div>
        </section>
        <section class="login-form-area">
            <div class="login-card">
                <span class="form-eyebrow">欢迎回来</span>
                <h2>欢迎来到你的工作空间</h2>
                <p class="form-description">使用你的账号，继续今天的工作。</p>
                <el-form
                    ref="loginForm"
                    :rules="rules"
                    :model="loginForm"
                    label-position="top"
                    size="large"
                    @submit.prevent="submitLogin"
                >
                    <el-form-item label="用户名" prop="username"
                        ><el-input
                            v-model="loginForm.username"
                            autocomplete="username"
                            placeholder="请输入用户名"
                            clearable
                            ><template #prefix
                                ><el-icon><User /></el-icon></template></el-input
                    ></el-form-item>
                    <el-form-item label="密码" prop="password"
                        ><el-input
                            v-model="loginForm.password"
                            type="password"
                            autocomplete="current-password"
                            placeholder="请输入密码"
                            show-password
                            ><template #prefix
                                ><el-icon><Lock /></el-icon></template></el-input
                    ></el-form-item>
                    <el-form-item label="验证码" prop="code"
                        ><div class="captcha-row">
                            <el-input v-model="loginForm.code" autocomplete="off" placeholder="请输入验证码" /><button
                                type="button"
                                class="captcha-button"
                                aria-label="刷新验证码"
                                title="点击刷新验证码"
                                @click="updateVerifyCode"
                            >
                                <img :src="vcUrl" alt="登录验证码" />
                            </button></div
                    ></el-form-item>
                    <p class="captcha-hint">看不清验证码？点击图片换一张。</p>
                    <el-button class="login-submit" type="primary" native-type="submit" :loading="loading"
                        >{{ loading ? '正在登录…' : '登录' }}<el-icon v-if="!loading"><ArrowRight /></el-icon
                    ></el-button>
                </el-form>
                <div class="login-help">
                    <el-icon><InfoFilled /></el-icon>账号或权限遇到问题，请联系管理员
                </div>
            </div>
            <footer class="login-footer">微人事 · 人力资源管理平台</footer>
        </section>
    </main>
</template>

<script>
export default {
    name: 'Login',
    data() {
        return {
            loading: false,
            vcUrl: '/verifyCode?time=' + Date.now(),
            loginForm: { username: '', password: '', code: '' },
            rules: {
                username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
                code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
            },
        };
    },
    methods: {
        updateVerifyCode() {
            this.vcUrl = '/verifyCode?time=' + Date.now();
        },
        submitLogin() {
            if (this.loading) return;
            this.$refs.loginForm.validate(async (valid) => {
                if (!valid || this.loading) return;
                this.loading = true;
                try {
                    const resp = await this.postRequest('/doLogin', this.loginForm);
                    if (resp) {
                        this.$store.commit('INIT_CURRENTHR', resp.obj);
                        window.sessionStorage.setItem('user', JSON.stringify(resp.obj));
                        const path = this.$route.query.redirect;
                        this.$router.replace(!path || path === '/' ? '/home' : path);
                    } else {
                        this.updateVerifyCode();
                    }
                } catch {
                    this.updateVerifyCode();
                    this.$message.error('登录请求失败，请检查网络后重试');
                } finally {
                    this.loading = false;
                }
            });
        },
    },
};
</script>

<style scoped>
.login-page {
    min-height: 100vh;
    display: grid;
    grid-template-columns: minmax(380px, 46%) 1fr;
    background: #fbfcfe;
}
.login-story {
    position: relative;
    overflow: hidden;
    background: #202b49;
    color: white;
    padding: 44px 9%;
    display: flex;
    flex-direction: column;
}
.login-story::after {
    content: '';
    position: absolute;
    width: 440px;
    height: 440px;
    right: -260px;
    bottom: -160px;
    border: 1px solid #ffffff10;
    border-radius: 50%;
    box-shadow:
        0 0 0 55px #ffffff03,
        0 0 0 110px #ffffff03;
    pointer-events: none;
}
.login-brand {
    display: flex;
    align-items: center;
    gap: 12px;
}
.login-brand > span {
    background: #8277ed;
    border-radius: 10px;
    width: 38px;
    height: 40px;
    display: grid;
    place-items: center;
    font-size: 23px;
    font-weight: 650;
}
.login-brand strong {
    font-size: 21px;
    letter-spacing: 2px;
}
.login-brand small {
    font-size: 11px;
    color: #9ca9c7;
    padding-left: 12px;
    border-left: 1px solid #59627a;
}
.story-content {
    margin: auto 0;
    padding: 65px 0;
    position: relative;
    z-index: 1;
}
.story-kicker {
    font-size: 12px;
    letter-spacing: 3px;
    color: #b2a9f2;
}
.story-content h1 {
    font-size: clamp(30px, 3vw, 46px);
    line-height: 1.55;
    font-weight: 600;
    letter-spacing: 1px;
    margin: 22px 0;
}
.story-content > p {
    font-size: 14px;
    color: #a5b0c9;
    line-height: 2;
}
.story-features {
    margin-top: 46px;
    display: flex;
    flex-direction: column;
    gap: 24px;
}
.story-features > div {
    display: flex;
    align-items: center;
    gap: 15px;
}
.story-features .el-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
    background: #ffffff08;
    border: 1px solid #ffffff0a;
    color: #b8b1f8;
    border-radius: 10px;
}
.story-features strong {
    display: block;
    font-size: 13px;
    font-weight: 500;
}
.story-features small {
    display: block;
    margin-top: 5px;
    font-size: 11px;
    color: #929fbc;
}
.story-footer {
    font-size: 11px;
    letter-spacing: 1px;
    color: #8592af;
}
.login-form-area {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 70px 32px;
}
.login-card {
    width: 100%;
    max-width: 370px;
}
.form-eyebrow {
    display: inline-block;
    font-size: 12px;
    font-weight: 600;
    color: #6d63cf;
    background: #f0eefb;
    padding: 7px 12px;
    border-radius: 6px;
}
.login-card h2 {
    font-size: 28px;
    margin: 20px 0 12px;
    letter-spacing: -0.6px;
}
.form-description {
    color: #8590a3;
    font-size: 14px;
    margin-bottom: 36px;
}
.login-card :deep(.el-form-item) {
    margin-bottom: 24px;
}
.login-card :deep(.el-form-item__label) {
    color: #4e5a70;
    font-size: 13px;
    margin-bottom: 9px;
}
.login-card :deep(.el-input__wrapper) {
    padding: 3px 13px;
}
.captcha-row {
    display: flex;
    width: 100%;
    gap: 12px;
    align-items: stretch;
}
.captcha-row .el-input {
    flex: 1;
    min-width: 0;
}
.captcha-button {
    padding: 0;
    background: #f3f5fa;
    border: 1px solid #e3e7ef;
    border-radius: 8px;
    overflow: hidden;
    width: 104px;
    flex-shrink: 0;
    cursor: pointer;
    display: grid;
    place-items: center;
}
.captcha-button img {
    display: block;
    max-width: 100%;
    height: 40px;
    object-fit: contain;
}
.captcha-hint {
    font-size: 11px;
    color: #8792a5;
    margin: -8px 0 26px;
}
.login-submit {
    width: 100%;
    height: 46px;
    letter-spacing: 2px;
    box-shadow: 0 5px 14px #4f46e51c;
}
.login-submit .el-icon {
    margin-left: 12px;
}
.login-help {
    border-top: 1px solid #e9edf3;
    margin-top: 30px;
    padding-top: 22px;
    font-size: 11px;
    color: #929bae;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}
.login-footer {
    position: absolute;
    bottom: 26px;
    color: #a1a9b7;
    font-size: 11px;
}
@media (max-width: 1000px) {
    .login-brand small {
        display: none;
    }
    .login-story {
        padding: 36px 30px;
    }
    .login-page {
        grid-template-columns: minmax(300px, 42%) 1fr;
    }
}
@media (max-width: 700px) {
    .login-page {
        display: flex;
        flex-direction: column;
    }
    .login-story {
        padding: 22px 26px;
    }
    .story-content,
    .story-footer {
        display: none;
    }
    .login-form-area {
        flex: 1;
        padding: 42px 26px 80px;
    }
    .login-card h2 {
        font-size: 25px;
    }
}
</style>
