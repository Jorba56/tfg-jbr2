// js/core/ui.js
import { AppState } from './state.js';

export function showScreen(screenId) {
    document.querySelectorAll('.screen').forEach(s => {
        s.classList.remove('active');
        s.classList.add('hidden');
        s.style.display = '';
    });

    const target = document.getElementById(screenId);
    if (target) {
        target.classList.remove('hidden');
        target.classList.add('active');
    }

    const topNav = document.getElementById('global-top-nav');
    if (topNav) {
        if (screenId === 'login-screen' || screenId === 'registro-screen') {
            topNav.classList.add('hidden');
        } else {
            topNav.classList.remove('hidden');
        }
    }
}

export function toggleTheme() {
    const body = document.body;
    body.classList.toggle('light-mode');
    const isLight = body.classList.contains('light-mode');
    const btnTheme = document.getElementById('theme-toggle');
    if(btnTheme) btnTheme.innerText = isLight ? "☀️" : "🌙";

    localStorage.setItem('theme', isLight ? 'light' : 'dark');
}

export function updateUserUI() {
    if(!AppState.currentUser) return;

    // Pintamos los Textos del Desplegable
    const elFullName = document.getElementById('dropdown-fullname');
    if (elFullName) {
        const nombre = AppState.currentUser.nombreReal || "";
        const apellido = AppState.currentUser.apellido || "";
        const nombreCompleto = `${nombre} ${apellido}`.trim();
        elFullName.innerText = nombreCompleto !== "" ? nombreCompleto : AppState.currentUser.username;
    }

    const elUsername = document.getElementById('dropdown-username');
    if (elUsername) elUsername.innerText = `Tu Perfil`;

    const elCredits = document.getElementById('user-credits');
    if (elCredits) elCredits.innerText = AppState.currentUser.creditos;

    const adminCard = document.getElementById('card-admin');
    if (adminCard) {
        AppState.currentUser.isAdmin ? adminCard.classList.remove('hidden') : adminCard.classList.add('hidden');
    }
}