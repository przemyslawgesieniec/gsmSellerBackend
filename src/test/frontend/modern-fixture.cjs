const fs=require('node:fs');
const path=require('node:path');
const http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve(__dirname,'../../main/resources/static');
const id='11111111-1111-4111-8111-111111111111';
const model={technicalId:'model-1',brand:'Apple',model:'iPhone 15 Pro',displayName:'Apple iPhone 15 Pro',memory:'128,256,512',ram:'8',colors:'Czarny,Naturalny',simCardType:'sim + e-sim',batteryCapacity:'3274',frontCamera:'12',backCamera:'48,12,12',displayPriority:5};
const phone={technicalId:id,name:'Apple iPhone 15 Pro',model:'iPhone 15 Pro',imei:'354892100768123',sellingPrice:3299,purchasePrice:2400,price:3299,description:'Bardzo dobry stan, komplet akcesoriów.',comment:'Sprawdzony w serwisie',color:'Naturalny tytan',memory:'256 GB',ram:'8 GB',simCardType:'sim + e-sim',source:'Skup lokalny',locationName:'Stryków',purchaseType:'CASH',used:true,batteryCondition:94,createDateTime:'2026-10-30T10:20:00',status:'DOSTĘPNY',phoneModelTechnicalId:'model-1',phoneModelDisplayName:model.displayName,hasOffer:true,afterService:false,isReserved:false};
const phones=[phone,{...phone,technicalId:'phone-2',name:'Samsung Galaxy S24',model:'Galaxy S24',color:'Onyx Black',sellingPrice:2499,purchasePrice:1900,used:false,batteryCondition:100,hasOffer:false},{...phone,technicalId:'phone-3',name:'Apple iPhone 13',model:'iPhone 13',sellingPrice:1599,purchasePrice:1100,status:'WPROWADZONY',locationName:null,hasOffer:false}];
const client={technicalId:'client-1',name:'Anna',surname:'Kowalska',phoneNumber:'500600700'};
const repair={technicalId:'repair-1',rma:'RMA/11/2026/001',client,clientName:'Anna',clientPhoneNumber:'500600700',forCustomer:true,isForCustomer:true,deviceType:'TELEFON',manufacturer:'Apple',model:'iPhone 14 Pro',imei:'354892100768123',color:'Czarny',status:'DO_NAPRAWY',problemDescription:'Wymiana wyświetlacza',deviceCondition:'Pęknięty ekran',receiptDate:'2026-11-01',estimatedRepairDate:'2026-11-08',estimatedCost:650,advancePayment:100,purchasePrice:200,repairPrice:650,locationName:'Stryków',location:'Stryków',businessId:'RMA/001',clientSurname:'Kowalska',createdBy:'admin',remarks:'Kontakt przed naprawą',notes:[],servicePoint:{technicalId:'service-1',name:'Serwis centralny'},servicePointName:'Serwis centralny',archived:false};
const repairs=[repair,{...repair,technicalId:'repair-2',rma:'RMA/11/2026/002',model:'Samsung S23',status:'W_NAPRAWIE'},{...repair,technicalId:'repair-3',rma:'RMA/11/2026/003',model:'iPhone 12',status:'NAPRAWIONY'}];
const receipt={id:1,technicalId:id,number:'012/11/2026',status:'AKTYWNA',createDate:'2026-11-01T10:20:00',dateAndPlace:{generateDate:'2026-11-01',sellDate:'2026-11-01',place:'Stryków'},items:[{id:1,name:phone.name,itemType:'PHONE',nettAmount:3299,vatRate:'VAT_EXEMPT',vatAmount:0,grossAmount:3299,technicalId:id}]};
const purchase={technicalId:'purchase-1',phoneNumber:'500600700',phoneModel:'Samsung Galaxy S23',description:'Telefon w dobrym stanie',status:'NEW',createdAt:'2026-11-01T10:20:00',photoIds:[],comments:[],agreedPrice:null};
const offer={technicalId:'offer-1',phoneTechnicalId:id,phoneModelName:phone.name,imei:phone.imei,location:'Stryków',color:phone.color,phoneName:phone.name,phoneModel:phone.model,name:phone.name,title:phone.name,description:'Telefon sprawdzony przez serwis.',price:3299,sellingPrice:3299,photos:[],photoUrls:[],locationName:'Stryków',phoneImei:phone.imei,phoneModelDisplayName:model.displayName,createdAt:'2026-11-01T10:00:00'};
const pageData=content=>({content,totalPages:1,totalElements:content.length,number:0,size:20,first:true,last:true});
async function start(){
 const server=http.createServer((req,res)=>{
  const url=new URL(req.url,'http://localhost');let file=path.join(root,decodeURIComponent(url.pathname));if(url.pathname.endsWith('/'))file=path.join(file,'index.html');
  if(!file.startsWith(root)||!fs.existsSync(file)){res.writeHead(404);res.end('Not found');return;}
  const ext=path.extname(file);res.setHeader('Content-Type',({'.html':'text/html','.js':'application/javascript','.css':'text/css','.json':'application/json','.png':'image/png','.svg':'image/svg+xml'})[ext]||'application/octet-stream');res.end(fs.readFileSync(file));
 });await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const browser=await chromium.launch({headless:true});const base=`http://127.0.0.1:${server.address().port}`;
 async function fixture(t,{mobile=false,admin=true,empty=false,cartItems=[],fail=false}={}){
  const context=await browser.newContext({viewport:mobile?{width:375,height:844}:{width:1440,height:1050},locale:'pl-PL',reducedMotion:'reduce'});t?.after(()=>context.close());const page=await context.newPage();page.setDefaultTimeout(8000);
  const errors=[],requests=[],external=[];page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error'&&!m.text().includes('status of 500'))errors.push(m.text());});page.on('request',r=>{if(!r.url().startsWith(base)&&!r.url().startsWith('blob:')&&!r.url().startsWith('data:'))external.push(r.url());});
  await page.route('**/api/**',async route=>{
   const req=route.request(),url=new URL(req.url()),p=url.pathname;requests.push({path:p,search:url.search,method:req.method(),body:req.postData()});
   if(fail&&p!='/api/v1/users/auth/me')return route.fulfill({status:500,json:{message:'Błąd serwera'}});
   if(p==='/api/v1/users/auth/me')return route.fulfill({json:{username:'admin',email:'admin@example.test',role:admin?'ROLE_ADMIN':'ROLE_SELLER',location:'Stryków',id:1}});
   if(req.method()!=='GET'){
    if(p.endsWith('/edit'))return route.fulfill({contentType:'application/pdf',body:'%PDF-1.4 test'});
    if(p==='/api/v1/receipt')return route.fulfill({json:id});
    if(p==='/api/v1/phones')return route.fulfill({json:[]});
    if(p.startsWith('/api/v1/cart'))return route.fulfill({json:{items:[{...phone,itemType:'PHONE'}],phones:[phone],miscItems:[]}});
    if(p.startsWith('/api/v1/repairs'))return route.fulfill({json:repair});
    if(p.startsWith('/api/v1/purchase'))return route.fulfill({json:{...purchase,status:'PRICE_AGREED',agreedPrice:1000}});
    return route.fulfill({json:{technicalId:id}});
   }
   let data;
   if(p==='/api/v1/locations')data=[{id:1,technicalId:'location-1',name:'Stryków',city:'Stryków'},{id:2,technicalId:'location-2',name:'Łódź',city:'Łódź'}];
   else if(p==='/api/v1/cart')data={items:cartItems};
   else if(p==='/api/v1/cart/full')data={phones:cartItems.filter(x=>x.itemType==='PHONE'),miscItems:cartItems.filter(x=>x.itemType==='MISC')};
   else if(p==='/api/v1/phones')data=pageData(empty?[]:phones);
   else if(p.startsWith('/api/v1/phones/'))data=phone;
   else if(p==='/api/v1/phone-models/filter-options')data={brands:[{technicalId:'apple',name:'Apple'},{technicalId:'samsung',name:'Samsung'}],modelsByBrand:{apple:[model],samsung:[]}};
   else if(p==='/api/v1/phone-models')data=pageData(empty?[]:[model]);
   else if(p.startsWith('/api/v1/phone-models/'))data=model;
   else if(p==='/api/v1/receipt/list')data=pageData(empty?[]:[receipt]);
   else if(p.endsWith('/edit')&&p.startsWith('/api/v1/receipt/'))data=receipt;
   else if(p.startsWith('/api/v1/receipt/'))return route.fulfill({contentType:'application/pdf',body:'%PDF-1.4 test'});
   else if(p==='/api/v1/repair-clients/search')data=pageData([client]);
   else if(p==='/api/v1/repair-clients')data=pageData(empty?[]:[client]);
   else if(p.includes('service-points'))data=[{technicalId:'service-1',name:'Serwis centralny'}];
   else if(p==='/api/v1/repairs')data=empty?[]:repairs;
   else if(p==='/api/v1/repairs/history')data=pageData(empty?[]:repairs);
   else if(p.includes('/repairs/')&&p.endsWith('/notes'))data=[];
   else if(p.startsWith('/api/v1/repairs/'))data=repair;
   else if(p==='/api/v1/purchase')data=pageData(empty?[]:[purchase]);
   else if(p.startsWith('/api/v1/purchase/'))data=purchase;
   else if(p==='/api/v1/offers/available-phones')data=pageData([phone]);
   else if(p==='/api/v1/offers')data=pageData(empty?[]:[offer]);
   else if(p.startsWith('/api/v1/offers/'))data={...offer,phone};
   else if(p==='/api/v1/admin/users')data=[{id:1,username:'admin@example.test',role:'ROLE_ADMIN',active:true,location:'Stryków'},{id:2,username:'seller@example.test',role:'ROLE_SELLER',active:true,location:'Łódź'}];
   else if(p.endsWith('repair-summary'))data={totalRepairs:18,totalProfit:4500,statusCounts:{ANULOWANY:1,NIE_DO_NAPRAWY:2,W_NAPRAWIE:4}};
   else if(p.endsWith('profit-per-day'))data=[{date:'2026-11-01',profit:800},{date:'2026-11-02',profit:1400},{date:'2026-11-03',profit:950}];
   else if(p.startsWith('/api/reports/'))data={turnover:18500,cost:13000,profit:5500,miscGrossAmount:500,stockCount:42,stockValue:87000,potentialProfit:23000,soldTodayCount:4,todayTurnover:9400,todayProfit:2200};
   else if(p.includes('reservations/status'))data={name:'Jan',phoneNumber:'500600700',expiryTime:'2026-11-01T20:00:00'};
   else data=[];
   return route.fulfill({json:data});
  });
  return {page,context,errors,requests,external,goto:async name=>{await page.goto(base+'/v2/'+name);await page.waitForLoadState('networkidle');}};
 }
 return {browser,base,fixture,close:async()=>{await browser.close();await new Promise(resolve=>server.close(resolve));}};
}
module.exports={start,root,id,phone,phones,client,repair,repairs,receipt,purchase,model,offer};
