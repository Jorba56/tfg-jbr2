// js/main.js

// 1. IMPORTACIÓN DE NÚCLEO Y ESTADO
import { AppState } from './core/state.js';
import { toggleTheme, showScreen, updateUserUI } from './core/ui.js';
import { login, logout, restoreSession } from './modules/auth.js';
import { startGame, abortGame, checkInput, handleKeydown } from './modules/game.js';
import { startRosco, checkRoscoAnswer, pasarPalabra } from './modules/rosco.js';
import { updateAvatarPreview, guardarPerfil } from './modules/avatar.js';

// --- 🛠️ SOLUCIÓN PARA EL HTML (PUENTE GLOBAL) ---
// Como el HTML usa inline events (onclick="app..."), exponemos estas funciones al objeto window

window.app = {
    showScreen: showScreen,
    openProfile: () => {
        showScreen('profile-screen');
        updateAvatarPreview(); // Forzamos a que pinte el avatar al abrir la pestaña
    },
    openShop: () => showScreen('shop-screen'),
    iniciarPartidaPersonalizada: () => console.log("Modo personalizado en desarrollo..."),
    completarTutorial: () => showScreen('dashboard-screen')
};

window.Rosco = {
    init: startRosco,
    check: checkRoscoAnswer,
    pass: pasarPalabra,
    stop: () => showScreen('dashboard-screen')
};

// Funciones de la Interfaz (Sidebar y Pestañas)
window.toggleSidebar = () => {
    document.getElementById('fc-sidebar')?.classList.toggle('active');
    document.getElementById('sidebar-overlay')?.classList.toggle('active');
};

window.switchProfileTab = (tabId) => {
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));

    document.getElementById(`tab-${tabId}`)?.classList.add('active');
    // Para marcar el botón clickeado como activo
    if (window.event && window.event.currentTarget) {
        window.event.currentTarget.classList.add('active');
    }
};

// --- ARRANQUE DEL SISTEMA ---
window.onload = () => {
    if (localStorage.getItem('isLogged') === 'true') {
        restoreSession();
    } else {
        showScreen('login-screen');
    }

    if (localStorage.getItem('theme') === 'light') {
        document.body.classList.add('light-mode');
        const btnTheme = document.getElementById('theme-toggle');
        if (btnTheme) btnTheme.innerHTML = '<i class="fas fa-sun"></i> <span>Modo Claro</span>';
    }
};

// --- CONEXIÓN DE EVENTOS DIRECTOS (LISTENERS) ---
document.addEventListener('DOMContentLoaded', () => {

    // Autenticación y generales
    document.getElementById('btn-login')?.addEventListener('click', login);
    // Nota: el botón de logout del sidebar ya llama a app.logout o toggleSidebar. Lo conectamos por si acaso.
    document.getElementById('btn-logout')?.addEventListener('click', logout);
    document.getElementById('theme-toggle')?.addEventListener('click', toggleTheme);

    // Juego de Frases
    document.getElementById('btn-abort')?.addEventListener('click', abortGame);
    const gameInput = document.getElementById('game-input');
    if (gameInput) {
        gameInput.addEventListener('input', checkInput);
        gameInput.addEventListener('keydown', handleKeydown);
        gameInput.addEventListener('paste', e => e.preventDefault());
    }

    // Taller de Avatares (Capturando los selectores)
    document.getElementById('av-top')?.addEventListener('change', updateAvatarPreview);
    document.getElementById('av-acc')?.addEventListener('change', updateAvatarPreview);

    // ⚠️ NUEVO ID: El HTML usa 'update-profile-form'
    document.getElementById('update-profile-form')?.addEventListener('submit', guardarPerfil);

    // Si pulsan el botón específico de guardar look sin enviar el formulario entero:
    document.getElementById('btn-save-avatar')?.addEventListener('click', (e) => {
        e.preventDefault();
        guardarPerfil(e);
    });

    // Botones de Navegación Finales
    document.getElementById('btn-back-profile')?.addEventListener('click', () => showScreen('dashboard-screen'));
    document.getElementById('btn-back-shop')?.addEventListener('click', () => showScreen('dashboard-screen'));
    document.getElementById('btn-retry')?.addEventListener('click', startGame);
    document.getElementById('btn-menu')?.addEventListener('click', () => showScreen('dashboard-screen'));
});