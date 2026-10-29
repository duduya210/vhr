export const emptyMutation = () => ({ saving: false, uncertain: false, error: '', pending: null });
export const typeLabel = (value) => (value === 0 ? '奖励' : value === 1 ? '处罚' : '历史未记录');
export const money = (value) => (value === null || value === undefined ? '历史未记录' : Number(value).toFixed(2));
export function requestId() {
    if (globalThis.crypto.randomUUID) return globalThis.crypto.randomUUID();
    const bytes = globalThis.crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
    return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-');
}

// Store the exact URL, method and JSON until an uncertain write is confirmed.
export async function submitMutation(vm, prepare) {
    const state = vm.mutation;
    if (state.saving) return;
    state.saving = true;
    state.error = '';
    try {
        if (!state.uncertain) {
            const operation = await prepare();
            if (!operation) return;
            state.pending = {
                path: operation.path,
                method: operation.method || 'POST',
                body: JSON.stringify({ ...operation.payload, requestId: requestId() }),
            };
        }
        const pending = state.pending;
        const response = await vm.request(pending.path, { method: pending.method, body: pending.body });
        if (response?.status !== 200) throw new Error('服务器未确认操作结果');
        state.pending = null;
        state.uncertain = false;
        vm.$message.success(response.msg || '操作成功');
        await vm.onSaved();
    } catch (error) {
        if (error === 'cancel' || error === 'close') return;
        state.uncertain = !!state.pending && (!error.status || error.status >= 500);
        state.error = state.uncertain
            ? '结果尚未确认。请点击“重试确认结果”，系统将使用原请求核实，避免重复操作。'
            : error.message || '操作失败，请重试';
        if (!state.uncertain) state.pending = null;
    } finally {
        state.saving = false;
    }
}
