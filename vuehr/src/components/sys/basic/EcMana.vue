<template>
    <div class="ec-page">
        <div class="page-heading">
            <div>
                <h1>奖惩规则</h1>
                <p>维护奖励与处罚项目。规则变更不影响已登记的历史记录。</p>
            </div>
            <el-button type="primary" @click="openEditor()">
                {{ mutation.uncertain ? '确认待处理操作' : '新增规则' }}
            </el-button>
        </div>
        <section class="surface-card">
            <el-form inline class="ec-filters" @submit.prevent="search">
                <el-form-item label="名称">
                    <el-input v-model="filters.keyword" maxlength="64" clearable placeholder="搜索规则名称" />
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="filters.type" clearable placeholder="全部类型" style="width: 130px">
                        <el-option label="奖励" :value="0" />
                        <el-option label="处罚" :value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filters.enabled" clearable placeholder="全部状态" style="width: 130px">
                        <el-option label="启用" :value="true" />
                        <el-option label="停用" :value="false" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" native-type="submit">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
            <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
            <el-table :data="rows" v-loading="loading" row-key="id">
                <el-table-column prop="name" label="规则名称" min-width="160" show-overflow-tooltip />
                <el-table-column label="类型" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.type === 0 ? 'success' : 'warning'">{{ typeLabel(row.type) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="默认金额（元）" width="150">
                    <template #default="{ row }">{{ money(row.amount) }}</template>
                </el-table-column>
                <el-table-column prop="description" label="规则说明" min-width="220" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="更新时间" width="180">
                    <template #default="{ row }">{{ displayTime(row.updatedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button type="primary" link @click="openEditor(row)">编辑</el-button>
                    </template>
                </el-table-column>
                <template #empty><el-empty description="暂无规则，请先新增奖励或处罚项目" :image-size="80" /></template>
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
            :title="editingId ? '编辑奖惩规则' : '新增奖惩规则'"
            width="580px"
            :show-close="!mutation.saving"
            :close-on-click-modal="false"
            :close-on-press-escape="!mutation.saving"
        >
            <el-form
                ref="ruleForm"
                :model="form"
                :rules="rules"
                label-position="top"
                :disabled="mutation.saving || mutation.uncertain"
            >
                <el-form-item label="规则名称" prop="name">
                    <el-input v-model="form.name" maxlength="64" show-word-limit />
                </el-form-item>
                <div class="ec-grid">
                    <el-form-item label="类型" prop="type">
                        <el-select v-model="form.type">
                            <el-option label="奖励" :value="0" />
                            <el-option label="处罚" :value="1" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="默认金额（元）" prop="amount">
                        <el-input-number
                            v-model="form.amount"
                            :min="0"
                            :max="99999999.99"
                            :precision="2"
                            :step="100"
                            style="width: 100%"
                        />
                    </el-form-item>
                </div>
                <el-form-item label="规则说明">
                    <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
                </el-form-item>
                <el-form-item label="启用状态">
                    <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
                </el-form-item>
                <p class="ec-hint">停用后不可用于新的登记，已有奖惩记录会保留。金额为非负数，奖励与处罚由类型区分。</p>
            </el-form>
            <el-alert v-if="mutation.error" :title="mutation.error" type="error" :closable="false" show-icon />
            <template #footer>
                <el-button :disabled="mutation.saving" @click="visible = false">
                    {{ mutation.uncertain ? '稍后核实' : '取消' }}
                </el-button>
                <el-button type="primary" :loading="mutation.saving" @click="save">
                    {{ mutation.uncertain ? '重试确认结果' : '保存规则' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script>
import { transferRequest, queryString, displayTime } from '../../../utils/transferApi.js';
import { emptyMutation, submitMutation, money, typeLabel } from '../../../utils/ecUi.js';
const blank = () => ({ name: '', type: 0, amount: 0, description: '', enabled: true, version: null });
export default {
    name: 'EcMana',
    data() {
        return {
            rows: [],
            total: 0,
            page: 1,
            size: 10,
            loading: false,
            error: '',
            version: 0,
            filters: { keyword: '', type: null, enabled: null },
            applied: {},
            visible: false,
            editingId: null,
            form: blank(),
            mutation: emptyMutation(),
            rules: {
                name: [{ required: true, whitespace: true, message: '请填写规则名称', trigger: 'blur' }],
                type: [{ required: true, message: '请选择类型', trigger: 'change' }],
                amount: [{ required: true, message: '请填写默认金额', trigger: 'blur' }],
            },
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
        async load() {
            const version = ++this.version;
            this.loading = true;
            this.error = '';
            try {
                const data = await this.request('/system/basic/ec/?' + queryString(this.applied, this.page, this.size));
                if (version === this.version) {
                    this.rows = data.data;
                    this.total = data.total;
                }
            } catch (error) {
                if (version === this.version) this.error = error.message || '规则加载失败，请重试';
            } finally {
                if (version === this.version) this.loading = false;
            }
        },
        search() {
            this.applied = { ...this.filters };
            this.page = 1;
            return this.load();
        },
        reset() {
            this.filters = { keyword: '', type: null, enabled: null };
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
        openEditor(row) {
            if (this.mutation.saving) return;
            if (!this.mutation.uncertain) {
                this.editingId = row?.id ?? null;
                this.mutation = emptyMutation();
                this.form = row
                    ? {
                          name: row.name,
                          type: row.type,
                          amount: Number(row.amount),
                          description: row.description,
                          enabled: row.enabled,
                          version: row.version,
                      }
                    : blank();
            }
            this.visible = true;
            this.$nextTick(() => this.$refs.ruleForm?.clearValidate());
        },
        save() {
            return submitMutation(this, async () => {
                if (!(await this.$refs.ruleForm.validate().catch(() => false))) return;
                await this.$confirm('保存后用于后续奖惩登记，历史记录保持不变。', '确认保存规则', {
                    type: 'warning',
                    confirmButtonText: '保存',
                    cancelButtonText: '取消',
                });
                return {
                    path: this.editingId ? '/system/basic/ec/' + this.editingId : '/system/basic/ec/',
                    method: this.editingId ? 'PUT' : 'POST',
                    payload: { ...this.form },
                };
            });
        },
        async onSaved() {
            this.visible = false;
            await this.load();
        },
    },
};
</script>
<style src="../../../styles/ec.css"></style>
