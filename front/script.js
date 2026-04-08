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

// ==========================================
// MOTOR DEL JUEGO: EL ROSCO (Clásico + IA)
// ==========================================

const Rosco = {
    data: [], curr: 0, ok: 0, bad: 0, time: 180, timer: null,

    // --- 1. ARRANQUE MODO CLÁSICO (Usa la variable DB si la tienes) ---
    init() {
        // OJO: Si no vas a usar la base de datos local (DB), este init() no te hará falta.
        // Lo dejamos por si quieres mantener ambos modos.
        if (typeof DB === 'undefined') {
            alert("No se encontró la base de datos local para el modo clásico.");
            return;
        }
        const availableLetters = Object.keys(DB.questionsPool).sort();
        this.data = availableLetters.map(l => {
            const variants = DB.questionsPool[l];
            const rand = variants[Math.floor(Math.random() * variants.length)];
            return { id: l, q: rand.question, a: rand.answer, st: null };
        });
        this.curr = 0; this.ok = 0; this.bad = 0; this.time = 180;
        document.getElementById('ui-rosco').classList.remove('hidden');
        document.getElementById('r-play').classList.remove('hidden');
        document.getElementById('r-end').classList.add('hidden');
        document.getElementById('r-summary').classList.add('hidden');
        this.draw(); this.loadQ(); this.startTimer();
    },

    // --- 2. ARRANQUE MODO IA (El nuevo motor) ---
    initIA: async function() {
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
            const url = `https://gateway-production-a1f6.up.railway.app/incidencias/game/rosco-ia?tema=${encodeURIComponent(temaInput)}`;
            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` }
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
            this.curr = 0; this.ok = 0; this.bad = 0; this.time = 180;
            this.draw();
            this.loadQ();
            this.startTimer();

        } catch (error) {
            console.error("Fallo:", error);
            alert("Hubo un fallo en boxes al contactar con la IA. Volviendo al menú.");
            // Cambia App.home() por tu función real para volver al menú, por ejemplo app.showScreen('menu-screen');
            if(typeof App !== 'undefined') App.home();
        }
    },

    // --- 3. DIBUJAR EL ROSCO CIRCULAR ---
    draw() {
        const ul = document.getElementById('r-circle'); ul.innerHTML = '';
        const total = this.data.length;
        this.data.forEach((d, i) => {
            const li = document.createElement('li'); li.className = 'letter-item';
            li.id = 'rn-' + i; li.innerText = d.id;
            const ang = (360 / total) * i - 90; const rad = ang * (Math.PI / 180);
            li.style.transform = `translate(${180 * Math.cos(rad)}px, ${180 * Math.sin(rad)}px)`;
            ul.appendChild(li);
        });
    },

    // --- 4. CARGAR LA SIGUIENTE PREGUNTA ---
    loadQ() {
        let p = -1;
        const total = this.data.length;
        for(let i = this.curr; i < total; i++) if (!this.data[i].st) { p = i; break; }
        if (p === -1) for(let i = 0; i < total; i++) if (!this.data[i].st) { p = i; break; }
        if (p === -1) { this.finish(); return; }
        this.curr = p;
        document.getElementById('r-char').innerText = this.data[p].id;
        document.getElementById('r-def').innerText = this.data[p].q;
        document.getElementById('r-input').value = ''; document.getElementById('r-input').focus();
        document.querySelectorAll('.letter-item').forEach(el => el.classList.remove('active'));
        document.getElementById('rn-' + p).classList.add('active');
    },

    // --- 5. COMPROBAR RESPUESTA ---
    check() {
        const v = normalize(document.getElementById('r-input').value);
        const item = this.data[this.curr];
        const node = document.getElementById('rn-' + this.curr);
        if (v === normalize(item.a)) { item.st = 'ok'; this.ok++; node.classList.add('correct'); }
        else { item.st = 'bad'; this.bad++; node.classList.add('wrong'); }
        this.curr++; this.loadQ();
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
            this.time--; document.getElementById('r-time').innerText = this.time;
            if (this.time <= 0) this.finish();
        }, 1000);
    },
    stop() { clearInterval(this.timer); },

    // --- 8. PANTALLA FINAL ---
    finish() {
        this.stop();
        document.getElementById('r-play').classList.add('hidden');
        document.getElementById('r-end').classList.remove('hidden');
        document.getElementById('r-ok').innerText = this.ok;
        document.getElementById('r-bad').innerText = this.bad;
        const sum = document.getElementById('r-summary'); sum.innerHTML = '';
        sum.classList.remove('hidden');
        this.data.forEach(d => {
            const div = document.createElement('div'); div.className = 'summary-item';
            const icon = d.st === 'ok' ? '✅' : '❌';
            div.innerHTML = `<span class="${d.st === 'ok' ? 'sum-correct' : 'sum-wrong'}">${d.id} ${icon}</span> <b>${d.a.toUpperCase()}</b><br><small>${d.q}</small>`;
            sum.appendChild(div);
        });

        // Aquí puedes enlazar tu lógica para guardar los créditos si quieres:
        // const creditosGanados = this.ok;
        // Lógica de actualizarBBDD(creditosGanados);
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

// Y no te olvides de esta pequeña función que usa el Rosco para quitar tildes
function normalize(s) {
    return s.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
}

const app = {
    // --- ESTADO (Igual) ---
    currentUser: null,
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
            target.classList.remove('hidden');
            target.classList.add('active'); // ¡Ahora la de resultados sí recibe esto!
    },
    restoreSession: () => {
        if (localStorage.getItem('theme') === 'light') {
            document.body.classList.add('light-mode');
            const btnTheme = document.getElementById('theme-toggle');
            if (btnTheme) btnTheme.innerText = "☀️";
        }

        // Leemos la memoria del navegador
        const token = localStorage.getItem('jwt_token');
        const userData = localStorage.getItem('currentUser');

        if (token && userData) {
            // Si hay sesión guardada, la montamos en RAM
            app.currentUser = JSON.parse(userData);

            // Refrescamos la interfaz (pinta créditos, el perfil y el menú admin)
            app.updateUserUI();

            // Saltamos directamente al menú principal, esquivando el login
            app.showScreen('dashboard-screen');
        } else {
            // Si no hay datos, mostramos la pantalla de login normal
            app.showScreen('login-screen');
        }
    },




 // --- LOGIN (Conectado al Backend Real) ---
    login: async () => {
        const userVal = document.getElementById('username').value.trim();
        const passVal = document.getElementById('password').value.trim();
        const errorMsg = document.getElementById('error-msg');

        // 1. Pequeña validación antes de molestar al servidor
        if (!userVal || !passVal) {
            errorMsg.innerText = "Por favor, rellena ambos campos.";
            errorMsg.classList.remove('hidden');
            return;
        }

        try {
            // Opcional: Feedback visual mientras el servidor piensa
            const btnLogin = document.getElementById('btn-login');
            const originalText = btnLogin.innerText;
            btnLogin.innerText = "Conectando...";
            btnLogin.disabled = true;

            const response = await fetch('https://gateway-production-a1f6.up.railway.app/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    correo_usuario: userVal,
                    contrasenha_usuario: passVal
                })
            });

            // Restauramos el botón
            btnLogin.innerText = originalText;
            btnLogin.disabled = false;

            if (!response.ok) throw new Error("Credenciales inválidas");

            // Leemos el JSON una única vez
            const data = await response.json();

            // Guardamos la llave de la API
            localStorage.setItem('jwt_token', data.token);

            // 2. EL TRUCO DEL INVENTARIO:
            // Si Java manda números [1, 2], lo convertimos a objetos [{idItem: 1}, {idItem: 2}]
            // para que no haya problemas de compatibilidad con la tienda.
            let inventarioSeguro = [];
            if (data.inventario && Array.isArray(data.inventario)) {
                inventarioSeguro = data.inventario.map(item => {
                    return (typeof item === 'object') ? item : { id_item: item };
                });
            }

            // 3. Montamos el usuario en la memoria RAM
            app.currentUser = {
                username: data.username,
                creditos: data.creditos || 0,
                inventario: inventarioSeguro,
                // AHORA LEE EL ROL REAL DE LA BASE DE DATOS
                isAdmin: data.isAdmin === true
            };

            // 4. GUARDADO EN DISCO: Guardamos el perfil para sobrevivir al F5
            localStorage.setItem('currentUser', JSON.stringify(app.currentUser));

            // 5. Actualizamos la interfaz y cambiamos de pantalla
            app.updateUserUI();
            app.showScreen('dashboard-screen');
            errorMsg.classList.add('hidden');

            // 6. Pre-cargamos las frases de la IA en la sombra
            app.fetchMorePhrases();

        } catch (error) {
            console.error("Fallo de inicio de sesión:", error);
            errorMsg.innerText = "Credenciales inválidas o servidor desconectado.";
            errorMsg.classList.remove('hidden');
        }
    },
    
    logout: () => { app.currentUser = null; app.showScreen('login-screen'); },

    updateUserUI: () => {
        if(!app.currentUser) return;
        document.getElementById('user-display').innerText = app.currentUser.username;
        document.getElementById('user-credits').innerText = app.currentUser.creditos;
        document.getElementById('profile-credits').innerText = app.currentUser.creditos;
        document.getElementById('shop-credits').innerText = app.currentUser.creditos;

        const adminCard = document.getElementById('card-admin');
        if (app.currentUser.isAdmin === true) {
            adminCard.classList.remove('hidden');
        } else {
            adminCard.classList.add('hidden');
        }

        app.applyCosmetics();
    },

    applyCosmetics: () => {

        document.body.classList.remove('tema-cyberpunk', 'teclado-neon');
        if (!app.currentUser) return;


        const equipado = app.currentUser.inventario.find(i => (i.id_item || i.idItem) === app.currentUser.colorTema);


        if (equipado) {
            if (equipado.nombre === 'Tema Cyberpunk') document.body.classList.add('tema-cyberpunk');
            if (equipado.nombre === 'Teclado Neón') document.body.classList.add('teclado-neon');
        }
    },



    // --- LÓGICA DE JUEGO ---
    startGame: () => {
        // 1. Ajustes del Modo y Limpieza TOTAL
        app.modoPersonalizado = false;
        app.temaElegido = "";
        app.phraseBuffer = []; // Vaciamos frases viejas

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
            let url = `https://gateway-production-a1f6.up.railway.app/incidencias/game/frase?dificultad=media&t=${timestamp}`;

            // 2. Si estamos en modo personalizado, acoplamos el tema a la URL
            if (app.modoPersonalizado && app.temaElegido) {
                // encodeURIComponent asegura que los espacios (ej: "Formula 1") viajen bien por internet
                url += `&tema=${encodeURIComponent(app.temaElegido)}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` }
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

            console.log("Frases añadidas con éxito:", nuevasFrases);

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
            // --- LÓGICA DEL ESCUDO DE ERROR ---
            if (app.escudoActivo) {
                // El borde se pone amarillo dorado advirtiendo del fallo, pero absorbe el golpe
                input.style.borderColor = '#ffd700';
                app.escudoActivo = false; // Se gasta el escudo en este fallo
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
        if(inputVal.length > 0) {
            const points = app.calculatePhraseScore(app.currentPhrase.texto, inputVal);
            app.score += points;
            app.gameHistory.push({ target: app.currentPhrase.texto, input: inputVal, points: points });
        }

        // Calcular créditos
        const creditsEarned = Math.floor(app.score * 0.1);

        app.currentUser.creditos += creditsEarned;
        app.updateUserUI();
        document.getElementById('earned-credits').innerText = creditsEarned;
        app.renderResults();
        app.showScreen('results-screen');
        app.animateValue("final-score", 0, app.score, 1500);

        // 🛑 EL FILTRO INTELIGENTE: Solo llamamos a Java si hemos ganado algo
        if (creditsEarned > 0) {
            // Forzamos a que sea un entero puro de JavaScript
            const payloadJSON = JSON.stringify({ creditosExtra: parseInt(creditsEarned) });

            try {
                const url = 'https://gateway-production-a1f6.up.railway.app/usuarios/actualizar-creditos';
                const response = await fetch(url, {
                    method: 'POST', // Aseguramos que es POST
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
                    },
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
                const nombre = item.nombre || 'Objeto Misterioso';

                // Determinamos el tipo de objeto basándonos en su nombre
                const esCosmetico = nombre.includes('Tema') || nombre.includes('Teclado');

                let isEquipped = false;
                if (esCosmetico) {
                    // Los cosméticos se guardan en colorTema
                    isEquipped = app.currentUser.colorTema === idDelObjeto;
                } else {
                    // Las pasivas se guardan en el nuevo array habilidadesEquipadas
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
    },

    toggleEquip: (id_item, esCosmetico) => {
        // Inicializamos el array de habilidades si el usuario es viejo y no lo tenía
        if (!app.currentUser.habilidadesEquipadas) app.currentUser.habilidadesEquipadas = [];

        if (esCosmetico) {
            // LÓGICA: 1 SOLO COSMÉTICO (Si pulsas el que ya tienes, te lo quitas)
            if (app.currentUser.colorTema === id_item) {
                app.currentUser.colorTema = null; // Desequipar
            } else {
                app.currentUser.colorTema = id_item; // Equipar nuevo (reemplaza al anterior)
            }
        } else {
            // LÓGICA: MÁXIMO 2 PASIVAS
            const index = app.currentUser.habilidadesEquipadas.indexOf(id_item);

            if (index > -1) {
                // Si ya la tenía equipada, la quitamos (Desequipar)
                app.currentUser.habilidadesEquipadas.splice(index, 1);
            } else {
                // Si no la tiene, comprobamos el límite antes de equipar
                if (app.currentUser.habilidadesEquipadas.length >= 2) {
                    alert("⚠️ Límite alcanzado: Solo puedes equipar 2 habilidades pasivas a la vez.");
                    return;
                }
                app.currentUser.habilidadesEquipadas.push(id_item);
            }
        }

        // Guardamos el perfil en memoria y refrescamos la vista
        localStorage.setItem('currentUser', JSON.stringify(app.currentUser));
        app.openProfile();
        app.applyCosmetics(); // Actualizamos la interfaz gráfica al instante
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
            const response = await fetch('https://gateway-production-a1f6.up.railway.app/usuarios/tienda', {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` }
            });
            const items = await response.json();

            const shopList = document.getElementById('shop-list');
            shopList.innerHTML = "";

            if(items.length === 0) {
                shopList.innerHTML = '<p class="empty-msg">No hay productos en la tienda.</p>';
                return;
            }

            items.forEach(item => {
                // CORRECCIÓN: Usamos id_item por culpa del SNAKE_CASE de Spring Boot
                const idDelObjeto = item.id_item || item.id_item;

                // Comprobamos si ya lo tiene comprado
                const yaComprado = app.currentUser.inventario.some(i => (i.id_item) === idDelObjeto);

                shopList.innerHTML += `
                    <div class="shop-item">
                        <h4>${item.nombre}</h4>
                        <span class="desc">${item.descripcion || item.tipo}</span>
                        <div class="price">🪙 ${item.precio}</div>
                        <button class="${yaComprado ? 'btn-equipped' : 'btn-buy'}" 
                            onclick="app.comprarObjeto(${idDelObjeto}, ${item.precio}, '${item.nombre}')"
                            ${yaComprado ? 'disabled' : ''}>
                            ${yaComprado ? 'Comprado' : 'Comprar'}
                        </button>
                    </div>
                `;
            });
        } catch (error) {
            console.error("Error cargando la tienda:", error);
            document.getElementById('shop-list').innerHTML = '<p class="error-msg">Error de conexión con el servidor.</p>';
        }
    },

    comprarObjeto: async (idItemParam, precio, nombreItem) => {
        if (app.currentUser.creditos < precio) {
            alert("Créditos insuficientes 😔");
            return;
        }

        try {
            const response = await fetch(`https://gateway-production-a1f6.up.railway.app/usuarios/buy/${idItemParam}`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` }
            });

            const data = await response.json();

            if (response.ok) {
                app.currentUser.creditos = data.nuevo_saldo || data.nuevoSaldo; // Por si el saldo también viene en snake_case

                // CORRECCIÓN: Guardamos usando id_item para mantener coherencia
                app.currentUser.inventario.push({ id_item: idItemParam, nombre: nombreItem });

                localStorage.setItem('currentUser', JSON.stringify(app.currentUser));

                alert("¡Has comprado: " + nombreItem + "!");

                app.updateUserUI();
                app.loadTienda();
            } else {
                alert(data.mensaje || "Hubo un error en la compra");
            }
        } catch (error) {
            console.error("Error:", error);
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

window.onload = () => {
    app.restoreSession();
};

document.addEventListener('DOMContentLoaded', () => {
    // 1. Generales y Menú
    document.getElementById('theme-toggle').addEventListener('click', app.toggleTheme);
    document.getElementById('btn-login').addEventListener('click', app.login);
    document.getElementById('btn-logout').addEventListener('click', app.logout);

    // 2. Navegación de las Tarjetas
    document.getElementById('card-play').addEventListener('click', app.startGame);
    document.getElementById('card-profile').addEventListener('click', app.openProfile);
    document.getElementById('card-shop').addEventListener('click', app.openShop);

    // 3. Tarjeta Admin (Ir a la otra página)
    document.getElementById('card-admin').addEventListener('click', () => {
        window.location.href = 'gestion-usuarios.html';
    });

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