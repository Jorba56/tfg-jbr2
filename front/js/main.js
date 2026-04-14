// js/main.js

// 1. IMPORTACIÓN DE NÚCLEO Y ESTADO
import { AppState } from './core/state.js';
import { toggleTheme, showScreen, updateUserUI } from './core/ui.js';

// 2. IMPORTACIÓN DE MÓDULOS DE LÓGICA
import { login, logout, restoreSession } from './modules/auth.js';
import { startGame, abortGame, checkInput, handleKeydown } from './modules/game.js';
import { startRosco, checkRoscoAnswer, pasarPalabra } from './modules/rosco.js';
import { updateAvatarPreview, guardarPerfil } from './modules/avatar.js';

/**
 * --- ARRANQUE DEL SISTEMA ---
 * Se ejecuta nada más cargar la página.
 */
window.onload = () => {
    // Verificamos si hay una sesión activa en el servidor
    if (localStorage.getItem('isLogged') === 'true') {
        restoreSession();
    } else {
        showScreen('login-screen');
    }

    // Aplicamos el tema guardado (oscuro por defecto si no hay nada)
    if (localStorage.getItem('theme') === 'light') {
        document.body.classList.add('light-mode');
        const btnTheme = document.getElementById('theme-toggle');
        if (btnTheme) btnTheme.innerText = "☀️";
    }
};

/**
 * --- CONEXIÓN DE EVENTOS (LISTENERS) ---
 * Aquí enganchamos los IDs del HTML con las funciones de los módulos.
 */
document.addEventListener('DOMContentLoaded', () => {

    // --- SECCIÓN: AUTENTICACIÓN Y SESIÓN ---
    document.getElementById('btn-login')?.addEventListener('click', login);
    document.getElementById('btn-logout')?.addEventListener('click', logout);
    document.getElementById('theme-toggle')?.addEventListener('click', toggleTheme);

    // --- SECCIÓN: NAVEGACIÓN GENERAL ---
    // Botón para ir al panel de Admin (solo si el usuario tiene permisos)
    document.getElementById('card-admin')?.addEventListener('click', () => {
        window.location.href = 'gestion-usuarios.html';
    });

    // Botones de "Volver al Menú" desde diferentes pantallas
    document.querySelectorAll('.btn-back-to-menu').forEach(btn => {
        btn.addEventListener('click', () => showScreen('dashboard-screen'));
    });

    // --- SECCIÓN: JUEGO DE MECANOGRAFÍA (FRASES) ---
    document.getElementById('btn-play-typing')?.addEventListener('click', startGame);
    document.getElementById('btn-abort-game')?.addEventListener('click', abortGame);

    const gameInput = document.getElementById('game-input');
    if (gameInput) {
        gameInput.addEventListener('input', checkInput);
        gameInput.addEventListener('keydown', handleKeydown);
        // Protecciones básicas
        gameInput.addEventListener('paste', e => e.preventDefault());
        gameInput.addEventListener('contextmenu', e => e.preventDefault());
    }

    // --- SECCIÓN: JUEGO DEL ROSCO (IA) ---
    document.getElementById('btn-play-rosco')?.addEventListener('click', startRosco);
    document.getElementById('form-rosco')?.addEventListener('submit', (e) => {
        e.preventDefault();
        checkRoscoAnswer();
    });
    document.getElementById('btn-pasapalabra')?.addEventListener('click', pasarPalabra);

    // --- SECCIÓN: PERFIL Y TALLER DE AVATAR ---
    // Actualizar vista previa cuando cambian los selectores
    document.getElementById('av-top')?.addEventListener('change', updateAvatarPreview);
    document.getElementById('av-acc')?.addEventListener('change', updateAvatarPreview);

    // Guardar los cambios del perfil
    document.getElementById('form-perfil')?.addEventListener('submit', guardarPerfil);

    // --- SECCIÓN: TIENDA Y EXTRAS ---
    document.getElementById('btn-open-shop')?.addEventListener('click', () => showScreen('shop-screen'));

    // Escuchar clics en items de la tienda (delegación de eventos si es necesario)
    document.getElementById('container-items-tienda')?.addEventListener('click', (e) => {
        const card = e.target.closest('.item-card');
        if (card) {
            const itemId = card.dataset.itemId;
            // Aquí llamarías a una función de comprarItem(itemId) en un módulo shop.js
            console.log("Intentando comprar item:", itemId);
        }
    });

});

/**
 * --- GESTIÓN DE ERRORES GLOBALES ---
 * Captura fallos de red o de carga de módulos para no romper la experiencia.
 */
window.addEventListener('unhandledrejection', (event) => {
    console.warn('Promesa no capturada:', event.reason);
    // Podrías mostrar una pequeña notificación visual aquí
});