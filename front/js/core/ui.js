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
    // 1. Limpiamos las clases cosméticas por si acaso
    document.body.classList.remove('tema-cyberpunk', 'teclado-neon');

    if (!AppState.currentUser) return;

    // 2. Buscamos qué tiene equipado el usuario
    const equipado = AppState.currentUser.inventario.find(i => (i.id_item || i.idItem) == (AppState.currentUser.colorTema || AppState.currentUser.color_tema));

    if (equipado) {
        const nombreNormal = equipado.nombre.trim();
        if (nombreNormal === 'Tema Cyberpunk') {
            document.body.classList.add('tema-cyberpunk');
            localStorage.setItem('theme', 'cyberpunk'); // <-- Usando variable 'theme'
        } else if (nombreNormal === 'Teclado Neón') {
            document.body.classList.add('teclado-neon');
            localStorage.setItem('theme', 'neon'); // <-- Usando variable 'theme'
        }
    } else {
        // 3. SALVAVIDAS: Si el servidor no responde rápido, leemos la variable 'theme'
        const currentTheme = localStorage.getItem('theme');
        if (currentTheme === 'cyberpunk') {
            document.body.classList.add('tema-cyberpunk');
        } else if (currentTheme === 'neon') {
            document.body.classList.add('teclado-neon');
        }
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

// Abrir y cerrar la ventana con animación
window.toggleChatbot = function() {
    const chat = document.getElementById('chatbot-window');
    chat.classList.toggle('mostrar-chat'); // Añade/quita la clase mágica
};

// Cambiar el texto según la pregunta elegida
window.responderChat = function(pregunta) {
    const respuesta = document.getElementById('chatbot-respuesta');

    if(pregunta === 'creditos') {
        respuesta.innerHTML = "<strong style='color: darkgoldenrod;'>Créditos:</strong> Los consigues jugando partidas en o ganando duelos online. ¡Más palabras, más recompensas!";
    }
    if(pregunta === 'powerups') {
        respuesta.innerHTML = "<strong style='color:#00f2fe;'>Power-Ups:</strong> Cómpralos en el Garaje. Te permiten congelar el tiempo, perdonar errores o multiplicar tus puntos en carrera.";
    }
    if(pregunta === 'online') {
        respuesta.innerHTML = "Ve a <strong>Partida Online</strong>. Un jugador crea la sala y pasa el código. El otro introduce el código para unirse al túnel WebSockets. ¡Que gane el más rápido!";
    }
};

export async function cargarEstadisticasDashboard() {
    const divEstadisticas = document.getElementById('stats');
    if (!divEstadisticas) return;

    // 1. Ahora sí, cogemos el ID directamente del estado global de forma elegante
    const miId = AppState.currentUser?.id;

    if (!miId) {
        console.warn("No hay ID disponible para cargar estadísticas.");
        return;
    }

    try {
        divEstadisticas.innerHTML = '<p style="font-size: 0.8rem; color: blue;">Cargando telemetría...</p>';

        // 2. Petición directa a Java
        const response = await apiFetch(`/usuarios/estadisticas/${miId}`);

        if (!response.ok) throw new Error("No se pudo obtener la telemetría");

        const stats = await response.json();

        // 3. Pintamos los resultados en el Box
        divEstadisticas.innerHTML = `
        <div class="stats-grid-box">
            <div class="stat-item"><strong>Contrarreloj:</strong> <span class="stat-val cr">${stats.mejorContrarreloj || 0}</span></div>
            <div class="stat-item"><strong>Rosco:</strong> <span class="stat-val rosco">${stats.mejorRosco || 0}</span></div>
            <div class="stat-item"><strong>Precisión:</strong> <span class="stat-val pct">${stats.pctFrases || 0}%</span></div>
        </div>
        <div class="progress-track">
            <div class="progress-fill" id="dash-bar-precision"></div>
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