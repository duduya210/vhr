import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parse } from '@vue/compiler-sfc';
import { getReadyMenuGroups, isFeatureReady } from '../src/utils/featureAvailability.js';

// Exercise the real Options API methods without a browser or live database.
async function component(file) {
    const sourceUrl = new URL(`../src/views/${file}`, import.meta.url);
    const source = await readFile(sourceUrl, 'utf8');
    const { descriptor } = parse(source);
    const script = descriptor.script.content.replace(
        /from\s+(['"])(\.[^'"]+)\1/g,
        (_, quote, path) => `from ${quote}${new URL(path, sourceUrl).href}${quote}`,
    );
    return (await import(`data:text/javascript;base64,${Buffer.from(script).toString('base64')}`)).default;
}
function instance(component, overrides = {}) {
    const vm = Object.assign(component.data(), overrides);
    for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
    for (const [name, getter] of Object.entries(component.computed || {})) {
        Object.defineProperty(vm, name, { get: () => getter.call(vm) });
    }
    return vm;
}
const employee = await component('emp/EmpBasic.vue');
const login = await component('Login.vue');
const home = await component('Home.vue');
const salary = await component('sal/SalSobCfg.vue');

test('cancelled editing leaves the original employee and nested department unchanged', () => {
    const original = { id: 1, name: '测试员工', department: { id: 2, name: '研发部' } };
    const vm = instance(employee, { getRequest: async () => [] });
    vm.showEditEmpView(original);
    vm.emp.name = '未保存的姓名';
    vm.emp.department.name = '未保存的部门';
    assert.equal(original.name, '测试员工');
    assert.equal(original.department.name, '研发部');
    assert.equal(vm.dialogVisible, true);
});

test('advanced filters and encoded employee name survive paging and page-size changes', async () => {
    const requests = [];
    const vm = instance(employee, {
        getRequest: async (url) => {
            requests.push(url);
            return { data: [], total: 120 };
        },
    });
    vm.keyword = '张 & 李';
    vm.searchValue.departmentId = 3;
    vm.searchValue.engageForm = '劳动合同';
    vm.applySearch('advanced');
    vm.currentChange(2);
    vm.sizeChange(20);
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(requests.length, 3);
    const parsed = requests.map((url) => new URL(url, 'http://test.local'));
    for (const url of parsed) {
        assert.equal(url.searchParams.get('name'), '张 & 李');
        assert.equal(url.searchParams.get('departmentId'), '3');
        assert.equal(url.searchParams.get('engageForm'), '劳动合同');
    }
    assert.equal(parsed[1].searchParams.get('page'), '2');
    assert.equal(parsed[2].searchParams.get('page'), '1');
    assert.equal(parsed[2].searchParams.get('size'), '20');
});

test('reset clears advanced conditions, keyword and pagination together', async () => {
    let requested;
    const vm = instance(employee, {
        getRequest: async (url) => {
            requested = url;
            return { data: [], total: 0 };
        },
    });
    vm.keyword = '测试';
    vm.page = 4;
    vm.searchMode = 'advanced';
    vm.searchValue.departmentId = 5;
    vm.resetSearch();
    await new Promise((resolve) => setImmediate(resolve));
    const query = new URL(requested, 'http://test.local').searchParams;
    assert.equal(query.get('page'), '1');
    assert.equal(query.get('name'), '');
    assert.equal(query.has('departmentId'), false);
    assert.equal(vm.searchMode, 'basic');
});

test('deleting the final row on a page returns to the last available page', async () => {
    const requests = [];
    const vm = instance(employee, {
        getRequest: async (url) => {
            requests.push(url);
            return { data: [], total: 10 };
        },
    });
    vm.page = 2;
    await vm.initEmps();
    assert.equal(requests.length, 2);
    assert.equal(vm.page, 1);
    assert.equal(vm.loading, false);
});

test('employee load failure releases the loading indicator', async () => {
    let message;
    const vm = instance(employee, {
        getRequest: async () => {
            throw new Error('offline');
        },
        $message: {
            error: (value) => {
                message = value;
            },
        },
    });
    await vm.initEmps();
    assert.equal(vm.loading, false);
    assert.match(message, /加载失败/);
});

test('detail panel handles missing relations and preserves zero values', () => {
    const vm = instance(employee);
    vm.showDetails({ name: '测试', contractTerm: 0, department: null });
    assert.equal(vm.detailValue('department.name'), '—');
    assert.equal(vm.detailValue('contractTerm'), 0);
    assert.equal(vm.detailVisible, true);
});

test('home shortcuts exclude hidden menu groups and hidden children', () => {
    const vm = {
        $store: {
            state: {
                routes: [
                    { name: '隐藏', hidden: true, children: [{ path: '/secret' }] },
                    {
                        name: '员工',
                        children: [
                            { path: '/emp/basic', name: '基本资料' },
                            { path: '/emp/adv', name: '高级资料' },
                            { path: '/hidden', hidden: true },
                        ],
                    },
                ],
            },
        },
    };
    vm.visibleRoutes = home.computed.visibleRoutes.call(vm);
    const shortcuts = home.computed.shortcuts.call(vm);
    assert.deepEqual(
        shortcuts.map((item) => item.path),
        ['/emp/basic'],
    );
});

test('release status removes empty groups, unknown and disabled pages without granting permissions', () => {
    const menus = [
        { name: '员工', children: [{ path: '/emp/basic' }, { path: '/emp/adv' }] },
        { name: '人事', children: [{ path: '/per/mv' }] },
        { name: '系统', children: [{ path: '/sys/basic', enabled: false }, { path: '/unknown' }] },
    ];
    const snapshot = JSON.stringify(menus);
    const visible = getReadyMenuGroups(menus);
    assert.equal(visible.length, 1);
    assert.deepEqual(
        visible[0].children.map((item) => item.path),
        ['/emp/basic'],
    );
    assert.equal(JSON.stringify(menus), snapshot);
    assert.deepEqual(getReadyMenuGroups([]), []);
    assert.equal(isFeatureReady('/per/mv'), false);
    assert.equal(isFeatureReady('/unknown'), false);
});

function salaryEditor(overrides = {}) {
    return instance(salary, {
        getRequest: async (url) =>
            url.endsWith('/salaries')
                ? [
                      { id: 1, name: '账套一' },
                      { id: 2, name: '账套二' },
                  ]
                : { data: [], total: 0 },
        $message: { error: () => {} },
        ...overrides,
    });
}

test('cancelled or unchanged salary selection never submits a mutation', async () => {
    let calls = 0;
    const employee = { id: 10, name: '测试员工', salary: { id: 1 } };
    const vm = salaryEditor({
        putRequest: async () => {
            calls++;
        },
    });
    vm.openSalaryEditor(employee);
    await new Promise((resolve) => setImmediate(resolve));
    await vm.saveSalary();
    vm.currentSalary = 2;
    vm.cancelSalaryEdit();
    await vm.saveSalary();
    assert.equal(calls, 0);
    assert.equal(employee.salary.id, 1);
    assert.equal(vm.salaryDialogVisible, false);
    assert.equal(vm.editingEmployee, null);
});

test('salary save targets the selected employee once and prevents closing during submission', async () => {
    let resolveRequest;
    const calls = [];
    const pending = new Promise((resolve) => {
        resolveRequest = resolve;
    });
    const vm = salaryEditor({
        putRequest: (url) => {
            calls.push(url);
            return pending;
        },
    });
    vm.openSalaryEditor({ id: 10, name: '测试员工', salary: { id: 1 } });
    await new Promise((resolve) => setImmediate(resolve));
    vm.currentSalary = 2;
    const saving = vm.saveSalary();
    await vm.saveSalary();
    vm.cancelSalaryEdit();
    assert.equal(vm.salaryDialogVisible, true);
    assert.deepEqual(calls, ['/salary/sobcfg/?eid=10&sid=2']);
    resolveRequest({ status: 200 });
    await saving;
    assert.equal(vm.salaryDialogVisible, false);
    assert.equal(vm.savingSalary, false);
});

test('salary business failure preserves the selection for a deliberate retry', async () => {
    const vm = salaryEditor({ putRequest: async () => ({ status: 500, msg: '该账套已停用' }) });
    vm.openSalaryEditor({ id: 20, salary: null });
    await new Promise((resolve) => setImmediate(resolve));
    vm.currentSalary = 2;
    await vm.saveSalary();
    assert.equal(vm.salaryDialogVisible, true);
    assert.equal(vm.currentSalary, 2);
    assert.equal(vm.salaryError, '该账套已停用');
    assert.equal(vm.savingSalary, false);
    assert.equal(vm.canSaveSalary, true);
});

test('salary connection failure releases the button and does not claim success', async () => {
    const vm = salaryEditor({
        putRequest: async () => {
            throw new Error('offline');
        },
    });
    vm.openSalaryEditor({ id: 20, salary: { id: 1 } });
    await new Promise((resolve) => setImmediate(resolve));
    vm.currentSalary = 2;
    await vm.saveSalary();
    assert.equal(vm.salaryDialogVisible, true);
    assert.equal(vm.savingSalary, false);
    assert.match(vm.salaryError, /未确认/);
});

test('invalid import files are rejected before upload and a valid file locks duplicate uploads', () => {
    const vm = instance(employee);
    for (const file of [
        { name: '员工.xlsx', size: 10 },
        { name: '空表.xls', size: 0 },
        { name: '大表.xls', size: 1024 * 1024 + 1 },
    ]) {
        assert.equal(vm.beforeUpload(file), false);
        assert.equal(vm.importDataDisabled, false);
        assert.equal(vm.importResult.type, 'error');
    }
    assert.equal(vm.beforeUpload({ name: '员工.XLS', size: 1024 }), true);
    assert.equal(vm.importDataDisabled, true);
    assert.equal(vm.beforeUpload({ name: '另一批.xls', size: 100 }), false);
});

test('HTTP success with an import business error is not treated as a successful import', () => {
    let reloads = 0;
    const vm = instance(employee);
    vm.initEmps = () => {
        reloads++;
    };
    vm.beforeUpload({ name: '员工.xls', size: 1024 });
    vm.onSuccess({ status: 500, msg: '工号重复' });
    assert.equal(vm.importResult.type, 'error');
    assert.equal(vm.importResult.description, '工号重复');
    assert.equal(vm.importDataDisabled, false);
    assert.equal(reloads, 0);
    vm.onSuccess('<html>登录页</html>');
    assert.equal(vm.importResult.type, 'error');
    assert.equal(reloads, 0);
});

test('confirmed import success refreshes the employee list and shows server feedback', async () => {
    let reloads = 0;
    const vm = instance(employee);
    vm.initEmps = async () => {
        reloads++;
    };
    vm.beforeUpload({ name: '员工.xls', size: 100 });
    await vm.onSuccess({ status: 200, msg: '上传成功' }, { name: '员工.xls' });
    assert.equal(reloads, 1);
    assert.equal(vm.importResult.type, 'success');
    assert.match(vm.importResult.description, /员工.xls/);
    assert.equal(vm.importDataDisabled, false);
});

test('import HTTP failures restore upload controls and distinguish auth and uncertain results', () => {
    const vm = instance(employee);
    for (const [error, message] of [
        [{ status: 401 }, /登录已失效/],
        [{ status: 403 }, /权限/],
        [{ status: 413 }, /大小/],
        [{ status: 500, message: '{"msg":"数据格式错误"}' }, /数据格式错误/],
        [{ status: 0, message: '<html>bad gateway</html>' }, /确认是否已导入/],
    ]) {
        vm.beforeUpload({ name: '员工.xls', size: 100 });
        vm.onError(error);
        assert.equal(vm.importDataDisabled, false);
        assert.equal(vm.importResult.type, 'error');
        assert.match(vm.importResult.description, message);
    }
});

test('failed login refreshes captcha, releases loading and blocks duplicate submissions', async () => {
    let complete,
        resolveLogin,
        calls = 0,
        refreshes = 0;
    const pending = new Promise((resolve) => {
        resolveLogin = resolve;
    });
    const vm = instance(login, {
        $refs: {
            loginForm: {
                validate: (callback) => {
                    complete = callback(true);
                },
            },
        },
        postRequest: () => {
            calls++;
            return pending;
        },
    });
    vm.updateVerifyCode = () => {
        refreshes++;
    };
    vm.submitLogin();
    vm.submitLogin();
    assert.equal(calls, 1);
    assert.equal(vm.loading, true);
    resolveLogin(undefined);
    await complete;
    assert.equal(vm.loading, false);
    assert.equal(refreshes, 1);
});
