import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { showScreen, updateUserUI, animateValue } from '../core/ui.js';

export function activarPowerUp(tipo) {
    if (AppState.powerUps.inventario[tipo] > 0) {
        AppState.powerUps.inventario[tipo]--;
        const qtySpan = document.getElementById(`qty-${tipo}`);
        if(qtySpan) qtySpan.innerText = AppState.powerUps.inventario[tipo];

        if (AppState.powerUps.inventario[tipo] === 0) {
            const btn = document.getElementById(`btn-pu-${tipo}`);
            if(btn) { btn.disabled = true; btn.style.opacity = "0.3"; }
        }

        if (tipo === 'freeze' && !AppState.powerUps.tiempoCongelado) {
            AppState.powerUps.tiempoCongelado = true;
            const btn = document.getElementById('btn-pu-freeze');
            if(btn) btn.style.boxShadow = "0 0 20px #00f2fe";
            setTimeout(() => {
                AppState.powerUps.tiempoCongelado = false;
                if(btn) btn.style.boxShadow = "";
            }, 5000);
        } else if (tipo === 'multiplier') {
            AppState.powerUps.multiplicadorActivo = 2;
            const btn = document.getElementById('btn-pu-multiplier');
            if(btn) btn.style.boxShadow = "0 0 20px #ffd700";
        }
    }
}

export function cargarPowerUps() {
    if (!AppState.currentUser) return;
    const inv = AppState.currentUser.inventario || [];
    const equipadas = AppState.currentUser.habilidadesEquipadas || [];

    const tieneHielo1 = inv.find(i=>i.nombre==='Tanque de Nitrógeno') && equipadas.includes(inv.find(i=>i.nombre==='Tanque de Nitrógeno').id_item || inv.find(i=>i.nombre==='Tanque de Nitrógeno').idItem);
    const tieneHielo2 = inv.find(i=>i.nombre==='Nitrógeno Criogénico') && equipadas.includes(inv.find(i=>i.nombre==='Nitrógeno Criogénico').id_item || inv.find(i=>i.nombre==='Nitrógeno Criogénico').idItem);
    const tieneMulti1 = inv.find(i=>i.nombre==='Contrato VIP') && equipadas.includes(inv.find(i=>i.nombre==='Contrato VIP').id_item || inv.find(i=>i.nombre==='Contrato VIP').idItem);

    const containerPasivas = document.getElementById('container-pasivas');
    const containerActivos = document.getElementById('powerup-bar');
    if(!containerPasivas || !containerActivos) return;

    containerPasivas.innerHTML = ""; containerActivos.innerHTML = "";

    const multiVal = tieneMulti1 ? 2 : 1;
    AppState.powerUps.multiplicadorActivo = multiVal;
    if (multiVal > 1) containerPasivas.innerHTML = `<div class="stat-badge">💰 x${multiVal}</div>`;

    let usosHielo = tieneHielo2 ? 2 : (tieneHielo1 ? 1 : 0);
    AppState.powerUps.inventario.freeze = usosHielo;

    if (usosHielo > 0) {
        containerActivos.innerHTML = `<button id="btn-pu-freeze" class="powerup-btn">❄️ <span id="qty-freeze" class="pu-qty">${usosHielo}</span></button>`;
        document.getElementById('btn-pu-freeze').onclick = () => activarPowerUp('freeze');
    }

    AppState.powerUps.tieneRebufo = inv.find(i=>i.nombre==='Rebufo') && equipadas.includes(inv.find(i=>i.nombre==='Rebufo').id_item);
    AppState.powerUps.tieneRadar = inv.find(i=>i.nombre==='Radar') && equipadas.includes(inv.find(i=>i.nombre==='Radar').id_item);
    AppState.powerUps.tieneSegundaOportunidad = inv.find(i=>i.nombre==='Segunda Oportunidad') && equipadas.includes(inv.find(i=>i.nombre==='Segunda Oportunidad').id_item);
}

export function startGame() { startCommon(false); }
export function iniciarPartidaPersonalizada() {
    const inputTema = document.getElementById('input-tema').value.trim();
    if (inputTema.length < 3) return alert("El tema debe tener al menos 3 letras.");
    startCommon(true, inputTema);
}

function startCommon(isCustom, tema = "") {
    AppState.powerUps.comboAcertadas = 0;
    AppState.powerUps.chanceUsada = false;
    AppState.game.modoPersonalizado = isCustom;
    AppState.game.temaElegido = tema;
    AppState.game.phraseBuffer = [];
    AppState.powerUps.tiempoCongelado = false;
    AppState.game.score = 0;
    AppState.game.gameHistory = [];

    const inv = AppState.currentUser.inventario || [];
    const habEquipadas = AppState.currentUser.habilidadesEquipadas || [];
    const tieneReloj = inv.find(i=>i.nombre==='Reloj de Arena') && habEquipadas.includes(inv.find(i=>i.nombre==='Reloj de Arena').id_item);
    AppState.game.timeLeft = tieneReloj ? 65 : 60;
    AppState.powerUps.escudoActivo = inv.find(i=>i.nombre==='Escudo de Error') && habEquipadas.includes(inv.find(i=>i.nombre==='Escudo de Error').id_item);

    cargarPowerUps();
    document.getElementById('score').innerText = "0";
    document.getElementById('timer').innerText = AppState.game.timeLeft;

    showScreen('game-screen');
    loadNewPhrase();

    if (AppState.game.timerInterval) clearInterval(AppState.game.timerInterval);
    AppState.game.timerInterval = setInterval(() => {
        if (AppState.powerUps.tiempoCongelado) return;
        AppState.game.timeLeft--;
        document.getElementById('timer').innerText = AppState.game.timeLeft;
        if(AppState.game.timeLeft <= 0) endGame();
    }, 1000);
}

export async function fetchMorePhrases() {
    if (AppState.game.isFetchingPhrases) return;
    AppState.game.isFetchingPhrases = true;
    try {
        let url = `/incidencias/game/frase?dificultad=media&t=${new Date().getTime()}`;
        if (AppState.game.modoPersonalizado && AppState.game.temaElegido) url += `&tema=${encodeURIComponent(AppState.game.temaElegido)}`;

        const response = await apiFetch(url, { method: 'GET' });
        if (!response.ok) throw new Error();

        const partes = (await response.text()).replace(/[\r\n]+/g, " ").trim().split('|');
        const nuevasFrases = partes.map(f => f.trim().replace(/^[0-9.\-\s]+/, "")).filter(f => f.length > 25);
        AppState.game.phraseBuffer.push(...nuevasFrases);
    } catch (e) { AppState.game.phraseBuffer.push("El coche de seguridad está en la pista por un fallo técnico"); }
    finally { AppState.game.isFetchingPhrases = false; }
}

export async function loadNewPhrase() {
    const display = document.getElementById('phrase-display');
    const input = document.getElementById('game-input');

    if (AppState.game.phraseBuffer.length === 0) {
        display.innerText = "⏳ Conectando con boxes... repostando frases";
        input.disabled = true;
        if (!AppState.game.isFetchingPhrases) fetchMorePhrases();
        while (AppState.game.phraseBuffer.length === 0) await new Promise(r => setTimeout(r, 200));
    }

    AppState.game.currentPhrase = { texto: AppState.game.phraseBuffer.shift() };
    display.innerText = AppState.game.currentPhrase.texto;
    input.disabled = false; input.value = ""; input.focus();
    AppState.powerUps.chanceUsada = false;

    const previewEl = document.getElementById('next-phrase-preview');
    if (AppState.powerUps.tieneRadar && previewEl) {
        previewEl.classList.remove('hidden');
        previewEl.innerText = AppState.game.phraseBuffer.length > 0 ? "Siguiente: " + AppState.game.phraseBuffer[0] : "Cargando...";
    } else if (previewEl) previewEl.classList.add('hidden');

    if (AppState.game.phraseBuffer.length <= 2 && !AppState.game.isFetchingPhrases) fetchMorePhrases();
}

export function checkInput() {
    const input = document.getElementById('game-input');
    const val = input.value;
    const target = AppState.game.currentPhrase.texto;

    if(val === target) { input.style.borderColor = 'var(--success)'; }
    else if(!target.startsWith(val)) {
        if (AppState.powerUps.tieneSegundaOportunidad && !AppState.powerUps.chanceUsada && val.length > 0) {
            input.value = val.slice(0, -1);
            AppState.powerUps.chanceUsada = true;
            input.style.boxShadow = "0 0 15px rgba(157, 78, 221, 0.8)";
            setTimeout(() => input.style.boxShadow = "", 500);
            return;
        }
        if (AppState.powerUps.escudoActivo) { input.style.borderColor = '#ffd700'; AppState.powerUps.escudoActivo = false; }
        else { input.style.borderColor = 'var(--error)'; }
    } else { input.style.borderColor = 'var(--text-secondary)'; }
}

function calculatePhraseScore(target, input) {
    const tW = target.split(' '); const iW = input.split(' ');
    let pts = 0; tW.forEach((w, i) => { if (iW[i] === w) pts += 5; });
    return pts;
}

export function handleKeydown(e) {
    if (e.key === "Enter") {
        const inputVal = document.getElementById('game-input').value.trim();
        const targetText = AppState.game.currentPhrase.texto;

        if (inputVal === targetText) {
            if (AppState.powerUps.tieneRebufo) {
                AppState.powerUps.comboAcertadas++;
                if (AppState.powerUps.comboAcertadas >= 3) {
                    AppState.game.timeLeft += 2; AppState.powerUps.comboAcertadas = 0;
                    const timerEl = document.getElementById('timer');
                    timerEl.style.textShadow = "0 0 15px #00f2fe"; timerEl.style.color = "#00f2fe";
                    setTimeout(() => { timerEl.style.textShadow = ""; timerEl.style.color = ""; }, 500);
                }
            }
        } else { AppState.powerUps.comboAcertadas = 0; }

        const pointsEarned = calculatePhraseScore(targetText, inputVal);
        AppState.game.score += pointsEarned;
        document.getElementById('score').innerText = AppState.game.score;
        AppState.game.gameHistory.push({ target: targetText, input: inputVal, points: pointsEarned });
        loadNewPhrase();
    }
}

export async function endGame() {
    clearInterval(AppState.game.timerInterval);
    const inputVal = document.getElementById('game-input').value;
    if (inputVal.length > 0) {
        const points = calculatePhraseScore(AppState.game.currentPhrase.texto, inputVal);
        AppState.game.score += points;
        AppState.game.gameHistory.push({target: AppState.game.currentPhrase.texto, input: inputVal, points: points});
    }

    let creditosBase = Math.floor(AppState.game.score * 0.1);
    const multiplicador = AppState.powerUps.multiplicadorActivo || 1;
    const creditsEarned = creditosBase * multiplicador;

    AppState.currentUser.creditos += creditsEarned;
    updateUserUI();
    document.getElementById('earned-credits').innerText = creditsEarned;

    // Render Results
    const list = document.getElementById('history-list');
    list.innerHTML = "";
    AppState.game.gameHistory.forEach((item, idx) => {
        const div = document.createElement('div');
        div.innerHTML = `Frase ${idx+1}: <span style="color:${item.points>0?'var(--success)':'var(--error)'}">${item.points} pts</span>`;
        div.className = "history-item"; list.appendChild(div);
    });

    showScreen('results-screen');
    animateValue("final-score", 0, AppState.game.score, 1500);

    if (creditsEarned > 0) {
        try {
            await apiFetch('/usuarios/actualizar-creditos', { method: 'POST', body: JSON.stringify({creditosExtra: parseInt(creditsEarned)}) });
        } catch (e) {}
    } else { alert("Cero créditos ganados. ¡Puedes hacerlo mejor!"); }
}

export function abortGame() { clearInterval(AppState.game.timerInterval); showScreen('dashboard-screen'); }