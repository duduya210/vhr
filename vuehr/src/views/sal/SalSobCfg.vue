<template>
    <div>
        <div>
            <el-table :data="emps" border stripe size="small">
                <el-table-column type="selection" align="left" width="55"></el-table-column>
                <el-table-column prop="name" label="姓名" fixed width="120" align="left"></el-table-column>
                <el-table-column prop="workID" label="工号" width="120" align="left"></el-table-column>
                <el-table-column prop="email" label="电子邮件" width="200" align="left"></el-table-column>
                <el-table-column prop="phone" label="电话号码" width="120" align="left"></el-table-column>
                <el-table-column prop="department.name" label="所属部门" width="120" align="left"></el-table-column>
                <el-table-column label="工资账套" align="center">
                    <template #default="scope">
                        <el-tooltip placement="right" v-if="scope.row.salary">
                            <template #content
                                ><div>
                                    <table>
                                        <tr>
                                            <td>基本工资</td>
                                            <td>{{ scope.row.salary.basicSalary }}</td>
                                        </tr>
                                        <tr>
                                            <td>交通补助</td>
                                            <td>{{ scope.row.salary.trafficSalary }}</td>
                                        </tr>
                                        <tr>
                                            <td>午餐补助</td>
                                            <td>{{ scope.row.salary.lunchSalary }}</td>
                                        </tr>
                                        <tr>
                                            <td>奖金</td>
                                            <td>{{ scope.row.salary.bonus }}</td>
                                        </tr>
                                        <tr>
                                            <td>养老金比率</td>
                                            <td>{{ scope.row.salary.pensionPer }}</td>
                                        </tr>
                                        <tr>
                                            <td>养老金基数</td>
                                            <td>{{ scope.row.salary.pensionBase }}</td>
                                        </tr>
                                        <tr>
                                            <td>医疗保险比率</td>
                                            <td>{{ scope.row.salary.medicalPer }}</td>
                                        </tr>
                                        <tr>
                                            <td>医疗保险基数</td>
                                            <td>{{ scope.row.salary.medicalBase }}</td>
                                        </tr>
                                        <tr>
                                            <td>公积金比率</td>
                                            <td>{{ scope.row.salary.accumulationFundPer }}</td>
                                        </tr>
                                        <tr>
                                            <td>公积金基数</td>
                                            <td>{{ scope.row.salary.accumulationFundBase }}</td>
                                        </tr>
                                        <tr>
                                            <td>启用时间</td>
                                            <td>{{ scope.row.salary.createDate }}</td>
                                        </tr>
                                    </table>
                                </div></template
                            >
                            <el-tag>{{ scope.row.salary.name }}</el-tag>
                        </el-tooltip>
                        <el-tag v-else>暂未设置</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center">
                    <template #default="scope">
                        <el-button type="primary" plain :disabled="savingSalary" @click="openSalaryEditor(scope.row)"
                            >修改工资账套</el-button
                        >
                    </template>
                </el-table-column>
            </el-table>
            <div style="display: flex; justify-content: flex-end">
                <el-pagination
                    background
                    @size-change="sizeChange"
                    @current-change="currentChange"
                    layout="sizes, prev, pager, next, jumper, ->, total, slot"
                    :total="total"
                >
                </el-pagination>
            </div>
        </div>
        <el-dialog
            v-model="salaryDialogVisible"
            title="修改工资账套"
            width="440px"
            :show-close="!savingSalary"
            :close-on-click-modal="!savingSalary"
            :close-on-press-escape="!savingSalary"
            @closed="resetSalaryEditor"
        >
            <p class="salary-employee">
                员工：{{ editingEmployee?.name }}（{{ editingEmployee?.workID || '未设置工号' }}）
            </p>
            <el-form label-position="top" @submit.prevent="saveSalary">
                <el-form-item label="工资账套" required>
                    <el-select
                        v-model="currentSalary"
                        placeholder="请选择工资账套"
                        :disabled="savingSalary || loadingSalaries"
                        :loading="loadingSalaries"
                        no-data-text="暂无工资账套，请先在工资账套管理中添加"
                    >
                        <el-option v-for="item in salaries" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                </el-form-item>
            </el-form>
            <el-alert v-if="salaryError" :title="salaryError" type="error" :closable="false" show-icon />
            <p v-if="!salaryChanged" class="salary-hint">选择不同的账套后，点击保存生效。</p>
            <template #footer>
                <el-button :disabled="savingSalary" @click="cancelSalaryEdit">取消</el-button>
                <el-button type="primary" :loading="savingSalary" :disabled="!canSaveSalary" @click="saveSalary"
                    >保存</el-button
                >
            </template>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'SalSobCfg',
    data() {
        return {
            emps: [],
            total: 0,
            currentPage: 1,
            currentSize: 10,
            currentSalary: null,
            originalSalaryId: null,
            editingEmployee: null,
            salaryDialogVisible: false,
            savingSalary: false,
            loadingSalaries: false,
            salaryError: '',
            salaries: [],
        };
    },
    computed: {
        salaryChanged() {
            return this.currentSalary !== this.originalSalaryId;
        },
        canSaveSalary() {
            return (
                this.salaryChanged &&
                !!this.editingEmployee &&
                !this.loadingSalaries &&
                !this.savingSalary &&
                this.salaries.some((salary) => salary.id === this.currentSalary)
            );
        },
    },
    mounted() {
        this.initEmps();
        this.initSalaries();
    },
    methods: {
        sizeChange(size) {
            this.currentSize = size;
            this.initEmps();
        },
        currentChange(page) {
            this.currentPage = page;
            this.initEmps();
        },
        openSalaryEditor(employee) {
            if (this.savingSalary) return;
            this.editingEmployee = { id: employee.id, name: employee.name, workID: employee.workID };
            this.originalSalaryId = employee.salary?.id ?? null;
            this.currentSalary = this.originalSalaryId;
            this.salaryError = '';
            this.salaryDialogVisible = true;
            this.initSalaries();
        },
        cancelSalaryEdit() {
            if (this.savingSalary) return;
            this.salaryDialogVisible = false;
            this.resetSalaryEditor();
        },
        resetSalaryEditor() {
            this.currentSalary = null;
            this.originalSalaryId = null;
            this.editingEmployee = null;
            this.salaryError = '';
        },
        async saveSalary() {
            if (!this.canSaveSalary) return;
            this.savingSalary = true;
            this.salaryError = '';
            try {
                const resp = await this.putRequest(
                    '/salary/sobcfg/?eid=' + this.editingEmployee.id + '&sid=' + this.currentSalary,
                );
                if (resp?.status === 200) {
                    this.salaryDialogVisible = false;
                    await this.initEmps();
                } else {
                    this.salaryError = resp?.msg || '保存失败，请重试。';
                }
            } catch {
                this.salaryError = '保存结果未确认，请检查网络并刷新列表核实后再操作。';
            } finally {
                this.savingSalary = false;
            }
        },
        async initSalaries() {
            this.loadingSalaries = true;
            try {
                const resp = await this.getRequest('/salary/sobcfg/salaries');
                this.salaries = Array.isArray(resp) ? resp : [];
                if (!Array.isArray(resp)) this.salaryError = '工资账套加载失败，请关闭后重试。';
            } catch {
                this.salaries = [];
                this.salaryError = '工资账套加载失败，请关闭后重试。';
            } finally {
                this.loadingSalaries = false;
            }
        },
        async initEmps() {
            try {
                const resp = await this.getRequest(
                    '/salary/sobcfg/?page=' + this.currentPage + '&size=' + this.currentSize,
                );
                if (resp) {
                    this.emps = resp.data;
                    this.total = resp.total;
                }
            } catch {
                this.$message.error('员工账套列表加载失败，请刷新重试');
            }
        },
    },
};
</script>

<style scoped>
.salary-employee {
    margin: 0 0 22px;
    color: #526078;
}
.salary-hint {
    color: #8390a6;
    font-size: 12px;
    margin-top: 18px;
}
</style>
