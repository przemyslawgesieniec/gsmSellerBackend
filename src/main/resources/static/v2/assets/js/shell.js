(() => {
    const page=document.body.dataset.page;
    if(page==='login')return;
    const groups=[['PRACA', [['index','Magazyn','inventory_2'],['addPhones','Dodaj telefony','add_circle']]],['SPRZEDAŻ', [['invoice','Wystaw dokument','receipt_long'],['receipts','Dokumenty','description']]],['SERWIS', [['repair','Zlecenia napraw','build'],['serviceHistory','Historia serwisu','history'],['clients','Klienci','people']]],['INTERNET', [['offers','Oferty','local_offer'],['purchase','Skup urządzeń','shopping_basket'],['modelsBase','Baza modeli','dns']]],['ZARZĄDZANIE', [['reports','Raporty','bar_chart'],['notifications','Powiadomienia','notifications'],['settings','Ustawienia','settings']]]];
    const activePage=page==='receipt-edit'?'receipts':page;
    const menu=groups.map(([title,items])=>`<section class="nav-group"><p>${title}</p>${items.map(([key,label,icon])=>`<a href="${key}.html" class="nav-link ${key===activePage?'selected':''} ${key==='reports'?'admin-only hide':''}" ${key===activePage?'aria-current="page"':''}><i class="ui-icon">${icon}</i><span>${label}</span>${key===activePage?'<span class="nav-dot"></span>':''}</a>`).join('')}</section>`).join('');
    document.getElementById('workspaceShell').innerHTML=`
        <div class="nav-scrim" id="navScrim"></div>
        <aside class="workspace-nav" aria-label="Menu główne"><a class="brand" href="index.html"><img class="brand-logo" src="assets/brand/logo_gsm_black.png" width="915" height="248" alt="Teleakcesoria — telefony, akcesoria, serwis"></a><button id="closeNavigation" class="nav-close button-quiet" aria-label="Zamknij menu">×</button><nav>${menu}</nav>
        <div class="nav-bottom"><a class="nav-link" id="classicNavigation" href="/${page}.html"><i class="ui-icon">arrow_back</i>Klasyczny widok</a><form action="/logout" method="post"><button class="nav-link" type="submit"><i class="ui-icon">exit_to_app</i>Wyloguj się</button></form><p>TELEAKCESORIA</p></div></aside>
        <header class="workspace-topbar"><button id="openNavigation" class="button-quiet nav-toggle" aria-label="Otwórz menu" aria-expanded="false"><i class="ui-icon">menu</i></button><a class="mobile-brand" href="index.html" aria-label="Teleakcesoria — magazyn"><img src="assets/brand/logo_gsm.png" width="1874" height="514" alt="Teleakcesoria"></a><div class="workspace-context"><span class="context-dot"></span><span>Centrum zarządzania</span><span class="context-divider">/</span><strong>${document.querySelector('h1')?.textContent||'Teleakcesoria'}</strong></div><div class="workspace-account"><span id="workspaceLocation">Twój zespół</span><a href="notifications.html" class="notification-link" aria-label="Powiadomienia"><i class="ui-icon">notifications</i><span class="notification-count" id="notificationCount" hidden>0</span></a><a href="settings.html" class="user-avatar" id="workspaceAvatar" aria-label="Ustawienia konta">G</a></div></header>`;
    const toggle=open=>{document.querySelector('.workspace-main').inert=open;document.querySelector('.workspace-topbar').inert=open;document.body.classList.toggle('nav-open',open);document.getElementById('openNavigation').setAttribute('aria-expanded',String(open));if(open)document.querySelector('.workspace-nav .selected')?.focus();else document.getElementById('openNavigation').focus();};
    document.querySelector('.nav-bottom form').addEventListener('submit', async event => {
        event.preventDefault();
        const button = event.currentTarget.querySelector('button');
        button.disabled = true;
        try {
            const response = await fetch('/logout', { method: 'POST' });
            if (!response.ok) throw new Error('Logout failed');
            location.replace('login.html');
        } catch {
            UI.toast({html: 'Nie udało się wylogować. Spróbuj ponownie.', classes: 'red'});
            button.disabled = false;
        }
    });
    document.getElementById('openNavigation').onclick=()=>toggle(true);
    document.getElementById('closeNavigation').onclick=()=>toggle(false);
    document.getElementById('navScrim').onclick=()=>toggle(false);
    document.addEventListener('keydown',e=>{
        if(!document.body.classList.contains('nav-open'))return;
        if(e.key==='Escape')toggle(false);
        if(e.key==='Tab'){
            const links=[...document.querySelectorAll('.workspace-nav a,.workspace-nav button')].filter(el=>el.offsetParent!==null);
            const first=links[0],last=links[links.length-1];
            if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}
            else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}
        }
    });
    window.addEventListener('resize',()=>{if(innerWidth>992&&document.body.classList.contains('nav-open'))toggle(false);});
    for(const id of ['legacyViewLink','classicNavigation'])document.getElementById(id)?.setAttribute('href','/'+page+'.html'+location.search);
    const actions=document.getElementById('pageActions');
    if(page==='index')actions.innerHTML='<a class="button" href="addPhones.html"><i class="ui-icon">add</i>Dodaj telefony</a>';
    if(page==='receipts')actions.innerHTML='<a class="button" href="invoice.html"><i class="ui-icon">add</i>Wystaw dokument</a>';
    if(page==='serviceHistory')actions.innerHTML='<a class="button" href="repair.html"><i class="ui-icon">build</i>Centrum serwisowe</a>';
    // Turn floating icon-only actions into clear actions in the page heading.
    const fab=document.querySelector('.fab-fixed');
    if(fab){actions.append(fab);const a=fab.querySelector('a');if(a){a.className='button dialog-trigger';a.append(document.createTextNode(page==='clients'?'Dodaj klienta':'Nowa naprawa'));}}
    if(page==='index'){
        const filters=document.querySelector('.filters-col'), list=document.querySelector('.phones-col');
        if(filters&&list){const layout=document.createElement('div');layout.className='inventory-layout';filters.before(layout);layout.append(filters,list);filters.classList.toggle('filters-collapsed',innerWidth<768);
        filters.insertAdjacentHTML('afterbegin','<button type="button" id="toggleInventoryFilters" class="filter-heading" aria-controls="inventoryFilters"><i class="ui-icon">filter_list</i><span>Filtry</span><i class="ui-icon">expand_more</i></button>');
        filters.querySelector('.filter-panel').id='inventoryFilters';
        const filterToggle=document.getElementById('toggleInventoryFilters');
        filterToggle.setAttribute('aria-expanded',String(innerWidth>=768));
        filterToggle.onclick=()=>{const collapsed=filters.classList.toggle('filters-collapsed');filterToggle.setAttribute('aria-expanded',String(!collapsed));};}
    }
    if(page==='invoice')document.querySelector('#invoiceForm')?.classList.add('document-editor');
    if(page==='addPhones')document.querySelector('#dropArea')?.insertAdjacentHTML('afterbegin','<span class="upload-symbol"><i class="ui-icon">scan</i></span><h2>Dodaj zdjęcia urządzeń</h2>');
    document.addEventListener('DOMContentLoaded',async()=>{
        const user=await loadCurrentUser();
        if(!user){location.replace('login.html');return;}
        if(page==='receipt-edit'&&!IS_ADMIN){document.getElementById('receiptEditForm')?.classList.add('hide');return;}
        if(page==='reports'&&!IS_ADMIN){location.replace('index.html');return;}
        document.getElementById('workspaceLocation').textContent=user.location||'Wszystkie lokalizacje';
        document.getElementById('workspaceAvatar').textContent=(user.username||user.email||'G').slice(0,2).toUpperCase();
        UI.enhance();
        window.AppNotifications.start();
    });
})();

/* Shared in-app notification delivery. The server keeps the read state per recipient. */
window.AppNotifications = (() => {
    let knownUnreadIds = null;
    let started = false;
    const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));

    const setBadge = count => {
        document.querySelectorAll('.notification-count').forEach(badge => {
            badge.hidden = !count;
            badge.textContent = count > 99 ? '99+' : String(count);
        });
    };

    async function getUnread() {
        const [notificationsResponse, countResponse] = await Promise.all([
            fetch('/api/v1/notifications?unreadOnly=true'),
            fetch('/api/v1/notifications/unread-count')
        ]);
        if (!notificationsResponse.ok || !countResponse.ok) throw new Error('Nie udało się pobrać powiadomień');
        return { items: await notificationsResponse.json(), count: Number(await countResponse.json()) || 0 };
    }

    async function refresh({ announce = false } = {}) {
        try {
            const { items: unread, count } = await getUnread();
            setBadge(count);
            const newItems = knownUnreadIds && announce
                ? unread.filter(item => !knownUnreadIds.has(item.id))
                : [];
            knownUnreadIds = unreadIds;
            if (newItems.length) {
                const item = newItems[0];
                UI.toast({
                    html: `<strong>${escapeHtml(item.title)}</strong><br>${escapeHtml(item.content)}${newItems.length > 1 ? `<br>+${newItems.length - 1} kolejne` : ''}`,
                    classes: 'notification-toast',
                    displayLength: 7000
                });
            }
            return unread;
        } catch (error) {
            // A temporary polling failure should never interrupt normal work in the app.
            return [];
        }
    }

    async function markRead(id) {
        const response = await fetch(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
        if (!response.ok) throw new Error('Nie udało się oznaczyć powiadomienia jako przeczytane');
        return refresh();
    }

    async function markAllRead() {
        const response = await fetch('/api/v1/notifications/read-all', { method: 'PUT' });
        if (!response.ok) throw new Error('Nie udało się oznaczyć powiadomień jako przeczytane');
        return refresh();
    }

    function start() {
        if (started) return;
        started = true;
        refresh();
        window.setInterval(() => { if (!document.hidden) refresh({ announce: true }); }, 15000);
        document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh({ announce: true }); });
    }

    return { start, refresh, markRead, markAllRead, escapeHtml };
})();
