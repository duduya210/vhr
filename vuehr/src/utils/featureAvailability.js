// UI release status only. A feature still needs to be present in the user's
// server-provided menu; this list does not grant any backend permissions.
export const featureAvailability = Object.freeze({
    '/emp/basic': true,
    '/emp/adv': false,
    '/per/emp': false,
    '/per/ec': true,
    '/per/train': false,
    '/per/salary': false,
    '/per/mv': true,
    '/sal/sob': true,
    '/sal/sobcfg': true,
    '/sal/table': false,
    '/sal/month': false,
    '/sal/search': false,
    '/sta/all': false,
    '/sta/score': false,
    '/sta/pers': false,
    '/sta/record': false,
    '/sys/basic': true,
    '/sys/cfg': false,
    '/sys/log': true,
    '/sys/hr': true,
    '/sys/data': false,
    '/sys/init': false,
});

export function isFeatureReady(path) {
    return featureAvailability[path] === true;
}

export function getReadyMenuGroups(routes = []) {
    return routes
        .filter((item) => !item.hidden && item.enabled !== false)
        .map((item) => ({
            ...item,
            children: (item.children || []).filter(
                (child) => !child.hidden && child.enabled !== false && isFeatureReady(child.path),
            ),
        }))
        .filter((item) => item.children.length > 0);
}
