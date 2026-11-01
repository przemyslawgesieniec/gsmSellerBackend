import { copyFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const root = new URL('../src/main/resources/static/v2/assets/vendor/', import.meta.url);
await mkdir(root, { recursive: true });
for (const [source, target] of Object.entries({
  'tom-select/dist/js/tom-select.complete.min.js': 'tom-select.js',
  'tom-select/dist/css/tom-select.css': 'tom-select.css',
  'flatpickr/dist/flatpickr.min.js': 'flatpickr.js',
  'flatpickr/dist/l10n/pl.js': 'flatpickr-pl.js',
  'flatpickr/dist/flatpickr.min.css': 'flatpickr.css',
  'chart.js/dist/chart.umd.min.js': 'chart.js',
  'lucide/dist/umd/lucide.min.js': 'lucide.js',
  'heic2any/dist/heic2any.min.js': 'heic2any.js',
  'heic2any/LICENSE.md': 'heic2any.LICENSE',
  'tom-select/LICENSE': 'tom-select.LICENSE',
  'flatpickr/LICENSE.md': 'flatpickr.LICENSE',
  'chart.js/LICENSE.md': 'chart.LICENSE',
  'lucide/LICENSE': 'lucide.LICENSE'
})) await copyFile(new URL(`node_modules/${source}`, import.meta.url), new URL(target, root));
console.log(`Assets ready: ${fileURLToPath(root)}`);
