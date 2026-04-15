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
    salaActual: null,
    esHost: false,
    puntosRival: 0,
    rivalFinalizado: false,

    conectar: function(codigoSala, esHost = false) {
        this.salaActual = codigoSala.toUpperCase().trim();
        this.esHost = esHost;
        this.rivalFinalizado = false;
        this.puntosRival = 0;

        const socket = new SockJS(`${API_URL}/ws-game`);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            stompClient.subscribe(`/topic/partida/${this.salaActual}`, (msg) => {
                const data = JSON.parse(msg.body);

                if (data.type === 'START_SIGNAL') {
                    // 🚨 AMBOS empiezan a la vez cuando llega esta señal
                    window.dispatchEvent(new CustomEvent('multiplayer-start'));
                } else if (data.jugador !== miNombreUsuario) {
                    if (data.type === 'FINISH') {
                        this.rivalFinalizado = true;
                        this.puntosRival = data.puntuacion;
                        window.dispatchEvent(new CustomEvent('rival-finished'));
                    } else {
                        actualizarHUDRival(data.jugador, data.puntuacion);
                    }
                }
            });

            // Si soy el invitado, envío señal de que he llegado para empezar
            if (!this.esHost) {
                this.enviarSeñal('START_SIGNAL');
            }
        });
    },

    enviarSeñal: function(type, extra = {}) {
        if (stompClient?.connected) {
            const paquete = {
                type: type,
                jugador: miNombreUsuario,
                puntuacion: AppState.game.score,
                ...extra
            };
            stompClient.send(`/app/progreso/${this.salaActual}`, {}, JSON.stringify(paquete));
        }
    }
};