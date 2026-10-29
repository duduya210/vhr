import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parse } from '@vue/compiler-sfc';
import { money, requestId } from '../src/utils/ecUi.js';
import { getReadyMenuGroups } from '../src/utils/featureAvailability.js';
async function component(path) {
    const url = new URL('../src/' + path, import.meta.url);
    const { descriptor } = parse(await readFile(url, 'utf8'));
    const code = descriptor.script.content.replace(
        /from\s+(['"])(\.[^'"]+)\1/g,
        (_, quote, path) => 'from ' + quote + new URL(path, url).href + quote,
    );
    return (await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'))).default;
}
const rules = await component('components/sys/basic/EcMana.vue');
const records = await component('views/per/PerEc.vue');
function instance(component) {
    const vm = component.data();
    for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
    vm.$nextTick = (callback) => callback();
    vm.$confirm = async () => {};
    vm.$message = { success: () => {}, error: () => {} };
    vm.$refs = {
        ruleForm: { validate: async () => true },
        recordForm: { validate: async () => true },
        cancelForm: { validate: async () => true },
    };
    for (const form of Object.values(vm.$refs)) form.clearValidate = () => {};
    return vm;
}
function editor() {
    const vm = instance(records);
    vm.visible = true;
    vm.selectedEmployee = { id: 10, name: '测试员工' };
    vm.selectedRule = { id: 2, name: '贡献奖', version: 3, type: 0, amount: 100 };
    vm.form = { employeeId: 10, ruleId: 2, date: '2026-10-01', amount: 123.45, reason: '项目贡献', remark: '' };
    return vm;
}
test('rule editing copies its row and cancellation does not mutate saved data', () => {
    const vm = instance(rules);
    const row = { id: 1, name: '贡献奖', type: 0, amount: 100, description: '说明', version: 4, enabled: true };
    vm.openEditor(row);
    vm.form.name = '未保存';
    vm.form.enabled = false;
    vm.visible = false;
    assert.equal(row.name, '贡献奖');
    assert.equal(row.enabled, true);
    vm.openEditor(row);
    assert.equal(vm.form.name, '贡献奖');
    assert.equal(vm.form.version, 4);
});
test('rule save includes optimistic version and uncertain retry preserves exact URL and payload', async () => {
    const vm = instance(rules);
    vm.openEditor({ id: 3, name: '贡献奖', type: 0, amount: 100, description: '', version: 4, enabled: true });
    const calls = [];
    let confirmed = 0;
    vm.$confirm = async () => {
        confirmed++;
    };
    vm.request = async (path, options) => {
        if (!options) return { data: [], total: 0 };
        calls.push({ path, ...options });
        if (calls.length === 1) throw new Error('network failure');
        return { status: 200 };
    };
    await vm.save();
    assert.equal(vm.mutation.uncertain, true);
    vm.openEditor();
    assert.equal(vm.editingId, 3);
    await vm.save();
    assert.deepEqual(calls[0], calls[1]);
    assert.equal(calls[0].method, 'PUT');
    assert.equal(JSON.parse(calls[0].body).version, 4);
    assert.equal(confirmed, 1);
    assert.equal(vm.visible, false);
});
test('register uses actual amount and rule version, blocks duplicates and refreshes on success', async () => {
    const vm = editor();
    let finish;
    const calls = [];
    vm.request = async (path, options) => {
        if (!options) return { data: [], total: 1 };
        calls.push({ path, ...options });
        return new Promise((resolve) => {
            finish = resolve;
        });
    };
    const saving = vm.save();
    await new Promise((resolve) => setImmediate(resolve));
    await vm.save();
    assert.equal(calls.length, 1);
    const body = JSON.parse(calls[0].body);
    assert.equal(body.amount, 123.45);
    assert.equal(body.expectedRuleVersion, 3);
    assert.equal(body.employeeId, 10);
    assert.equal('operatorId' in body, false);
    finish({ status: 200 });
    await saving;
    assert.equal(vm.mutation.saving, false);
    assert.equal(vm.visible, false);
});
test('cancelled confirmation or invalid form never writes', async () => {
    for (const component of [records, rules]) {
        for (const invalid of [true, false]) {
            const vm = component === records ? editor() : instance(rules);
            let calls = 0;
            vm.request = async () => {
                calls++;
            };
            if (invalid)
                for (const ref of Object.values(vm.$refs))
                    ref.validate = async () => {
                        throw new Error('invalid');
                    };
            else
                vm.$confirm = async () => {
                    throw 'cancel';
                };
            await vm.save();
            assert.equal(calls, 0);
            assert.equal(vm.mutation.saving, false);
            assert.equal(vm.mutation.pending, null);
        }
    }
});
test('uncertain cancellation keeps its original target and reason when another action is clicked', async () => {
    const vm = editor();
    vm.openCancel({ id: 9, employeeName: '张三', ecType: 0, amount: 20 });
    vm.cancelForm.reason = '重复登记';
    const calls = [];
    vm.request = async (path, options) => {
        if (!options) return { data: [], total: 0 };
        calls.push({ path, ...options });
        if (calls.length === 1) throw Object.assign(new Error('gateway timeout'), { status: 504 });
        return { status: 200 };
    };
    await vm.save();
    vm.openRegister();
    vm.openCancel({ id: 10 });
    assert.equal(vm.mode, 'cancel');
    assert.equal(vm.cancelTarget.id, 9);
    await vm.save();
    assert.deepEqual(calls[0], calls[1]);
    assert.equal(calls[0].path, '/personnel/ec/9/cancel');
    assert.equal(JSON.parse(calls[0].body).reason, '重复登记');
});
test('definite server rejection preserves fields and allows correction', async () => {
    const vm = editor();
    vm.request = async () => {
        throw Object.assign(new Error('规则已被修改'), { status: 400 });
    };
    await vm.save();
    assert.equal(vm.mutation.uncertain, false);
    assert.equal(vm.mutation.pending, null);
    assert.equal(vm.form.amount, 123.45);
    assert.equal(vm.visible, true);
    assert.equal(vm.mutation.error, '规则已被修改');
});
test('record filters preserve reward type zero and dates across paging, then reset', async () => {
    const vm = instance(records);
    const calls = [];
    vm.request = async (url) => {
        calls.push(new URL(url, 'http://test.local'));
        return { data: [], total: 100 };
    };
    vm.filters = { keyword: '张 & 李', type: 0, status: 'CANCELLED' };
    vm.dateRange = ['2026-01-01', '2026-10-01'];
    await vm.search();
    vm.filters.keyword = '尚未查询';
    await vm.changePage(2);
    await vm.changeSize(20);
    for (const url of calls) {
        assert.equal(url.searchParams.get('keyword'), '张 & 李');
        assert.equal(url.searchParams.get('type'), '0');
        assert.equal(url.searchParams.get('endDate'), '2026-10-01');
    }
    assert.equal(calls[1].searchParams.get('page'), '2');
    assert.equal(calls[2].searchParams.get('size'), '20');
    await vm.reset();
    assert.equal(calls[3].searchParams.has('type'), false);
    assert.equal(calls[3].searchParams.has('endDate'), false);
});
test('rule filtering preserves disabled false and handles load errors', async () => {
    const vm = instance(rules);
    const calls = [];
    vm.request = async (url) => {
        calls.push(new URL(url, 'http://test.local'));
        return { data: [], total: 10 };
    };
    vm.filters = { keyword: '测试', type: 1, enabled: false };
    await vm.search();
    await vm.changePage(2);
    assert.equal(calls[1].searchParams.get('enabled'), 'false');
    vm.request = async () => {
        throw new Error('offline');
    };
    await vm.load();
    assert.equal(vm.loading, false);
    assert.equal(vm.error, 'offline');
});
test('stale rule search cannot replace a newer result and choosing rule sets its default amount', async () => {
    const vm = editor();
    let resolve;
    vm.request = (url) =>
        url.endsWith('first')
            ? new Promise((done) => {
                  resolve = done;
              })
            : Promise.resolve({ data: [{ id: 5, version: 2, amount: 88, type: 1 }] });
    const first = vm.searchRules('first');
    await vm.searchRules('second');
    resolve({ data: [{ id: 6, amount: 22 }] });
    await first;
    vm.selectRule(5);
    assert.equal(vm.form.amount, 88);
    assert.equal(vm.selectedRule.version, 2);
    assert.equal(
        vm.ruleOptions.some((row) => row.id === 6),
        false,
    );
});
test('legacy missing amount is not displayed as zero and feature availability never grants menus', () => {
    assert.equal(money(null), '历史未记录');
    assert.equal(money(0), '0.00');
    assert.deepEqual(getReadyMenuGroups([]), []);
    assert.equal(getReadyMenuGroups([{ children: [{ path: '/per/ec' }] }])[0].children[0].path, '/per/ec');
    assert.match(requestId(), /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/);
});
