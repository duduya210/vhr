<template>
    <div class="ec-page">
        <div class="page-heading">
            <div>
                <h1>员工奖惩</h1>
                <p>记录奖励与处罚，保留登记和撤销的完整过程。金额暂不计入工资。</p>
            </div>
            <el-button type="primary" @click="openRegister">
                {{ mutation.uncertain ? '确认待处理操作' : '登记奖惩' }}
            </el-button>
        </div>
        <section class="surface-card">
            <el-form inline class="ec-filters" @submit.prevent="search">
                <el-form-item label="员工">
                    <el-input v-model="filters.keyword" placeholder="姓名或工号" maxlength="64" clearable />
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 125px">
                        <el-option label="奖励" :value="0" />
                        <el-option label="处罚" :value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 125px">
                        <el-option label="已生效" value="ACTIVE" />
                        <el-option label="已撤销" value="CANCELLED" />
                    </el-select>
                </el-form-item>
                <el-form-item label="发生日期">
                    <el-date-picker
                        v-model="dateRange"
                        type="daterange"
                        value-format="YYYY-MM-DD"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        style="width: 250px"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button native-type="submit" type="primary">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
            <el-table :data="rows" v-loading="loading" row-key="id">
                <el-table-column label="员工" min-width="140">
                    <template #default="{ row }">
                        <strong>{{ row.employeeName || '历史员工' }}</strong>
                        <div class="ec-muted">{{ row.workId || '—' }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="类型" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.ecType === 0 ? 'success' : 'warning'">{{ typeLabel(row.ecType) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="奖惩规则" min-width="150" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.ruleName || '历史未记录' }}</template>
                </el-table-column>
                <el-table-column label="金额（元）" width="140">
                    <template #default="{ row }">{{ money(row.amount) }}</template>
                </el-table-column>
                <el-table-column prop="ecDate" label="发生日期" width="120" />
                <el-table-column prop="ecReason" label="原因" min-width="180" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                            {{ row.status === 'ACTIVE' ? '已生效' : '已撤销' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="145" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="detail = row">详情</el-button>
                        <el-button
                            v-if="row.status === 'ACTIVE'"
                            link
                            type="danger"
                            :disabled="mutation.saving"
                            @click="openCancel(row)"
                        >
                            撤销
                        </el-button>
                    </template>
                </el-table-column>
                <template #empty><el-empty description="暂无符合条件的奖惩记录" :image-size="80" /></template>
            </el-table>
            <div class="ec-pagination">
                <el-pagination
                    background
                    :current-page="page"
                    :page-size="size"
                    :total="total"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next"
                    @current-change="changePage"
                    @size-change="changeSize"
                />
            </div>
        </section>
        <el-dialog
            v-model="visible"
            :title="mode === 'cancel' ? '撤销奖惩' : '登记员工奖惩'"
            width="640px"
            :show-close="!mutation.saving"
            :close-on-click-modal="false"
            :close-on-press-escape="!mutation.saving"
        >
            <template v-if="mode === 'register'">
                <el-form
                    ref="recordForm"
                    :model="form"
                    :rules="rules"
                    label-position="top"
                    :disabled="mutation.saving || mutation.uncertain"
                >
                    <el-form-item label="在职员工" prop="employeeId">
                        <el-select
                            v-model="form.employeeId"
                            remote
                            filterable
                            clearable
                            :remote-method="searchEmployees"
                            :loading="employeeLoading"
                            placeholder="输入姓名或工号搜索，最多显示 30 条"
                            @change="selectEmployee"
                            style="width: 100%"
                        >
                            <el-option
                                v-for="item in employeeOptions"
                                :key="item.id"
                                :value="item.id"
                                :label="item.name + ' · ' + (item.workId || item.id)"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="启用的奖惩规则" prop="ruleId">
                        <el-select
                            v-model="form.ruleId"
                            remote
                            filterable
                            clearable
                            :remote-method="searchRules"
                            :loading="ruleLoading"
                            placeholder="按名称搜索规则，最多显示 30 条"
                            @change="selectRule"
                            style="width: 100%"
                        >
                            <el-option
                                v-for="item in ruleOptions"
                                :key="item.id"
                                :value="item.id"
                                :label="typeLabel(item.type) + ' · ' + item.name"
                            />
                        </el-select>
                    </el-form-item>
                    <el-alert v-if="lookupError" :title="lookupError" type="error" :closable="false" show-icon />
                    <p class="ec-hint" v-if="!ruleOptions.length && !ruleLoading">
                        尚未找到可用规则，请更换关键词，或请有基础设置权限的管理员新增并启用规则。
                    </p>
                    <div v-if="selectedRule" class="ec-summary">
                        {{ typeLabel(selectedRule.type) }} · 默认 {{ money(selectedRule.amount) }} 元
                        <div class="ec-muted">{{ selectedRule.description || '暂无规则说明' }}</div>
                    </div>
                    <div class="ec-grid">
                        <el-form-item label="发生日期" prop="date">
                            <el-date-picker
                                v-model="form.date"
                                type="date"
                                value-format="YYYY-MM-DD"
                                :disabled-date="disabledDate"
                                style="width: 100%"
                            />
                        </el-form-item>
                        <el-form-item label="实际金额（元）" prop="amount">
                            <el-input-number
                                v-model="form.amount"
                                :precision="2"
                                :min="0"
                                :max="99999999.99"
                                style="width: 100%"
                            />
                        </el-form-item>
                    </div>
                    <el-form-item label="奖惩原因" prop="reason">
                        <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="255" show-word-limit />
                    </el-form-item>
                    <el-form-item label="备注">
                        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
                    </el-form-item>
                    <p class="ec-hint">金额可按实际情况调整，填写非负数。本次登记不自动增加或扣减工资。</p>
                </el-form>
            </template>
            <template v-else>
                <div class="ec-summary">
                    {{ cancelTarget?.employeeName || '历史员工' }} · {{ typeLabel(cancelTarget?.ecType) }} ·
                    {{ money(cancelTarget?.amount) }} 元
                    <br />
                    {{ cancelTarget?.ruleName || '历史规则' }} / {{ cancelTarget?.ecDate }}
                </div>
                <el-form
                    ref="cancelForm"
                    :model="cancelForm"
                    :rules="cancelRules"
                    label-position="top"
                    :disabled="mutation.saving || mutation.uncertain"
                >
                    <el-form-item label="撤销原因" prop="reason">
                        <el-input
                            v-model="cancelForm.reason"
                            type="textarea"
                            :rows="3"
                            maxlength="255"
                            show-word-limit
                        />
                    </el-form-item>
                </el-form>
                <p class="ec-hint">撤销后保留原记录及撤销原因，不支持恢复。需要重新登记时请发起新的奖惩。</p>
            </template>
            <el-alert v-if="mutation.error" :title="mutation.error" type="error" :closable="false" show-icon />
            <template #footer>
                <el-button :disabled="mutation.saving" @click="visible = false">
                    {{ mutation.uncertain ? '稍后核实' : '取消' }}
                </el-button>
                <el-button :type="mode === 'cancel' ? 'danger' : 'primary'" :loading="mutation.saving" @click="save">
                    {{ mutation.uncertain ? '重试确认结果' : mode === 'cancel' ? '确认撤销' : '确认登记' }}
                </el-button>
            </template>
        </el-dialog>
        <el-drawer :model-value="!!detail" title="奖惩详情" size="min(600px, 100vw)" @close="detail = null">
            <el-descriptions v-if="detail" :column="1" border>
                <el-descriptions-item label="员工">
                    {{ detail.employeeName || '历史员工' }}（{{ detail.workId || '—' }}）
                </el-descriptions-item>
                <el-descriptions-item label="登记时部门">
                    {{ detail.departmentName || '历史未记录' }}
                </el-descriptions-item>
                <el-descriptions-item label="类型 / 状态">
                    {{ typeLabel(detail.ecType) }} / {{ detail.status === 'ACTIVE' ? '已生效' : '已撤销' }}
                </el-descriptions-item>
                <el-descriptions-item label="规则快照">
                    {{ detail.ruleName || '历史未记录' }}
                    <div>{{ detail.ruleDescription }}</div>
                </el-descriptions-item>
                <el-descriptions-item label="默认 / 实际金额">
                    {{ money(detail.defaultAmount) }} / {{ money(detail.amount) }} 元
                </el-descriptions-item>
                <el-descriptions-item v-if="detail.ecPoint != null" label="历史奖惩分">
                    {{ detail.ecPoint }}（与金额独立）
                </el-descriptions-item>
                <el-descriptions-item label="发生日期">{{ detail.ecDate }}</el-descriptions-item>
                <el-descriptions-item label="原因">{{ detail.ecReason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
                <el-descriptions-item label="登记人 / 时间">
                    {{ detail.operatorName || '历史未记录' }} / {{ displayTime(detail.createdAt) }}
                </el-descriptions-item>
                <template v-if="detail.status === 'CANCELLED'">
                    <el-descriptions-item label="撤销人 / 时间">
                        {{ detail.cancelledByName }} / {{ displayTime(detail.cancelledAt) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="撤销原因">{{ detail.cancelReason }}</el-descriptions-item>
                </template>
            </el-descriptions>
        </el-drawer>
    </div>
</template>
<script>
import { transferRequest, queryString, displayTime } from '../../utils/transferApi.js';
import { emptyMutation, submitMutation, money, typeLabel } from '../../utils/ecUi.js';
const today = () => {
    const d = new Date();
    return (
        d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    );
};
const blank = () => ({ employeeId: null, ruleId: null, date: today(), amount: 0, reason: '', remark: '' });
export default {
    name: 'PerEc',
    data() {
        return {
            rows: [],
            total: 0,
            page: 1,
            size: 10,
            loading: false,
            error: '',
            version: 0,
            filters: { keyword: '', type: null, status: null },
            applied: {},
            dateRange: [],
            detail: null,
            visible: false,
            mode: 'register',
            form: blank(),
            cancelForm: { reason: '' },
            cancelTarget: null,
            mutation: emptyMutation(),
            employeeOptions: [],
            ruleOptions: [],
            selectedEmployee: null,
            selectedRule: null,
            employeeLoading: false,
            ruleLoading: false,
            employeeVersion: 0,
            ruleVersion: 0,
            lookupError: '',
            rules: {
                employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
                ruleId: [{ required: true, message: '请选择规则', trigger: 'change' }],
                date: [{ required: true, message: '请选择发生日期', trigger: 'change' }],
                amount: [{ required: true, message: '请填写金额', trigger: 'blur' }],
                reason: [{ required: true, whitespace: true, message: '请填写奖惩原因', trigger: 'blur' }],
            },
            cancelRules: { reason: [{ required: true, whitespace: true, message: '请填写撤销原因', trigger: 'blur' }] },
        };
    },
    mounted() {
        this.load();
    },
    methods: {
        money,
        typeLabel,
        displayTime,
        request(path, options) {
            return transferRequest(path, options);
        },
        disabledDate(date) {
            const end = new Date();
            end.setHours(23, 59, 59, 999);
            return date > end || date.getFullYear() < 1900;
        },
        async load() {
            const version = ++this.version;
            this.loading = true;
            this.error = '';
            try {
                const data = await this.request('/personnel/ec/?' + queryString(this.applied, this.page, this.size));
                if (version === this.version) {
                    this.rows = data.data;
                    this.total = data.total;
                }
            } catch (error) {
                if (version === this.version) this.error = error.message || '奖惩记录加载失败';
            } finally {
                if (version === this.version) this.loading = false;
            }
        },
        search() {
            this.applied = { ...this.filters, startDate: this.dateRange?.[0], endDate: this.dateRange?.[1] };
            this.page = 1;
            return this.load();
        },
        reset() {
            this.filters = { keyword: '', type: null, status: null };
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
        openRegister() {
            if (this.mutation.saving) return;
            if (!this.mutation.uncertain) {
                this.mode = 'register';
                this.form = blank();
                this.selectedEmployee = null;
                this.selectedRule = null;
                this.mutation = emptyMutation();
                this.lookupError = '';
                this.searchEmployees('');
                this.searchRules('');
            }
            this.visible = true;
            this.$nextTick(() => this.$refs.recordForm?.clearValidate());
        },
        openCancel(row) {
            if (this.mutation.saving) return;
            if (!this.mutation.uncertain) {
                this.mode = 'cancel';
                this.cancelTarget = row;
                this.cancelForm = { reason: '' };
                this.mutation = emptyMutation();
            }
            this.visible = true;
            this.$nextTick(() => this.$refs.cancelForm?.clearValidate());
        },
        async searchEmployees(keyword) {
            const version = ++this.employeeVersion;
            this.employeeLoading = true;
            try {
                const rows = await this.request('/personnel/ec/employees?keyword=' + encodeURIComponent(keyword));
                if (version === this.employeeVersion) {
                    this.employeeOptions = rows;
                    if (this.selectedEmployee && !rows.some((row) => row.id === this.selectedEmployee.id))
                        this.employeeOptions = [this.selectedEmployee, ...rows];
                }
            } catch (error) {
                if (version === this.employeeVersion) this.$message.error(error.message || '员工查询失败');
            } finally {
                if (version === this.employeeVersion) this.employeeLoading = false;
            }
        },
        async searchRules(keyword) {
            const version = ++this.ruleVersion;
            this.ruleLoading = true;
            this.lookupError = '';
            try {
                const data = await this.request('/personnel/ec/rules?keyword=' + encodeURIComponent(keyword));
                if (version === this.ruleVersion) {
                    this.ruleOptions = data.data;
                    if (this.selectedRule && !data.data.some((row) => row.id === this.selectedRule.id))
                        this.ruleOptions = [this.selectedRule, ...data.data];
                }
            } catch (error) {
                if (version === this.ruleVersion) this.lookupError = error.message || '规则查询失败';
            } finally {
                if (version === this.ruleVersion) this.ruleLoading = false;
            }
        },
        selectEmployee(id) {
            this.selectedEmployee = this.employeeOptions.find((row) => row.id === id) || null;
        },
        selectRule(id) {
            this.selectedRule = this.ruleOptions.find((row) => row.id === id) || null;
            this.form.amount = this.selectedRule ? Number(this.selectedRule.amount) : 0;
        },
        save() {
            return submitMutation(this, async () => {
                if (this.mode === 'cancel') {
                    if (!(await this.$refs.cancelForm.validate().catch(() => false))) return;
                    await this.$confirm('撤销后原记录仍保留，且无法恢复。', '确认撤销奖惩', {
                        type: 'warning',
                        confirmButtonText: '撤销',
                        cancelButtonText: '取消',
                    });
                    return {
                        path: '/personnel/ec/' + this.cancelTarget.id + '/cancel',
                        payload: { reason: this.cancelForm.reason },
                    };
                }
                if (
                    !(await this.$refs.recordForm.validate().catch(() => false)) ||
                    !this.selectedEmployee ||
                    !this.selectedRule
                )
                    return;
                await this.$confirm(
                    '为 ' +
                        this.selectedEmployee.name +
                        ' 登记' +
                        typeLabel(this.selectedRule.type) +
                        ' ' +
                        money(this.form.amount) +
                        ' 元，本次不计入工资。',
                    '确认登记奖惩',
                    { type: 'warning', confirmButtonText: '登记', cancelButtonText: '取消' },
                );
                return {
                    path: '/personnel/ec/',
                    payload: { ...this.form, expectedRuleVersion: this.selectedRule.version },
                };
            });
        },
        async onSaved() {
            this.visible = false;
            this.detail = null;
            this.page = 1;
            await this.load();
        },
    },
};
</script>
<style src="../../styles/ec.css"></style>
