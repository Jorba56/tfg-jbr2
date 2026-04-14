// js/modules/game.js
import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { showScreen, updateUserUI } from '../core/ui.js';

export async function startGame() {
    AppState.game.score = 0;
    AppState.game.timeLeft = 60;
    AppState.game.phraseBuffer = [];

    showScreen('game-screen');
    const input = document.getElementById('game-input');
    const display = document.getElementById('phrase-display');

    if (input) {
        input.value = '';
        input.disabled = true;
        display.innerHTML = '<span class="text-blue-500 animate-pulse">Cargando motores...</span>';
    }

    try {
        // 1. Pedimos la frase al servidor
        // (Sustituye esta URL por la tuya si era distinta, ej: '/incidencias/game/frase')
        const response = await apiFetch(`/incidencias/game/frase?dificultad=MEDIA`);
        if (!response.ok) throw new Error("Fallo en la red de la pista");

        const data = await response.json();
        AppState.game.currentPhrase = data.texto || "Frase de emergencia en caso de fallo de servidor.";

        // 2. Pintamos la frase
        display.innerHTML = AppState.game.currentPhrase;

        // 3. Arrancamos el coche
        input.disabled = false;
        input.focus();
        iniciarTemporizador();

    } catch (error) {
        display.innerHTML = '<span class="text-red-500">Error al cargar la pista. Intenta de nuevo.</span>';
    }
}

function iniciarTemporizador() {
    clearInterval(AppState.game.timerInterval);
    const timerDisplay = document.getElementById('timer-display');

    AppState.game.timerInterval = setInterval(() => {
        AppState.game.timeLeft--;
        if (timerDisplay) timerDisplay.innerText = AppState.game.timeLeft;

        if (AppState.game.timeLeft <= 0) {
            endGame();
        }
    }, 1000);
}

export function checkInput(e) {
    const textoEscrito = e.target.value;
    const fraseObjetivo = AppState.game.currentPhrase;

    // Aquí pegas tu lógica de colores (verde si acierta, rojo si falla)
    // ...

    // Si ha terminado la frase entera:
    if (textoEscrito === fraseObjetivo) {
        endGame(true); // true = Victoria
    }
}

export function handleKeydown(e) {
    // Tu lógica antibloqueo (ej. no dejar borrar si es hardcore)
    if (e.key === 'Backspace' && AppState.game.modoHardcore) {
        e.preventDefault();
    }
}

export async function endGame(victoria = false) {
    clearInterval(AppState.game.timerInterval);
    document.getElementById('game-input').disabled = true;

    if (victoria) {
        alert("¡Vuelta rápida! Has completado la frase.");
        // Aquí tu lógica para sumar créditos:
        // await apiFetch(`/usuarios/actualizar-creditos?cantidad=50`, { method: 'PUT' });
        // updateUserUI();
    } else {
        alert("¡Tiempo agotado! Has pinchado una rueda.");
    }

    showScreen('dashboard-screen');
}

export function abortGame() {
    if (!confirm('¿Seguro que quieres salir de boxes? Perderás el progreso de esta carrera.')) return;
    clearInterval(AppState.game.timerInterval);
    showScreen('dashboard-screen');
}