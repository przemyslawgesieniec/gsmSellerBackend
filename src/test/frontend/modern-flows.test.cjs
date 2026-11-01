const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const {start,id,phone}=require('./modern-fixture.cjs');
let app;before(async()=>app=await start());after(async()=>await app?.close());
async function waitRequest(page,requests,path,method='POST'){
 await page.waitForFunction(()=>true);
 for(let i=0;i<50;i++){const r=requests.find(r=>r.path===path&&r.method===method);if(r)return {...r,json:()=>JSON.parse(r.body)};await new Promise(r=>setTimeout(r,40));}
 assert.fail('Missing '+method+' '+path);
}

test('stock menus edit, reserve, assign a model and sell through existing API',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('index.html');
 await page.locator(`[data-target="dropdown-${id}"]`).click();
 await page.locator(`#dropdown-${id} [data-action=edit]`).click();
 await page.locator('#editName').fill('Poprawiony telefon');
 await page.locator('#saveEditBtn').click();
 const edit=await waitRequest(page,requests,'/api/v1/phones/'+id,'PATCH');assert.equal(edit.json().name,'Poprawiony telefon');
 await page.waitForFunction(()=>!document.getElementById('editPhoneModal').open);
 await page.locator(`[data-target="dropdown-${id}"]`).click();await page.locator(`#dropdown-${id} [data-action=reserve]`).click();
 await page.locator('#reservationName').fill('Jan');await page.locator('#reservationPhone').fill('500600700');await page.locator('#confirmReservationBtn').click();
 assert.equal((await waitRequest(page,requests,'/api/v1/external/reservations')).json().name,'Jan');
 await page.waitForFunction(()=>!document.getElementById('reservationModal').open);
 await page.locator(`[data-target="dropdown-${id}"]`).click();await page.locator(`#dropdown-${id} [data-action=assign-model]`).click();
 await page.locator('#assignModelModal').waitFor({state:'visible'});await page.keyboard.press('Escape');
 await page.locator('#sellBtn'+id).click();await waitRequest(page,requests,'/api/v1/cart/add');
 await page.locator('#cartBtn').click();await page.locator('#cartModal').waitFor({state:'visible'});
 assert.deepEqual(errors,[]);
});

test('manual invoice and edit download retain payloads and administrator controls',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('invoice.html');
 await page.locator('#addManualItemBtn').click();await page.locator('.manual-description').fill('Usługa');await page.locator('.manual-price').fill('200.50');
 await page.evaluate(()=>document.getElementById('vatSelect').tomselect.setValue('23%'));
 assert.equal(await page.locator('#invoiceTotal').textContent(),'246.62');
 await page.locator('#saveInvoiceBtn').click();const req=await waitRequest(page,requests,'/api/v1/receipt');assert.equal(req.json().items[0].price,200.5);
 await goto('receipt-edit.html?id='+id);await page.locator('#amount-0').fill('99');
 const download=page.waitForEvent('download');await page.locator('button[value=download]').click();await download;
 const edit=await waitRequest(page,requests,`/api/v1/receipt/${id}/edit`);assert.equal(edit.search,'?save=false');assert.equal(edit.json().items[0].nettAmount,'99');assert.deepEqual(errors,[]);
});

test('documents expand by keyboard, offer PDF downloads and cancellation confirmation',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('receipts.html');
 await page.locator('.disclosure-heading').focus();await page.keyboard.press('Enter');
 assert.equal(await page.locator('.disclosure-body').isVisible(),true);
 assert.equal(await page.locator('a[href^="receipt-edit.html"]').count(),1);
 const download=page.waitForEvent('download');await page.getByRole('button',{name:'Pobierz fakturę serwisową'}).click();await download;
 await page.getByRole('button',{name:'Anuluj sprzedaż'}).click();assert.equal(await page.locator('#cancelModal').isVisible(),true);
 await page.locator('#confirmCancelBtn').click();await waitRequest(page,requests,`/api/v1/receipt/${id}/cancel`);assert.deepEqual(errors,[]);
});

test('client create and delete retain existing API and field validation',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('clients.html');
 await page.locator('#btnAddClient').click();await page.locator('#modalClientName').fill('Jan');await page.locator('#modalClientSurname').fill('Nowak');await page.locator('#modalClientPhone').fill('500700900');await page.locator('#btnSaveClient').click();
 const req=await waitRequest(page,requests,'/api/v1/repair-clients');assert.equal(req.json().name,'Jan');
 await page.waitForFunction(()=>!document.getElementById('clientModal').open);
 await page.evaluate(()=>openDeleteModal('client-1','Anna Kowalska'));
 await page.locator('#btnConfirmDelete').click();await waitRequest(page,requests,'/api/v1/repair-clients/client-1','DELETE');assert.deepEqual(errors,[]);
});

test('repair create, autocomplete and mobile status tabs remain available',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t,{mobile:true});await goto('repair.html');
 assert.equal(await page.locator('#mobile-cards-DO_NAPRAWY .panel').count(),1);
 await page.locator('a[href="#tab-W_NAPRAWIE"]').click();assert.equal(await page.locator('#tab-W_NAPRAWIE').isVisible(),true);
 await page.locator('.fab-fixed a').click();
 await page.locator('#clientSearch').fill('Ann');await page.locator('#clientSearch-suggestions button').first().click();
 await page.locator('#repairModel').fill('Telefon testowy');await page.locator('#repairProblemDescription').fill('Wymiana baterii');await page.locator('#saveRepairBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/repairs');assert.equal(req.json().clientTechnicalId,'client-1');assert.equal(req.json().model,'Telefon testowy');assert.deepEqual(errors,[]);
});

test('repair history filters and links stay inside v2',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('serviceHistory.html');
 await page.locator('.disclosure-heading').first().click();assert.equal(await page.locator('.disclosure-body').first().isVisible(),true);
 await page.getByRole('button',{name:'Do tablicy'}).first().click();await page.waitForURL('**/v2/repair.html?highlight=repair-1');assert.deepEqual(errors,[]);
});

test('models can be edited with dynamic variant fields',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('modelsBase.html');
 await page.locator('#modelsTableBody [data-action=edit]').first().click();await page.locator('#brand').fill('Apple test');
 await page.locator('#addColorBtn').click();await page.locator('#colorsFields input').last().fill('Niebieski');await page.locator('#saveModelBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/phone-models/model-1','PUT');assert.equal(req.json().brand,'Apple test');assert.match(req.json().colors,/Niebieski/);assert.deepEqual(errors,[]);
});

test('purchase details preserve price agreement, comments and closure controls',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('purchase.html');
 await page.locator('#purchaseTableBody button').first().click();await page.locator('#agreedPriceInput').fill('1200');await page.locator('#saveAgreedPriceBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/purchase/purchase-1/price-agreed');assert.equal(req.json().agreedPrice,'1200');
 await page.locator('#newCommentInput').fill('Rozmowa z klientem');await page.locator('#addCommentBtn').click();await waitRequest(page,requests,'/api/v1/purchase/purchase-1/comments');assert.deepEqual(errors,[]);
});

test('settings preserve account activation and dynamic-location actions',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('settings.html');
 const row=page.locator('#usersTableBody tr').nth(1);await row.locator('input[type=checkbox]').first().check();
 assert.equal((await waitRequest(page,requests,'/api/v1/admin/users/2/active','PUT')).json().active,true);
 await row.locator('input[type=checkbox]').nth(1).check();assert.equal((await waitRequest(page,requests,'/api/v1/admin/users/2/dynamic-location','PUT')).json().enabled,true);assert.deepEqual(errors,[]);
});

test('mobile navigation, legacy switch and seller-only visibility work',async t=>{
 const {page,goto,errors}=await app.fixture(t,{mobile:true,admin:false});await goto('receipts.html');
 assert.equal(await page.locator('a[href^="receipt-edit.html"]').count(),0);
 await page.locator('#openNavigation').click();assert.equal(await page.locator('.workspace-nav').evaluate(el=>el.getBoundingClientRect().left),0);
 assert.equal(await page.locator('.workspace-nav a[href="reports.html"]').isVisible(),false);
 await page.keyboard.press('Escape');assert.equal(await page.locator('#openNavigation').getAttribute('aria-expanded'),'false');
 assert.equal(await page.locator('#legacyViewLink').getAttribute('href'),'/receipts.html');assert.deepEqual(errors,[]);
});

test('manual phone entry saves the same batch payload and clears the submitted form',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('addPhones.html');
 await page.locator('#identitySourceToggle').uncheck();
 await page.locator('#manualName').fill('Testowy telefon');await page.locator('#manualToggle').check();
 await page.locator('.scan-item [data-field=imei]').fill('123456789012345');
 await page.locator('#savePhonesBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/phones');assert.equal(req.json()[0].name,'Testowy telefon');assert.equal(req.json()[0].imei,'123456789012345');
 await page.waitForFunction(()=>!document.querySelector('.scan-item'));assert.deepEqual(errors,[]);
});

test('offers preserve expand, phone selection, photo ordering and multipart save',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('offers.html');
 await page.locator('.offer-summary').click();assert.equal(await page.locator('.offer-details').isVisible(),true);
 await page.locator('#offerFormToggle').click();
 await page.evaluate(id=>document.getElementById('phoneSelect').tomselect.setValue(id),id);
 const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a2ioAAAAASUVORK5CYII=','base64');
 await page.locator('#photoFiles').setInputFiles([{name:'first.png',mimeType:'image/png',buffer:png},{name:'second.png',mimeType:'image/png',buffer:png}]);
 assert.equal(await page.locator('.photo-container').count(),2);
 await page.locator('.photo-container').nth(1).getByTitle('Ustaw jako miniaturkę').click();
 await page.locator('#saveBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/offers');assert.match(req.body,/phoneStockTechnicalId/);assert.match(req.body,/photoOrder/);assert.ok(req.body.indexOf('second.png')<req.body.indexOf('first.png'));assert.deepEqual(errors,[]);
});

test('login failure stays in v2, register and reset keep the original endpoints',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);
 await page.route('**/login',route=>route.fulfill({status:401,body:'Unauthorized'}));await goto('login.html');
 await page.locator('#username').fill('user@example.test');await page.locator('#password').fill('wrong');await page.locator('#loginForm button[type=submit]').click();
 await page.waitForFunction(()=>document.querySelector('.auth-message').textContent.length>0);assert.match(page.url(),/v2\/login.html/);
 await page.getByRole('button',{name:'Zarejestruj się'}).click();await page.locator('#regEmail').fill('new@example.test');await page.locator('#regPass1').fill('new-password');await page.locator('#regPass2').fill('new-password');await page.locator('#registerBtn').click();await waitRequest(page,requests,'/api/v1/auth/register');
 await page.waitForFunction(()=>!document.getElementById('registerModal').open);await page.locator('#showReset').click();await page.locator('#resetPass1').fill('another-password');await page.locator('#resetPass2').fill('another-password');await page.locator('#resetPasswordBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/auth/reset-password');assert.equal(req.json().email,'user@example.test');
 assert.deepEqual(errors.filter(e=>!e.includes('status of 401')),[]);
});

test('photo intake sends compressed multipart images and includes the HEIC decoder',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('addPhones.html');
 assert.equal(await page.evaluate(()=>typeof heic2any),'function');
 await page.locator('#identitySourceToggle').uncheck();await page.locator('#manualName').fill('Telefon ze zdjęcia');
 const png=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=c.height=8;return c.toDataURL('image/png').split(',')[1];});
 await page.locator('#fileInput').setInputFiles({name:'label.png',mimeType:'image/png',buffer:Buffer.from(png,'base64')});
 await page.locator('#addBulkBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/phones');assert.match(req.body,/name="photos"/);assert.match(req.body,/image\/jpeg/);assert.match(req.body,/Telefon ze zdjęcia/);
 await page.waitForFunction(()=>document.getElementById('loadingOverlay').classList.contains('hide'));assert.deepEqual(errors,[]);
});

test('repair notes and status changes are usable on mobile without dragging',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t,{mobile:true});await goto('repair.html');
 await page.locator('#mobile-cards-DO_NAPRAWY .panel').first().click();
 await page.locator('#newRepairNoteContent').fill('Klient zatwierdził naprawę');await page.locator('#btnAddRepairNote').click();
 assert.equal((await waitRequest(page,requests,'/api/v1/repairs/repair-1/notes')).body,'Klient zatwierdził naprawę');
 await page.keyboard.press('Escape');
 await page.locator('#mobile-cards-DO_NAPRAWY .repair-status-action select').selectOption('W_NAPRAWIE');
 await page.locator('#statusServicePointSearch').fill('Serwis');await page.locator('#statusServicePointSearch-suggestions button').first().click();await page.locator('#confirmServicePointBtn').click();
 const req=await waitRequest(page,requests,'/api/v1/repairs/repair-1/status','PATCH');assert.match(req.search,/status=W_NAPRAWIE/);assert.match(req.search,/servicePointTechnicalId=service-1/);assert.deepEqual(errors,[]);
});

test('report date ranges refresh sales and service data and redraw the chart',async t=>{
 const {page,goto,requests,errors}=await app.fixture(t);await goto('reports.html');
 assert.equal(await page.locator('#profit').textContent(),'5500,00 zł');
 await page.evaluate(()=>{const picker=document.getElementById('dateRangePicker')._flatpickr;picker.setDate(['2026-10-01','2026-10-31']);picker.config.onClose.forEach(fn=>fn(picker.selectedDates));});
 for(let i=0;i<50&&!requests.some(r=>r.path==='/api/reports/profit-per-day'&&r.search.includes('from=2026-10-01'));i++)await new Promise(r=>setTimeout(r,40));
 assert.ok(requests.some(r=>r.path==='/api/reports/profit-per-day'&&r.search==='?from=2026-10-01&to=2026-10-31'));
 assert.equal(await page.evaluate(()=>Chart.getChart(document.getElementById('profitPerDayChart')).data.datasets[0].data.length),3);assert.deepEqual(errors,[]);
});

test('failed invoice correction shows an error and does not enable saving',async t=>{
 const {page,goto,requests}=await app.fixture(t,{fail:true});await goto('receipt-edit.html?id='+id);
 assert.equal(await page.locator('#receiptEditForm').isVisible(),false);assert.match(await page.locator('#editStatus').textContent(),/Błąd serwera/);
 assert.equal(requests.some(r=>r.method==='POST'),false);
});

test('logout keeps the user in the new frontend',async t=>{
 const {page,goto,errors}=await app.fixture(t);
 await page.route('**/logout',route=>route.fulfill({status:200,body:'Logged out'}));
 await goto('index.html');await page.getByRole('button',{name:'Wyloguj się'}).click();
 await page.waitForURL('**/v2/login.html');assert.deepEqual(errors,[]);
});

test('empty inventory, lists and repair board render without runtime errors',async t=>{
 const {page,goto,errors}=await app.fixture(t,{empty:true,mobile:true});
 for(const screen of ['index.html','receipts.html','clients.html','modelsBase.html','offers.html','purchase.html','repair.html','serviceHistory.html']){
  await goto(screen);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,screen);assert.deepEqual(errors,[],screen);
 }
});
