import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { showScreen, updateUserUI, verificarTutorial } from '../core/ui.js';
import { inicializarTaller } from './profile.js';
import { fetchMorePhrases } from './game.js';

export async function restoreSession() {
    try {
        const response = await apiFetch(`/usuarios/perfil?t=${new Date().getTime()}`, { method: 'GET', headers: {'Cache-Control': 'no-cache'} });
        if (response.ok) {
            const data = await response.json();
            const inventarioSeguro = (data.items || data.inventario || []).map(i => ({ id_item: i.idItem || i.id_item || i.id, nombre: i.nombre || i.name, descripcion: i.descripcion || i.description }));
            AppState.currentUser = {
                username: data.nombreUsuario || data.username || data.nombre_usuario || 'Piloto',
                correo: data.correo_usuario || data.correoUsuario || data.email || '',
                id: data.id,
                apellido: data.apellido_usuario || data.apellidoUsuario || data.apellido || '',
                creditos: data.creditos || 0,
                inventario: inventarioSeguro,
                isAdmin: (data.roles && data.roles.some(r => (r.name||r.nombre)==='ADMIN')) || data.isAdmin === true,
                habilidadesEquipadas: data.habilidadesEquipadas || [],
                colorTema: data.colorTema || null,
                avatar: data.avatar || data.avatarConfig || (data.username || data.nombreUsuario)
            };
            updateUserUI();
            showScreen('dashboard-screen');
            verificarTutorial();
        } else { showScreen('login-screen'); }
    } catch (e) { showScreen('login-screen'); }
}

export async function login() {
    const userVal = document.getElementById('username').value.trim();
    const passVal = document.getElementById('password').value.trim();
    const errorMsg = document.getElementById('error-msg');
    if (!userVal || !passVal) return errorMsg.classList.remove('hidden');

    try {
        const btnLogin = document.getElementById('btn-login');
        btnLogin.innerText = "Conectando..."; btnLogin.disabled = true;

        const response = await apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ correo_usuario: userVal, contrasenha_usuario: passVal }) });
        btnLogin.innerText = "Entrar a la Arena"; btnLogin.disabled = false;

        if (!response.ok) throw new Error("Inválidas");

        localStorage.setItem('isLogged', 'true');
        inicializarTaller();
        await restoreSession();
        errorMsg.classList.add('hidden');
        fetchMorePhrases();
    } catch (e) {
        errorMsg.innerText = "Credenciales inválidas.";
        errorMsg.classList.remove('hidden');
    }
}

export async function logout() {
    try { await apiFetch('/usuarios/logout-manual', { method: 'POST' }); } catch (e) {}
    finally {
        AppState.currentUser = null;
        localStorage.clear(); sessionStorage.clear();
        setTimeout(() => window.location.href = "index.html", 200);
    }
}