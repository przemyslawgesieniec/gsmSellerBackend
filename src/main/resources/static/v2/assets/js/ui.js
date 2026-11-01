/* GSM Workspace components. Native dialogs, keyboard menus and accessible disclosures.
 * No dependency on Materialize or the classic frontend. */
(() => {
    const registry = new WeakMap();
    class Component {
        constructor(el, options = {}) { this.el = el; this.options = options; }
        static init(elements, options = {}) {
            if (!elements) return null;
            if (!(elements instanceof Element)) return Array.from(elements, el => this.init(el, options));
            const records = registry.get(elements) || new Map();
            if (!records.has(this)) records.set(this, new this(elements, options));
            registry.set(elements, records);
            return records.get(this);
        }
        static getInstance(el) { return el ? this.init(el) : null; }
        destroy() { registry.get(this.el)?.delete(this.constructor); }
    }
    class Dialog extends Component {
        constructor(el, options) {
            super(el, options);
            // Dynamically rendered dialogs can use the same declarative class.
            if (!(el instanceof HTMLDialogElement)) {
                const dialog = document.createElement('dialog');
                for (const attr of el.attributes) dialog.setAttribute(attr.name, attr.value);
                dialog.append(...el.childNodes); el.replaceWith(dialog); this.el = dialog;
            }
            this.el.addEventListener('close', () => {
                this.isOpen = false;
                document.body.classList.toggle('dialog-open', !!document.querySelector('dialog[open]'));
                this.options.onCloseEnd?.call(this, this.el);
                this.trigger?.focus({ preventScroll: true });
            });
            this.el.addEventListener('click', event => {
                if (event.target.closest('.dialog-close')) this.close();
                if (event.target === this.el && this.options.dismissible !== false) {
                    const r = this.el.getBoundingClientRect();
                    if (event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom) this.close();
                }
            });
            this.el.addEventListener('cancel', event => { if (this.options.dismissible === false) event.preventDefault(); });
        }
        open() {
            if (this.el.open) return;
            this.trigger = document.activeElement;
            enhance(this.el);
            if (!this.el.querySelector('.dialog-dismiss')) {
                const close = document.createElement('button'); close.type = 'button'; close.className = 'dialog-dismiss dialog-close';
                close.setAttribute('aria-label', 'Zamknij okno'); close.textContent = '×'; this.el.prepend(close);
            }
            this.options.onOpenStart?.call(this, this.el);
            this.el.showModal(); this.isOpen = true; document.body.classList.add('dialog-open');
            this.options.onOpenEnd?.call(this, this.el);
        }
        close() { if (this.el.open) { this.options.onCloseStart?.call(this, this.el); this.el.close(); } }
    }
    class Menu extends Component {
        constructor(el, options) {
            super(el, options); this.dropdownEl = document.getElementById(el.dataset.target);
            if (!this.dropdownEl) return;
            this.dropdownEl.setAttribute('popover','auto'); this.dropdownEl.setAttribute('role','menu');
            el.setAttribute('aria-haspopup','menu'); el.setAttribute('aria-expanded','false');
            this.click = event => { event.preventDefault(); this.isOpen ? this.close() : this.open(); };
            el.addEventListener('click', this.click);
            this.key = event => {
                if (['ArrowDown','ArrowUp','Enter',' '].includes(event.key)) { event.preventDefault(); this.open(); }
            };
            el.addEventListener('keydown', this.key);
            this.menuKey = event => {
                const items = [...this.dropdownEl.querySelectorAll('a:not(.disabled-link),button:not(:disabled)')];
                let i = items.indexOf(document.activeElement);
                if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                    event.preventDefault(); items[(i + (event.key === 'ArrowDown' ? 1 : items.length - 1)) % items.length]?.focus();
                }
                if (event.key === 'Escape') { this.close(); el.focus(); }
            };
            this.dropdownEl.addEventListener('keydown', this.menuKey);
            this.dropdownEl.addEventListener('toggle', event => {
                this.isOpen = event.newState === 'open'; el.setAttribute('aria-expanded', String(this.isOpen));
            });
        }
        open() {
            if (!this.dropdownEl || this.isOpen) return;
            this.dropdownEl.showPopover(); this.isOpen = true;
            const r = this.el.getBoundingClientRect(), menu = this.dropdownEl;
            menu.style.left = `${Math.max(8, Math.min(r.right - menu.offsetWidth, innerWidth - menu.offsetWidth - 8))}px`;
            menu.style.top = `${Math.max(8, Math.min(r.bottom + 6, innerHeight - menu.offsetHeight - 8))}px`;
            menu.querySelector('a:not(.disabled-link),button:not(:disabled)')?.focus();
        }
        close() { if (this.dropdownEl?.matches(':popover-open')) this.dropdownEl.hidePopover(); this.isOpen = false; }
        destroy() { this.close(); this.el.removeEventListener('click', this.click); this.el.removeEventListener('keydown', this.key); this.dropdownEl?.removeEventListener('keydown', this.menuKey); super.destroy(); }
    }
    class Disclosure extends Component {
        constructor(el, options) {
            super(el, options);
            this.toggle = event => {
                const heading = event.target.closest('.disclosure-heading');
                if (!heading || event.target.closest('button,a,input,select')) return;
                if (event.type === 'keydown' && !['Enter',' '].includes(event.key)) return;
                event.preventDefault(); const item = heading.parentElement;
                item.classList.toggle('expanded'); heading.setAttribute('aria-expanded', String(item.classList.contains('expanded')));
            };
            el.addEventListener('click', this.toggle); el.addEventListener('keydown', this.toggle);
        }
        static init(el, options) {
            const result = super.init(el,options);
            const roots = el instanceof Element ? [el] : [...(el || [])];
            roots.forEach(root => root.querySelectorAll('.disclosure-heading').forEach(h => { h.tabIndex = 0; h.setAttribute('role','button'); h.setAttribute('aria-expanded',String(h.parentElement.classList.contains('expanded'))); }));
            return result;
        }
    }
    class Tabs extends Component {
        constructor(el, options) {
            super(el,options); el.setAttribute('role','tablist');
            this.links = [...el.querySelectorAll('a[href^="#"]')];
            this.links.forEach((link,index) => {
                link.setAttribute('role','tab');
                link.addEventListener('click',event => { event.preventDefault(); this.select(link.hash.slice(1)); });
                link.addEventListener('keydown',event => {
                    if (['ArrowLeft','ArrowRight'].includes(event.key)) {
                        event.preventDefault(); const next = this.links[(index + (event.key === 'ArrowRight' ? 1 : this.links.length - 1)) % this.links.length];
                        this.select(next.hash.slice(1)); next.focus();
                    }
                });
            });
            this.select(this.links.find(l => l.classList.contains('active'))?.hash.slice(1) || this.links[0]?.hash.slice(1));
        }
        select(id) {
            this.links.forEach(link => {
                const active = link.hash === '#'+id; link.classList.toggle('active',active); link.setAttribute('aria-selected',String(active)); link.tabIndex = active ? 0 : -1;
                const panel = document.getElementById(link.hash.slice(1)); if (panel) { panel.hidden = !active; panel.setAttribute('role','tabpanel'); }
            });
        }
    }
    class Autocomplete extends Component {
        constructor(el,options) {
            super(el,options); this.data={};
            this.list=document.createElement('div'); this.list.className='suggestions'; this.list.hidden=true;
            this.list.id=el.id+'-suggestions'; this.list.setAttribute('role','listbox'); el.parentElement.append(this.list);
            el.setAttribute('role','combobox'); el.setAttribute('aria-controls',this.list.id); el.setAttribute('aria-expanded','false'); el.setAttribute('autocomplete','off');
            el.addEventListener('keydown',event => {
                const buttons=[...this.list.querySelectorAll('button')];
                if(event.key==='ArrowDown'){ event.preventDefault(); this.open(); buttons[0]?.focus(); }
                if(event.key==='Escape')this.close();
            });
            this.list.addEventListener('keydown',event=>{
                const buttons=[...this.list.querySelectorAll('button')], i=buttons.indexOf(document.activeElement);
                if(['ArrowDown','ArrowUp'].includes(event.key)){event.preventDefault();buttons[(i+(event.key==='ArrowDown'?1:buttons.length-1))%buttons.length]?.focus();}
                if(event.key==='Escape'){this.close();el.focus();}
            });
            document.addEventListener('click',event=>{if(!el.parentElement.contains(event.target))this.close();});
        }
        updateData(data) {
            this.data=data; this.list.replaceChildren();
            Object.keys(data).forEach(value=>{
                const item=document.createElement('button');item.type='button';item.textContent=value;item.setAttribute('role','option');
                item.addEventListener('click',()=>{this.el.value=value;this.close();this.options.onAutocomplete?.call(this,value);this.el.focus();});this.list.append(item);
            });
        }
        open(){this.list.hidden=!Object.keys(this.data).length;this.el.setAttribute('aria-expanded',String(!this.list.hidden));}
        close(){this.list.hidden=true;this.el.setAttribute('aria-expanded','false');}
    }
    class DateInput extends Component {
        constructor(el,options){super(el,options);el.type='date';}
        setDate(date){this.date=date;}
        setInputValue(){const d=this.date;if(d)this.el.value=[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');}
    }
    class Select extends Component {}
    class Tooltip extends Component { constructor(el,options){super(el,options);el.title=el.dataset.tooltip||'';} }
    class Drawer extends Component {open(){document.body.classList.add('nav-open');}close(){document.body.classList.remove('nav-open');}}
    const glyphs={star:'Star',scan:'ScanLine',filter_alt_off:'FilterX',warning_amber:'TriangleAlert',add_comment:'MessageSquarePlus',archive:'Archive',unarchive:'ArchiveRestore',dashboard:'LayoutDashboard',event_available:'CalendarCheck',event_busy:'CalendarX',extension:'Puzzle',price_check:'BadgeDollarSign',smartphone:'Smartphone',phone:'Phone',phone_iphone:'Smartphone',phonelink_ring:'Smartphone',inventory_2:'Package',inventory:'Package',add:'Plus',add_circle:'CirclePlus',close:'X',search:'Search',menu:'Menu',more_vert:'EllipsisVertical',more_horiz:'Ellipsis',chevron_left:'ChevronLeft',chevron_right:'ChevronRight',expand_more:'ChevronDown',expand_less:'ChevronUp',arrow_drop_down:'ChevronDown',arrow_back:'ArrowLeft',arrow_forward:'ArrowRight',edit:'Pencil',delete:'Trash2',delete_forever:'Trash2',restore:'RotateCcw',history:'History',refresh:'RefreshCw',check:'Check',check_circle:'CircleCheck',done:'Check',cancel:'CircleX',error:'CircleAlert',error_outline:'CircleAlert',warning:'TriangleAlert',info:'Info',info_outline:'Info',settings:'Settings',home:'House',build:'Wrench',people:'Users',person:'UserRound',person_outline:'UserRound',person_add:'UserRoundPlus',mail:'Mail',email:'Mail',lock:'LockKeyhole',lock_open:'LockKeyholeOpen',visibility:'Eye',visibility_off:'EyeOff',place:'MapPin',location_on:'MapPin',store:'Store',storefront:'Store',local_shipping:'Truck',shopping_cart:'ShoppingCart',shopping_bag:'ShoppingBag',shopping_basket:'ShoppingBasket',sell:'Tag',local_offer:'Tag',receipt:'Receipt',receipt_long:'ReceiptText',description:'FileText',print:'Printer',download:'Download',file_download:'Download',upload:'Upload',upload_file:'Upload',cloud_upload:'CloudUpload',cloud:'Cloud',save:'Save',payments:'Wallet',attach_money:'Banknote',money:'Banknote',bar_chart:'ChartNoAxesCombined',show_chart:'ChartLine',trending_up:'TrendingUp',event:'CalendarDays',calendar_today:'CalendarDays',schedule:'Clock',access_time:'Clock',alarm:'AlarmClock',fingerprint:'ScanBarcode',dns:'Database',storage:'Database',memory:'Cpu',palette:'Palette',color_lens:'Palette',notes:'NotebookPen',note:'NotebookPen',comment:'MessageSquare',chat:'MessageSquare',image:'Image',photo:'Image',photo_camera:'Camera',add_a_photo:'Camera',camera_alt:'Camera',photo_library:'Images',battery_full:'BatteryFull',battery_std:'BatteryMedium',battery_alert:'BatteryWarning',battery_3_bar:'BatteryMedium',battery_1_bar:'BatteryLow',battery_5_bar:'BatteryFull',exit_to_app:'LogOut',logout:'LogOut',filter_list:'ListFilter',filter_alt:'ListFilter',clear:'X',autorenew:'RefreshCw',sync:'RefreshCw',open_in_new:'ExternalLink',launch:'ExternalLink',link:'Link',content_copy:'Copy',assignment:'ClipboardList',assignment_return:'Undo2',verified:'BadgeCheck',security:'ShieldCheck',send:'Send',sim_card:'CardSim',devices:'MonitorSmartphone',done_all:'CheckCheck',pending_actions:'ClipboardClock',account_balance_wallet:'Wallet',business:'Building2',public:'Globe',web:'Globe',language:'Globe',help_outline:'CircleHelp',tune:'SlidersHorizontal',handyman:'Wrench',arrow_upward:'ArrowUp',arrow_downward:'ArrowDown'};
    function enhance(root=document) {
        root.querySelectorAll?.('.ui-icon').forEach(el=>{
            const text=[...el.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE).map(n=>n.textContent).join('').trim();
            if(!text)return;
            const name=glyphs[text]||'CircleHelp';const icon=window.lucide?.icons[name];
            if(icon){el.replaceChildren(lucide.createElement(icon,{width:20,height:20,'stroke-width':1.7,'aria-hidden':'true'}));el.dataset.icon=text;el.setAttribute('aria-hidden','true');}
        });
        root.querySelectorAll?.('.field').forEach(field=>{
            const label=field.querySelector(':scope > label');
            if(label&&field.firstElementChild!==label)field.prepend(label);
            if(label&&!label.htmlFor){const input=field.querySelector('input,textarea,select');if(input?.id)label.htmlFor=input.id;}
        });
        root.querySelectorAll?.('table').forEach(table=>{if(!table.parentElement.classList.contains('table-scroll')){const wrapper=document.createElement('div');wrapper.className='table-scroll';table.before(wrapper);wrapper.append(table);}});
        root.querySelectorAll?.('.hint').forEach(el=>{if(el.title!==el.dataset.tooltip)el.title=el.dataset.tooltip||'';});
        root.querySelectorAll?.('a:not([href]),a[href="#!"],a[href="#"]').forEach(a=>{a.setAttribute('role','button');a.tabIndex=a.classList.contains('disabled-link')?-1:0;});
        root.querySelectorAll?.('button,a[role="button"]').forEach(el=>{
            if(!el.textContent.trim()&&!el.getAttribute('aria-label'))el.setAttribute('aria-label',el.title||({close:'Zamknij',edit:'Edytuj',delete:'Usuń',chevron_left:'Poprzednia strona',chevron_right:'Następna strona',filter_alt_off:'Wyczyść filtry'}[el.querySelector('[data-icon]')?.dataset.icon]||'Akcje'));
        });
    }
    function toast({html='',classes='',displayLength=5000}){
        let region=document.getElementById('workspaceToasts');
        if(!region){region=document.createElement('div');region.id='workspaceToasts';region.setAttribute('aria-live','polite');document.body.append(region);}
        const item=document.createElement('div');item.className='toast '+(classes.includes('red')?'toast-error':classes.includes('green')?'toast-success':'');
        item.textContent=new DOMParser().parseFromString(String(html),'text/html').body.textContent;
        const close=document.createElement('button');close.type='button';close.textContent='×';close.setAttribute('aria-label','Zamknij powiadomienie');close.onclick=()=>item.remove();item.append(close);region.append(item);setTimeout(()=>item.remove(),displayLength);
    }
    window.UI={Dialog,Menu,Disclosure,Tabs,Autocomplete,DateInput,Select,Tooltip,Drawer,toast,enhance,
        updateTextFields:()=>enhance(),textareaAutoResize:el=>{if(el){el.style.height='auto';el.style.height=Math.min(360,Math.max(96,el.scrollHeight))+'px';}}};
    document.addEventListener('click',event=>{
        const trigger=event.target.closest('.dialog-trigger');
        if(trigger){event.preventDefault();const id=trigger.dataset.target||trigger.hash?.slice(1);Dialog.getInstance(document.getElementById(id))?.open();}
        const disabled=event.target.closest('.disabled-link');if(disabled){event.preventDefault();event.stopImmediatePropagation();}
    },true);
    document.addEventListener('keydown',event=>{const a=event.target.closest('a[role="button"]');if(a&&event.key===' '){event.preventDefault();a.click();}});
    document.addEventListener('DOMContentLoaded',()=>{
        Dialog.init(document.querySelectorAll('dialog'));Tabs.init(document.querySelectorAll('.tab-list'));Disclosure.init(document.querySelectorAll('.disclosure'));enhance();
        let scheduled=false;
        new MutationObserver(records=>{if(records.some(r=>r.type==='childList'&&r.addedNodes.length)&&!scheduled){scheduled=true;requestAnimationFrame(()=>{scheduled=false;enhance();});}}).observe(document.body,{childList:true,subtree:true});
    });
})();
