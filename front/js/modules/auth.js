// js/modules/auth.js
import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { showScreen, updateUserUI } from '../core/ui.js';

export async function restoreSession() {
    if (localStorage.getItem('theme') === 'light') {
        document.body.classList.add('light-mode');
        const btnTheme = document.getElementById('theme-toggle');
        if (btnTheme) btnTheme.innerText = "☀️";
    }

    try {
        const timestamp = new Date().getTime();
        const response = await apiFetch(`/usuarios/perfil?t=${timestamp}`, { method: 'GET' });

        if (response.ok) {
            const data = await response.json();

            let inventarioSeguro = [];
            const itemsBackend = data.items || data.inventario || [];
            if (Array.isArray(itemsBackend)) {
                inventarioSeguro = itemsBackend.map(item => ({
                    id_item: item.idItem || item.id_item || item.id,
                    nombre: item.nombre || item.name,
                    descripcion: item.descripcion || item.description
                }));
            }

            let esAdmin = false;
            if (data.roles && Array.isArray(data.roles)) {
                esAdmin = data.roles.some(rol => (rol.name || rol.nombre) === 'ADMIN');
            } else if (data.isAdmin === true) {
                esAdmin = true;
            }

            AppState.currentUser = {
                username: data.nombreUsuario || data.username || data.nombre_usuario || 'Piloto',
                nombreReal: data.nombre || data.nombre_usuario || '',
                apellido: data.apellido_usuario || data.apellidoUsuario || data.apellido || '',
                creditos: data.creditos || 0,
                inventario: inventarioSeguro,
                isAdmin: esAdmin,
                habilidadesEquipadas: data.habilidadesEquipadas || [],
                colorTema: data.colorTema || null,
                avatar: data.avatar || data.avatarConfig || (data.username || data.nombreUsuario)
            };

            updateUserUI();
            showScreen('dashboard-screen');
            // TODO: Inicializar avatar (Lo haremos en el siguiente paso)

        } else {
            showScreen('login-screen');
        }
    } catch (error) {
        showScreen('login-screen');
    }
}

export async function login() {
    const userVal = document.getElementById('username').value.trim();
    const passVal = document.getElementById('password').value.trim();
    const errorMsg = document.getElementById('error-msg');

    if (!userVal || !passVal) {
        errorMsg.innerText = "Por favor, rellena ambos campos.";
        errorMsg.classList.remove('hidden');
        return;
    }

    try {
        const btnLogin = document.getElementById('btn-login');
        const originalText = btnLogin.innerText;
        btnLogin.innerText = "Conectando...";
        btnLogin.disabled = true;

        const response = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ correo_usuario: userVal, contrasenha_usuario: passVal })
        });

        btnLogin.innerText = originalText;
        btnLogin.disabled = false;

        if (!response.ok) throw new Error("Credenciales inválidas");

        localStorage.setItem('isLogged', 'true');
        errorMsg.classList.add('hidden');

        // Llamamos a restaurar sesión para que cargue todo el perfil de la BD
        await restoreSession();

    } catch (error) {
        errorMsg.innerText = "Credenciales inválidas o servidor desconectado.";
        errorMsg.classList.remove('hidden');
    }
}

export async function logout() {
    try {
        await apiFetch('/usuarios/logout-manual', { method: 'POST' });
    } catch (error) {
        console.error("Error al salir:", error);
    } finally {
        AppState.currentUser = null;
        localStorage.clear();
        sessionStorage.clear();
        setTimeout(() => window.location.href = "index.html", 200);
    }
}