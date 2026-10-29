<template>
    <div class="employee-page">
        <div class="page-heading">
            <div>
                <h1>员工管理</h1>
                <p>集中维护员工档案，让每一份信息清晰可查。</p>
            </div>
            <el-button type="primary" @click="showAddEmpView"
                ><el-icon><Plus /></el-icon><span>新增员工</span></el-button
            >
        </div>
        <section class="surface-card employee-list">
            <div class="employee-toolbar">
                <form class="employee-search" @submit.prevent="applySearch('basic')">
                    <el-input
                        v-model="keyword"
                        placeholder="搜索员工姓名"
                        aria-label="搜索员工姓名"
                        clearable
                        @clear="applySearch('basic')"
                        ><template #prefix
                            ><el-icon><Search /></el-icon></template></el-input
                    ><el-button native-type="submit">搜索</el-button
                    ><el-button
                        :type="showAdvanceSearchView ? 'primary' : 'default'"
                        plain
                        :aria-expanded="showAdvanceSearchView"
                        @click="showAdvanceSearchView = !showAdvanceSearchView"
                        ><el-icon><Filter /></el-icon>筛选</el-button
                    >
                </form>
                <div class="employee-tools">
                    <el-upload
                        :show-file-list="false"
                        :before-upload="beforeUpload"
                        :on-success="onSuccess"
                        :on-error="onError"
                        :disabled="importDataDisabled"
                        action="/employee/basic/import"
                        ><el-button :disabled="importDataDisabled"
                            ><el-icon><component :is="importDataBtnIcon" /></el-icon>{{ importDataBtnText }}</el-button
                        ></el-upload
                    ><el-button @click="exportData"
                        ><el-icon><Download /></el-icon>导出数据</el-button
                    >
                </div>
            </div>
            <el-collapse-transition
                ><el-form
                    v-show="showAdvanceSearchView"
                    class="employee-filters"
                    label-position="top"
                    @submit.prevent="applySearch('advanced')"
                    ><div class="filter-grid">
                        <el-form-item label="所属部门"
                            ><el-tree-select
                                v-model="searchValue.departmentId"
                                :data="allDeps"
                                :props="defaultProps"
                                node-key="id"
                                check-strictly
                                clearable
                                :render-after-expand="false"
                                placeholder="全部部门"
                        /></el-form-item>
                        <el-form-item label="职位"
                            ><el-select v-model="searchValue.posId" clearable placeholder="全部职位"
                                ><el-option
                                    v-for="item in positions"
                                    :key="item.id"
                                    :label="item.name"
                                    :value="item.id" /></el-select
                        ></el-form-item>
                        <el-form-item label="职称"
                            ><el-select v-model="searchValue.jobLevelId" clearable placeholder="全部职称"
                                ><el-option
                                    v-for="item in joblevels"
                                    :key="item.id"
                                    :label="item.name"
                                    :value="item.id" /></el-select
                        ></el-form-item>
                        <el-form-item label="聘用形式"
                            ><el-select v-model="searchValue.engageForm" clearable placeholder="全部形式"
                                ><el-option label="劳动合同" value="劳动合同" /><el-option
                                    label="劳务合同"
                                    value="劳务合同" /></el-select
                        ></el-form-item>
                        <el-form-item label="政治面貌"
                            ><el-select v-model="searchValue.politicId" clearable placeholder="全部政治面貌"
                                ><el-option
                                    v-for="item in politicsstatus"
                                    :key="item.id"
                                    :label="item.name"
                                    :value="item.id" /></el-select
                        ></el-form-item>
                        <el-form-item label="民族"
                            ><el-select v-model="searchValue.nationId" clearable placeholder="全部民族"
                                ><el-option
                                    v-for="item in nations"
                                    :key="item.id"
                                    :label="item.name"
                                    :value="item.id" /></el-select
                        ></el-form-item>
                        <el-form-item label="入职日期" class="date-filter"
                            ><el-date-picker
                                v-model="searchValue.beginDateScope"
                                type="daterange"
                                unlink-panels
                                value-format="YYYY-MM-DD"
                                range-separator="至"
                                start-placeholder="开始日期"
                                end-placeholder="结束日期"
                        /></el-form-item>
                    </div>
                    <div class="filter-actions">
                        <el-button @click="resetSearch">重置筛选</el-button
                        ><el-button type="primary" native-type="submit">应用筛选</el-button>
                    </div></el-form
                ></el-collapse-transition
            >
            <div class="list-summary">
                <span
                    >员工档案 <strong>{{ total }}</strong> 条</span
                ><el-tag v-if="searchMode === 'advanced'" closable effect="plain" @close="resetSearch"
                    >高级筛选已应用</el-tag
                ><span class="summary-hint">点击详情，查看完整员工资料</span>
            </div>
            <el-table
                :data="emps"
                v-loading="loading"
                row-key="id"
                element-loading-text="正在加载员工档案…"
                element-loading-background="rgba(255,255,255,0.8)"
                style="width: 100%"
            >
                <el-table-column label="员工" fixed min-width="160"
                    ><template #default="scope"
                        ><div class="employee-identity">
                            <span class="employee-avatar">{{ (scope.row.name || '员').slice(-2) }}</span>
                            <div>
                                <span class="employee-name">{{ scope.row.name }}</span
                                ><small>{{ scope.row.workID || '未设置工号' }}</small>
                            </div>
                        </div></template
                    ></el-table-column
                >
                <el-table-column prop="department.name" label="所属部门" min-width="130" show-overflow-tooltip />
                <el-table-column prop="position.name" label="职位" min-width="120" show-overflow-tooltip />
                <el-table-column label="联系方式" min-width="200" show-overflow-tooltip
                    ><template #default="scope"
                        ><div>{{ scope.row.phone || '—' }}</div>
                        <span class="secondary-text">{{ scope.row.email || '—' }}</span></template
                    ></el-table-column
                >
                <el-table-column prop="beginDate" label="入职日期" width="120" />
                <el-table-column label="操作" fixed="right" width="174"
                    ><template #default="scope"
                        ><el-button link type="primary" @click="showDetails(scope.row)">详情</el-button
                        ><el-button link type="primary" @click="showEditEmpView(scope.row)">编辑</el-button
                        ><el-button link type="danger" @click="deleteEmp(scope.row)">删除</el-button></template
                    ></el-table-column
                >
                <template #empty
                    ><el-empty description="没有找到符合条件的员工" :image-size="88"
                        ><el-button v-if="keyword || searchMode === 'advanced'" @click="resetSearch"
                            >清除筛选</el-button
                        ></el-empty
                    ></template
                >
            </el-table>
            <div class="employee-pagination">
                <el-pagination
                    background
                    :current-page="page"
                    :page-size="size"
                    :page-sizes="[10, 20, 50]"
                    @current-change="currentChange"
                    @size-change="sizeChange"
                    layout="total, sizes, prev, pager, next"
                    :pager-count="5"
                    :total="total"
                />
            </div>
        </section>
        <el-drawer v-model="detailVisible" title="员工档案" size="min(640px, 100vw)">
            <template v-if="selectedEmployee"
                ><div class="detail-profile">
                    <span class="employee-avatar large">{{ (selectedEmployee.name || '员').slice(-2) }}</span>
                    <div>
                        <h2>{{ selectedEmployee.name }}</h2>
                        <p>{{ detailValue('department.name') }} · {{ detailValue('position.name') }}</p>
                    </div>
                </div>
                <section v-for="section in detailSections" :key="section.title" class="detail-section">
                    <h3>{{ section.title }}</h3>
                    <dl>
                        <div v-for="field in section.fields" :key="field[0]">
                            <dt>{{ field[1] }}</dt>
                            <dd>{{ detailValue(field[0]) }}</dd>
                        </div>
                    </dl>
                </section></template
            >
        </el-drawer>
        <el-dialog
            :title="title"
            v-model="dialogVisible"
            width="900px"
            class="employee-dialog"
            top="5vh"
            @closed="clearFormValidation"
        >
            <el-form :model="emp" :rules="rules" ref="empForm" label-position="top"
                ><section class="employee-form-section">
                    <h3>基本信息</h3>
                    <div class="employee-form-grid">
                        <el-form-item label="姓名" prop="name">
                            <el-input size="default" v-model="emp.name" placeholder="请输入员工姓名"
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="性别" prop="gender">
                            <el-radio-group v-model="emp.gender">
                                <el-radio label="男" value="男">男</el-radio>
                                <el-radio label="女" value="女">女</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="出生日期" prop="birthday">
                            <el-date-picker
                                v-model="emp.birthday"
                                size="default"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="出生日期"
                            >
                            </el-date-picker>
                        </el-form-item>
                        <el-form-item label="身份证号码" prop="idCard">
                            <el-input size="default" v-model="emp.idCard" placeholder="请输入身份证号码"
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="民族" prop="nationId">
                            <el-select v-model="emp.nationId" placeholder="民族" size="default">
                                <el-option v-for="item in nations" :key="item.id" :label="item.name" :value="item.id">
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="籍贯" prop="nativePlace">
                            <el-input size="default" v-model="emp.nativePlace" placeholder="请输入籍贯"
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="政治面貌" prop="politicId">
                            <el-select v-model="emp.politicId" placeholder="政治面貌" size="default">
                                <el-option
                                    v-for="item in politicsstatus"
                                    :key="item.id"
                                    :label="item.name"
                                    :value="item.id"
                                >
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="婚姻状况" prop="wedlock">
                            <el-radio-group v-model="emp.wedlock">
                                <el-radio label="已婚" value="已婚">已婚</el-radio>
                                <el-radio label="未婚" value="未婚">未婚</el-radio>
                                <el-radio label="离异" value="离异">离异</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </div>
                </section>
                <section class="employee-form-section">
                    <h3>联系方式与教育经历</h3>
                    <div class="employee-form-grid">
                        <el-form-item label="电话号码" prop="phone">
                            <el-input size="default" v-model="emp.phone" placeholder="电话号码"
                                ><template #prefix
                                    ><el-icon><Phone /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="电子邮箱" prop="email">
                            <el-input size="default" v-model="emp.email" placeholder="请输入电子邮箱"
                                ><template #prefix
                                    ><el-icon><Message /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="联系地址" prop="address">
                            <el-input size="default" v-model="emp.address" placeholder="请输入联系地址"
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="学历" prop="tiptopDegree">
                            <el-select v-model="emp.tiptopDegree" placeholder="学历" size="default">
                                <el-option v-for="item in tiptopDegrees" :key="item" :label="item" :value="item">
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="毕业院校" prop="school">
                            <el-input size="default" v-model="emp.school" placeholder="毕业院校名称"
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="专业名称" prop="specialty">
                            <el-input size="default" v-model="emp.specialty" placeholder="请输入专业名称"
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                    </div>
                </section>
                <section class="employee-form-section">
                    <h3>任职信息</h3>
                    <div class="employee-form-grid">
                        <el-form-item label="工号" prop="workID">
                            <el-input size="default" v-model="emp.workID" placeholder="工号" disabled
                                ><template #prefix
                                    ><el-icon><Edit /></el-icon></template
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="所属部门" prop="departmentId"
                            ><el-tree-select
                                v-model="emp.departmentId"
                                :data="allDeps"
                                :props="defaultProps"
                                node-key="id"
                                check-strictly
                                :render-after-expand="false"
                                placeholder="请选择部门"
                        /></el-form-item>
                        <el-form-item label="职位" prop="posId">
                            <el-select v-model="emp.posId" placeholder="职位" size="default">
                                <el-option v-for="item in positions" :key="item.id" :label="item.name" :value="item.id">
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="职称" prop="jobLevelId">
                            <el-select v-model="emp.jobLevelId" placeholder="职称" size="default">
                                <el-option v-for="item in joblevels" :key="item.id" :label="item.name" :value="item.id">
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="入职日期" prop="beginDate">
                            <el-date-picker
                                v-model="emp.beginDate"
                                size="default"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="入职日期"
                            >
                            </el-date-picker>
                        </el-form-item>
                        <el-form-item label="转正日期" prop="conversionTime">
                            <el-date-picker
                                v-model="emp.conversionTime"
                                size="default"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="转正日期"
                            >
                            </el-date-picker>
                        </el-form-item>
                    </div>
                </section>
                <section class="employee-form-section">
                    <h3>合同信息</h3>
                    <div class="employee-form-grid">
                        <el-form-item label="聘用形式" prop="engageForm">
                            <el-radio-group v-model="emp.engageForm">
                                <el-radio label="劳动合同" value="劳动合同">劳动合同</el-radio>
                                <el-radio label="劳务合同" value="劳务合同">劳务合同</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="合同起始日期" prop="beginContract">
                            <el-date-picker
                                v-model="emp.beginContract"
                                size="default"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="合同起始日期"
                            >
                            </el-date-picker>
                        </el-form-item>
                        <el-form-item label="合同终止日期" prop="endContract">
                            <el-date-picker
                                v-model="emp.endContract"
                                size="default"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="合同终止日期"
                            >
                            </el-date-picker>
                        </el-form-item>
                    </div></section
            ></el-form>
            <template #footer
                ><el-button @click="dialogVisible = false">取消</el-button
                ><el-button type="primary" :loading="saving" @click="doAddEmp">保存员工信息</el-button></template
            >
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'EmpBasic',
    data() {
        return {
            saving: false,
            searchMode: 'basic',
            detailVisible: false,
            selectedEmployee: null,
            detailSections: [
                {
                    title: '基本信息',
                    fields: [
                        ['name', '姓名'],
                        ['workID', '工号'],
                        ['gender', '性别'],
                        ['birthday', '出生日期'],
                        ['idCard', '身份证号码'],
                        ['wedlock', '婚姻状况'],
                        ['nation.name', '民族'],
                        ['nativePlace', '籍贯'],
                        ['politicsstatus.name', '政治面貌'],
                    ],
                },
                {
                    title: '联系方式与教育经历',
                    fields: [
                        ['phone', '电话号码'],
                        ['email', '电子邮箱'],
                        ['address', '联系地址'],
                        ['tiptopDegree', '最高学历'],
                        ['school', '毕业院校'],
                        ['specialty', '专业'],
                    ],
                },
                {
                    title: '任职与合同',
                    fields: [
                        ['department.name', '所属部门'],
                        ['position.name', '职位'],
                        ['jobLevel.name', '职称'],
                        ['workState', '在职状态'],
                        ['beginDate', '入职日期'],
                        ['conversionTime', '转正日期'],
                        ['engageForm', '聘用形式'],
                        ['beginContract', '合同起始日期'],
                        ['endContract', '合同截止日期'],
                        ['contractTerm', '合同期限（年）'],
                    ],
                },
            ],

            searchValue: {
                politicId: null,
                nationId: null,
                jobLevelId: null,
                posId: null,
                engageForm: null,
                departmentId: null,
                beginDateScope: null,
            },
            title: '',
            importDataBtnText: '导入数据',
            importDataBtnIcon: 'Upload',
            importDataDisabled: false,
            showAdvanceSearchView: false,
            allDeps: [],
            emps: [],
            loading: false,
            popVisible: false,
            popVisible2: false,
            dialogVisible: false,
            total: 0,
            page: 1,
            keyword: '',
            size: 10,
            nations: [],
            joblevels: [],
            politicsstatus: [],
            positions: [],
            tiptopDegrees: ['本科', '大专', '硕士', '博士', '高中', '初中', '小学', '其他'],
            options: [
                {
                    value: '选项1',
                    label: '黄金糕',
                },
                {
                    value: '选项2',
                    label: '双皮奶',
                },
                {
                    value: '选项3',
                    label: '蚵仔煎',
                },
                {
                    value: '选项4',
                    label: '龙须面',
                },
                {
                    value: '选项5',
                    label: '北京烤鸭',
                },
            ],
            inputDepName: '所属部门',
            emp: {
                name: 'javaboy',
                gender: '男',
                birthday: '1989-12-31',
                idCard: '610122199001011256',
                wedlock: '已婚',
                nationId: 1,
                nativePlace: '陕西',
                politicId: 13,
                email: 'laowang@qq.com',
                phone: '18565558897',
                address: '深圳市南山区',
                departmentId: null,
                jobLevelId: 9,
                posId: 29,
                engageForm: '劳务合同',
                tiptopDegree: '本科',
                specialty: '信息管理与信息系统',
                school: '深圳大学',
                beginDate: '2017-12-31',
                workState: '在职',
                workID: '00000057',
                contractTerm: 2,
                conversionTime: '2018-03-31',
                notworkDate: null,
                beginContract: '2017-12-31',
                endContract: '2019-12-31',
                workAge: null,
            },
            defaultProps: {
                children: 'children',
                label: 'name',
            },
            rules: {
                name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
                gender: [{ required: true, message: '请输入性别', trigger: 'blur' }],
                birthday: [{ required: true, message: '请输入出生日期', trigger: 'blur' }],
                idCard: [
                    { required: true, message: '请输入身份证号码', trigger: 'blur' },
                    {
                        pattern:
                            /(^[1-9]\d{5}(18|19|([23]\d))\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$)|(^[1-9]\d{5}\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\d{2}$)/,
                        message: '身份证号码格式不正确',
                        trigger: 'blur',
                    },
                ],
                wedlock: [{ required: true, message: '请输入婚姻状况', trigger: 'blur' }],
                nationId: [{ required: true, message: '请输入您组', trigger: 'blur' }],
                nativePlace: [{ required: true, message: '请输入籍贯', trigger: 'blur' }],
                politicId: [{ required: true, message: '请输入政治面貌', trigger: 'blur' }],
                email: [
                    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
                    {
                        type: 'email',
                        message: '邮箱格式不正确',
                        trigger: 'blur',
                    },
                ],
                phone: [{ required: true, message: '请输入电话号码', trigger: 'blur' }],
                address: [{ required: true, message: '请输入员工地址', trigger: 'blur' }],
                departmentId: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
                jobLevelId: [{ required: true, message: '请输入职称', trigger: 'blur' }],
                posId: [{ required: true, message: '请输入职位', trigger: 'blur' }],
                engageForm: [{ required: true, message: '请输入聘用形式', trigger: 'blur' }],
                tiptopDegree: [{ required: true, message: '请输入学历', trigger: 'blur' }],
                specialty: [{ required: true, message: '请输入专业', trigger: 'blur' }],
                school: [{ required: true, message: '请输入毕业院校', trigger: 'blur' }],
                beginDate: [{ required: true, message: '请输入入职日期', trigger: 'blur' }],
                workState: [{ required: true, message: '请输入工作状态', trigger: 'blur' }],
                workID: [{ required: true, message: '请输入工号', trigger: 'blur' }],
                contractTerm: [{ required: true, message: '请输入合同期限', trigger: 'blur' }],
                conversionTime: [{ required: true, message: '请输入转正日期', trigger: 'blur' }],
                notworkDate: [{ required: true, message: '请输入离职日期', trigger: 'blur' }],
                beginContract: [{ required: true, message: '请输入合同起始日期', trigger: 'blur' }],
                endContract: [{ required: true, message: '请输入合同结束日期', trigger: 'blur' }],
                workAge: [{ required: true, message: '请输入工龄', trigger: 'blur' }],
            },
        };
    },
    mounted() {
        this.initEmps();
        this.initData();
        this.initPositions();
    },
    methods: {
        showDetails(employee) {
            this.selectedEmployee = employee;
            this.detailVisible = true;
        },
        detailValue(path) {
            const value = path.split('.').reduce((value, key) => value?.[key], this.selectedEmployee);
            return value === null || value === undefined || value === '' ? '—' : value;
        },
        clearFormValidation() {
            this.$refs.empForm?.clearValidate();
        },
        applySearch(mode) {
            this.searchMode = mode;
            this.page = 1;
            this.initEmps();
        },
        resetSearch() {
            this.keyword = '';
            Object.keys(this.searchValue).forEach((key) => {
                this.searchValue[key] = null;
            });
            this.applySearch('basic');
        },
        searvhViewHandleNodeClick(data) {
            this.inputDepName = data.name;
            this.searchValue.departmentId = data.id;
            this.popVisible2 = !this.popVisible2;
        },
        onError(err, file, fileList) {
            this.importDataBtnText = '导入数据';
            this.importDataBtnIcon = 'Upload';
            this.importDataDisabled = false;
        },
        onSuccess(response, file, fileList) {
            this.importDataBtnText = '导入数据';
            this.importDataBtnIcon = 'Upload';
            this.importDataDisabled = false;
            this.initEmps();
        },
        beforeUpload() {
            this.importDataBtnText = '正在导入';
            this.importDataBtnIcon = 'Loading';
            this.importDataDisabled = true;
        },
        exportData() {
            window.open('/employee/basic/export', '_parent');
        },
        emptyEmp() {
            this.emp = {
                name: '',
                gender: '',
                birthday: '',
                idCard: '',
                wedlock: '',
                nationId: 1,
                nativePlace: '',
                politicId: 13,
                email: '',
                phone: '',
                address: '',
                departmentId: null,
                jobLevelId: 9,
                posId: 29,
                engageForm: '',
                tiptopDegree: '',
                specialty: '',
                school: '',
                beginDate: '',
                workID: '',
                contractTerm: 2,
                conversionTime: '',
                notworkDate: null,
                beginContract: '',
                endContract: '',
                workAge: null,
            };
            this.inputDepName = '';
        },
        showEditEmpView(data) {
            this.initPositions();
            this.title = '编辑员工信息';
            this.emp = JSON.parse(JSON.stringify(data));
            this.inputDepName = data.department?.name || '';
            this.dialogVisible = true;
        },
        deleteEmp(data) {
            this.$confirm('此操作将永久删除【' + data.name + '】, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            })
                .then(() => {
                    this.deleteRequest('/employee/basic/' + data.id).then((resp) => {
                        if (resp) {
                            this.initEmps();
                        }
                    });
                })
                .catch(() => {
                    this.$message({
                        type: 'info',
                        message: '已取消删除',
                    });
                });
        },
        doAddEmp() {
            if (this.saving) return;
            this.$refs.empForm.validate(async (valid) => {
                if (!valid || this.saving) return;
                this.saving = true;
                try {
                    const resp = this.emp.id
                        ? await this.putRequest('/employee/basic/', this.emp)
                        : await this.postRequest('/employee/basic/', this.emp);
                    if (resp) {
                        this.dialogVisible = false;
                        this.initEmps();
                    }
                } catch {
                    this.$message.error('保存失败，请稍后重试');
                } finally {
                    this.saving = false;
                }
            });
        },
        handleNodeClick(data) {
            this.inputDepName = data.name;
            this.emp.departmentId = data.id;
            this.popVisible = !this.popVisible;
        },
        showDepView() {
            this.popVisible = !this.popVisible;
        },
        showDepView2() {
            this.popVisible2 = !this.popVisible2;
        },
        initPositions() {
            this.getRequest('/employee/basic/positions').then((resp) => {
                if (resp) {
                    this.positions = resp;
                }
            });
        },
        getMaxWordID() {
            this.getRequest('/employee/basic/maxWorkID').then((resp) => {
                if (resp) {
                    this.emp.workID = resp.obj;
                }
            });
        },
        initData() {
            if (!window.sessionStorage.getItem('nations')) {
                this.getRequest('/employee/basic/nations').then((resp) => {
                    if (resp) {
                        this.nations = resp;
                        window.sessionStorage.setItem('nations', JSON.stringify(resp));
                    }
                });
            } else {
                this.nations = JSON.parse(window.sessionStorage.getItem('nations'));
            }
            if (!window.sessionStorage.getItem('joblevels')) {
                this.getRequest('/employee/basic/joblevels').then((resp) => {
                    if (resp) {
                        this.joblevels = resp;
                        window.sessionStorage.setItem('joblevels', JSON.stringify(resp));
                    }
                });
            } else {
                this.joblevels = JSON.parse(window.sessionStorage.getItem('joblevels'));
            }
            if (!window.sessionStorage.getItem('politicsstatus')) {
                this.getRequest('/employee/basic/politicsstatus').then((resp) => {
                    if (resp) {
                        this.politicsstatus = resp;
                        window.sessionStorage.setItem('politicsstatus', JSON.stringify(resp));
                    }
                });
            } else {
                this.politicsstatus = JSON.parse(window.sessionStorage.getItem('politicsstatus'));
            }
            if (!window.sessionStorage.getItem('deps')) {
                this.getRequest('/employee/basic/deps').then((resp) => {
                    if (resp) {
                        this.allDeps = resp;
                        window.sessionStorage.setItem('deps', JSON.stringify(resp));
                    }
                });
            } else {
                this.allDeps = JSON.parse(window.sessionStorage.getItem('deps'));
            }
        },
        sizeChange(currentSize) {
            this.size = currentSize;
            this.page = 1;
            this.initEmps();
        },
        currentChange(currentPage) {
            this.page = currentPage;
            this.initEmps();
        },
        showAddEmpView() {
            this.emptyEmp();
            this.title = '添加员工';
            this.getMaxWordID();
            this.dialogVisible = true;
        },
        initEmps() {
            this.loading = true;
            let url = '/employee/basic/?page=' + this.page + '&size=' + this.size;
            url += '&name=' + encodeURIComponent(this.keyword);
            if (this.searchMode === 'advanced') {
                if (this.searchValue.politicId) {
                    url += '&politicId=' + this.searchValue.politicId;
                }
                if (this.searchValue.nationId) {
                    url += '&nationId=' + this.searchValue.nationId;
                }
                if (this.searchValue.jobLevelId) {
                    url += '&jobLevelId=' + this.searchValue.jobLevelId;
                }
                if (this.searchValue.posId) {
                    url += '&posId=' + this.searchValue.posId;
                }
                if (this.searchValue.engageForm) {
                    url += '&engageForm=' + encodeURIComponent(this.searchValue.engageForm);
                }
                if (this.searchValue.departmentId) {
                    url += '&departmentId=' + this.searchValue.departmentId;
                }
                if (this.searchValue.beginDateScope) {
                    url += '&beginDateScope=' + this.searchValue.beginDateScope;
                }
            }
            return this.getRequest(url)
                .then((resp) => {
                    if (resp) {
                        this.emps = resp.data;
                        this.total = resp.total;
                        const lastPage = Math.max(1, Math.ceil(this.total / this.size));
                        if (this.page > lastPage) {
                            this.page = lastPage;
                            return this.initEmps();
                        }
                    }
                })
                .catch(() => {
                    this.$message.error('员工档案加载失败，请稍后重试');
                })
                .finally(() => {
                    this.loading = false;
                });
        },
    },
};
</script>

<style scoped>
.employee-page {
    min-width: 0;
}
.page-heading .el-button span {
    margin-left: 7px;
}
.employee-list {
    overflow: hidden;
}
.employee-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    padding: 22px 24px;
}
.employee-search,
.employee-tools {
    display: flex;
    align-items: center;
    gap: 10px;
}
.employee-search {
    flex: 1;
    min-width: 0;
}
.employee-search .el-input {
    width: 260px;
}
.employee-toolbar .el-button + .el-button {
    margin-left: 0;
}
.employee-tools {
    flex-shrink: 0;
}
.employee-filters {
    padding: 22px 24px;
    background: #f8f9fd;
    border-top: 1px solid #edf0f6;
    border-bottom: 1px solid #edf0f6;
}
.filter-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0 20px;
}
.date-filter {
    grid-column: span 2;
}
.filter-grid :deep(.el-date-editor) {
    width: 100%;
}
.filter-actions {
    display: flex;
    justify-content: flex-end;
}
.employee-filters :deep(.el-form-item__label) {
    font-size: 12px;
    color: #6e7a91;
}
.list-summary {
    padding: 0 24px 18px;
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #7d889c;
}
.employee-filters + .list-summary {
    padding-top: 18px;
}
.list-summary strong {
    color: #4f46e5;
    margin: 0 4px;
}
.summary-hint {
    margin-left: auto;
    color: #929bae;
}
.employee-list :deep(.el-table .cell) {
    padding-left: 18px;
    padding-right: 18px;
}
.employee-identity {
    display: flex;
    align-items: center;
    gap: 10px;
}
.employee-avatar {
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: #efedfc;
    color: #7266c7;
    font-size: 12px;
    flex-shrink: 0;
}
.employee-name {
    font-size: 14px;
    color: #27344c;
    font-weight: 600;
}
.employee-identity small {
    display: block;
    font-size: 11px;
    color: #98a1b1;
    margin-top: 2px;
}
.secondary-text {
    font-size: 11px;
    color: #8a95a8;
}
.employee-pagination {
    display: flex;
    justify-content: flex-end;
    padding: 22px 24px;
    overflow-x: auto;
}
.employee-list :deep(.el-button.is-link) {
    font-size: 12px;
}
.detail-profile {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 8px 0 28px;
}
.employee-avatar.large {
    width: 58px;
    height: 58px;
    border-radius: 16px;
    font-size: 18px;
}
.detail-profile h2 {
    font-size: 22px;
    margin: 0 0 7px;
}
.detail-profile p {
    font-size: 13px;
    color: #8390a6;
    margin: 0;
}
.detail-section {
    border-top: 1px solid #edf0f5;
    padding: 16px 0;
}
.detail-section h3 {
    font-size: 14px;
    margin: 0 0 20px;
}
.detail-section dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
    margin: 0;
}
.detail-section dt {
    font-size: 12px;
    color: #939dae;
    margin-bottom: 7px;
}
.detail-section dd {
    margin: 0;
    font-size: 14px;
    color: #3d4960;
    overflow-wrap: anywhere;
}
.employee-form-section + .employee-form-section {
    border-top: 1px solid #edf0f5;
    padding-top: 18px;
    margin-top: 6px;
}
.employee-form-section h3 {
    margin: 0 0 22px;
    font-size: 15px;
    display: flex;
    align-items: center;
    gap: 9px;
}
.employee-form-section h3::before {
    content: '';
    height: 15px;
    width: 3px;
    background: #7266de;
    border-radius: 2px;
}
.employee-form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 28px;
}
.employee-form-grid :deep(.el-select),
.employee-form-grid :deep(.el-date-editor.el-input) {
    width: 100%;
}
.employee-form-grid :deep(.el-form-item__label) {
    font-size: 12px;
    color: #69768c;
}
.employee-form-grid :deep(.el-radio) {
    margin-right: 20px;
}
@media (max-width: 1100px) {
    .filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
@media (max-width: 700px) {
    .employee-toolbar {
        padding: 16px;
    }
    .employee-search {
        flex-wrap: wrap;
        flex-basis: 100%;
    }
    .employee-search .el-input {
        width: 100%;
    }
    .employee-tools {
        flex-wrap: wrap;
    }
    .employee-filters {
        padding: 16px;
    }
    .filter-grid,
    .employee-form-grid {
        grid-template-columns: 1fr;
    }
    .date-filter {
        grid-column: auto;
    }
    .summary-hint {
        display: none;
    }
    .list-summary {
        padding-left: 16px;
    }
    .employee-pagination {
        justify-content: flex-start;
        padding: 18px 12px;
    }
    .detail-section dl {
        grid-template-columns: 1fr;
    }
}
</style>
