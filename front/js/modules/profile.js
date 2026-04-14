import { AppState } from '../core/state.js';
import { showScreen, updateUserUI, applyCosmetics, switchProfileTab } from '../core/ui.js';
import { apiFetch } from '../core/api.js';
import { logout } from './auth.js';

export function updateAvatarPreview() {
    if (!AppState.currentUser) return '';
    const base = AppState.currentUser.username;
    const top = document.getElementById('av-top').value;
    const acc = document.getElementById('av-acc').value;
    let config = `${encodeURIComponent(base)}&top=${top}`;
    config += acc !== 'none' ? `&accessories=${acc}&accessoriesProbability=100` : `&accessoriesProbability=0`;
    document.getElementById('avatar-preview').src = `https://api.dicebear.com/9.x/avataaars/svg?seed=${config}`;
    return config;
}

export function inicializarTaller() {
    if (AppState.currentUser && AppState.currentUser.avatar) {
        const savedString = AppState.currentUser.avatar;
        if (savedString.includes('&')) {
            const params = new URLSearchParams(savedString.substring(savedString.indexOf('&')));
            if (params.has('top')) document.getElementById('av-top').value = params.get('top');
            if (params.has('accessories')) document.getElementById('av-acc').value = params.get('accessories');
        }
    }
    updateAvatarPreview();
}

export function openProfile() {
    updateUserUI();
    const inventoryList = document.getElementById('inventory-list');
    inventoryList.innerHTML = "";
    if(!AppState.currentUser.inventario || AppState.currentUser.inventario.length === 0) {
        inventoryList.innerHTML = '<p class="empty-msg">Tu inventario está vacío.</p>';
    } else {
        AppState.currentUser.inventario.forEach(item => {
            const idObj = item.id_item || item.idItem;
            const esCosmetico = (item.nombre || '').includes('Tema') || (item.nombre || '').includes('Teclado');
            let isEquipped = esCosmetico ? AppState.currentUser.colorTema == idObj : (AppState.currentUser.habilidadesEquipadas || []).includes(idObj);

            inventoryList.innerHTML += `
                <div class="shop-item">
                    <h4>${item.nombre || 'Objeto'}</h4>
                    <span class="desc" style="display:block; margin-bottom: 10px;">${esCosmetico ? '🎨 Visual (Máx 1)' : '⚡ Pasiva (Máx 2)'}</span> 
                    <button class="${isEquipped ? 'btn-equipped' : 'btn-equip'}" onclick="app.toggleEquip(${idObj}, ${esCosmetico})">${isEquipped ? (esCosmetico ? 'Equipado' : 'Desequipar') : 'Equipar'}</button>
                </div>`;
        });
    }
    showScreen('profile-screen');
    inicializarTaller();
}

export function toggleEquip(id_item, esCosmetico) {
    if (!AppState.currentUser.habilidadesEquipadas) AppState.currentUser.habilidadesEquipadas = [];
    if (esCosmetico) {
        AppState.currentUser.colorTema = (AppState.currentUser.colorTema == id_item) ? null : id_item;
    } else {
        const index = AppState.currentUser.habilidadesEquipadas.indexOf(id_item);
        if (index > -1) AppState.currentUser.habilidadesEquipadas.splice(index, 1);
        else {
            if (AppState.currentUser.habilidadesEquipadas.length >= 2) return alert("⚠️ Límite: Solo 2 habilidades.");
            AppState.currentUser.habilidadesEquipadas.push(id_item);
        }
    }
    openProfile(); applyCosmetics();
}

export async function guardarPerfilForm(e) {
    e.preventDefault();
    const btnSubmit = e.target.querySelector('button[type="submit"]');
    try {
        btnSubmit.innerHTML = '<div><i class="fas fa-spinner fa-spin"></i> GUARDANDO...</div>'; btnSubmit.disabled = true;
        const payloadPut = { nombre_usuario: document.getElementById('upd-username').value };
        const newPassword = document.getElementById('upd-password').value;
        if (newPassword.trim() !== '') payloadPut.contrasenha_usuario = newPassword;

        const responsePut = await apiFetch('/usuarios/perfil', { method: 'PUT', body: JSON.stringify(payloadPut) });
        if (responsePut.ok) {
            alert("¡Perfil actualizado! Vuelve a iniciar sesión.");
            logout();
        } else throw new Error();
    } catch (error) { alert("Fallo al guardar."); }
    finally { if(btnSubmit) { btnSubmit.innerHTML = '<div>GUARDAR CAMBIOS</div>'; btnSubmit.disabled = false; } }
}

export async function guardarLookBtn(e) {
    const btn = e.target; btn.innerText = "Guardando...";
    const config = updateAvatarPreview();
    try {
        const response = await apiFetch(`/usuarios/avatar`, { method: 'PUT', body: JSON.stringify({ config: config }) });
        if (response.ok) {
            AppState.currentUser.avatar = config;
            btn.innerText = "¡Look Guardado! ✔️"; btn.style.backgroundColor = "#2e7d32";
            setTimeout(() => { btn.innerText = "💾 Guardar Look"; btn.style.backgroundColor = "#4CAF50"; }, 2000);
            updateUserUI();
        }
    } catch (e) { btn.innerText = "💾 Guardar Look"; }
}