// Keep HTTP errors visible to the transfer form so an uncertain submission can
// be retried with exactly the same idempotency key and payload.
export async function transferRequest(path, options = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 20000);
    try {
        const response = await fetch(path, {
            credentials: 'same-origin',
            ...options,
            signal: controller.signal,
            headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...options.headers },
        });
        const data = await response.json().catch(() => null);
        if (!response.ok || data?.status === 500) {
            const message =
                response.status === 401
                    ? '登录已失效，请重新登录'
                    : response.status === 403
                      ? '没有该功能的操作权限'
                      : '请求失败，请稍后重试';
            const error = new Error(data?.msg || message);
            error.status = response.ok ? 500 : response.status;
            throw error;
        }
        if (data === null) throw new Error('服务器响应异常，请刷新记录核实后重试');
        return data;
    } finally {
        clearTimeout(timer);
    }
}

export function queryString(filters, page, size) {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    for (const [key, value] of Object.entries(filters)) {
        if (value !== null && value !== undefined && value !== '') query.set(key, value);
    }
    return query.toString();
}

export function displayTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 19) : '历史未记录';
}
export function assignment(name, id) {
    return name || (id != null ? `编号 ${id}（历史名称未记录）` : '未记录');
}
