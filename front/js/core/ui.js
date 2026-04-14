import { AppState } from './state.js';

export function showScreen(screenId) {
    document.querySelectorAll('.screen').forEach(s => {
        s.classList.remove('active'); s.classList.add('hidden'); s.style.display = '';
    });
    const target = document.getElementById(screenId);
    if (target) { target.classList.remove('hidden'); target.classList.add('active'); }

    const topNav = document.getElementById('global-top-nav');
    if (topNav) {
        if (screenId === 'login-screen' || screenId === 'registro-screen') topNav.classList.add('hidden');
        else topNav.classList.remove('hidden');
    }
}

export function toggleTheme() {
    const body = document.body;
    body.classList.toggle('light-mode');
    const isLight = body.classList.contains('light-mode');
    document.getElementById('theme-toggle').innerHTML = isLight ? '<i class="fas fa-sun"></i> <span>Modo Claro</span>' : '<i class="fas fa-moon"></i> <span>Modo Oscuro</span>';
    localStorage.setItem('theme', isLight ? 'light' : 'dark');
}

export function toggleSidebar() {
    document.getElementById('fc-sidebar')?.classList.toggle('active');
    document.getElementById('sidebar-overlay')?.classList.toggle('active');
}

export function switchProfileTab(tabName) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    if (window.event && window.event.currentTarget) window.event.currentTarget.classList.add('active');
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById(`tab-${tabName}`).classList.add('active');

    if(tabName === 'settings' && AppState.currentUser) {
        document.getElementById('upd-username').value = AppState.currentUser.username;
        document.getElementById('upd-email').value = AppState.currentUser.email || "No disponible";
    }
}

export function updateUserUI() {
    if(!AppState.currentUser) return;
    const urlAvatar = `https://api.dicebear.com/9.x/avataaars/svg?seed=${AppState.currentUser.avatar}`;
    const mini = document.getElementById('nav-mini-avatar');
    const large = document.getElementById('dropdown-large-avatar');
    if (mini) mini.src = urlAvatar;
    if (large) large.src = urlAvatar;

    const elFullName = document.getElementById('dropdown-fullname');
    if (elFullName) {
        const nombreCompleto = `${AppState.currentUser.username} ${AppState.currentUser.apellido}`.trim();
        elFullName.innerText = nombreCompleto !== '' ? nombreCompleto : AppState.currentUser.username;
    }
    const elCredits = document.getElementById('user-credits');
    if (elCredits) elCredits.innerText = AppState.currentUser.creditos;

    const adminCard = document.getElementById('card-admin');
    if (adminCard) AppState.currentUser.isAdmin ? adminCard.classList.remove('hidden') : adminCard.classList.add('hidden');
    applyCosmetics();
}

export function applyCosmetics() {
    document.body.classList.remove('tema-cyberpunk', 'teclado-neon');
    if (!AppState.currentUser) return;
    const equipado = AppState.currentUser.inventario.find(i => (i.id_item || i.idItem) == AppState.currentUser.colorTema);
    if (equipado) {
        const nombreNormal = equipado.nombre.trim();
        if (nombreNormal === 'Tema Cyberpunk') document.body.classList.add('tema-cyberpunk');
        if (nombreNormal === 'Teclado Neón') document.body.classList.add('teclado-neon');
    }
}

export function openTutorial() { showScreen('tutorial-screen'); }
export function completarTutorial() {
    localStorage.setItem('tutorial_completado', 'true');
    verificarTutorial();
    showScreen('dashboard-screen');
}
export function verificarTutorial() {
    const completado = localStorage.getItem('tutorial_completado');
    const card = document.getElementById('card-tutorial');
    if (completado === 'true' && card) card.style.display = 'none';
}
export function animateValue(id, start, end, duration) {
    const obj = document.getElementById(id);
    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        if(obj) obj.innerHTML = Math.floor(progress * (end - start) + start);
        if (progress < 1) window.requestAnimationFrame(step);
    };
    window.requestAnimationFrame(step);
}