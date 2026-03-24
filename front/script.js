import { frasesRepository, usuariosRepository, catalogoCosmeticos } from './data.js';

const app = {
    // --- ESTADO (Igual) ---
    currentUser: null,
    currentPhrase: null,
    score: 0,
    timeLeft: 60,
    timerInterval: null,
    gameHistory: [],

    // --- NAVEGACIÓN Y TEMA (Igual) ---
    toggleTheme: () => {
        const body = document.body;
        body.classList.toggle('light-mode');
        document.getElementById('theme-toggle').innerText = body.classList.contains('light-mode') ? "☀️" : "🌙";
    },

    showScreen: (screenId) => {
        document.querySelectorAll('.screen').forEach(s => {
            s.classList.remove('active');
            s.classList.add('hidden');
        });
        const target = document.getElementById(screenId);
        target.classList.remove('hidden');
        if(screenId === 'results-screen') target.style.display = 'flex';
        else {
            target.style.display = 'block';
            target.classList.add('active');
        }
    },

 // --- LOGIN (Conectado al Backend Real) ---
    login: async () => {
        const userVal = document.getElementById('username').value.trim();
        const passVal = document.getElementById('password').value.trim();
        const errorMsg = document.getElementById('error-msg');
        
        try {
            // Petición real a Backend en Docker (API Gateway o Usuarios Service)
            const response = await fetch('http://localhost:8080/auth/login', { 
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    correo_usuario: userVal, 
                    contrasenha_usuario: passVal 
                })
            });

            if (!response.ok) {
                throw new Error("Credenciales inválidas");
            }

            // Extraemos el Token JWT
            const data = await response.json(); 
            
            // Guardamos el token en el navegador
            localStorage.setItem('jwt_token', data.token); 

            // Montamos el usuario activo
            app.currentUser = {
                username: userVal,
                creditos: data.creditos || 0, 
                inventario: [], 
                isAdmin: false 
            };

            // Actualizamos la interfaz y pasamos al Dashboard
            app.updateUserUI();
            app.showScreen('dashboard-screen');
            errorMsg.classList.add('hidden');

        } catch (error) {
            console.error(error);
            errorMsg.classList.remove('hidden');
        }
    },
    
    logout: () => { app.currentUser = null; app.showScreen('login-screen'); },

    updateUserUI: () => {
        if(!app.currentUser) return;
        document.getElementById('user-display').innerText = app.currentUser.username;
        document.getElementById('user-credits').innerText = app.currentUser.creditos;
        document.getElementById('shop-credits').innerText = app.currentUser.creditos;
        document.getElementById('profile-credits').innerText = app.currentUser.creditos;
    },

    // --- LÓGICA DE JUEGO (Resumida para centrar en Tienda) ---
    startGame: () => {

        app.score = 0; app.timeLeft = 60; app.gameHistory = [];
        document.getElementById('score').innerText = "0";
        app.showScreen('game-screen');
        app.loadNewPhrase();
        const input = document.getElementById('game-input');
        input.value = ""; input.focus();
        if (app.timerInterval) clearInterval(app.timerInterval);
        app.timerInterval = setInterval(() => {
            app.timeLeft--;
            document.getElementById('timer').innerText = app.timeLeft;
            if(app.timeLeft <= 0) app.endGame();
        }, 1000);
    },
    
    loadNewPhrase: () => {
        const randomIndex = Math.floor(Math.random() * frasesRepository.length);
        app.currentPhrase = frasesRepository[randomIndex];
        document.getElementById('phrase-display').innerText = app.currentPhrase.texto;
        document.getElementById('game-input').value = "";
        document.getElementById('game-input').style.borderColor = "var(--text-secondary)";
    },
    
    checkInput: () => {
        const input = document.getElementById('game-input');
        const val = input.value;
        const target = app.currentPhrase.texto;
        if(val === target) input.style.borderColor = 'var(--success)';
        else if(!target.startsWith(val)) input.style.borderColor = 'var(--error)';
        else input.style.borderColor = 'var(--text-secondary)';
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

    endGame: () => {
        clearInterval(app.timerInterval);
        const inputVal = document.getElementById('game-input').value;
        if(inputVal.length > 0) {
            const points = app.calculatePhraseScore(app.currentPhrase.texto, inputVal);
            app.score += points;
            app.gameHistory.push({ target: app.currentPhrase.texto, input: inputVal, points: points });
        }
        const creditsEarned = Math.floor(app.score * 0.1);
        app.currentUser.creditos += creditsEarned;
        app.updateUserUI();
        document.getElementById('earned-credits').innerText = creditsEarned;
        app.renderResults();
        app.showScreen('results-screen');
        app.animateValue("final-score", 0, app.score, 1500);
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
        
        // 1. Mostrar Panel Admin si corresponde
        const adminPanel = document.getElementById('admin-panel');
        if(app.currentUser.isAdmin) adminPanel.classList.remove('hidden');
        else adminPanel.classList.add('hidden');

        // 2. Renderizar Inventario (Solo lo comprado)
        const inventoryList = document.getElementById('inventory-list');
        inventoryList.innerHTML = "";

        if(app.currentUser.inventario.length === 0) {
            inventoryList.innerHTML = '<p class="empty-msg">Tu inventario está vacío.</p>';
        } else {
            app.currentUser.inventario.forEach(itemId => {
                const item = catalogoCosmeticos.find(c => c.id === itemId);
                if(item) {
                    const isEquipped = app.currentUser.colorTema === item.id;
                    inventoryList.innerHTML += `
                        <div class="shop-item">
                            <h4>${item.nombre}</h4>
                            <span class="desc">${item.tipo}</span>
                            <button class="${isEquipped ? 'btn-equipped' : 'btn-equip'}" onclick="app.equipItem('${item.id}')">
                                ${isEquipped ? 'Equipado' : 'Equipar'}
                            </button>
                        </div>
                    `;
                }
            });
        }
        app.showScreen('profile-screen');
    },

    // --- NUEVO: TIENDA (Catálogo + Buscador) ---
    openShop: () => {
        app.updateUserUI();
        document.getElementById('shop-search').value = ""; // Limpiar buscador
        app.renderShopItems(catalogoCosmeticos); // Mostrar todo al inicio
        app.showScreen('shop-screen');
    },

    // Función de renderizado que acepta un array (para poder filtrar)
    renderShopItems: (itemsToRender) => {
        const shopList = document.getElementById('shop-list');
        shopList.innerHTML = "";

        // Filtramos items que YA tenemos comprados para no mostrarlos en la tienda
        const itemsAvailable = itemsToRender.filter(item => !app.currentUser.inventario.includes(item.id));

        if(itemsAvailable.length === 0) {
            shopList.innerHTML = '<p class="empty-msg">No se encontraron productos.</p>';
            return;
        }

        itemsAvailable.forEach(item => {
            shopList.innerHTML += `
                <div class="shop-item">
                    <h4>${item.nombre}</h4>
                    <span class="desc">${item.desc}</span>
                    <div class="price">🪙 ${item.precio}</div>
                    <button class="btn-buy" onclick="app.buyItem('${item.id}')">Comprar</button>
                </div>
            `;
        });
    },

    // Lógica del buscador
    filterShop: () => {
        const term = document.getElementById('shop-search').value.toLowerCase();
        // Filtramos el catálogo global
        const filtered = catalogoCosmeticos.filter(item => 
            item.nombre.toLowerCase().includes(term) || 
            item.desc.toLowerCase().includes(term)
        );
        app.renderShopItems(filtered);
    },

    // --- ACCIONES DE TIENDA/PERFIL ---
    buyItem: (itemId) => {
        const item = catalogoCosmeticos.find(c => c.id === itemId);
        if(!item) return;

        if(app.currentUser.creditos >= item.precio) {
            app.currentUser.creditos -= item.precio;
            app.currentUser.inventario.push(itemId);
            alert(`¡Has comprado ${item.nombre}!`);
            app.updateUserUI();
            
            // Recargamos la tienda para que desaparezca el item comprado
            app.filterShop(); 
        } else {
            alert("No tienes suficientes créditos.");
        }
    },

    equipItem: (itemId) => {
        app.currentUser.colorTema = itemId;
        // Aquí iría la lógica para aplicar el CSS real (class changes)
        alert("¡Objeto equipado!");
        app.openProfile(); // Recargar para actualizar botones
    },

    addCreditsAdmin: () => {
        const targetUser = document.getElementById('admin-user-target').value;
        const amount = parseInt(document.getElementById('admin-credits-amount').value);
        const user = usuariosRepository.find(u => u.username === targetUser);
        
        if(user && !isNaN(amount)) {
            user.creditos += amount;
            alert(`Éxito. Saldo de ${user.username}: ${user.creditos}`);
            if(user.username === app.currentUser.username) app.updateUserUI();
        } else {
            alert("Error: Usuario no encontrado.");
        }
    }
};

window.app = app;

document.addEventListener('DOMContentLoaded', () => {
    // Listeners generales
    document.getElementById('theme-toggle').addEventListener('click', app.toggleTheme);
    document.getElementById('btn-login').addEventListener('click', app.login);
    document.getElementById('btn-logout').addEventListener('click', app.logout);
    
    // Navegación
    document.getElementById('card-play').addEventListener('click', app.startGame);
    
    // PERFIL
    document.getElementById('card-profile').addEventListener('click', app.openProfile);
    document.getElementById('btn-back-profile').addEventListener('click', () => app.showScreen('dashboard-screen'));
    document.getElementById('btn-add-credits').addEventListener('click', app.addCreditsAdmin);

    // TIENDA
    document.getElementById('card-shop').addEventListener('click', app.openShop);
    document.getElementById('btn-back-shop').addEventListener('click', () => app.showScreen('dashboard-screen'));
    
    // BUSCADOR (Evento 'input' para búsqueda en tiempo real)
    document.getElementById('shop-search').addEventListener('input', app.filterShop);

    // Juego
    document.getElementById('btn-abort').addEventListener('click', app.abortGame);
    document.getElementById('btn-retry').addEventListener('click', app.startGame);
    document.getElementById('btn-menu').addEventListener('click', () => app.showScreen('dashboard-screen'));
    const gameInput = document.getElementById('game-input');
    gameInput.addEventListener('input', app.checkInput);
    gameInput.addEventListener('keydown', app.handleKeydown);
    gameInput.addEventListener('paste', e => e.preventDefault());
    gameInput.addEventListener('contextmenu', e => e.preventDefault());
});