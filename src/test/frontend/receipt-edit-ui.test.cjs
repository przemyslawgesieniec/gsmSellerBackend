const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const staticRoot = path.resolve(__dirname, '../../main/resources/static');
let browser;
before(async () => { browser = await chromium.launch({ headless: true }); });
after(async () => { await browser?.close(); });
const receipt = {
    technicalId: '00000000-0000-0000-0000-000000000001', number: '001/10/2026', status: 'AKTYWNA',
    dateAndPlace: { generateDate: '2026-10-25', sellDate: '2026-10-24' },
    items: [{ id: 1, name: 'Telefon <test>', nettAmount: 100, vatRate: 'VAT_EXEMPT', vatAmount: 0, grossAmount: 100 },
        { id: 2, name: 'Etui', nettAmount: 20, vatRate: 'VAT_23', vatAmount: 4.6, grossAmount: 24.6 }]
};

async function fixture(t, { admin = true, status = 200, mobile = false } = {}) {
    const context = await browser.newContext({ viewport: mobile ? { width: 390, height: 844 } : { width: 1280, height: 900 } });
    t.after(() => context.close());
    const page = await context.newPage();
    const posts = [];
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/*', async route => {
        const url = new URL(route.request().url());
        if (url.hostname !== 'gsm.test') {
            if (url.pathname.endsWith('materialize.min.js')) return route.fulfill({ path: path.join(staticRoot, 'js/materialize.js'), contentType: 'text/javascript' });
            if (url.pathname.endsWith('materialize.min.css')) return route.fulfill({ path: path.join(staticRoot, 'css/materialize.css'), contentType: 'text/css' });
            return route.fulfill({ body: '' });
        }
        if (url.pathname === '/api/v1/users/auth/me') return route.fulfill({ json: { role: admin ? 'ROLE_ADMIN' : 'ROLE_USER' } });
        if (url.pathname.startsWith('/api/v1/receipt/')) {
            if (route.request().method() === 'GET') return route.fulfill({ json: receipt });
            posts.push({ save: url.searchParams.get('save'), body: route.request().postDataJSON() });
            await new Promise(resolve => setTimeout(resolve, 100));
            return status === 200
                ? route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 test' })
                : route.fulfill({ status, json: { message: 'Nie udało się zapisać faktury' } });
        }
        if (url.pathname === '/navbar.html') await new Promise(resolve => setTimeout(resolve, 200));
        const file = path.join(staticRoot, url.pathname);
        const contentType = file.endsWith('.js') ? 'text/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html';
        return route.fulfill({ path: file, contentType });
    });
    await page.goto(`http://gsm.test/receipt-edit.html?id=${receipt.technicalId}`);
    await page.waitForFunction(() => !document.getElementById('editStatus').textContent.includes('Ładowanie'));
    return { page, posts, errors };
}

for (const save of [true, false]) {
    test(`editing dates, amount and VAT downloads PDF with save=${save}`, async t => {
        const { page, posts, errors } = await fixture(t);
        await page.locator('#generateDate').fill('2026-10-22');
        await page.locator('#sellDate').fill('2026-10-21');
        await page.locator('#amount-0').fill('200.50');
        await page.locator('#vat-0').selectOption('23%');
        assert.equal(await page.locator('#totalVat').textContent(), '50,72');
        assert.equal(await page.locator('#totalGross').textContent(), '271,22');
        assert.equal(await page.locator('#editItems h6').first().textContent(), '1. Telefon <test>');
        const download = page.waitForEvent('download');
        await page.locator(`button[value="${save ? 'save' : 'download'}"]`).click();
        assert.equal((await download).suggestedFilename(), 'faktura_001_10_2026.pdf');
        await page.waitForFunction(() => !document.getElementById('editFields').disabled);
        assert.equal(posts.length, 1);
        assert.equal(posts[0].save, String(save));
        assert.deepEqual(posts[0].body, {
            generateDate: '2026-10-22', sellDate: '2026-10-21',
            items: [{ id: 1, nettAmount: '200.50', vatRate: '23%' }, { id: 2, nettAmount: '20.00', vatRate: '23%' }]
        });
        assert.match(await page.locator('#editStatus').textContent(), save ? /Zmiany zapisane/ : /nie zostały zapisane/);
        assert.deepEqual(errors, []);
    });
}

test('employee cannot access the editor', async t => {
    const { page, posts } = await fixture(t, { admin: false });
    assert.equal(await page.locator('#receiptEditForm').isVisible(), false);
    assert.match(await page.locator('#editStatus').textContent(), /tylko dla administratora/);
    assert.equal(posts.length, 0);
});

test('failure preserves edits for retry and rapid submit sends only one request', async t => {
    const { page, posts } = await fixture(t, { status: 500 });
    await page.locator('#amount-0').fill('321.00');
    await page.evaluate(() => {
        const button = document.querySelector('button[value="save"]');
        button.click();
        document.getElementById('receiptEditForm').dispatchEvent(new SubmitEvent('submit', { cancelable: true, submitter: button }));
    });
    await page.waitForFunction(() => document.getElementById('editStatus').textContent.includes('Nie udało'));
    assert.equal(posts.length, 1);
    assert.equal(await page.locator('#amount-0').inputValue(), '321.00');
    assert.equal(await page.locator('button[value="save"]').isEnabled(), true);
});

test('invalid amount prevents sending changes', async t => {
    const { page, posts } = await fixture(t);
    for (const value of ['-1', '1.001', '']) {
        await page.locator('#amount-0').fill(value);
        await page.locator('button[value="save"]').click();
        assert.equal(posts.length, 0);
    }
});

test('shared navigation and form fit a mobile screen', async t => {
    const { page, errors } = await fixture(t, { mobile: true });
    await page.locator('#navbar nav').waitFor();
    await page.waitForFunction(() => document.querySelector('#navbar .admin-only:not(.hide)'));
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
    for (const selector of ['#amount-0', '#vat-0', 'button[value="save"]', 'button[value="download"]']) {
        const box = await page.locator(selector).boundingBox();
        assert.ok(box.x >= 0 && box.x + box.width <= 390);
    }
    await page.screenshot({ path: '/tmp/gsm-receipt-edit-mobile.png', fullPage: true });
    assert.deepEqual(errors, []);
});

for (const admin of [true, false]) {
    test(`receipt list exposes edit link only for active invoices and administrators (admin=${admin})`, async t => {
        const context = await browser.newContext();
        t.after(() => context.close());
        const page = await context.newPage();
        const html = fs.readFileSync(path.join(staticRoot, 'receipts.html'), 'utf8')
            .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '').replace(/<link\b[^>]*>/gi, '');
        await page.setContent(html);
        await page.addScriptTag({ path: path.join(staticRoot, 'js/materialize.js') });
        await page.evaluate(admin => { window.IS_ADMIN = admin; }, admin);
        await page.addScriptTag({ path: path.join(staticRoot, 'js/receipts.js') });
        await page.evaluate(receipt => renderReceipts({ content: [receipt, { ...receipt, status: 'WYCOFANA' }], last: true }), receipt);
        assert.equal(await page.locator('a[href^="receipt-edit.html"]').count(), admin ? 1 : 0);
        assert.match(await page.locator('.collapsible-header').first().textContent(), /Wystawiono: 25.10.2026/);
    });
}
