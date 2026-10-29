import {access, cp, mkdir, readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';

const dist = new URL('../dist/', import.meta.url);
const webModule = new URL('../../vhr/vhrserver/vhr-web/', import.meta.url);
const index = await readFile(new URL('index.html', dist), 'utf8');

// The Java service already permits /js/** before authentication.
if (!index.includes('/js/ui/')) {
    throw new Error('Missing /js/ui/ bundle references. Run npm run build before syncing.');
}

const targets = [new URL('src/main/resources/static/', webModule)];
try {
    await access(new URL('target/classes/', webModule));
    targets.push(new URL('target/classes/static/', webModule));
} catch {
    // Maven or the IDE will copy the source resources on its next build.
}

for (const target of targets) {
    await mkdir(target, {recursive: true});
    // Preserve existing unrelated static resources; only overwrite built files.
    await cp(fileURLToPath(dist), fileURLToPath(target), {recursive: true});
    console.log(`UI synced to ${fileURLToPath(target)}`);
}
