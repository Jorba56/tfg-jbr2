import { AppState } from './state.js';
export const API_URL = '';
let stompClient = null;
let miNombreUsuario = "Piloto_" + Math.floor(Math.random() * 1000);

export async function apiFetch(endpoint, options = {}) {
    const defaultOptions = { credentials: 'include', headers: { 'Content-Type': 'application/json' } };
    const finalOptions = { ...defaultOptions, ...options, headers: { ...defaultOptions.headers, ...options.headers } };
    if (finalOptions.method === 'GET' || !options.body) delete finalOptions.headers['Content-Type'];
    return fetch(`${API_URL}${endpoint}`, finalOptions);
}

export const Multiplayer = {
    salaActual: null, // Guardamos en qué sala estamos compitiendo

    conectar: function(codigoSala) {
        if (!codigoSala) {
            alert("Necesitas un código de sala para competir.");
            return;
        }

        this.salaActual = codigoSala.toUpperCase().trim();

        // 🚨 CORTAMOS EL CORREO: Cogemos todo lo que hay antes del '@'
        if (AppState.currentUser && AppState.currentUser.correo) {
            miNombreUsuario = AppState.currentUser.correo.split('@')[0];
        }

        const hud = document.getElementById('online-hud');
        if (hud) hud.style.display = 'flex';
        document.getElementById('hud-my-name').innerText = miNombreUsuario;
        document.getElementById('hud-op-name').innerText = `Sala: ${this.salaActual} - Esperando...`;

        const socket = new SockJS(`${API_URL}/ws-game`);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            console.log(`📡 Conectado a la sala privada: ${Multiplayer.salaActual}`);

            // 🚨 Sintonizamos EXCLUSIVAMENTE el canal de esta sala
            stompClient.subscribe(`/topic/partida/${Multiplayer.salaActual}`, function (mensajeRebota) {
                const datos = JSON.parse(mensajeRebota.body);

                if (datos.jugador !== miNombreUsuario) {
                    document.getElementById('hud-op-name').innerText = datos.jugador;
                    document.getElementById('hud-op-score').innerText = datos.puntuacion;

                    const opScoreSpan = document.getElementById('hud-op-score');
                    opScoreSpan.style.color = "var(--primary)";
                    setTimeout(() => opScoreSpan.style.color = "", 300);
                }
            });

            Multiplayer.enviarProgreso(0);
        });
    },

    enviarProgreso: function(puntosActuales) {
        // Solo enviamos si el túnel está abierto y estamos en una sala
        if (stompClient && stompClient.connected && this.salaActual) {
            document.getElementById('hud-my-score').innerText = puntosActuales;

            const paquete = {
                jugador: miNombreUsuario,
                puntuacion: puntosActuales
            };

            // 🚨 Enviamos el paquete al buzón exclusivo de la sala
            stompClient.send(`/app/progreso/${this.salaActual}`, {}, JSON.stringify(paquete));
        }
    },

    desconectar: function() {
        if (stompClient !== null) {
            stompClient.disconnect();
        }
        console.log("📡 Desconectado de la sala.");
        this.salaActual = null;
        const hud = document.getElementById('online-hud');
        if (hud) hud.style.display = 'none';
    }
};