<template>
    <section class="surface-card unavailable-feature">
        <el-empty :description="`${$route.meta.menuName || '该功能'}暂未上线`" :image-size="120">
            <p>该功能尚未开放使用，请先使用已上线的功能。</p>
            <el-button type="primary" @click="$router.push('/home')">返回工作台</el-button>
        </el-empty>
    </section>
</template>

<style scoped>
.unavailable-feature {
    padding: 36px 20px;
}
.unavailable-feature p {
    color: #8390a6;
    font-size: 14px;
    margin: 0 0 24px;
}
</style>
