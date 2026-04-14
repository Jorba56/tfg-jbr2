import { AppState } from './core/state.js';
import { toggleTheme, showScreen, toggleSidebar, switchProfileTab, openTutorial, completarTutorial } from './core/ui.js';
import { login, logout, restoreSession } from './modules/auth.js';
import { startGame, iniciarPartidaPersonalizada, checkInput, handleKeydown, abortGame, activarPowerUp } from './modules/game.js';
import { Rosco } from './modules/rosco.js';
import { updateAvatarPreview, openProfile, toggleEquip, guardarPerfilForm, guardarLookBtn } from './modules/profile.js';
import { openShop, comprarObjeto } from './modules/shop.js';
import { openRanking, openPlayerStats, closeStatsModal } from './modules/ranking.js';

// --- PUENTE HACIA EL HTML (Inline events) ---
window.app = {
    showScreen, openProfile, openShop, iniciarPartidaPersonalizada,
    completarTutorial, toggleEquip, comprarObjeto, abortGame, startGame, checkInput, handleKeydown,
    openRanking: openRanking,
    openPlayerStats: openPlayerStats,
    closeStatsModal: closeStatsModal
};
window.Rosco = Rosco;
window.toggleSidebar = toggleSidebar;
window.switchProfileTab = switchProfileTab;
window.activarPowerUp = activarPowerUp;

window.onload = () => {
    if (localStorage.getItem('isLogged') === 'true') restoreSession();
    else showScreen('login-screen');
    if (localStorage.getItem('theme') === 'light') toggleTheme();
};

document.addEventListener('DOMContentLoaded', () => {
    // Escuchadores de Clicks Rápidos
    document.getElementById('tutorial_comp')?.addEventListener('click', completarTutorial);
    document.getElementById('card-tutorial')?.addEventListener('click', openTutorial);
    document.getElementById('theme-toggle')?.addEventListener('click', toggleTheme);
    document.getElementById('btn-login')?.addEventListener('click', login);
    document.getElementById('btn-logout')?.addEventListener('click', logout);

    // Tarjetas principales
    document.getElementById('card-play')?.addEventListener('click', startGame);
    document.getElementById('card-profile')?.addEventListener('click', openProfile);
    document.getElementById('card-shop')?.addEventListener('click', openShop);

    // Taller
    document.getElementById('av-top')?.addEventListener('change', updateAvatarPreview);
    document.getElementById('av-acc')?.addEventListener('change', updateAvatarPreview);
    document.getElementById('btn-save-avatar')?.addEventListener('click', (e) => { e.preventDefault(); guardarLookBtn(e); });
    document.getElementById('update-profile-form')?.addEventListener('submit', guardarPerfilForm);

    // Botones de Navegación
    document.getElementById('btn-abort')?.addEventListener('click', abortGame);
    document.getElementById('btn-menu')?.addEventListener('click', () => showScreen('dashboard-screen'));
    document.getElementById('card-admin').addEventListener('click', () => {
        window.location.href = 'gestion-usuarios.html';
    });
    document.getElementById('btn-back-profile')?.addEventListener('click', () => showScreen('dashboard-screen'));
    document.getElementById('btn-back-shop')?.addEventListener('click', () => showScreen('dashboard-screen'));
    document.getElementById('btn-retry')?.addEventListener('click', startGame);

    // Eventos del Input del Juego principal
    const gameInput = document.getElementById('game-input');
    if (gameInput) {
        gameInput.addEventListener('input', checkInput);
        gameInput.addEventListener('keydown', handleKeydown);
        gameInput.addEventListener('paste', e => e.preventDefault());
        gameInput.addEventListener('contextmenu', e => e.preventDefault());
    }

    // Input del Rosco
    document.getElementById('r-input')?.addEventListener('keypress', e => { if(e.key === 'Enter') Rosco.check(); });
});