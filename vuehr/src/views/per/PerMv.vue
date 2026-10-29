<template>
    <div class="transfer-page">
        <div class="page-heading">
            <div>
                <h1>员工调动</h1>
                <p>记录每一次岗位变化，保持员工档案与组织信息一致。</p>
            </div>
            <el-button type="primary" @click="openTransfer"
                ><el-icon><Plus /></el-icon>发起调动</el-button
            >
        </div>
        <section class="surface-card">
            <el-form class="transfer-filters" inline @submit.prevent="search">
                <el-form-item label="员工"
                    ><el-input v-model="filters.keyword" placeholder="姓名或工号" clearable maxlength="64"
                /></el-form-item>
                <el-form-item label="部门"
                    ><el-select
                        v-model="filters.departmentId"
                        placeholder="原部门或目标部门"
                        clearable
                        style="width: 190px"
                        ><el-option
                            v-for="item in departments"
                            :key="item.id"
                            :label="item.name"
                            :value="item.id" /></el-select
                ></el-form-item>
                <el-form-item label="调动时间"
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
                    ><el-button @click="resetFilters">重置</el-button></el-form-item
                >
            </el-form>
            <el-alert v-if="listError" :title="listError" type="error" show-icon :closable="false" />
            <el-table :data="records" v-loading="loading" row-key="id">
                <el-table-column label="员工" width="150"
                    ><template #default="{ row }"
                        ><strong>{{ row.employeeName || '历史员工' }}</strong>
                        <div class="muted">{{ row.workId || '—' }}</div></template
                    ></el-table-column
                >
                <el-table-column label="调动前" min-width="190"
                    ><template #default="{ row }"
                        >{{ assignment(row.beforeDepName, row.beforeDepId) }}
                        <div class="muted">{{ assignment(row.beforeJobName, row.beforeJobId) }}</div></template
                    ></el-table-column
                >
                <el-table-column label="调动后" min-width="190"
                    ><template #default="{ row }"
                        >{{ assignment(row.afterDepName, row.afterDepId) }}
                        <div class="muted">{{ assignment(row.afterJobName, row.afterJobId) }}</div></template
                    ></el-table-column
                >
                <el-table-column prop="reason" label="调动原因" min-width="160" show-overflow-tooltip />
                <el-table-column label="操作人" width="120"
                    ><template #default="{ row }">{{ row.operatorName || '历史未记录' }}</template></el-table-column
                >
                <el-table-column label="生效时间" width="180"
                    ><template #default="{ row }">{{
                        displayTime(row.createdAt || row.removeDate)
                    }}</template></el-table-column
                >
                <el-table-column label="操作" width="75" fixed="right"
                    ><template #default="{ row }"
                        ><el-button link type="primary" @click="selectedRecord = row">详情</el-button></template
                    ></el-table-column
                >
                <template #empty><el-empty description="暂无符合条件的调动记录" :image-size="80" /></template>
            </el-table>
            <div class="transfer-pagination">
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
        <el-dialog
            v-model="dialogVisible"
            title="发起员工调动"
            width="660px"
            :show-close="!saving"
            :close-on-click-modal="false"
            :close-on-press-escape="!saving"
        >
            <el-alert
                title="确认后立即生效，员工档案、调动记录和操作日志将同时保存。"
                type="info"
                :closable="false"
                show-icon
            />
            <el-form
                ref="transferForm"
                :model="form"
                :rules="rules"
                label-position="top"
                class="transfer-form"
                :disabled="saving || uncertain"
            >
                <el-form-item label="选择在职员工" prop="employeeId"
                    ><el-select
                        v-model="form.employeeId"
                        filterable
                        remote
                        :remote-method="searchEmployees"
                        :loading="employeeLoading"
                        placeholder="输入姓名或工号搜索，最多显示 30 条"
                        @change="selectEmployee"
                        style="width: 100%"
                        ><el-option
                            v-for="item in employeeOptions"
                            :key="item.id"
                            :value="item.id"
                            :label="`${item.name} · ${item.workId || item.id}`" /></el-select
                ></el-form-item>
                <div v-if="selectedEmployee" class="current-assignment">
                    当前部门：{{ selectedEmployee.departmentName || '未设置' }}<br />当前职位：{{
                        selectedEmployee.positionName || '未设置'
                    }}
                </div>
                <div class="transfer-grid">
                    <el-form-item label="目标部门" prop="departmentId"
                        ><el-select v-model="form.departmentId" filterable placeholder="请选择部门"
                            ><el-option
                                v-for="item in departments"
                                :key="item.id"
                                :value="item.id"
                                :label="item.name"
                                :disabled="!item.enabled" /></el-select></el-form-item
                    ><el-form-item label="目标职位" prop="positionId"
                        ><el-select v-model="form.positionId" filterable placeholder="请选择职位"
                            ><el-option
                                v-for="item in positions"
                                :key="item.id"
                                :value="item.id"
                                :label="item.name"
                                :disabled="!item.enabled" /></el-select
                    ></el-form-item>
                </div>
                <el-form-item label="调动原因" prop="reason"
                    ><el-input
                        v-model="form.reason"
                        type="textarea"
                        :rows="3"
                        maxlength="255"
                        show-word-limit
                        placeholder="请填写调动原因"
                /></el-form-item>
                <el-form-item label="备注"
                    ><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit
                /></el-form-item>
            </el-form>
            <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon />
            <template #footer
                ><el-button :disabled="saving" @click="dialogVisible = false">{{
                    uncertain ? '稍后确认' : '取消'
                }}</el-button
                ><el-button type="primary" :loading="saving" :disabled="optionsLoading" @click="submitTransfer">{{
                    uncertain ? '重试确认结果' : '确认并立即生效'
                }}</el-button></template
            >
        </el-dialog>
        <el-drawer
            :model-value="!!selectedRecord"
            title="调动详情"
            size="min(580px, 100vw)"
            @close="selectedRecord = null"
        >
            <el-descriptions v-if="selectedRecord" :column="1" border>
                <el-descriptions-item label="员工"
                    >{{ selectedRecord.employeeName }}（{{ selectedRecord.workId || '—' }}）</el-descriptions-item
                >
                <el-descriptions-item label="原部门 / 职位"
                    >{{ assignment(selectedRecord.beforeDepName, selectedRecord.beforeDepId) }} /
                    {{ assignment(selectedRecord.beforeJobName, selectedRecord.beforeJobId) }}</el-descriptions-item
                >
                <el-descriptions-item label="目标部门 / 职位"
                    >{{ assignment(selectedRecord.afterDepName, selectedRecord.afterDepId) }} /
                    {{ assignment(selectedRecord.afterJobName, selectedRecord.afterJobId) }}</el-descriptions-item
                >
                <el-descriptions-item label="原因">{{ selectedRecord.reason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ selectedRecord.remark || '—' }}</el-descriptions-item>
                <el-descriptions-item label="操作人">{{
                    selectedRecord.operatorName || '历史未记录'
                }}</el-descriptions-item>
                <el-descriptions-item label="生效时间">{{
                    displayTime(selectedRecord.createdAt || selectedRecord.removeDate)
                }}</el-descriptions-item>
            </el-descriptions>
        </el-drawer>
    </div>
</template>

<script>
import { transferRequest, queryString, displayTime, assignment } from '../../utils/transferApi.js';
const blankForm = () => ({ employeeId: null, departmentId: null, positionId: null, reason: '', remark: '' });
export default {
    name: 'PerMv',
    data() {
        return {
            records: [],
            total: 0,
            page: 1,
            size: 10,
            loading: false,
            listError: '',
            filters: { keyword: '', departmentId: null },
            appliedFilters: {},
            dateRange: [],
            departments: [],
            positions: [],
            optionsLoading: false,
            dialogVisible: false,
            saving: false,
            uncertain: false,
            submitError: '',
            pendingCommand: null,
            form: blankForm(),
            employeeOptions: [],
            selectedEmployee: null,
            selectedRecord: null,
            employeeLoading: false,
            employeeSearchVersion: 0,
            listVersion: 0,
            rules: {
                employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
                departmentId: [{ required: true, message: '请选择目标部门', trigger: 'change' }],
                positionId: [{ required: true, message: '请选择目标职位', trigger: 'change' }],
                reason: [{ required: true, whitespace: true, message: '请填写调动原因', trigger: 'blur' }],
            },
        };
    },
    mounted() {
        this.loadRecords();
        this.loadOptions();
    },
    methods: {
        displayTime,
        assignment,
        request(path, options) {
            return transferRequest(path, options);
        },
        async loadOptions() {
            this.optionsLoading = true;
            try {
                const data = await this.request('/personnel/remove/options');
                this.departments = data.departments;
                this.positions = data.positions;
            } catch (error) {
                this.$message.error(error.message || '部门和职位加载失败');
            } finally {
                this.optionsLoading = false;
            }
        },
        async loadRecords() {
            const version = ++this.listVersion;
            this.loading = true;
            this.listError = '';
            try {
                const data = await this.request(
                    '/personnel/remove/?' + queryString(this.appliedFilters, this.page, this.size),
                );
                if (version === this.listVersion) {
                    this.records = data.data;
                    this.total = data.total;
                }
            } catch (error) {
                if (version === this.listVersion) this.listError = error.message || '调动记录加载失败，请重试';
            } finally {
                if (version === this.listVersion) this.loading = false;
            }
        },
        search() {
            this.appliedFilters = { ...this.filters, startDate: this.dateRange?.[0], endDate: this.dateRange?.[1] };
            this.page = 1;
            return this.loadRecords();
        },
        resetFilters() {
            this.filters = { keyword: '', departmentId: null };
            this.dateRange = [];
            return this.search();
        },
        changePage(page) {
            this.page = page;
            return this.loadRecords();
        },
        changeSize(size) {
            this.size = size;
            this.page = 1;
            return this.loadRecords();
        },
        openTransfer() {
            if (!this.uncertain) {
                this.form = blankForm();
                this.selectedEmployee = null;
                this.pendingCommand = null;
                this.submitError = '';
            }
            this.dialogVisible = true;
            this.loadOptions();
            this.searchEmployees('');
            this.$nextTick(() => this.$refs.transferForm?.clearValidate());
        },
        async searchEmployees(keyword) {
            const version = ++this.employeeSearchVersion;
            this.employeeLoading = true;
            try {
                const rows = await this.request('/personnel/remove/employees?keyword=' + encodeURIComponent(keyword));
                if (version === this.employeeSearchVersion) {
                    this.employeeOptions = rows;
                    if (this.selectedEmployee && !rows.some((row) => row.id === this.selectedEmployee.id))
                        this.employeeOptions = [this.selectedEmployee, ...rows];
                }
            } catch (error) {
                if (version === this.employeeSearchVersion) this.$message.error(error.message || '员工搜索失败');
            } finally {
                if (version === this.employeeSearchVersion) this.employeeLoading = false;
            }
        },
        selectEmployee(id) {
            this.selectedEmployee = this.employeeOptions.find((employee) => employee.id === id) || null;
            this.form.departmentId = this.selectedEmployee?.departmentId ?? null;
            this.form.positionId = this.selectedEmployee?.positionId ?? null;
        },
        async submitTransfer() {
            if (this.saving) return;
            this.saving = true;
            try {
                if (!this.uncertain) {
                    const valid = await this.$refs.transferForm.validate().catch(() => false);
                    if (!valid || !this.selectedEmployee) return;
                    if (
                        this.form.departmentId === this.selectedEmployee.departmentId &&
                        this.form.positionId === this.selectedEmployee.positionId
                    ) {
                        this.submitError = '目标部门和职位与当前相同，无需调动';
                        return;
                    }
                    const department = this.departments.find((item) => item.id === this.form.departmentId);
                    const position = this.positions.find((item) => item.id === this.form.positionId);
                    if (!department?.enabled || !position?.enabled) {
                        this.submitError = '请选择有效的目标部门和职位';
                        return;
                    }
                    await this.$confirm(
                        `将 ${this.selectedEmployee.name} 调动至 ${department.name} / ${position.name}，确认后立即生效。`,
                        '确认员工调动',
                        { type: 'warning', confirmButtonText: '确认调动', cancelButtonText: '取消' },
                    );
                    this.pendingCommand = {
                        ...this.form,
                        reason: this.form.reason.trim(),
                        remark: this.form.remark.trim(),
                        expectedDepartmentId: this.selectedEmployee.departmentId,
                        expectedPositionId: this.selectedEmployee.positionId,
                        requestId: crypto.randomUUID(),
                    };
                }
                this.submitError = '';
                const response = await this.request('/personnel/remove/', {
                    method: 'POST',
                    body: JSON.stringify(this.pendingCommand),
                });
                if (response?.status !== 200) throw new Error('服务器未确认调动结果');
                this.uncertain = false;
                this.pendingCommand = null;
                this.dialogVisible = false;
                this.$message.success(response.msg || '调动成功');
                this.page = 1;
                await this.loadRecords();
            } catch (error) {
                if (error === 'cancel' || error === 'close') return;
                this.uncertain = !!this.pendingCommand && (!error.status || error.status >= 500);
                this.submitError = this.uncertain
                    ? '结果尚未确认，请点击“重试确认结果”。系统会使用原请求核实，避免重复调动。'
                    : error.message || '提交失败，请重试';
                if (!this.uncertain) this.pendingCommand = null;
            } finally {
                this.saving = false;
            }
        },
    },
};
</script>

<style scoped>
.transfer-filters {
    padding: 24px 24px 0;
}
.transfer-pagination {
    padding: 22px;
    overflow-x: auto;
    display: flex;
    justify-content: flex-end;
}
.muted {
    color: #8792a5;
    font-size: 12px;
    margin-top: 5px;
}
.transfer-form {
    margin-top: 24px;
}
.transfer-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}
.current-assignment {
    background: #f5f6fc;
    color: #69768c;
    font-size: 13px;
    line-height: 2;
    padding: 14px 18px;
    border-radius: 8px;
    margin-bottom: 24px;
}
.transfer-page :deep(.el-descriptions__content) {
    overflow-wrap: anywhere;
    white-space: pre-wrap;
}
@media (max-width: 700px) {
    .transfer-grid {
        grid-template-columns: 1fr;
        gap: 0;
    }
    .transfer-filters {
        padding: 16px 16px 0;
    }
    .transfer-pagination {
        justify-content: flex-start;
    }
}
</style>
