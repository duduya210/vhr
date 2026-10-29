<template>
    <div class="log-page">
        <div class="page-heading">
            <div>
                <h1>操作日志</h1>
                <p>追溯已完成的员工调动。日志为只读记录，不支持修改或删除。</p>
            </div>
        </div>
        <section class="surface-card">
            <el-form inline class="log-filters" @submit.prevent="search">
                <el-form-item label="关键词"
                    ><el-input
                        v-model="filters.keyword"
                        placeholder="员工姓名、工号或操作内容"
                        maxlength="64"
                        clearable
                /></el-form-item>
                <el-form-item label="操作人"
                    ><el-input v-model="filters.operator" placeholder="操作人姓名" maxlength="64" clearable
                /></el-form-item>
                <el-form-item label="时间"
                    ><el-date-picker
                        v-model="dateRange"
                        type="daterange"
                        value-format="YYYY-MM-DD"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        style="width: 250px"
                /></el-form-item>
                <el-form-item
                    ><el-button type="primary" native-type="submit">查询</el-button
                    ><el-button @click="reset">重置</el-button></el-form-item
                >
            </el-form>
            <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
            <el-table :data="rows" v-loading="loading" row-key="id">
                <el-table-column prop="id" label="编号" width="90" />
                <el-table-column label="操作时间" width="190"
                    ><template #default="{ row }">{{ displayTime(row.createdAt) }}</template></el-table-column
                >
                <el-table-column label="操作人" width="130"
                    ><template #default="{ row }">{{ row.operatorName || '历史未记录' }}</template></el-table-column
                >
                <el-table-column prop="operate" label="操作内容" min-width="280" show-overflow-tooltip />
                <el-table-column label="操作" width="90"
                    ><template #default="{ row }"
                        ><el-button link type="primary" @click="detail = row">详情</el-button></template
                    ></el-table-column
                >
                <template #empty><el-empty description="暂无符合条件的操作日志" :image-size="80" /></template>
            </el-table>
            <div class="log-pagination">
                <el-pagination
                    background
                    :current-page="page"
                    :page-size="size"
                    :page-sizes="[10, 20, 50]"
                    :total="total"
                    layout="total, sizes, prev, pager, next"
                    @current-change="changePage"
                    @size-change="changeSize"
                />
            </div>
        </section>
        <el-drawer :model-value="!!detail" title="操作日志详情" size="min(580px, 100vw)" @close="detail = null">
            <el-descriptions v-if="detail" :column="1" border>
                <el-descriptions-item label="操作人">{{ detail.operatorName || '历史未记录' }}</el-descriptions-item>
                <el-descriptions-item label="时间">{{ displayTime(detail.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="操作内容">{{ detail.operate }}</el-descriptions-item>
                <template v-if="detail.transferId">
                    <el-descriptions-item label="调动记录">#{{ detail.transferId }}</el-descriptions-item>
                    <el-descriptions-item label="员工"
                        >{{ detail.employeeName }}（{{ detail.workId || '—' }}）</el-descriptions-item
                    >
                    <el-descriptions-item label="调动前"
                        >{{ detail.beforeDepName || '未记录' }} /
                        {{ detail.beforeJobName || '未记录' }}</el-descriptions-item
                    >
                    <el-descriptions-item label="调动后"
                        >{{ detail.afterDepName || '未记录' }} /
                        {{ detail.afterJobName || '未记录' }}</el-descriptions-item
                    >
                    <el-descriptions-item label="原因">{{ detail.reason }}</el-descriptions-item>
                    <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
                </template>
            </el-descriptions>
        </el-drawer>
    </div>
</template>

<script>
import { transferRequest, queryString, displayTime } from '../../utils/transferApi.js';
export default {
    name: 'SysLog',
    data() {
        return {
            rows: [],
            total: 0,
            page: 1,
            size: 10,
            loading: false,
            error: '',
            filters: { keyword: '', operator: '' },
            dateRange: [],
            appliedFilters: {},
            detail: null,
            version: 0,
        };
    },
    mounted() {
        this.load();
    },
    methods: {
        displayTime,
        request(path) {
            return transferRequest(path);
        },
        async load() {
            const version = ++this.version;
            this.loading = true;
            this.error = '';
            try {
                const data = await this.request(
                    '/system/log/?' + queryString(this.appliedFilters, this.page, this.size),
                );
                if (version === this.version) {
                    this.rows = data.data;
                    this.total = data.total;
                }
            } catch (error) {
                if (version === this.version) this.error = error.message || '操作日志加载失败，请重试';
            } finally {
                if (version === this.version) this.loading = false;
            }
        },
        search() {
            this.appliedFilters = { ...this.filters, startDate: this.dateRange?.[0], endDate: this.dateRange?.[1] };
            this.page = 1;
            return this.load();
        },
        reset() {
            this.filters = { keyword: '', operator: '' };
            this.dateRange = [];
            return this.search();
        },
        changePage(page) {
            this.page = page;
            return this.load();
        },
        changeSize(size) {
            this.size = size;
            this.page = 1;
            return this.load();
        },
    },
};
</script>

<style scoped>
.log-filters {
    padding: 24px 24px 0;
}
.log-pagination {
    display: flex;
    justify-content: flex-end;
    padding: 22px;
    overflow-x: auto;
}
.log-page :deep(.el-descriptions__content) {
    overflow-wrap: anywhere;
    white-space: pre-wrap;
}
@media (max-width: 700px) {
    .log-filters {
        padding: 16px 16px 0;
    }
    .log-pagination {
        justify-content: flex-start;
    }
}
</style>
