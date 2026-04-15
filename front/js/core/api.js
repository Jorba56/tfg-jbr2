import { AppState } from './state.js';

export const API_URL = ''; // Se queda vacío para que las llamadas HTTP vayan a Render
// 🚨 TRUCO DE VELOCIDAD: Pon aquí tu URL directa de Railway (Ej: https://tu-backend.up.railway.app)
const WS_URL = 'https://gateway-production-a1f6.up.railway.app';

export let miNombreUsuario = "Piloto_" + Math.floor(Math.random() * 1000);
export let miAvatar = "👤"; // Avatar por defecto por si acaso

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
    rivalAvatar: "👤", // Guardamos el avatar del rival

    conectar: function(codigoSala, esHost = false) {
        if (!codigoSala) return;
        this.salaActual = codigoSala.toUpperCase().trim();
        this.esHost = esHost;
        this.rivalFinalizado = false;
        this.puntosRival = 0;

        // 🚨 1. COGEMOS LA IDENTIDAD REAL DEL ESTADO DEL JUEGO
        if (AppState.currentUser) {
            // Prioridad absoluta al username real. Si no hay, pillamos el del correo.
            miNombreUsuario = AppState.currentUser.username || AppState.currentUser.nombreUsuario || (AppState.currentUser.correo ? AppState.currentUser.correo.split('@')[0] : miNombreUsuario);
            miAvatar = AppState.currentUser.avatar || "👤";
        }

        const hud = document.getElementById('online-hud');
        if (hud) hud.style.display = 'flex';
        document.getElementById('hud-my-name').innerText = miNombreUsuario;
        document.getElementById('hud-op-name').innerText = `Sala: ${this.salaActual} - Esperando...`;

        const socket = new SockJS(`${WS_URL}/ws-game`);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            console.log(`📡 Conectado a la sala privada: ${this.salaActual}`);

            stompClient.subscribe(`/topic/partida/${this.salaActual}`, (mensaje) => {
                const data = JSON.parse(mensaje.body);
                if (data.jugador === miNombreUsuario) return; // Ignoro mis propios mensajes

                this.rivalNombre = data.jugador;
                if (data.avatar) this.rivalAvatar = data.avatar;

                switch (data.type) {
                    case 'PLAYER_JOINED':
                        if (this.esHost) {
                            // Le paso mi nombre Y MI AVATAR al invitado
                            this.enviarSeñal('BATTLE_START', { hostName: miNombreUsuario, hostAvatar: miAvatar });

                            // Disparo mi pantalla Versus con los datos de ambos
                            window.dispatchEvent(new CustomEvent('multiplayer-vs-screen', {
                                detail: { host: miNombreUsuario, hostAvatar: miAvatar, guest: data.jugador, guestAvatar: data.avatar }
                            }));
                        }
                        break;
                    case 'BATTLE_START':
                        if (!this.esHost) {
                            // El host me responde. Disparo mi pantalla Versus.
                            window.dispatchEvent(new CustomEvent('multiplayer-vs-screen', {
                                detail: { host: data.hostName, hostAvatar: data.hostAvatar, guest: miNombreUsuario, guestAvatar: miAvatar }
                            }));
                        }
                        break;

                    // ... (Aquí dejas los case 'FINISH' y 'PROGRESS' que ya tenías)
                    case 'FINISH':
                        this.rivalFinalizado = true;
                        this.puntosRival = data.puntuacion;
                        window.dispatchEvent(new CustomEvent('rival-finished'));
                        break;
                    case 'PROGRESS':
                        document.getElementById('hud-op-name').innerText = data.jugador;
                        document.getElementById('hud-op-score').innerText = data.puntuacion;
                        const opScoreSpan = document.getElementById('hud-op-score');
                        if (opScoreSpan) {
                            opScoreSpan.style.color = "var(--primary)";
                            setTimeout(() => opScoreSpan.style.color = "", 300);
                        }
                        break;
                }
            });

            if (!this.esHost) this.enviarSeñal('PLAYER_JOINED');
        });
    },

    enviarSeñal: function(type, extra = {}) {
        if (stompClient && stompClient.connected && this.salaActual) {
            // 🚨 2. METEMOS EL AVATAR EN EL PAQUETE DE DATOS
            const paquete = {
                type: type,
                jugador: miNombreUsuario,
                avatar: miAvatar,
                puntuacion: AppState.game ? AppState.game.score : 0,
                ...extra
            };
            stompClient.send(`/app/progreso/${this.salaActual}`, {}, JSON.stringify(paquete));
        }
    },
    enviarProgreso: function(puntosActuales) {
        if (document.getElementById('hud-my-score')) document.getElementById('hud-my-score').innerText = puntosActuales;
        this.enviarSeñal('PROGRESS', { puntuacion: puntosActuales });
    },

    desconectar: function() {
        if (stompClient !== null) stompClient.disconnect();
        this.salaActual = null;
        if (document.getElementById('online-hud')) document.getElementById('online-hud').style.display = 'none';
    }
};