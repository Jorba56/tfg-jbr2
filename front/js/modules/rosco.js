// js/modules/rosco.js
import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { showScreen } from '../core/ui.js';

// Variables privadas del Rosco
let letrasRosco = [];
let indiceLetraActual = 0;
let roscoCompletado = false;

export async function startRosco() {
    showScreen('rosco-screen'); // Asegúrate de tener un id "rosco-screen" en tu HTML
    document.getElementById('rosco-definition').innerHTML = "Generando rosco con IA...";

    try {
        // Tu llamada a la IA (ajusta la URL según tu código)
        const response = await apiFetch(`/incidencias/game/rosco-ia?tema=programacion`);
        if (!response.ok) throw new Error("Fallo en la IA");

        letrasRosco = await response.json(); // Array de [{letra: 'A', definicion: '...', respuesta: '...'}, ...]
        indiceLetraActual = 0;
        roscoCompletado = false;

        dibujarCirculoRosco();
        mostrarDefinicion();

    } catch(error) {
        document.getElementById('rosco-definition').innerHTML = "Fallo en la conexión del Rosco.";
    }
}

function dibujarCirculoRosco() {
    const contenedor = document.getElementById('rosco-circle');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    const radio = 120; // Tamaño del círculo
    const centro = 150;

    letrasRosco.forEach((item, index) => {
        const angulo = (index / letrasRosco.length) * (2 * Math.PI) - (Math.PI / 2);
        const x = centro + radio * Math.cos(angulo);
        const y = centro + radio * Math.sin(angulo);

        const letraDiv = document.createElement('div');
        letraDiv.className = 'rosco-letter bg-blue-500 text-white rounded-full absolute flex items-center justify-center font-bold w-8 h-8 transition-all';
        letraDiv.style.left = `${x}px`;
        letraDiv.style.top = `${y}px`;
        letraDiv.innerText = item.letra;
        letraDiv.id = `letra-${item.letra}`;

        contenedor.appendChild(letraDiv);
    });
}

function mostrarDefinicion() {
    if (roscoCompletado) return;
    const itemActual = letrasRosco[indiceLetraActual];
    document.getElementById('rosco-definition').innerText = `Con la ${itemActual.letra}: ${itemActual.definicion}`;

    // Resaltar letra actual en el círculo
    document.querySelectorAll('.rosco-letter').forEach(el => el.classList.remove('ring-4', 'ring-yellow-400', 'scale-125'));
    document.getElementById(`letra-${itemActual.letra}`).classList.add('ring-4', 'ring-yellow-400', 'scale-125');
}

export function checkRoscoAnswer(e) {
    e.preventDefault(); // Si es un formulario
    const input = document.getElementById('rosco-input');
    const respuestaUsuario = input.value.trim().toLowerCase();
    const itemActual = letrasRosco[indiceLetraActual];

    const esCorrecta = respuestaUsuario === itemActual.respuesta.toLowerCase();
    const letraDiv = document.getElementById(`letra-${itemActual.letra}`);

    if (esCorrecta) {
        letraDiv.classList.replace('bg-blue-500', 'bg-green-500'); // Acierto
    } else {
        letraDiv.classList.replace('bg-blue-500', 'bg-red-500'); // Fallo
    }

    input.value = '';
    avanzarRosco();
}

export function pasarPalabra(e) {
    if(e) e.preventDefault();
    avanzarRosco();
}

function avanzarRosco() {
    indiceLetraActual++;
    if (indiceLetraActual >= letrasRosco.length) {
        roscoCompletado = true;
        alert("¡Rosco terminado!");
        showScreen('dashboard-screen');
    } else {
        mostrarDefinicion();
    }
}