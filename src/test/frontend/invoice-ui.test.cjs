const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const staticRoot = path.resolve(__dirname, '../../main/resources/static');
let browser;
before(async () => { browser = await chromium.launch({ headless: true }); });
after(async () => { await browser?.close(); });

async function fixture(t, { items = [], status = 200 } = {}) {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'pl-PL' });
    t.after(() => context.close());
    const page = await context.newPage();
    const posts = [];
    const html = fs.readFileSync(path.join(staticRoot, 'invoice.html'), 'utf8')
        .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '').replace(/<link\b[^>]*>/gi, '');
    await page.route('http://gsm.test/**', async route => {
        const url = new URL(route.request().url());
        if (url.pathname === '/api/v1/cart') return route.fulfill({ json: { items } });
        if (url.pathname === '/api/v1/receipt') {
            posts.push(route.request().postDataJSON());
            await new Promise(resolve => setTimeout(resolve, 100));
            return route.fulfill({ status, json: 'new-receipt-id' });
        }
        return route.fulfill({ contentType: 'text/html', body: html });
    });
    await page.goto('http://gsm.test/invoice.html');
    for (const file of ['css/materialize.css', 'css/style.css']) await page.addStyleTag({ path: path.join(staticRoot, file) });
    await page.addScriptTag({ path: path.join(staticRoot, 'js/materialize.js') });
    await page.evaluate(() => {
        // Exercise invoice logic without the CDN select enhancer.
        window.TomSelect = class {};
        document.getElementById('vatSelect').classList.add('browser-default');
        window.openedPdf = null;
        window.open = url => { window.openedPdf = url; };
    });
    await page.addScriptTag({ path: path.join(staticRoot, 'js/invoice.js') });
    await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
    await page.waitForFunction(() => !document.getElementById('invoiceFields').disabled);
    return { page, posts };
}

async function addItem(page, description, price) {
    await page.locator('#addManualItemBtn').click();
    const row = page.locator('.manual-invoice-item').last();
    await row.locator('.manual-description').fill(description);
    await row.locator('.manual-price').fill(price);
}

test('empty cart accepts manual items with correct VAT, clears after save and prevents duplicate saves', async t => {
    const { page, posts } = await fixture(t);
    await addItem(page, '  Usługa <montażu>  ', '200.50');
    await addItem(page, 'Etui', '12.10');
    await page.locator('#vatSelect').selectOption('23%');
    assert.equal(await page.locator('#totalNet').textContent(), '212.60');
    assert.equal(await page.locator('#totalVat').textContent(), '48.90');
    assert.equal(await page.locator('#invoiceTotal').textContent(), '261.50');
    await page.evaluate(() => { saveAndPrint(); saveAndPrint(); });
    await page.waitForFunction(() => window.openedPdf !== null);
    assert.equal(posts.length, 1);
    assert.deepEqual(posts[0].items.map(({ itemType, technicalId, description, price }) => ({ itemType, technicalId, description, price })), [
        { itemType: 'MISC', technicalId: null, description: 'Usługa <montażu>', price: 200.5 },
        { itemType: 'MISC', technicalId: null, description: 'Etui', price: 12.1 }
    ]);
    assert.equal(posts[0].vatRate, '23%');
    assert.equal(await page.evaluate(() => window.openedPdf), '/api/v1/receipt/new-receipt-id');
    assert.equal(await page.locator('.manual-invoice-item').count(), 0);
    assert.equal(await page.locator('#invoiceTotal').textContent(), '0.00');
    await page.locator('#saveInvoiceBtn').click();
    assert.equal(posts.length, 1);
});

test('mixed invoice preserves phone details through manual items and VAT changes', async t => {
    const { page, posts } = await fixture(t, { items: [{ technicalId: 'phone-1', itemType: 'PHONE', description: 'Telefon', price: 100 }] });
    await page.locator('#gw-phone-1-num').fill('12');
    await page.locator('#sellingInfo-phone-1').fill('Informacja o telefonie');
    await page.locator('label').filter({ has: page.locator('#used-phone-1') }).click();
    await page.locator('#customerNote').fill('Uwagi klienta');
    await addItem(page, 'Akcesorium', '25');
    await page.locator('#vatSelect').selectOption('8%');
    assert.equal(await page.locator('#gw-phone-1-num').inputValue(), '12');
    assert.equal(await page.locator('#invoiceTotal').textContent(), '135.00');
    await page.locator('#saveInvoiceBtn').click();
    await page.waitForFunction(() => window.openedPdf !== null);
    assert.equal(posts[0].items.length, 2);
    assert.equal(posts[0].items[0].warrantyMonths, 12);
    assert.equal(posts[0].items[0].used, true);
    assert.equal(posts[0].items[0].sellingInfo, 'Informacja o telefonie');
    assert.equal(posts[0].customerNote, 'Uwagi klienta');
    assert.match(await page.locator('#invoiceItems').textContent(), /Koszyk jest pusty/);
});

test('invalid or absent manual items cannot be submitted; removal updates totals and payload', async t => {
    const { page, posts } = await fixture(t);
    await page.locator('#saveInvoiceBtn').click();
    assert.equal(posts.length, 0);
    await addItem(page, '   ', '10');
    await page.locator('#saveInvoiceBtn').click();
    assert.equal(posts.length, 0);
    await page.locator('.manual-description').fill('Pozycja');
    for (const price of ['', '-1', '0', '1.001']) {
        await page.locator('.manual-price').fill(price);
        await page.locator('#saveInvoiceBtn').click();
        assert.equal(posts.length, 0);
    }
    await page.locator('.manual-price').fill('40');
    await addItem(page, 'Pozostaje', '10');
    await page.locator('.remove-manual-item').first().click();
    assert.equal(await page.locator('#totalNet').textContent(), '10.00');
    await page.locator('#saveInvoiceBtn').click();
    await page.waitForFunction(() => window.openedPdf !== null);
    assert.deepEqual(posts[0].items.map(item => item.description), ['Pozostaje']);
});

test('failed save preserves entered items for retry', async t => {
    const { page, posts } = await fixture(t, { status: 500 });
    await addItem(page, 'Naprawa', '80');
    await page.locator('#saveInvoiceBtn').click();
    await page.waitForFunction(() => document.querySelector('.toast')?.textContent.includes('Błąd zapisu'));
    assert.equal(await page.locator('.manual-description').inputValue(), 'Naprawa');
    assert.equal(await page.locator('.manual-price').inputValue(), '80');
    assert.equal(await page.locator('#saveInvoiceBtn').isEnabled(), true);
    assert.equal(posts.length, 1);
    assert.equal(await page.evaluate(() => window.openedPdf), null);
});

test('VAT rounds per item like PDF and manual fields fit on mobile', async t => {
    const { page } = await fixture(t);
    await addItem(page, 'Pozycja 1', '0.10');
    await addItem(page, 'Pozycja 2', '0.10');
    await page.locator('#vatSelect').selectOption('5%');
    assert.equal(await page.locator('#totalVat').textContent(), '0.00');
    assert.equal(await page.locator('#invoiceTotal').textContent(), '0.20');
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
    await page.screenshot({ path: '/tmp/gsm-manual-invoice-mobile.png', fullPage: true, animations: 'disabled' });
});
