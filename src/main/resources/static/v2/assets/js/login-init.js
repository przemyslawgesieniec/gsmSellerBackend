(() => {
    const card=document.querySelector('.login-card');
    card.querySelector('h5').textContent='Witaj ponownie';
    const intro=document.createElement('p');intro.className='grey-text';intro.textContent='Zaloguj się do swojego centrum zarządzania.';card.querySelector('.center-align').append(intro);
    const message=document.createElement('p');message.className='auth-message';message.setAttribute('role','alert');document.getElementById('loginForm').append(message);
    document.getElementById('username').autocomplete='username';document.getElementById('password').autocomplete='current-password';
    document.getElementById('showReset').onclick=()=>{document.getElementById('resetEmail').value=document.getElementById('username').value;};
    document.getElementById('loginForm').addEventListener('submit',async event=>{
        event.preventDefault();const button=event.currentTarget.querySelector('button[type=submit]');if(button.disabled)return;
        button.disabled=true;button.textContent='Logowanie…';message.textContent='';
        try{
            const response=await fetch('/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({username:document.getElementById('username').value,password:document.getElementById('password').value})});
            if(!response.ok||response.url.includes('login.html'))throw new Error('Sprawdź login i hasło. Konto musi być aktywne.');
            location.href='index.html';
        }catch(error){message.textContent=error.message||'Nie udało się połączyć. Spróbuj ponownie.';}
        finally{button.disabled=false;button.textContent='Zaloguj się';}
    });
    async function submitAccount(kind){
        const reset=kind==='reset';const button=document.getElementById(reset?'resetPasswordBtn':'registerBtn');if(button.disabled)return;
        const email=document.getElementById(reset?'resetEmail':'regEmail'),pass=document.getElementById(reset?'resetPass1':'regPass1'),confirm=document.getElementById(reset?'resetPass2':'regPass2');
        if(!email.value.trim()||!email.checkValidity()||!pass.value||pass.value!==confirm.value){UI.toast({html:'Podaj poprawny email i dwa identyczne hasła.',classes:'red'});return;}
        button.disabled=true;
        try{
            const response=await fetch('/api/v1/auth/'+(reset?'reset-password':'register'),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:email.value.trim(),password:pass.value,confirmPassword:confirm.value})});
            if(!response.ok)throw new Error((await response.text())||'Nie udało się zapisać. Spróbuj ponownie.');
            UI.toast({html:reset?'Hasło zmienione. Konto wymaga ponownej aktywacji.':'Konto utworzone — czeka na aktywację.',classes:'green'});
            UI.Dialog.getInstance(document.getElementById(reset?'resetModal':'registerModal')).close();pass.value='';confirm.value='';
        }catch(error){UI.toast({html:error.message,classes:'red'});}finally{button.disabled=false;}
    }
    document.getElementById('registerBtn').onclick=()=>submitAccount('register');
    document.getElementById('resetPasswordBtn').onclick=()=>submitAccount('reset');
})();
