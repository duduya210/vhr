import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parse } from '@vue/compiler-sfc';
import { transferRequest } from '../src/utils/transferApi.js';

async function component(file) {
    const url = new URL(`../src/views/${file}`, import.meta.url);
    const { descriptor } = parse(await readFile(url, 'utf8'));
    const script = descriptor.script.content.replace(
        /from\s+(['"])(\.[^'"]+)\1/g,
        (_, quote, path) => `from ${quote}${new URL(path, url).href}${quote}`,
    );
    return (await import(`data:text/javascript;base64,${Buffer.from(script).toString('base64')}`)).default;
}
const transfer = await component('per/PerMv.vue');
const log = await component('sys/SysLog.vue');
function instance(component) {
    const vm = component.data();
    for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
    return vm;
}
function editor() {
    const vm = instance(transfer);
    vm.dialogVisible = true;
    vm.selectedEmployee = { id: 10, name: '测试员工', departmentId: 1, positionId: 1 };
    vm.form = { employeeId: 10, departmentId: 2, positionId: 2, reason: ' 岗位调整 ', remark: ' 备注 ' };
    vm.departments = [{ id: 2, name: '研发部', enabled: true }];
    vm.positions = [{ id: 2, name: '工程师', enabled: true }];
    vm.$refs = { transferForm: { validate: async () => true } };
    vm.$confirm = async () => {};
    vm.$message = { success: () => {}, error: () => {} };
    return vm;
}

test('transfer submits snapshots and a request ID once, blocks duplicate clicks, then reloads', async () => {
    const vm = editor();
    const calls = [];
    let finish;
    vm.request = async (url, options) => {
        calls.push({ url, options });
        return options
            ? new Promise((resolve) => {
                  finish = resolve;
              })
            : { data: [], total: 1 };
    };
    const saving = vm.submitTransfer();
    await new Promise((resolve) => setImmediate(resolve));
    await vm.submitTransfer();
    assert.equal(calls.length, 1);
    const command = JSON.parse(calls[0].options.body);
    assert.equal(command.employeeId, 10);
    assert.equal(command.expectedDepartmentId, 1);
    assert.equal(command.expectedPositionId, 1);
    assert.equal(command.departmentId, 2);
    assert.equal(command.reason, '岗位调整');
    assert.equal(command.remark, '备注');
    assert.match(command.requestId, /^[a-f0-9-]{36}$/);
    assert.equal('operatorId' in command, false);
    finish({ status: 200 });
    await saving;
    assert.equal(calls.length, 2);
    assert.equal(vm.dialogVisible, false);
    assert.equal(vm.saving, false);
    assert.equal(vm.pendingCommand, null);
});

test('uncertain transfer retries the exact payload without creating another request ID', async () => {
    const vm = editor();
    const bodies = [];
    let confirmations = 0;
    vm.$confirm = async () => {
        confirmations++;
    };
    vm.request = async (_, options) => {
        if (!options) return { data: [], total: 1 };
        bodies.push(options.body);
        if (bodies.length === 1) throw new Error('network disconnected after server commit');
        return { status: 200 };
    };
    await vm.submitTransfer();
    assert.equal(vm.uncertain, true);
    assert.equal(vm.dialogVisible, true);
    assert.equal(vm.saving, false);
    assert.match(vm.submitError, /重试确认结果/);
    await vm.submitTransfer();
    assert.equal(bodies[0], bodies[1]);
    assert.equal(confirmations, 1);
    assert.equal(vm.uncertain, false);
});

test('cancel, invalid fields, unchanged assignment and disabled target never write', async () => {
    for (const scenario of ['cancel', 'invalid', 'unchanged', 'disabled']) {
        const vm = editor();
        let requests = 0;
        vm.request = async () => {
            requests++;
        };
        if (scenario === 'cancel')
            vm.$confirm = async () => {
                throw 'cancel';
            };
        if (scenario === 'invalid')
            vm.$refs.transferForm.validate = async () => {
                throw new Error('invalid');
            };
        if (scenario === 'unchanged') {
            vm.form.departmentId = 1;
            vm.form.positionId = 1;
        }
        if (scenario === 'disabled') vm.departments[0].enabled = false;
        await vm.submitTransfer();
        assert.equal(requests, 0, scenario);
        assert.equal(vm.saving, false, scenario);
        assert.equal(vm.pendingCommand, null, scenario);
    }
});

test('rejected transfer retains form fields for correction and releases uncertainty', async () => {
    const vm = editor();
    vm.request = async () => {
        throw Object.assign(new Error('员工部门已被修改'), { status: 400 });
    };
    await vm.submitTransfer();
    assert.equal(vm.form.departmentId, 2);
    assert.equal(vm.uncertain, false);
    assert.equal(vm.pendingCommand, null);
    assert.equal(vm.dialogVisible, true);
    assert.equal(vm.submitError, '员工部门已被修改');
});

test('transfer paging uses submitted filters, reset clears them and load failure is recoverable', async () => {
    const vm = instance(transfer);
    const requests = [];
    vm.request = async (url) => {
        requests.push(new URL(url, 'http://test.local'));
        return { data: [], total: 100 };
    };
    vm.filters = { keyword: '张 & 李', departmentId: 2 };
    vm.dateRange = ['2026-10-01', '2026-10-30'];
    await vm.search();
    vm.filters.keyword = '尚未查询';
    await vm.changePage(2);
    await vm.changeSize(20);
    for (const url of requests) {
        assert.equal(url.searchParams.get('keyword'), '张 & 李');
        assert.equal(url.searchParams.get('departmentId'), '2');
        assert.equal(url.searchParams.get('endDate'), '2026-10-30');
    }
    assert.equal(requests[1].searchParams.get('page'), '2');
    assert.equal(requests[2].searchParams.get('page'), '1');
    await vm.resetFilters();
    assert.equal(requests[3].searchParams.has('departmentId'), false);
    assert.equal(requests[3].searchParams.has('startDate'), false);
    vm.request = async () => {
        throw new Error('offline');
    };
    await vm.loadRecords();
    assert.equal(vm.loading, false);
    assert.equal(vm.listError, 'offline');
    vm.request = async () => ({ data: [{ id: 3 }], total: 1 });
    await vm.loadRecords();
    assert.equal(vm.listError, '');
    assert.equal(vm.records[0].id, 3);
});

test('late employee search response does not overwrite the latest options', async () => {
    const vm = editor();
    let completeFirst;
    vm.request = (url) =>
        url.endsWith('first')
            ? new Promise((resolve) => {
                  completeFirst = resolve;
              })
            : Promise.resolve([{ id: 20 }]);
    const first = vm.searchEmployees('first');
    await vm.searchEmployees('second');
    completeFirst([{ id: 30 }]);
    await first;
    assert.deepEqual(
        vm.employeeOptions.map((row) => row.id),
        [10, 20],
    );
    assert.equal(vm.employeeLoading, false);
});

test('operation log preserves filters while paging and only issues read requests', async () => {
    const vm = instance(log);
    const calls = [];
    vm.request = async (...args) => {
        calls.push(args);
        return { data: [], total: 30 };
    };
    vm.filters = { keyword: '员工调动', operator: '管理员' };
    vm.dateRange = ['2026-10-01', '2026-10-30'];
    await vm.search();
    vm.filters.operator = '未查询';
    await vm.changePage(2);
    for (const args of calls) {
        assert.equal(args.length, 1);
        const url = new URL(args[0], 'http://test.local');
        assert.equal(url.pathname, '/system/log/');
        assert.equal(url.searchParams.get('operator'), '管理员');
        assert.equal(url.searchParams.get('startDate'), '2026-10-01');
    }
    await vm.reset();
    assert.equal(new URL(calls[2][0], 'http://test.local').searchParams.has('operator'), false);
    vm.request = async () => {
        throw new Error('没有权限');
    };
    await vm.load();
    assert.equal(vm.error, '没有权限');
    assert.equal(vm.loading, false);
});

test('transfer transport keeps HTTP failure status for deliberate retry handling', async () => {
    const original = globalThis.fetch;
    try {
        globalThis.fetch = async (_, options) => {
            assert.equal(options.credentials, 'same-origin');
            return { ok: false, status: 403, json: async () => ({ status: 500, msg: '没有权限' }) };
        };
        await assert.rejects(transferRequest('/test'), (error) => error.status === 403 && error.message === '没有权限');
        globalThis.fetch = async () => ({ ok: true, status: 200, json: async () => ({ status: 200 }) });
        assert.deepEqual(await transferRequest('/test'), { status: 200 });
    } finally {
        globalThis.fetch = original;
    }
});
