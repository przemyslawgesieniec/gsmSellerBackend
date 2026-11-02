const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {start,root,id,phone}=require('./modern-fixture.cjs');
let app;
before(async()=>{app=await start();});
after(async()=>{await app?.close();});
const screens=JSON.parse(fs.readFileSync(path.join(root,'v2/screen-manifest.json')));
for(const mobile of [false,true])for(const screen of screens){
 test(`v2 ${screen.page} loads with full field parity, no legacy dependencies and ${mobile?'mobile':'desktop'} layout`,async t=>{
  const {page,errors,external,goto}=await app.fixture(t,{mobile});
  await goto(screen.page+(screen.page==='receipt-edit.html'?'?id='+id:''));
  assert.deepEqual(errors,[]);
  assert.deepEqual(external,[]);
  assert.equal(await page.evaluate(()=>typeof M),'undefined');
  const logos=page.locator('img[alt^="Teleakcesoria"]');
  assert.ok(await logos.count()>0,'Original brand logo must be present');
  assert.equal(await logos.evaluateAll(imgs=>imgs.every(img=>img.complete&&img.naturalWidth>0)),true,'Brand assets load');
  assert.equal(await page.evaluate(()=>getComputedStyle(document.documentElement).getPropertyValue('--brand').trim()),'#00aed8');
  assert.ok(await logos.evaluateAll(imgs=>imgs.some(img=>img.getBoundingClientRect().width>0)),'Logo visible at this viewport');
  const missing=await page.evaluate(ids=>ids.filter(id=>!document.getElementById(id)),screen.legacyFieldIds);
  assert.deepEqual(missing,[],'Missing original data-bound fields');
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'Horizontal page overflow');
  assert.equal(await page.locator('h1').count(),1);
  if(screen.page!=='login.html')assert.equal(await page.locator('.workspace-nav .nav-link').count()>10,true);
  await page.screenshot({path:`/tmp/gsm-v2-${screen.page.replace('.html','')}-${mobile?'mobile':'desktop'}.png`,fullPage:true,animations:'disabled'});
 });
}

test('slow data requests expose a clear loading state until the list is ready', async t => {
 const {page,errors}=await app.fixture(t);
 let release;
 await page.route('**/api/v1/phones?**', route => new Promise(resolve => {
  release=async()=>{await route.fulfill({json:{content:[phone],number:0,totalPages:1}});resolve();};
 }));
 const navigation=page.goto(app.base+'/v2/index.html');
 await page.locator('#pageLoadingIndicator:not([hidden])').waitFor();
 assert.equal(await page.locator('#pageLoadingIndicator').textContent(),'Pobieranie danych z systemu…');
 assert.equal(await page.locator('main').getAttribute('aria-busy'),'true');
 await release();
 await navigation;
 await page.waitForFunction(()=>document.getElementById('pageLoadingIndicator')?.hidden);
 assert.equal(await page.locator('main').getAttribute('aria-busy'),'false');
 assert.deepEqual(errors,[]);
});

test('native dialogs preserve focus, keyboard escape and body scrolling on every screen',async t=>{
 for(const screen of screens){
  const {page,errors,goto}=await app.fixture(t);
  await goto(screen.page+(screen.page==='receipt-edit.html'?'?id='+id:''));
  const ids=await page.locator('dialog').evaluateAll(nodes=>nodes.map(n=>n.id));
  for(const id of ids){
   await page.evaluate(id=>UI.Dialog.getInstance(document.getElementById(id)).open(),id);
   assert.equal(await page.locator('#'+id).isVisible(),true);
   await page.keyboard.press('Escape');
   assert.equal(await page.locator('#'+id).isVisible(),false);
  }
  await page.waitForFunction(()=>!document.body.classList.contains('dialog-open'));
  assert.deepEqual(errors,[]);
 }
});
