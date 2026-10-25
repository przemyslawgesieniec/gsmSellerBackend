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

async function openPhoneMenu(page) {
    await page.evaluate(() => {
        const trigger = document.querySelector('[data-target="dropdown-test-0"]');
        trigger.scrollIntoView();
        M.Dropdown.getInstance(trigger).open();
    });
    await page.waitForFunction(() => document.activeElement?.id === 'dropdown-test-0');
}

test('touching and swiping a menu item does not execute it; a completed tap does', async t => {
    const page = await fixture(t);
    await page.evaluate(() => loadStock());
    await openPhoneMenu(page);
    const action = page.locator('#dropdown-test-0 a[data-action="edit"]');
    const box = await action.boundingBox();
    const cdp = await page.context().newCDPSession(page);
    const point = { x: box.x + box.width / 2, y: box.y + box.height / 2 };
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
    assert.equal(await page.evaluate(() => M.Modal.getInstance(document.getElementById('editPhoneModal')).isOpen), false);
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ ...point, y: point.y - 70 }] });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
    assert.equal(await page.evaluate(() => M.Modal.getInstance(document.getElementById('editPhoneModal')).isOpen), false);
    await openPhoneMenu(page);
    await action.tap();
    assert.equal(await page.evaluate(() => M.Modal.getInstance(document.getElementById('editPhoneModal')).isOpen), true);
    assert.equal(await page.evaluate(() => M.Modal._modalsOpen), 1);
});

test('menu actions still work with keyboard and ignore disabled items', async t => {
    const page = await fixture(t);
    await page.evaluate(() => loadStock());
    await openPhoneMenu(page);
    await page.locator('#dropdown-test-0 a[data-action="edit"]').focus();
    await page.keyboard.press('Enter');
    assert.equal(await page.evaluate(() => M.Modal.getInstance(document.getElementById('editPhoneModal')).isOpen), true);
    const ignored = await page.evaluate(() => {
        const link = document.querySelector('#dropdown-test-0 a[data-action="remove-from-location"]');
        link.classList.add('disabled-link');
        let requests = 0;
        window.fetch = () => { requests++; return new Promise(() => {}); };
        link.click();
        return requests;
    });
    assert.equal(ignored, 0);
});

test('repeated refreshes release detached menus, tooltips and reservation timers', async t => {
    const page = await fixture(t);
    await page.evaluate(() => {
        window.testIntervals = new Map();
        let nextId = 0;
        window.setInterval = fn => { testIntervals.set(++nextId, fn); return nextId; };
        window.clearInterval = id => testIntervals.delete(id);
    });
    for (let i = 0; i < 10; i++) await page.evaluate(() => loadStock());
    const counts = await page.evaluate(() => ({
        cards: document.querySelectorAll('#phone-list .card').length,
        dropdowns: M.Dropdown._dropdowns.length,
        detached: M.Dropdown._dropdowns.filter(d => !d.el.isConnected).length,
        tooltips: document.querySelectorAll('.material-tooltip').length,
        timers: testIntervals.size
    }));
    assert.deepEqual(counts, { cards: 20, dropdowns: 20, detached: 0, tooltips: 40, timers: 20 });
    await openPhoneMenu(page);
    const empty = await page.evaluate(async () => {
        testPhones = [];
        await loadStock();
        return { dropdowns: M.Dropdown._dropdowns.length,
            tooltips: document.querySelectorAll('.material-tooltip').length, timers: testIntervals.size };
    });
    assert.deepEqual(empty, { dropdowns: 0, tooltips: 0, timers: 0 });
});

test('late reservation responses are aborted and cannot overwrite the next render', async t => {
    const page = await fixture(t);
    const result = await page.evaluate(async () => {
        const originalFetch = window.fetch;
        const pending = [];
        window.fetch = (url, options) => url.includes('/reservations/status/')
            ? new Promise(resolve => pending.push({ resolve, signal: options.signal }))
            : originalFetch(url, options);
        await loadStock();
        const stale = pending.splice(0);
        window.fetch = originalFetch;
        await loadStock();
        // Simulate a response that ignores abort (already buffered/in flight).
        for (const request of stale) request.resolve({ ok: true, json: async () => ({
            name: 'STALE', phoneNumber: 'OLD', expiryTime: new Date(Date.now() + 14400000).toISOString()
        }) });
        await new Promise(resolve => setTimeout(resolve, 0));
        return {
            aborted: stale.every(request => request.signal.aborted),
            tooltip: document.getElementById('reservation-test-0').getAttribute('data-tooltip'),
            timers: reservationIntervals.size
        };
    });
    assert.deepEqual(result, { aborted: true, tooltip: 'Rezerwujący: Test (123)', timers: 20 });
});
