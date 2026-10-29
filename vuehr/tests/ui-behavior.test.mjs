import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parse } from '@vue/compiler-sfc';

// Exercise the real Options API methods without a browser or live database.
async function component(file) {
    const source = await readFile(new URL(`../src/views/${file}`, import.meta.url), 'utf8');
    const { descriptor } = parse(source);
    return (await import(`data:text/javascript;base64,${Buffer.from(descriptor.script.content).toString('base64')}`))
        .default;
}
function instance(component, overrides = {}) {
    const vm = Object.assign(component.data(), overrides);
    for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
    return vm;
}
const employee = await component('emp/EmpBasic.vue');
const login = await component('Login.vue');
const home = await component('Home.vue');

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
