const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const staticRoot = path.resolve(__dirname, '../../main/resources/static');
let browser;
before(async () => { browser = await chromium.launch({ headless: true }); });
after(async () => { await browser?.close(); });

async function fixture(t) {
    const context = await browser.newContext({
        viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true
    });
    t.after(() => context.close());
    const page = await context.newPage();
    const html = fs.readFileSync(path.join(staticRoot, 'index.html'), 'utf8')
        .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
        .replace(/<link\b[^>]*>/gi, '');
    await page.route('http://gsm.test/**', route => route.fulfill({ contentType: 'text/html', body: html }));
    await page.goto('http://gsm.test/index.html');
    for (const file of ['css/materialize.css', 'css/style.css']) {
        await page.addStyleTag({ path: path.join(staticRoot, file) });
    }
    await page.addScriptTag({ path: path.join(staticRoot, 'js/materialize.js') });
    await page.evaluate(() => { window.IS_ADMIN = false; window.cartState = { items: [] }; });
    await page.addScriptTag({ path: path.join(staticRoot, 'js/main.js') });
    await page.evaluate(() => {
        initStockModals();
        window.testPhones = Array.from({ length: 20 }, (_, i) => ({
            technicalId: `test-${i}`, name: 'Test phone', model: 'Test',
            status: 'DOSTĘPNY', locationName: 'Shop', purchaseType: 'VAT_23',
            sellingPrice: 100, isReserved: true
        }));
        window.fetch = async url => ({ ok: true, json: async () =>
            url.startsWith('/api/v1/phones?')
                ? { content: testPhones, number: 0, totalPages: 1 }
                : { name: 'Test', phoneNumber: '123', expiryTime: new Date(Date.now() + 14400000).toISOString() }
        });
    });
    return page;
}

test('refresh arriving while a modal is open preserves its instance and releases scrolling on close', async t => {
    const page = await fixture(t);
    const state = await page.evaluate(async () => {
        await loadStock();
        const el = document.getElementById('editPhoneModal');
        const original = M.Modal.getInstance(el);
        original.open();
        await refreshStockListPreservingState();
        initStockModals();
        const sameInstance = original === M.Modal.getInstance(el);
        M.Modal.getInstance(el).close();
        return { sameInstance, overflow: document.body.style.overflow, openCount: M.Modal._modalsOpen };
    });
    assert.deepEqual(state, { sameInstance: true, overflow: '', openCount: 0 });
    await page.waitForFunction(() => document.querySelectorAll('.modal-overlay').length === 0);
});
