import { AppState } from './state.js';
import { apiFetch } from '../core/api.js';

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
    cargarEstadisticasDashboard();
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

export function showToast(mensaje, tipo = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    // 1. Creamos el elemento
    const toast = document.createElement('div');
    toast.className = `cyber-toast ${tipo}`; // Se le asigna 'success' o 'error'

    // 2. Le ponemos un icono dependiendo del tipo
    const icono = tipo === 'success' ? '✅' : '❌';
    toast.innerHTML = `<span>${icono}</span> <span>${mensaje}</span>`;

    // 3. Lo añadimos a la pantalla
    container.appendChild(toast);

    // 4. Programamos su destrucción tras 3 segundos
    setTimeout(() => {
        toast.classList.add('fade-out'); // Empieza la animación de salida

        // Esperamos a que acabe la animación (500ms) para borrar el HTML y liberar memoria
        setTimeout(() => {
            if (container.contains(toast)) {
                toast.remove();
            }
        }, 500);
    }, 3000); // <-- 3000ms = 3 segundos en pantalla
}
export async function cargarEstadisticasDashboard() {
    const divEstadisticas = document.getElementById('stats');
    if (!divEstadisticas) return;

    // 1. Obtenemos el correo del estado global
    const id = AppState.currentUser.id;

    if (!id) {
        console.warn("No hay id disponible para cargar estadísticas.");
        return;
    }

    try {
        // 2. Petición directa al endpoint de búsqueda por correo
        const response = await apiFetch(`/usuarios/estadisticas/${id}`);

        if (!response.ok) throw new Error("No se pudo obtener la telemetría");

        const stats = await response.json();

        // 3. Pintamos los resultados en el Box del Dashboard
        divEstadisticas.innerHTML = `
            <div style="display: flex; gap: 20px; flex-wrap: wrap; margin-top: 10px; font-size: 0.9rem;">
                <div><strong>⏱Contrarreloj:</strong> <span style="color: magenta;">${stats.mejorContrarreloj || 0}</span></div>
                <div><strong>Rosco:</strong> <span style="color: gold;">${stats.mejorRosco || 0}</span></div>
                <div><strong>Precisión:</strong> <span style="color: blue;">${stats.pctFrases || 0}%</span></div>
            </div>
            
            <div style="margin-top: 10px; width: 100%; max-width: 300px; background: rgba(255,255,255,0.1); border-radius: 4px; overflow: hidden; height: 6px;">
                <div style="width: 0%; background: blue; height: 100%; transition: width 1s ease-out;" id="dash-bar-precision"></div>
            </div>
        `;

        // Animación de la barra de precisión
        setTimeout(() => {
            const bar = document.getElementById('dash-bar-precision');
            if (bar) bar.style.width = `${stats.pctFrases || 0}%`;
        }, 100);

    } catch (error) {
        console.error("Error al cargar stats:", error);
        divEstadisticas.innerHTML = '<p style="font-size:0.7rem; color:gray;">Telemetría no disponible actualmente</p>';
    }
}