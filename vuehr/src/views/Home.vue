<template>
    <div class="app-shell" :class="{ 'is-collapsed': collapsed }">
        <aside class="app-sidebar" aria-label="主导航">
            <router-link to="/home" class="brand" aria-label="微人事工作台">
                <span class="brand-mark">微</span>
                <span v-if="!collapsed" class="brand-copy"><strong>微人事</strong><small>人力资源管理平台</small></span>
            </router-link>
            <div v-if="!collapsed" class="nav-caption">工作空间</div>
            <el-menu
                router
                :default-active="$route.path"
                :collapse="collapsed"
                :collapse-transition="false"
                unique-opened
                class="sidebar-menu"
            >
                <el-menu-item index="/home"
                    ><el-icon><House /></el-icon><template #title>工作台</template></el-menu-item
                >
                <el-sub-menu v-for="(item, index) in visibleRoutes" :key="item.path + index" :index="'group-' + index">
                    <template #title
                        ><el-icon><component :is="menuIcon(item)" /></el-icon><span>{{ item.name }}</span></template
                    >
                    <el-menu-item
                        v-for="child in (item.children || []).filter((child) => !child.hidden)"
                        :key="child.path"
                        :index="child.path"
                        >{{ child.name }}</el-menu-item
                    >
                </el-sub-menu>
            </el-menu>
            <div v-if="!collapsed" class="sidebar-footer"><span class="workspace-dot"></span>微人事 · 管理工作空间</div>
        </aside>
        <div class="app-body">
            <header class="app-topbar">
                <div class="topbar-location">
                    <el-button
                        text
                        circle
                        :aria-label="collapsed ? '展开导航' : '收起导航'"
                        :aria-expanded="!collapsed"
                        @click="collapsed = !collapsed"
                        ><el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon
                    ></el-button>
                    <el-breadcrumb
                        ><el-breadcrumb-item :to="{ path: '/home' }">工作台</el-breadcrumb-item
                        ><el-breadcrumb-item v-if="$route.path !== '/home'">{{
                            $route.meta.menuName || $route.name
                        }}</el-breadcrumb-item></el-breadcrumb
                    >
                </div>
                <div class="topbar-account">
                    <el-button text circle aria-label="打开在线聊天" title="在线聊天" @click="$router.push('/chat')"
                        ><el-icon><ChatDotRound /></el-icon
                    ></el-button>
                    <span class="topbar-divider"></span>
                    <el-dropdown @command="commandHandler">
                        <button class="account-button" aria-label="打开个人菜单">
                            <el-avatar :size="34" :src="user.userface">{{
                                (user.name || '用户').slice(0, 1)
                            }}</el-avatar>
                            <span>{{ user.name || '用户' }}</span
                            ><el-icon><ArrowDown /></el-icon>
                        </button>
                        <template #dropdown
                            ><el-dropdown-menu
                                ><el-dropdown-item command="userinfo">个人中心</el-dropdown-item
                                ><el-dropdown-item command="logout" divided
                                    >退出登录</el-dropdown-item
                                ></el-dropdown-menu
                            ></template
                        >
                    </el-dropdown>
                </div>
            </header>
            <main class="app-content">
                <template v-if="$route.path === '/home'">
                    <div class="page-heading">
                        <div>
                            <h1>工作台</h1>
                            <p>从这里开始，让日常人事工作更有条理。</p>
                        </div>
                        <span class="today-label">{{ today }}</span>
                    </div>
                    <section class="welcome-panel">
                        <div class="welcome-copy">
                            <span class="eyebrow">你好，{{ user.name || '用户' }}</span>
                            <h2>专注于人，让管理更从容。</h2>
                            <p>员工档案、组织信息与薪资事务，<br />在一个工作空间里井然有序。</p>
                            <el-button type="primary" @click="openFirstModule"
                                >{{ shortcuts.length ? '进入' + shortcuts[0].name : '查看个人资料'
                                }}<el-icon class="button-arrow"><ArrowRight /></el-icon
                            ></el-button>
                        </div>
                        <div class="welcome-art" aria-hidden="true">
                            <span class="art-orbit"></span>
                            <div class="art-card">
                                <span class="art-avatar"
                                    ><el-icon><UserFilled /></el-icon></span
                                ><span class="art-line"></span><span class="art-line short"></span>
                                <div class="art-members"><span></span><span></span><span></span></div>
                                <span class="art-caption">连接团队 · 有序协作</span>
                            </div>
                            <span class="art-check"
                                ><el-icon><Check /></el-icon
                            ></span>
                        </div>
                    </section>
                    <section class="shortcut-section">
                        <div class="section-heading">
                            <h2>快捷入口</h2>
                            <span>你的常用工作，从这里展开</span>
                        </div>
                        <div class="shortcut-grid">
                            <router-link
                                v-for="item in shortcuts"
                                :key="item.path"
                                :to="item.path"
                                class="shortcut-card surface-card"
                                ><span class="shortcut-icon"
                                    ><el-icon><component :is="menuIcon(item)" /></el-icon
                                ></span>
                                <div>
                                    <h3>{{ item.name }}</h3>
                                    <p>{{ item.groupName }}</p>
                                </div>
                                <el-icon class="shortcut-arrow"><ArrowRight /></el-icon
                            ></router-link>
                        </div>
                        <el-empty
                            v-if="!shortcuts.length"
                            description="暂无已上线且已授权的功能，请联系管理员"
                            :image-size="80"
                        />
                    </section>
                </template>
                <router-view v-else />
            </main>
        </div>
    </div>
</template>

<script>
import { getReadyMenuGroups } from '../utils/featureAvailability.js';

export default {
    name: 'Home',
    data() {
        return {
            collapsed: false,
            viewport: null,
            today: new Intl.DateTimeFormat('zh-CN', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                weekday: 'long',
            }).format(new Date()),
        };
    },
    computed: {
        visibleRoutes() {
            return getReadyMenuGroups(this.$store.state.routes);
        },
        user() {
            return this.$store.state.currentHr || {};
        },
        shortcuts() {
            return this.visibleRoutes
                .flatMap((item) =>
                    (item.children || [])
                        .filter((child) => !child.hidden)
                        .map((child) => ({ ...child, groupName: item.name })),
                )
                .slice(0, 6);
        },
    },
    mounted() {
        this.viewport = window.matchMedia('(max-width: 900px)');
        this.updateNavigation(this.viewport);
        this.viewport.addEventListener('change', this.updateNavigation);
        this.getRequest('/hr/info').then((resp) => {
            if (resp) {
                window.sessionStorage.setItem('user', JSON.stringify(resp));
                this.$store.commit('INIT_CURRENTHR', resp);
            }
        });
    },
    beforeUnmount() {
        this.viewport?.removeEventListener('change', this.updateNavigation);
    },
    methods: {
        updateNavigation(event) {
            this.collapsed = event.matches;
        },
        menuIcon(item) {
            const path = item.path || '';
            return path.includes('emp')
                ? 'User'
                : path.includes('sal')
                  ? 'Wallet'
                  : path.includes('per')
                    ? 'Briefcase'
                    : path.includes('sta')
                      ? 'DataAnalysis'
                      : 'Setting';
        },
        openFirstModule() {
            this.$router.push(this.shortcuts[0]?.path || '/hrinfo');
        },
        commandHandler(cmd) {
            if (cmd === 'userinfo') {
                this.$router.push('/hrinfo');
                return;
            }
            if (cmd === 'logout') {
                this.$confirm('确定退出当前账号吗？', '退出登录', {
                    confirmButtonText: '退出',
                    cancelButtonText: '取消',
                    type: 'warning',
                })
                    .then(() => {
                        this.getRequest('/logout');
                        window.sessionStorage.removeItem('user');
                        this.$store.commit('initRoutes', []);
                        this.$router.replace('/');
                    })
                    .catch(() => {});
            }
        },
    },
};
</script>

<style scoped>
.app-shell {
    --sidebar-width: 224px;
    min-height: 100vh;
}
.app-shell.is-collapsed {
    --sidebar-width: 72px;
}
.app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 20;
    width: var(--sidebar-width);
    background: #192338;
    display: flex;
    flex-direction: column;
    color: #fff;
}
.brand {
    display: flex;
    align-items: center;
    gap: 12px;
    height: 84px;
    padding: 0 20px;
    text-decoration: none;
    flex-shrink: 0;
}
.brand-mark {
    display: grid;
    place-items: center;
    width: 34px;
    height: 36px;
    border-radius: 10px;
    background: #7066f1;
    color: white;
    font-weight: 700;
    font-size: 20px;
    flex-shrink: 0;
}
.brand-copy strong {
    display: block;
    font-size: 19px;
    letter-spacing: 2px;
}
.brand-copy small {
    display: block;
    font-size: 10px;
    color: #99a7c0;
    margin-top: 4px;
    letter-spacing: 1px;
}
.nav-caption {
    padding: 20px 24px 12px;
    color: #8492ad;
    font-size: 11px;
    letter-spacing: 2px;
}
.sidebar-menu {
    flex: 1;
    overflow-y: auto;
    border-right: 0;
    width: 100%;
    --el-menu-bg-color: #192338;
    --el-menu-text-color: #b6c1d5;
    --el-menu-hover-bg-color: #26324a;
    --el-menu-active-color: #fff;
}
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
    margin: 4px 10px;
    border-radius: 8px;
    height: 46px;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
    background: #4f46e5;
}
.sidebar-menu :deep(.el-sub-menu .el-menu) {
    background: #151e30;
}
.sidebar-menu :deep(.el-icon) {
    font-size: 18px;
}
.sidebar-footer {
    color: #8d9bb4;
    padding: 22px;
    font-size: 11px;
    white-space: nowrap;
}
.workspace-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    background: #8d86f4;
    border-radius: 50%;
    margin-right: 8px;
}
.app-body {
    margin-left: var(--sidebar-width);
    min-width: 0;
}
.app-topbar {
    height: 68px;
    background: white;
    border-bottom: 1px solid #e9edf3;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 28px;
    gap: 12px;
}
.topbar-location,
.topbar-account {
    display: flex;
    align-items: center;
    gap: 16px;
}
.topbar-divider {
    height: 24px;
    border-left: 1px solid #e7ebf2;
}
.account-button {
    display: flex;
    align-items: center;
    gap: 10px;
    border: 0;
    background: none;
    cursor: pointer;
    color: #39465d;
    padding: 3px;
    font-size: 13px;
}
.account-button .el-avatar {
    background: #eae8fd;
    color: #4f46e5;
}
.app-content {
    padding: 30px;
    max-width: 1800px;
    margin: 0 auto;
}
.today-label {
    font-size: 13px;
    color: #7a869b;
}
.welcome-panel {
    background: #eeedfc;
    border: 1px solid #e3e1fa;
    border-radius: 14px;
    padding: 38px 42px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    overflow: hidden;
}
.eyebrow {
    font-size: 13px;
    color: #655cc8;
}
.welcome-copy h2 {
    margin: 14px 0;
    font-size: 29px;
    font-weight: 650;
    letter-spacing: -1px;
}
.welcome-copy p {
    color: #7b7a96;
    font-size: 14px;
    line-height: 1.9;
    margin-bottom: 24px;
}
.button-arrow {
    margin-left: 12px;
}
.welcome-art {
    position: relative;
    width: 260px;
    height: 220px;
    display: grid;
    place-items: center;
    flex-shrink: 0;
}
.art-orbit {
    position: absolute;
    width: 210px;
    height: 210px;
    border: 1px solid #d6d2f5;
    border-radius: 50%;
    box-shadow: 0 0 0 22px #e4e1f850;
}
.art-card {
    transform: rotate(-6deg);
    width: 160px;
    padding: 22px;
    background: #ffffffeb;
    border-radius: 14px;
    box-shadow: 0 18px 40px #5147a91a;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
}
.art-avatar {
    background: #eae7fc;
    color: #7a6cdb;
    border-radius: 50%;
    width: 44px;
    height: 44px;
    display: grid;
    place-items: center;
    font-size: 24px;
}
.art-line {
    background: #e6e9f3;
    width: 80px;
    height: 6px;
    border-radius: 4px;
}
.art-line.short {
    width: 50px;
}
.art-members {
    display: flex;
    margin-top: 4px;
}
.art-members span {
    border: 3px solid white;
    width: 27px;
    height: 27px;
    border-radius: 50%;
    background: #b6b0ee;
    margin-left: -5px;
}
.art-members span:nth-child(2) {
    background: #b2d5d6;
}
.art-members span:nth-child(3) {
    background: #efccb4;
}
.art-caption {
    font-size: 9px;
    color: #9a97af;
}
.art-check {
    position: absolute;
    bottom: 28px;
    right: 32px;
    background: #4f46e5;
    border: 5px solid #eeedfc;
    color: white;
    width: 48px;
    height: 48px;
    border-radius: 16px;
    display: grid;
    place-items: center;
    font-size: 23px;
}
.shortcut-section {
    margin-top: 32px;
}
.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    gap: 12px;
}
.section-heading h2 {
    font-size: 17px;
    margin: 0;
}
.section-heading span {
    font-size: 12px;
    color: #8b95a6;
}
.shortcut-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
}
.shortcut-card {
    display: flex;
    gap: 16px;
    align-items: center;
    text-decoration: none;
    padding: 24px 20px;
    transition:
        border-color 0.2s,
        transform 0.2s;
}
.shortcut-card:hover {
    border-color: #aaa4ed;
    transform: translateY(-2px);
}
.shortcut-icon {
    width: 44px;
    height: 44px;
    display: grid;
    place-items: center;
    flex-shrink: 0;
    border-radius: 12px;
    background: #f0eefc;
    color: #6b61cd;
    font-size: 22px;
}
.shortcut-card h3 {
    font-size: 14px;
    font-weight: 600;
    margin: 0 0 7px;
}
.shortcut-card p {
    font-size: 12px;
    color: #929caf;
    margin: 0;
}
.shortcut-arrow {
    margin-left: auto;
    color: #adb4c2;
}
@media (max-width: 1100px) {
    .shortcut-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .welcome-panel {
        padding: 30px;
    }
    .welcome-copy h2 {
        font-size: 25px;
    }
}
@media (max-width: 700px) {
    .app-content {
        padding: 20px 14px;
    }
    .app-topbar {
        padding: 0 12px;
    }
    .topbar-location,
    .topbar-account {
        gap: 5px;
    }
    .account-button > span:not(.el-avatar),
    .topbar-divider {
        display: none;
    }
    .welcome-art {
        display: none;
    }
    .welcome-panel {
        padding: 26px 22px;
    }
    .welcome-copy h2 {
        font-size: 23px;
    }
    .shortcut-grid {
        grid-template-columns: 1fr;
    }
    .section-heading span {
        display: none;
    }
}
</style>
