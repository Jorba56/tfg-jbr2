import { frasesRepository, usuariosRepository, catalogoCosmeticos } from './data.js';

function navegarA(idPantalla) {
    // Escondemos todas las secciones con clase .screen
    document.querySelectorAll('.screen').forEach(s => {
        s.classList.remove('active');
        s.classList.add('hidden');
    });

    // Mostramos solo la elegida
    const destino = document.getElementById(idPantalla);
    destino.classList.remove('hidden');
    destino.classList.add('active');

}

function updateAvatarPreview() {
    const DICEBEAR_API = 'https://api.dicebear.com/9.x/avataaars/svg';
    if (!app.currentUser) return '';

    const base = app.currentUser.username;
    const top = document.getElementById('av-top').value;
    const acc = document.getElementById('av-acc').value;

    // 1. Construimos la receta básica
    let config = `${encodeURIComponent(base)}&top=${top}`;

    // 2. 🛠️ REPARACIÓN GAFAS: Obligamos al servidor (100% probabilidad) a ponerlas
    if (acc !== 'none') {
        config += `&accessories=${acc}&accessoriesProbability=100`;
    } else {
        config += `&accessoriesProbability=0`; // Si elige "Ninguno", forzamos a 0
    }

    // 3. Pintamos la imagen al instante
    document.getElementById('avatar-preview').src = `${DICEBEAR_API}?seed=${config}`;
    return config;
}

// 🛠️ REPARACIÓN DE CARGA INICIAL: Lee la BD y coloca los botones en su sitio
function inicializarTaller() {
    if (app.currentUser && app.currentUser.avatar) {
        const savedString = app.currentUser.avatar;

        // Usamos magia de JS para extraer los parámetros de la cadena guardada
        if (savedString.includes('&')) {
            const params = new URLSearchParams(savedString.substring(savedString.indexOf('&')));

            // Colocamos los desplegables en el valor que el piloto guardó la última vez
            if (params.has('top')) document.getElementById('av-top').value = params.get('top');
            if (params.has('accessories')) document.getElementById('av-acc').value = params.get('accessories');
        }
    }
    // Forzamos la primera actualización visual para que no salga invisible
    updateAvatarPreview();
}

// Escuchamos los cambios (Efecto Kahoot en vivo)
document.getElementById('av-top').addEventListener('change', updateAvatarPreview);
document.getElementById('av-acc').addEventListener('change', updateAvatarPreview);

// Guardado en Base de Datos (Railway)

// ==========================================
// SISTEMA CENTRAL DE POWER-UPS (LIMPIO)
// ==========================================

window.estadoPartida = {
    tiempoCongelado: false,
    multiplicadorActivo: false
};

let inventarioPowerUps = { freeze: 0, multiplier: 0 };

window.activarPowerUp = function(tipo) {
    if (inventarioPowerUps[tipo] > 0) {

        inventarioPowerUps[tipo]--;
        const qtySpan = document.getElementById(`qty-${tipo}`);
        if(qtySpan) qtySpan.innerText = inventarioPowerUps[tipo];

        if (inventarioPowerUps[tipo] === 0) {
            const btn = document.getElementById(`btn-pu-${tipo}`);
            if(btn) {
                btn.disabled = true;
                btn.style.opacity = "0.3"; // Opcional: que se vea gastado
            }
        }

        if (tipo === 'freeze' && !window.estadoPartida.tiempoCongelado) {
            window.estadoPartida.tiempoCongelado = true;
            const btn = document.getElementById('btn-pu-freeze');
            if(btn) btn.style.boxShadow = "0 0 20px #00f2fe";

            setTimeout(() => {
                window.estadoPartida.tiempoCongelado = false;
                if(btn) btn.style.boxShadow = "";
            }, 5000);
        }
        else if (tipo === 'multiplier') {
            // En lugar de poner un 2 fijo, usamos el valor que calculamos al cargar
            window.estadoPartida.multiplicadorActivo = window.estadoPartida.multiplicadorValor || 2;

            const btn = document.getElementById('btn-pu-multiplier');
            if(btn) btn.style.boxShadow = "0 0 20px #ffd700";
        }
    }
};

document.addEventListener('click', (e) => {
    const btnFreeze = e.target.closest('#btn-pu-freeze');
    if (btnFreeze && !btnFreeze.disabled) window.activarPowerUp('freeze');

    const btnMulti = e.target.closest('#btn-pu-multiplier');
    if (btnMulti && !btnMulti.disabled) window.activarPowerUp('multiplier');
});

/*document.addEventListener('keydown', (e) => {
    const gameScreen = document.getElementById('game-screen');
    const roscoScreen = document.getElementById('ui-rosco');

    if ((gameScreen && gameScreen.classList.contains('active')) ||
        (roscoScreen && roscoScreen.classList.contains('active'))) {

        if (e.key === '1') {
            e.preventDefault();
            const btnFreeze = document.getElementById('btn-pu-freeze');
            if(btnFreeze && !btnFreeze.disabled) window.activarPowerUp('freeze');
        }
        if (e.key === '2') {
            e.preventDefault();
            const btnMulti = document.getElementById('btn-pu-multiplier');
            if(btnMulti && !btnMulti.disabled) window.activarPowerUp('multiplier');
        }
    }
});*/


window.toggleSidebar = function() {
    const sidebar = document.getElementById('fc-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (sidebar && overlay) {
        sidebar.classList.toggle('active');
        overlay.classList.toggle('active');
    }
};

window.switchProfileTab = function(tabName) {
    // 1. Gestionar botones
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    if (event && event.currentTarget) {
        event.currentTarget.classList.add('active');
    }

    // 2. Gestionar contenido
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    document.getElementById(`tab-${tabName}`).classList.add('active');

    if(tabName === 'settings') {
        document.getElementById('upd-username').value = app.currentUser.username;
        document.getElementById('upd-email').value = app.currentUser.email || "No disponible";
    }
};

window.cerrarPerfil = function() {
    app.showScreen('dashboard-screen');
};

// ==========================================
// MOTOR DEL JUEGO: EL ROSCO (Clásico + IA)
// ==========================================

const Rosco = {
        data: [], curr: 0, ok: 0, bad: 0, time: 180, timer: null,

        // --- 1. ARRANQUE MODO CLÁSICO (Usa la variable DB si la tienes) ---
        init: async function () {
            const temaInput = document.getElementById('input-tema-rosco').value.trim();

            if (temaInput.length < 3) {
                alert("Introduce un tema válido de al menos 3 letras. ¡Acelera un poco más!");
                return;
            }

            // 1. Ocultamos el menú principal y mostramos la pantalla del Rosco
            document.getElementById('dashboard-screen').classList.remove('active');
            document.getElementById('dashboard-screen').classList.add('hidden');

            const pantallaRosco = document.getElementById('ui-rosco');
            pantallaRosco.classList.remove('hidden');
            pantallaRosco.classList.add('active');

            // Mostramos la zona de juego y ocultamos los resultados
            document.getElementById('r-play').classList.remove('hidden');
            document.getElementById('r-end').classList.add('hidden');
            document.getElementById('r-summary').classList.add('hidden');

            // 2. Pantalla de carga
            document.getElementById('r-char').innerText = "⏳";
            document.getElementById('r-def').innerText = "Conectando con boxes... Generando 25 palabras sobre: " + temaInput;
            document.getElementById('r-circle').innerHTML = '';
            document.getElementById('r-input').disabled = true; // Bloqueamos el input mientras carga

            try {
                // 3. Petición a tu backend
                const url = `/incidencias/game/rosco-ia?tema=${encodeURIComponent(temaInput)}`;
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`},
                    credentials: 'include'
                });

                if (!response.ok) throw new Error("La IA no pudo generar el rosco");

                const roscoIA = await response.json();

                // 4. Transformamos el JSON al formato del juego
                this.data = roscoIA.map(item => {
                    return {
                        id: item.letra.toUpperCase(),
                        q: item.definicion,
                        a: item.palabra,
                        st: null
                    };
                });

                // 5. Desbloqueamos, reseteamos contadores y arrancamos
                document.getElementById('r-input').disabled = false;
                this.curr = 0;
                this.ok = 0;
                this.bad = 0;
                this.time = 180;
                this.draw();
                this.loadQ();
                this.startTimer();

            } catch (error) {
                console.error("Fallo:", error);
                alert("Hubo un fallo en boxes al contactar con la IA. Volviendo al menú.");
                app.showScreen('dashboard-screen');
            }
        },

        // --- 2. ARRANQUE MODO IA (El nuevo motor) ---
        initIA: async function () {
            const temaInput = document.getElementById('input-tema-rosco').value.trim();
            if (temaInput.length < 3) {
                alert("Introduce un tema válido de al menos 3 letras. ¡Acelera un poco más!");
                return;
            }

            // Ocultamos el menú (ajusta esto si tu menú principal tiene otro ID)
            document.getElementById('main-menu').classList.add('hidden');
            document.getElementById('ui-rosco').classList.remove('hidden');
            document.getElementById('r-play').classList.remove('hidden');
            document.getElementById('r-end').classList.add('hidden');
            document.getElementById('r-summary').classList.add('hidden');

            // Texto temporal mientras carga
            document.getElementById('r-char').innerText = "⏳";
            document.getElementById('r-def').innerText = "Conectando con la IA... Generando 25 palabras sobre: " + temaInput;
            document.getElementById('r-circle').innerHTML = '';

            try {
                // Petición a tu backend (asegúrate de que la ruta coincide con tu GameController)
                const url = `/incidencias/game/rosco-ia?tema=${encodeURIComponent(temaInput)}`;
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`},
                    credentials: 'include'
                });

                if (!response.ok) throw new Error("La IA no pudo generar el rosco");

                const roscoIA = await response.json();

                // Transformamos el JSON de la IA al formato del juego
                this.data = roscoIA.map(item => {
                    return {
                        id: item.letra.toUpperCase(),
                        q: item.definicion,
                        a: item.palabra,
                        st: null
                    };
                });

                // Reseteamos contadores y arrancamos
                this.curr = 0;
                this.ok = 0;
                this.bad = 0;
                this.time = 180;
                this.draw();
                this.loadQ();
                this.startTimer();

            } catch (error) {
                console.error("Fallo:", error);
                alert("Hubo un fallo en boxes al contactar con la IA. Volviendo al menú.");
                // Cambia App.home() por tu función real para volver al menú, por ejemplo app.showScreen('menu-screen');
                if (typeof App !== 'undefined') App.home();
            }
        },

        // --- 3. DIBUJAR EL ROSCO CIRCULAR ---
        draw() {
            const ul = document.getElementById('r-circle');
            ul.innerHTML = '';
            const total = this.data.length;
            this.data.forEach((d, i) => {
                const li = document.createElement('li');
                li.className = 'letter-item';
                li.id = 'rn-' + i;
                li.innerText = d.id;
                const ang = (360 / total) * i - 90;
                const rad = ang * (Math.PI / 180);
                li.style.transform = `translate(${300 * Math.cos(rad)}px, ${300 * Math.sin(rad)}px)`;
                ul.appendChild(li);
            });
        },

        // --- 4. CARGAR LA SIGUIENTE PREGUNTA ---
        loadQ() {
            let p = -1;
            const total = this.data.length;
            for (let i = this.curr; i < total; i++) if (!this.data[i].st) {
                p = i;
                break;
            }
            if (p === -1) for (let i = 0; i < total; i++) if (!this.data[i].st) {
                p = i;
                break;
            }
            if (p === -1) {
                this.finish();
                return;
            }
            this.curr = p;
            document.getElementById('r-char').innerText = this.data[p].id;
            document.getElementById('r-def').innerText = this.data[p].q;
            document.getElementById('r-input').value = '';
            document.getElementById('r-input').focus();
            document.querySelectorAll('.letter-item').forEach(el => el.classList.remove('active'));
            document.getElementById('rn-' + p).classList.add('active');
        },

        // --- 5. COMPROBAR RESPUESTA ---
        check() {
            const v = normalize(document.getElementById('r-input').value);
            const item = this.data[this.curr];
            const node = document.getElementById('rn-' + this.curr);
            if (v === normalize(item.a)) {
                item.st = 'ok';
                this.ok++;
                node.classList.add('correct');
            } else {
                item.st = 'bad';
                this.bad++;
                node.classList.add('wrong');
            }
            this.curr++;
            this.loadQ();
        },

        // --- 6. PASAR PALABRA ---
        pass() {
            document.getElementById('rn-' + this.curr).classList.remove('active');
            this.curr++;
            this.loadQ();
        },

        // --- 7. TEMPORIZADOR ---
        startTimer() {
            clearInterval(this.timer);
            this.timer = setInterval(() => {
                this.time--;
                document.getElementById('r-time').innerText = this.time;
                if (this.time <= 0) this.finish();
            }, 1000);
        },
        stop() {
            clearInterval(this.timer);
        },

        // --- 8. PANTALLA FINAL ---
        finish: async function () {
            this.stop();
            document.getElementById('r-play').classList.add('hidden');
            document.getElementById('r-end').classList.remove('hidden');
            document.getElementById('r-ok').innerText = this.ok;
            document.getElementById('r-bad').innerText = this.bad;

            // 🛠️ FIX VISUAL: Apagamos cualquier letra que se haya quedado encendida
            document.querySelectorAll('.letter-item').forEach(el => el.classList.remove('active'));

            const sum = document.getElementById('r-summary');
            sum.innerHTML = '';
            sum.classList.remove('hidden');

            this.data.forEach(d => {
                const div = document.createElement('div');
                div.className = 'summary-item';
                const icon = d.st === 'ok' ? '✅' : '❌';
                div.innerHTML = `<span class="${d.st === 'ok' ? 'sum-correct' : 'sum-wrong'}">${d.id} ${icon}</span> <b>${d.a.toUpperCase()}</b><br><small>${d.q}</small>`;
                sum.appendChild(div);
            });

            // 💰 SISTEMA DE RECOMPENSAS: 10 créditos por cada acierto
            const creditosGanados = this.ok * 10;

            // 🎯 AQUI PINTAMOS LOS CRÉDITOS EN LA PANTALLA
            document.getElementById('r-credits').innerText = creditosGanados;

            if (creditosGanados > 0) {
                // Sumamos al perfil en RAM y actualizamos la interfaz
                if (typeof app !== 'undefined' && app.currentUser) {
                    app.currentUser.creditos += creditosGanados;
                    app.updateUserUI();
                }

                // Enviamos el paquete a boxes (Java)
                const payloadJSON = JSON.stringify({creditosExtra: parseInt(creditosGanados)});

                try {
                    const url = '/usuarios/actualizar-creditos';
                    const response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
                        },
                        credentials: 'include',
                        body: payloadJSON
                    });

                    const serverMessage = await response.text();

                    if (!response.ok) {
                        throw new Error(`Mecánico dice: "${serverMessage}"`);
                    }

                } catch (error) {
                    console.error("Fallo de conexión al guardar créditos del Rosco ->", error.message);
                }
            }
        }
    };
// HERRAMIENTA OBLIGATORIA: Función para quitar tildes y mayúsculas al comprobar
function normalize(s) {
    return s.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
}

// Añadir evento al input del rosco para que funcione al pulsar "Enter"
// (Coloca esto en el bloque donde inicias tus otros EventListeners)
document.getElementById('r-input').addEventListener('keypress', e => {
    if(e.key === 'Enter') Rosco.check();
});

const app = {
    // --- ESTADO (Igual) ---
    currentPhrase: null,
    phraseBuffer: [],
    score: 0,
    timeLeft: 60,
    timerInterval: null,
    gameHistory: [],
    escudoActivo: false,
    isFetchingPhrases: false,
    modoPersonalizado: false,
    temaElegido: "",
    tieneRebufo: false,
    tieneRadar: false,
    tieneSegundaOportunidad: false,
    comboAcertadas: 0,
    chanceUsada: false,

    openTutorial: () => {
        // En lugar de manipular el modal, usamos tu sistema de navegación de pantallas
        app.showScreen('tutorial-screen');
    },

    closeTutorial: () => {
        // Redirigimos al dashboard
        app.showScreen('dashboard-screen');
    },

    completarTutorial: () => {
        console.log("🏁 Tutorial completado. Guardando...");
        // 1. Guardamos el booleano en el disco duro
        localStorage.setItem('tutorial_completado', 'true');

        // 2. Ejecutamos la verificación para que la tarjeta se oculte YA
        app.verificarTutorial();

        // 3. Volvemos al dashboard
        app.showScreen('dashboard-screen');
    },

    verificarTutorial: () => {
        const completado = localStorage.getItem('tutorial_completado');
        const card = document.getElementById('card-tutorial');

        // 🚨 AQUÍ ESTABA EL ERROR: Tenías body.classList.card = 'hidden' (Eso no existe)
        if (completado === 'true' && card) {
            card.style.display = 'none'; // Esto borra la tarjeta físicamente
            console.log("🚫 Tarjeta de tutorial oculta por completado.");
        }
    },

    // --- NAVEGACIÓN Y TEMA (Igual) ---
    toggleTheme: () => {
        const body = document.body;
        body.classList.toggle('light-mode');
        const isLight = body.classList.contains('light-mode');
        document.getElementById('theme-toggle').innerText = isLight ? "☀️" : "🌙";

        // ¡ESTA LÍNEA ES LA CLAVE! Guarda tu elección para que el panel Admin la pueda leer
        localStorage.setItem('theme', isLight ? 'light' : 'dark');
    },

    showScreen: (screenId) => {
        // 1. Apagamos todas las pantallas y limpiamos estilos basura
        document.querySelectorAll('.screen').forEach(s => {
            s.classList.remove('active');
            s.classList.add('hidden');
            s.style.display = '';
        });

        // 2. Encendemos únicamente la pantalla de destino
        const target = document.getElementById(screenId);
        if (target) {
            target.classList.remove('hidden');
            target.classList.add('active');
        }

        // 🚨 3. NUEVO: Control inteligente de la barra global (Fíjate que ahora dice screenId)
        const topNav = document.getElementById('global-top-nav');
        if (topNav) {
            if (screenId === 'login-screen' || screenId === 'registro-screen') {
                topNav.classList.add('hidden');
            } else {
                topNav.classList.remove('hidden');
            }
        }
    },

    cargarPowerUps: () => {
        if (!app.currentUser) return;

        const inv = app.currentUser.inventario || [];
        const equipadas = app.currentUser.habilidadesEquipadas || [];

        // 1. Buscamos los objetos específicos en el inventario para obtener sus IDs
        const itemHielo1 = inv.find(i => i.nombre === 'Tanque de Nitrógeno');
        const itemHielo2 = inv.find(i => i.nombre === 'Nitrógeno Criogénico');
        const itemMulti1 = inv.find(i => i.nombre === 'Contrato VIP');

        // 2. COMPROBACIÓN REAL: ¿Está el ID en la lista de equipados?
        const tieneHielo1 = itemHielo1 && equipadas.includes(itemHielo1.id_item || itemHielo1.idItem);
        const tieneHielo2 = itemHielo2 && equipadas.includes(itemHielo2.id_item || itemHielo2.idItem);
        const tieneMulti1 = itemMulti1 && equipadas.includes(itemMulti1.id_item || itemMulti1.idItem);

        // El Multiplicador dijimos que era PASIVA automática,
        // pero si quieres que ocupe slot, usa 'equipadas.includes(...)'.
        // Si quieres que sea "comprar y tener", usa 'inv.some(...)'.

        const containerPasivas = document.getElementById('container-pasivas');
        const containerActivos = document.getElementById('powerup-bar');
        if(!containerPasivas || !containerActivos) return;

        containerPasivas.innerHTML = "";
        containerActivos.innerHTML = "";

        // --- RENDER MULTIPLICADOR ---
        const multiVal =(tieneMulti1 ? 2 : 1);
        window.estadoPartida.multiplicadorActivo = multiVal;
        if (multiVal > 1) {
            containerPasivas.innerHTML = `<div class="stat-badge">💰 x${multiVal}</div>`;
        }

        // --- RENDER HIELO (Solo si está equipado) ---
        let usosHielo = 0;
        if (tieneHielo2) usosHielo = 2;
        else if (tieneHielo1) usosHielo = 1;

        inventarioPowerUps.freeze = usosHielo;

        if (usosHielo > 0) {
            containerActivos.innerHTML = `
                <button id="btn-pu-freeze" class="powerup-btn">
                    ❄️ <span id="qty-freeze" class="pu-qty">${usosHielo}</span>
                </button>`;
            document.getElementById('btn-pu-freeze').onclick = () => window.activarPowerUp('freeze');
        }

        // --- DETECCIÓN DE NUEVAS PASIVAS ---
        const itemRebufo = inv.find(i => i.nombre === 'Rebufo');
        const itemRadar = inv.find(i => i.nombre === 'Radar');
        const itemSegundaOp = inv.find(i => i.nombre === 'Segunda Oportunidad');

        app.tieneRebufo = itemRebufo && equipadas.includes(itemRebufo.id_item || itemRebufo.idItem);
        app.tieneRadar = itemRadar && equipadas.includes(itemRadar.id_item || itemRadar.idItem);
        app.tieneSegundaOportunidad = itemSegundaOp && equipadas.includes(itemSegundaOp.id_item || itemSegundaOp.idItem);

    },

    restoreSession: async () => {
        if (localStorage.getItem('theme') === 'light') {
            document.body.classList.add('light-mode');
            const btnTheme = document.getElementById('theme-toggle');
            if (btnTheme) btnTheme.innerText = "☀️";
        }

        try {
            // 🛡️ REPARACIÓN: Añadimos la hora actual a la URL para reventar la caché del navegador
            const timestamp = new Date().getTime();
            const response = await fetch(`/usuarios/perfil?t=${timestamp}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Cache-Control': 'no-cache' // Forzamos a no usar memoria antigua
                },
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();

                // 1. Traducir el inventario
                let inventarioSeguro = [];
                const itemsDelBackend = data.items || data.inventario || [];
                if (Array.isArray(itemsDelBackend)) {
                    inventarioSeguro = itemsDelBackend.map(item => ({
                        id_item: item.idItem || item.id_item || item.id,
                        nombre: item.nombre || item.name,
                        descripcion: item.descripcion || item.description
                    }));
                }

                // 2. Traducir los Roles
                let esAdmin = false;
                if (data.roles && Array.isArray(data.roles)) {
                    esAdmin = data.roles.some(rol => (rol.name || rol.nombre) === 'ADMIN');
                } else if (data.isAdmin === true) {
                    esAdmin = true;
                }

                // 3. Montar el currentUser a prueba de balas
                app.currentUser = {
                    username: data.nombreUsuario || data.username || data.nombre_usuario || 'Piloto',
                    apellido: data.apellido_usuario || data.apellidoUsuario || data.apellido,
                    creditos: data.creditos || 0,
                    inventario: inventarioSeguro,
                    isAdmin: esAdmin,
                    habilidadesEquipadas: data.habilidadesEquipadas || [],
                    colorTema: data.colorTema || null,
                    avatar: data.avatar || data.avatarConfig || (data.username || data.nombreUsuario)
                };

                app.updateUserUI();
                app.showScreen('dashboard-screen');
                app.verificarTutorial();
            } else {
                app.showScreen('login-screen');
            }
        } catch (error) {
            app.showScreen('login-screen');
        }
    },


    login: async () => {
        const userVal = document.getElementById('username').value.trim();
        const passVal = document.getElementById('password').value.trim();
        const errorMsg = document.getElementById('error-msg');

        if (!userVal || !passVal) {
            errorMsg.innerText = "Por favor, rellena ambos campos.";
            errorMsg.classList.remove('hidden');
            return;
        }

        try {
            const btnLogin = document.getElementById('btn-login');
            const originalText = btnLogin.innerText;
            btnLogin.innerText = "Conectando...";
            btnLogin.disabled = true;

            const response = await fetch('/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include', // 🔑 Permite que el backend nos instale la Cookie
                body: JSON.stringify({
                    correo_usuario: userVal,
                    contrasenha_usuario: passVal
                })
            });

            btnLogin.innerText = originalText;
            btnLogin.disabled = false;

            if (!response.ok) throw new Error("Credenciales inválidas");

            const data = await response.json();

            let inventarioSeguro = [];
            if (data.inventario && Array.isArray(data.inventario)) {
                inventarioSeguro = data.inventario.map(item => (typeof item === 'object') ? item : { id_item: item });
            }

            // Guardamos SOLO en RAM
            app.currentUser = {
                username: data.username,
                apellido: data.apellido_usuario || data.apellidoUsuario || data.apellido,
                creditos: data.creditos || 0,
                inventario: inventarioSeguro,
                isAdmin: data.isAdmin === true,
                habilidadesEquipadas: data.habilidadesEquipadas || [],
                colorTema: data.colorTema || null,
                avatar: data.avatar || data.avatarConfig || (data.username || data.nombreUsuario)
            };

            localStorage.setItem('isLogged', 'true');
            inicializarTaller();
            app.restoreSession();
            app.updateUserUI();
            app.showScreen('dashboard-screen');
            app.verificarTutorial();
            errorMsg.classList.add('hidden');
            app.fetchMorePhrases();

        } catch (error) {
            errorMsg.innerText = "Credenciales inválidas o servidor desconectado.";
            errorMsg.classList.remove('hidden');
        }
    },

    logout: async () => {
        try {
            // Llamamos a la ruta exacta
            await fetch('/usuarios/logout-manual', {
                method: 'POST',
                credentials: 'include' // 🔑 VITAL: Envia la cookie 'jwt_token' para que Java la pueda machacar
            });
        } catch (error) {
            console.error("No se pudo contactar con el servidor:", error);
        } finally {
            // Vaciamos la RAM
            app.currentUser = null;
            localStorage.clear();
            sessionStorage.clear();

            // Pequeña pausa para que el navegador procese el borrado antes de viajar
            setTimeout(() => {
                window.location.href = "index.html";
            }, 200);
        }
    },

    updateUserUI: () => {
        if(!app.currentUser) return;

        // 1. Pintamos los Avatares (El pequeño y el grande)
        const DICEBEAR_API = 'https://api.dicebear.com/9.x/avataaars/svg';
        const urlAvatar = `${DICEBEAR_API}?seed=${app.currentUser.avatar}`;

        const miniAvatar = document.getElementById('nav-mini-avatar');
        if (miniAvatar) miniAvatar.src = urlAvatar;

        const largeAvatar = document.getElementById('dropdown-large-avatar');
        if (largeAvatar) largeAvatar.src = urlAvatar;

        // 2. Pintamos los Textos del Desplegable
        const elFullName = document.getElementById('dropdown-fullname');
        if (elFullName) {
            // Unimos nombre y apellido. Si están vacíos, ponemos el username.
            const nombreCompleto = `${app.currentUser.username} ${app.currentUser.apellido}`.trim();
            elFullName.innerText = nombreCompleto !== '' ? nombreCompleto : app.currentUser.username;
        }

        const elUsername = document.getElementById('dropdown-username');
        if (elUsername) elUsername.innerText = `Tu perfil`;

        // 3. Pintamos los Créditos
        const elCredits = document.getElementById('user-credits');
        if (elCredits) elCredits.innerText = app.currentUser.creditos;

        // 4. Lógica de la Tarjeta de Admin
        const adminCard = document.getElementById('card-admin');
        if (adminCard) {
            if (app.currentUser.isAdmin === true) {
                adminCard.classList.remove('hidden');
            } else {
                adminCard.classList.add('hidden');
            }
        }
        console.log(app.currentUser);
        // Aplicamos los cosméticos guardados
        app.applyCosmetics();
    },

    applyCosmetics: () => {
        // 1. Limpiamos la pintura anterior
        document.body.classList.remove('tema-cyberpunk', 'teclado-neon');
        if (!app.currentUser) return;

        // 🔍 TELEMETRÍA: Muestra en consola (F12) qué ID intentamos buscar
        console.log("🎨 ID del cosmético equipado (colorTema):", app.currentUser.colorTema);

        // 🚨 LA SOLUCIÓN: Usamos == (doble igual) en lugar de === para que "5" y 5 sean lo mismo.
        const equipado = app.currentUser.inventario.find(i => (i.id_item || i.idItem) == app.currentUser.colorTema);

        // 🔍 TELEMETRÍA: Muestra si ha encontrado el objeto en el inventario
        console.log("🎨 Objeto encontrado en el inventario:", equipado);

        if (equipado) {
            // Usamos .trim() por si en la base de datos se guardó con un espacio final (ej: "Teclado Neón ")
            const nombreNormal = equipado.nombre.trim();

            if (nombreNormal === 'Tema Cyberpunk') {
                document.body.classList.add('tema-cyberpunk');
                console.log("✅ Tema Cyberpunk aplicado.");
            }
            if (nombreNormal === 'Teclado Neón') {
                document.body.classList.add('teclado-neon');
                console.log("✅ Teclado Neón aplicado.");
            }
        }
    },



    // --- LÓGICA DE JUEGO ---
    startGame: () => {
        // 1. Ajustes del Modo y Limpieza TOTAL
        app.comboAcertadas = 0;
        app.chanceUsada = false;
        app.modoPersonalizado = false;
        app.temaElegido = "";
        app.phraseBuffer = []; // Vaciamos frases viejas
        window.estadoPartida.tiempoCongelado = false;
        window.estadoPartida.multiplicadorActivo = 1;

        app.score = 0;
        app.gameHistory = [];

        // 2. Lectura de Inventario y Power-ups
        const inv = app.currentUser.inventario || [];
        const habEquipadas = app.currentUser.habilidadesEquipadas || [];

        const relojItem = inv.find(i => i.nombre === 'Reloj de Arena');
        const escudoItem = inv.find(i => i.nombre === 'Escudo de Error');

        const tieneReloj = relojItem && habEquipadas.includes(relojItem.id_item || relojItem.idItem);
        app.timeLeft = tieneReloj ? 65 : 60;

        app.escudoActivo = escudoItem && habEquipadas.includes(escudoItem.id_item || escudoItem.idItem);

        app.cargarPowerUps();

        // 3. Preparar Interfaz
        document.getElementById('score').innerText = "0";
        document.getElementById('timer').innerText = app.timeLeft;

        app.showScreen('game-screen');
        app.loadNewPhrase();

        const input = document.getElementById('game-input');
        input.value = "";
        input.focus();

        // 4. Arrancar el Motor (Temporizador)
        if (app.timerInterval) clearInterval(app.timerInterval);
        app.timerInterval = setInterval(() => {

            // 🚨 AQUÍ ESTÁ LA CLAVE: Si el tiempo está congelado, no hace nada
            if (window.estadoPartida.tiempoCongelado) return;

            app.timeLeft--;
            document.getElementById('timer').innerText = app.timeLeft;
            if(app.timeLeft <= 0) app.endGame();
        }, 1000);
    },

    // --- MODO PERSONALIZADO ---
    iniciarPartidaPersonalizada: () => {
        const inputTema = document.getElementById('input-tema').value.trim();

        if (inputTema.length < 3) {
            alert("El tema debe tener al menos 3 letras. ¡Acelera un poco más!");
            return;
        }

        // 1. Ajustes del Modo y Limpieza TOTAL
        app.modoPersonalizado = true;
        app.temaElegido = inputTema;
        app.phraseBuffer = []; // Vaciamos frases viejas
        window.estadoPartida.tiempoCongelado = false;
        window.estadoPartida.multiplicadorActivo = 1;

        app.score = 0;
        app.gameHistory = [];

        // 2. Lectura de Inventario y Power-ups
        const inv = app.currentUser.inventario || [];
        const habEquipadas = app.currentUser.habilidadesEquipadas || [];

        const relojItem = inv.find(i => i.nombre === 'Reloj de Arena');
        const escudoItem = inv.find(i => i.nombre === 'Escudo de Error');

        const tieneReloj = relojItem && habEquipadas.includes(relojItem.id_item || relojItem.idItem);
        app.timeLeft = tieneReloj ? 65 : 60;

        app.escudoActivo = escudoItem && habEquipadas.includes(escudoItem.id_item || escudoItem.idItem);

        // 3. Preparar Interfaz
        document.getElementById('score').innerText = "0";
        document.getElementById('timer').innerText = app.timeLeft;

        app.showScreen('game-screen');
        app.loadNewPhrase();

        const input = document.getElementById('game-input');
        input.value = "";
        input.focus();

        // 4. Arrancar el Motor (Temporizador)
        if (app.timerInterval) clearInterval(app.timerInterval);
        app.timerInterval = setInterval(() => {

            // 🚨 AQUÍ ESTÁ LA CLAVE: Si el tiempo está congelado, no hace nada
            if (window.estadoPartida.tiempoCongelado) return;

            app.timeLeft--;
            document.getElementById('timer').innerText = app.timeLeft;
            if(app.timeLeft <= 0) app.endGame();
        }, 1000);
    },

    // --- NUEVA CARGA DE FRASE CON IA ---
    loadNewPhrase: async () => {
        const display = document.getElementById('phrase-display');
        const input = document.getElementById('game-input');

        // 1. Si no hay frases, esperamos pacientemente sin bloquear la pantalla
        if (!app.phraseBuffer || app.phraseBuffer.length === 0) {
            display.innerText = "⏳ Conectando con boxes... repostando frases";
            input.disabled = true; // Bloqueamos el input temporalmente

            // Si nadie está buscando frases, mandamos a buscarlas
            if (!app.isFetchingPhrases) {
                app.fetchMorePhrases(); // Sin await, que lo haga a su ritmo
            }

            // EL TRUCO MAGICO: Hacemos pausas de 200ms en bucle hasta que lleguen.
            // El 'await new Promise' le da tiempo al navegador de actualizar la pantalla.
            while (!app.phraseBuffer || app.phraseBuffer.length === 0) {
                await new Promise(resolve => setTimeout(resolve, 200));
            }
        }

        // 2. AHORA SÍ tenemos frases en el buffer asegurado. Sacamos la primera.
        const textoSacado = app.phraseBuffer.shift();
        app.currentPhrase = { texto: textoSacado };
        display.innerText = app.currentPhrase.texto;

        display.style.opacity = "1";
        input.disabled = false;
        input.value = "";
        input.focus();

        app.chanceUsada = false;

        // 📡 LÓGICA DEL RADAR
        const previewEl = document.getElementById('next-phrase-preview');
        if (app.tieneRadar && previewEl) {
            previewEl.classList.remove('hidden');
            previewEl.innerText = app.phraseBuffer.length > 0
                ? "Siguiente: " + app.phraseBuffer[0]
                : "Siguiente: (Cargando...)";
        } else if (previewEl) {
            previewEl.classList.add('hidden');
        }

        // 3. Si quedan pocas frases, pedimos más para el futuro
        if (app.phraseBuffer.length <= 2 && !app.isFetchingPhrases) {
            app.fetchMorePhrases();
        }
    },
    fetchMorePhrases: async () => {
        if (app.isFetchingPhrases) return;
        app.isFetchingPhrases = true;

        try {
            const timestamp = new Date().getTime();

            // 1. Armamos la URL base
            let url = `/incidencias/game/frase?dificultad=media&t=${timestamp}`;

            // 2. Si estamos en modo personalizado, acoplamos el tema a la URL
            if (app.modoPersonalizado && app.temaElegido) {
                // encodeURIComponent asegura que los espacios (ej: "Formula 1") viajen bien por internet
                url += `&tema=${encodeURIComponent(app.temaElegido)}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` },
                credentials: 'include'
            });

            if (!response.ok) throw new Error("Error en la petición a boxes");

            let textoBruto = await response.text();
            textoBruto = textoBruto.replace(/[\r\n]+/g, " ").trim();
            const partes = textoBruto.split('|');

            const nuevasFrases = partes
                .map(f => f.trim())
                .map(f => f.replace(/^[0-9.\-\s]+/, ""))
                .filter(f => f.length > 25);

            if (!app.phraseBuffer) app.phraseBuffer = [];
            app.phraseBuffer.push(...nuevasFrases);


        } catch (error) {
            console.error("Fallo al procesar frases de la IA:", error);
            app.phraseBuffer.push("El coche de seguridad está en la pista por un fallo técnico");
        } finally {
            app.isFetchingPhrases = false;
        }
    },
    checkInput: () => {
        const input = document.getElementById('game-input');
        const val = input.value;
        const target = app.currentPhrase.texto;

        if(val === target) {
            input.style.borderColor = 'var(--success)';
        } else if(!target.startsWith(val)) {

            // 🔄 LÓGICA: SEGUNDA OPORTUNIDAD
            if (app.tieneSegundaOportunidad && !app.chanceUsada && val.length > 0) {
                // Borramos la última letra que provocó el fallo
                input.value = val.slice(0, -1);
                app.chanceUsada = true; // Se gasta la oportunidad de esta frase

                // Efecto visual de "salvación" (brillo morado momentáneo)
                input.style.boxShadow = "0 0 15px rgba(157, 78, 221, 0.8)";
                setTimeout(() => input.style.boxShadow = "", 500);
                return; // Salimos para que no marque el error visualmente
            }

            // --- LÓGICA DEL ESCUDO DE ERROR ORIGINAL ---
            if (app.escudoActivo) {
                input.style.borderColor = '#ffd700';
                app.escudoActivo = false;
            } else {
                input.style.borderColor = 'var(--error)';
            }
        } else {
            input.style.borderColor = 'var(--text-secondary)';
        }
    },

    handleKeydown: (e) => {
        if (e.key === "Enter") {
            const inputVal = document.getElementById('game-input').value.trim();
            const targetText = app.currentPhrase.texto;

            // 🏎️ LÓGICA: REBUFO (Combos)
            if (inputVal === targetText) { // Frase perfecta
                if (app.tieneRebufo) {
                    app.comboAcertadas++;
                    if (app.comboAcertadas >= 3) {
                        app.timeLeft += 2; // Ganas 2 segundos
                        app.comboAcertadas = 0; // Reinicia el combo

                        // Efecto visual en el cronómetro
                        const timerEl = document.getElementById('timer');
                        timerEl.style.textShadow = "0 0 15px #00f2fe";
                        timerEl.style.color = "#00f2fe";
                        setTimeout(() => {
                            timerEl.style.textShadow = "";
                            timerEl.style.color = "";
                        }, 500);
                    }
                }
            } else {
                app.comboAcertadas = 0; // Si hay fallos o está incompleta, pierdes el combo
            }

            const pointsEarned = app.calculatePhraseScore(targetText, inputVal);
            app.score += pointsEarned;
            document.getElementById('score').innerText = app.score;
            app.gameHistory.push({ target: targetText, input: inputVal, points: pointsEarned });
            app.loadNewPhrase();
        }
    },

    calculatePhraseScore: (target, input) => {
        const targetWords = target.split(' ');
        const inputWords = input.split(' ');
        let points = 0;
        targetWords.forEach((word, index) => {
            if (inputWords[index] && inputWords[index] === word) points += 5;
        });
        return points;
    },

    endGame: async () => { // <-- 1. Añadimos 'async' aquí
            clearInterval(app.timerInterval);
            const inputVal = document.getElementById('game-input').value;

            // Sumar los puntos de la última frase a medias
            if (inputVal.length > 0) {
                const points = app.calculatePhraseScore(app.currentPhrase.texto, inputVal);
                app.score += points;
                app.gameHistory.push({target: app.currentPhrase.texto, input: inputVal, points: points});
            }

            // 🚨 AQUÍ APLICAMOS EL MULTIPLICADOR DE PODER
        let creditosBase = Math.floor(app.score * 0.1);

        // 🚨 USA LA VARIABLE GLOBAL QUE HEMOS SETEADO EN cargarPowerUps
            const multiplicador = window.estadoPartida.multiplicadorActivo || 1;
            const creditsEarned = creditosBase * multiplicador;

            app.currentUser.creditos += creditsEarned;
            app.updateUserUI();
            document.getElementById('earned-credits').innerText = creditsEarned;
            app.renderResults();
            app.showScreen('results-screen');
            app.animateValue("final-score", 0, app.score, 1500);

            // 🛑 EL FILTRO INTELIGENTE: Solo llamamos a Java si hemos ganado algo
            if (creditsEarned > 0) {
                // Forzamos a que sea un entero puro de JavaScript
                const payloadJSON = JSON.stringify({creditosExtra: parseInt(creditsEarned)});

                try {
                    const url = '/usuarios/actualizar-creditos';
                    const response = await fetch(url, {
                        method: 'POST', // Aseguramos que es POST
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
                        },
                        credentials: 'include',
                        body: payloadJSON
                    });

                    // 🚨 EL CHIVATO MÁXIMO: Leemos qué dice Java exactamente si nos rechaza
                    const serverMessage = await response.text();

                    if (!response.ok) {
                        // Si hay error, imprimimos el mensaje devuelto por Spring Boot
                        throw new Error(`Mecánico dice: "${serverMessage}"`);
                    }
                } catch (error) {
                    // Esto nos dirá el motivo exacto del rechazo
                    console.error("Fallo de conexión ->", error.message);
                }
            } else {
                alert("Cero créditos ganados. ¡Puedes hacerlo mejor!");
            }
        },

    renderResults: () => {
        const list = document.getElementById('history-list');
        list.innerHTML = "";
        app.gameHistory.forEach((item, idx) => {
            const div = document.createElement('div');
            div.innerHTML = `Frase ${idx+1}: <span style="color:${item.points>0?'var(--success)':'var(--error)'}">${item.points} pts</span>`;
            div.className = "history-item"; // Usamos la clase CSS
            list.appendChild(div);
        });
    },
    
    animateValue: (id, start, end, duration) => {
        const obj = document.getElementById(id);
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            obj.innerHTML = Math.floor(progress * (end - start) + start);
            if (progress < 1) window.requestAnimationFrame(step);
        };
        window.requestAnimationFrame(step);
    },
    
    abortGame: () => { clearInterval(app.timerInterval); app.showScreen('dashboard-screen'); },


    // --- NUEVO: PERFIL (Inventario + Admin) ---
    openProfile: () => {
        app.updateUserUI();

        const inventoryList = document.getElementById('inventory-list');
        inventoryList.innerHTML = "";

        if(!app.currentUser.inventario || app.currentUser.inventario.length === 0) {
            inventoryList.innerHTML = '<p class="empty-msg">Tu inventario está vacío.</p>';
        } else {
            app.currentUser.inventario.forEach(item => {
                const idDelObjeto = item.id_item || item.idItem;
                const desc = item.descripcion;
                const nombre = item.nombre || 'Objeto Misterioso';

                const esCosmetico = nombre.includes('Tema') || nombre.includes('Teclado');

                let isEquipped = false;
                if (esCosmetico) {
                    isEquipped = app.currentUser.colorTema === idDelObjeto;
                } else {
                    const hab = app.currentUser.habilidadesEquipadas || [];
                    isEquipped = hab.includes(idDelObjeto);
                }

                inventoryList.innerHTML += `
                    <div class="shop-item">
                        <h4>${nombre}</h4>
                        <span class="desc" style="color: var(--text-secondary); font-size: 0.8rem; display:block; margin-bottom: 10px;">
                            ${esCosmetico ? '🎨 Estilo Visual (Máx 1)' : '⚡ Habilidad Pasiva (Máx 2)'}
                        </span> 
                        <button class="${isEquipped ? 'btn-equipped' : 'btn-equip'}" onclick="app.toggleEquip(${idDelObjeto}, ${esCosmetico})">
                            ${isEquipped ? (esCosmetico ? 'Equipado' : 'Desequipar') : 'Equipar'}
                        </button>
                    </div>
                `;
            });
        }
        app.showScreen('profile-screen');

        // 🛠️ REPARACIÓN: Si abres el perfil, arrancamos el motor visual
        inicializarTaller();
    },

    toggleEquip: (id_item, esCosmetico) => {
        if (!app.currentUser.habilidadesEquipadas) app.currentUser.habilidadesEquipadas = [];

        if (esCosmetico) {
            if (app.currentUser.colorTema === id_item) app.currentUser.colorTema = null;
            else app.currentUser.colorTema = id_item;
        } else {
            const index = app.currentUser.habilidadesEquipadas.indexOf(id_item);
            if (index > -1) {
                app.currentUser.habilidadesEquipadas.splice(index, 1);
            } else {
                if (app.currentUser.habilidadesEquipadas.length >= 2) {
                    alert("⚠️ Límite alcanzado: Solo puedes equipar 2 habilidades pasivas a la vez.");
                    return;
                }
                app.currentUser.habilidadesEquipadas.push(id_item);
            }
        }

        app.openProfile();
        app.applyCosmetics();
    },


    // --- TIENDA (Conectada a la BBDD) ---
    openShop: () => {
        app.updateUserUI();
        app.showScreen('shop-screen');
        // ¡Aquí está la magia! Llama a Java para traer los objetos reales
        app.loadTienda();
    },

    loadTienda: async () => {
        try {
            const response = await fetch('/usuarios/tienda', {
                headers: {'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`},
                credentials: 'include'
            });
            const items = await response.json();
            const shopList = document.getElementById('shop-list');
            if (!shopList) return;
            shopList.innerHTML = "";

            const inv = app.currentUser.inventario || [];
            const equipadas = app.currentUser.habilidadesEquipadas || [];

            const tieneHielo1 = inv.some(i => i.nombre === 'Tanque de Nitrógeno');
            const tieneHielo2 = inv.some(i => i.nombre === 'Nitrógeno Criogénico');
            const itemM1 = inv.find(i => i.nombre === 'Contrato VIP');
            const tieneMulti1 = itemM1 && equipadas.includes(itemM1.id_item || itemM1.idItem);
            const tieneMulti2 = inv.some(i => i.nombre === 'Socio de Honor');

            items.forEach(item => {
                let mostrar = true;

                // 🚨 LÓGICA DE DEPENDENCIAS (Niveles)
                // No mostrar Nivel 2 si no tiene el Nivel 1
                if (item.nombre === 'Nitrógeno Criogénico' && !tieneHielo1) mostrar = false;
                if (item.nombre === 'Socio de Honor' && !tieneMulti1) mostrar = false;

                // Opcional: No mostrar Nivel 1 si ya compró el Nivel 2 (para limpiar la tienda)
                if (item.nombre === 'Tanque de Nitrógeno' && tieneHielo1) mostrar = false;
                if (item.nombre === 'Contrato VIP' && tieneMulti1) mostrar = false;

                // No mostrar nada que ya haya comprado (si es mejora única)
                const yaLoTiene = inv.some(i => (i.id_item || i.idItem) === (item.id_item || item.idItem));
                if (yaLoTiene) mostrar = false;

                if (mostrar) {
                    const idDelObjeto = item.id_item || item.idItem;
                    shopList.innerHTML += `
                        <div class="shop-item group">
                            <div class="item-type-tag">MEJORA</div>
                            <h4>${item.nombre}</h4>
                            <span class="desc">${item.descripcion}</span>
                            <div class="price">🪙 ${item.precio}</div>
                            <button class="btn-item-action btn-buy"onclick="app.comprarObjeto(${idDelObjeto}, ${item.precio}, '${item.nombre}', '${item.descripcion}')">
                                <div>COMPRAR</div>
                            </button>
                        </div>
                    `;
                }
            });

            if (shopList.innerHTML === "") {
                shopList.innerHTML = '<p class="empty-msg">¡Has comprado todas las mejoras disponibles!</p>';
            }

        } catch (error) {
            console.error("Error cargando la tienda:", error);
        }
    },

    comprarObjeto: async (idItemParam, precio, nombreItem, descItem) => {
        // 🛡️ Seguro anti-crashes: si por algún motivo inventario es undefined, lo creamos vacío
        if (!app.currentUser.inventario) app.currentUser.inventario = [];

        if (app.currentUser.creditos < precio) {
            alert("Créditos insuficientes 😔");
            return;
        }

        try {
            const response = await fetch(`/usuarios/buy/${idItemParam}`, {
                method: 'POST',
                credentials: 'include' // 🔑
            });

            // 🛡️ Leer la respuesta de forma segura (por si el backend devuelve un String y no un JSON)
            const textResponse = await response.text();
            let data = {};
            if (textResponse) {
                try { data = JSON.parse(textResponse); }
                catch (e) { data = { mensaje: textResponse }; }
            }

            if (response.ok) {
                // Actualizar los créditos en pantalla
                app.currentUser.creditos = data.nuevo_saldo || data.nuevoSaldo || (app.currentUser.creditos - precio);

                // Meter el objeto en el inventario visual
                app.currentUser.inventario.push({
                    id_item: idItemParam,
                    nombre: nombreItem,
                    descripcion: descItem
                });

                alert("¡Compra exitosa! Has adquirido: " + nombreItem);
                app.updateUserUI();
                app.loadTienda();
            } else {
                alert(data.mensaje || data.error || "El servidor rechazó la compra.");
            }
        } catch (error) {
            console.error("Error en la transacción:", error);
            alert("Fallo de conexión con la tienda.");
        }
    },

    addCreditsAdmin: () => {
        // Tu función admin (La dejamos tal cual estaba)
        const targetUser = document.getElementById('admin-user-target').value;
        const amount = parseInt(document.getElementById('admin-credits-amount').value);
        if(app.currentUser.isAdmin && !isNaN(amount)) {
            app.currentUser.creditos += amount;
            alert(`Éxito. Saldo sumado a tu cuenta.`);
            app.updateUserUI();
            localStorage.setItem('currentUser', JSON.stringify(app.currentUser));
        }
    }
}; // <-- FINAL DEL OBJETO APP (Asegúrate de no borrar esto ni el window.onload de debajo)

window.app = app;
window.Rosco= Rosco;

window.onload = () => {
    if (localStorage.getItem('isLogged') === 'true') {
        app.restoreSession();
    } else {
        // Si no hay nota, ni lo intentamos. Mostramos el login directamente y evitamos el error 401.
        app.showScreen('login-screen');
    }
};

document.addEventListener('DOMContentLoaded', () => {
    // 1. Generales y Menú
    document.getElementById('tutorial_comp').addEventListener('click', app.completarTutorial);
    document.getElementById('card-tutorial').addEventListener('click', app.openTutorial);
    document.getElementById('theme-toggle').addEventListener('click', app.toggleTheme);
    document.getElementById('btn-login').addEventListener('click', app.login);
    document.getElementById('btn-logout').addEventListener('click', app.logout);

    // 2. Navegación de las Tarjetas
    document.getElementById('card-play').addEventListener('click', app.startGame);
    document.getElementById('card-profile').addEventListener('click', app.openProfile);
    document.getElementById('card-shop').addEventListener('click', app.openShop);
    document.getElementById('av-top').addEventListener('change', updateAvatarPreview);
    document.getElementById('av-acc').addEventListener('change', updateAvatarPreview);

    // 3. Tarjeta Admin (Ir a la otra página)
    document.getElementById('card-admin').addEventListener('click', () => {
        window.location.href = 'gestion-usuarios.html';
    });

    document.getElementById('btn-save-avatar').addEventListener('click', async (e) => {
        const btn = e.target;
        btn.innerText = "Guardando...";
        const config = updateAvatarPreview();

        try {
            // Usa aquí tu constante API_URL o el enlace directo de Railway
            const response = await fetch(`/usuarios/avatar`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ config: config })
            });

            if (response.ok) {
                app.currentUser.avatar = config;
                btn.innerText = "¡Look Guardado! ✔️";
                btn.style.backgroundColor = "#2e7d32";
                setTimeout(() => {
                    btn.innerText = "💾 Guardar Look";
                    btn.style.backgroundColor = "#4CAF50";
                }, 2000);
            }
        } catch (e) {
            console.error("Error en el taller:", e);
            btn.innerText = "💾 Guardar Look";
        }
    });

    document.getElementById('update-profile-form').onsubmit = async (e) => {
        e.preventDefault();

        try {
            const API_URL = '';

            // Ponemos el botón en modo "Carga" para evitar doble clic
            const btnSubmit = e.target.querySelector('button[type="submit"]');
            btnSubmit.innerHTML = '<div><i class="fas fa-spinner fa-spin"></i> GUARDANDO...</div>';
            btnSubmit.disabled = true;

            // 📦 EL PAQUETE LIMPIO: Solo enviamos el nombre y la nueva clave (si la hay)
            const payloadPut = {
                nombre_usuario: document.getElementById('upd-username').value
            };

            const newPassword = document.getElementById('upd-password').value;
            if (newPassword.trim() !== '') {
                payloadPut.contrasenha_usuario = newPassword;
            }

            // 🚀 Disparamos la actualización al pit lane
            const responsePut = await fetch(`${API_URL}/usuarios/perfil`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                    // 🛑 ELIMINADO: 'Authorization': `Bearer ${token}`
                    // Ya no hace falta, porque la seguridad va por la Cookie.
                },
                credentials: 'include', // 🔑 ESTA LÍNEA ES LA MAGIA. Envía la cookie automáticamente.
                body: JSON.stringify(payloadPut)
            });

            if (responsePut.ok) {
                alert("¡Look y Perfil actualizados con éxito! Por seguridad, vuelve a iniciar sesión.");
                // Obligamos al usuario a reloguearse para que Spring Boot renueve sus credenciales
                app.logout();
            } else {
                // Leemos la queja del servidor si algo va mal
                const errText = await responsePut.text();
                throw new Error(errText || "El taller rechazó los cambios.");
            }

        } catch (error) {
            alert("Fallo en boxes: " + error.message);
            console.error("Detalle del error:", error);
        } finally {
            // Restauramos el botón a la normalidad
            const btnSubmit = e.target.querySelector('button[type="submit"]');
            if(btnSubmit) {
                btnSubmit.innerHTML = '<div>GUARDAR CAMBIOS</div>';
                btnSubmit.disabled = false;
            }
        }
    };

    // 4. Botones de Volver
    document.getElementById('btn-back-profile').addEventListener('click', () => app.showScreen('dashboard-screen'));
    document.getElementById('btn-back-shop').addEventListener('click', () => app.showScreen('dashboard-screen'));

    // 5. Controles del Juego
    document.getElementById('btn-abort').addEventListener('click', app.abortGame);
    document.getElementById('btn-retry').addEventListener('click', app.startGame);
    document.getElementById('btn-menu').addEventListener('click', () => app.showScreen('dashboard-screen'));

    const gameInput = document.getElementById('game-input');
    if (gameInput) {
        gameInput.addEventListener('input', app.checkInput);
        gameInput.addEventListener('keydown', app.handleKeydown);
        gameInput.addEventListener('paste', e => e.preventDefault()); // Evitar hacer trampa copiando
        gameInput.addEventListener('contextmenu', e => e.preventDefault());
    }
});