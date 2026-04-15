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
    rivalNombre: "Rival",

    conectar: function(codigoSala, esHost = false) {
        this.salaActual = codigoSala.toUpperCase().trim();
        this.esHost = esHost;
        this.rivalFinalizado = false;
        this.puntosRival = 0;

        // Extraer el nombre de usuario del correo (lo que hay antes de la @)
        if (AppState.currentUser && AppState.currentUser.correo) {
            miNombreUsuario = AppState.currentUser.correo.split('@')[0];
        } else if (AppState.currentUser && AppState.currentUser.username) {
            miNombreUsuario = AppState.currentUser.username;
        }

        // Mostrar HUD
        const hud = document.getElementById('online-hud');
        if (hud) hud.style.display = 'flex';
        document.getElementById('hud-my-name').innerText = miNombreUsuario;
        document.getElementById('hud-op-name').innerText = `Sala: ${this.salaActual} - Esperando...`;

        // 🚨 CAMBIA ESTO POR TU URL DE RAILWAY SI PRUEBAS EN PRODUCCIÓN
        const socket = new SockJS(`${API_URL}/ws-game`);
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Ocultar logs de consola para no ensuciar

        stompClient.connect({}, () => {
            console.log(`📡 Conectado a la sala: ${this.salaActual}`);

            // Suscribirse a la radio de la sala
            stompClient.subscribe(`/topic/partida/${this.salaActual}`, (mensaje) => {
                const data = JSON.parse(mensaje.body);

                // Ignorar mis propios mensajes
                if (data.jugador === miNombreUsuario) return;

                this.rivalNombre = data.jugador;

                // 🚦 GESTIÓN DE SEÑALES
                switch (data.type) {
                    case 'PLAYER_JOINED':
                        if (this.esHost) {
                            // El invitado ha llegado. Le digo que empiece la batalla.
                            this.enviarSeñal('BATTLE_START', { hostName: miNombreUsuario });
                            // Y yo también lanzo mi pantalla VS
                            window.dispatchEvent(new CustomEvent('multiplayer-vs-screen', {
                                detail: { host: miNombreUsuario, guest: data.jugador }
                            }));
                        }
                        break;

                    case 'BATTLE_START':
                        if (!this.esHost) {
                            // El Host me confirma la batalla, lanzo mi pantalla VS
                            window.dispatchEvent(new CustomEvent('multiplayer-vs-screen', {
                                detail: { host: data.hostName, guest: miNombreUsuario }
                            }));
                        }
                        break;

                    case 'FINISH':
                        this.rivalFinalizado = true;
                        this.puntosRival = data.puntuacion;
                        window.dispatchEvent(new CustomEvent('rival-finished'));
                        break;

                    case 'PROGRESS':
                        // Actualizar puntos del rival en el HUD
                        document.getElementById('hud-op-name').innerText = data.jugador;
                        document.getElementById('hud-op-score').innerText = data.puntuacion;

                        // Efecto visual de parpadeo
                        const opScoreSpan = document.getElementById('hud-op-score');
                        opScoreSpan.style.color = "var(--primary)";
                        setTimeout(() => opScoreSpan.style.color = "", 300);
                        break;
                }
            });

            // Si soy el invitado, aviso a la sala de que he llegado
            if (!this.esHost) {
                this.enviarSeñal('PLAYER_JOINED');
            }
        });
    },

    enviarSeñal: function(type, extra = {}) {
        if (stompClient && stompClient.connected && this.salaActual) {
            const paquete = {
                type: type,
                jugador: miNombreUsuario,
                puntuacion: AppState.game ? AppState.game.score : 0,
                ...extra
            };
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