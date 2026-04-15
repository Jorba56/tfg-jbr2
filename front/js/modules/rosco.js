import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { showScreen, updateUserUI } from '../core/ui.js';


function normalize(s) { return s.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim(); }

export const Rosco = {
    data: [], curr: 0, ok: 0, bad: 0, time: 180, timer: null,

    init: async function () {
        const temaInput = document.getElementById('input-tema-rosco').value.trim();
        if (temaInput.length < 3) return alert("Introduce un tema válido de al menos 3 letras.");

        showScreen('ui-rosco');
        document.getElementById('r-play').classList.remove('hidden');
        document.getElementById('r-end').classList.add('hidden');
        document.getElementById('r-summary').classList.add('hidden');

        document.getElementById('r-char').innerText = "⏳";
        document.getElementById('r-def').innerText = "Conectando con boxes... Generando 25 palabras sobre: " + temaInput;
        document.getElementById('r-circle').innerHTML = '';
        document.getElementById('r-input').disabled = true;

        try {
            const response = await apiFetch(`/incidencias/game/rosco-ia?tema=${encodeURIComponent(temaInput)}`, { method: 'GET' });
            if (!response.ok) throw new Error();

            this.data = (await response.json()).map(item => ({ id: item.letra.toUpperCase(), q: item.definicion, a: item.palabra, st: null }));
            document.getElementById('r-input').disabled = false;
            this.curr = 0; this.ok = 0; this.bad = 0; this.time = 180;
            this.draw(); this.loadQ(); this.startTimer();
        } catch (e) {
            alert("Hubo un fallo en boxes al contactar con la IA."); showScreen('dashboard-screen');
        }
    },
    draw() {
        const ul = document.getElementById('r-circle'); ul.innerHTML = '';
        this.data.forEach((d, i) => {
            const li = document.createElement('li');
            li.className = 'letter-item'; li.id = 'rn-' + i; li.innerText = d.id;
            const rad = ((360 / this.data.length) * i - 90) * (Math.PI / 180);
            li.style.transform = `translate(${300 * Math.cos(rad)}px, ${300 * Math.sin(rad)}px)`;
            ul.appendChild(li);
        });
    },
    loadQ() {
        let p = -1;
        for (let i = this.curr; i < this.data.length; i++) if (!this.data[i].st) { p = i; break; }
        if (p === -1) for (let i = 0; i < this.data.length; i++) if (!this.data[i].st) { p = i; break; }
        if (p === -1) return this.finish();

        this.curr = p;
        document.getElementById('r-char').innerText = this.data[p].id;
        document.getElementById('r-def').innerText = this.data[p].q;
        document.getElementById('r-input').value = ''; document.getElementById('r-input').focus();
        document.querySelectorAll('.letter-item').forEach(el => el.classList.remove('active'));
        document.getElementById('rn-' + p).classList.add('active');
    },

    check() {
        const v = normalize(document.getElementById('r-input').value);
        const item = this.data[this.curr];
        const node = document.getElementById('rn-' + this.curr);
        if (v === normalize(item.a)) {
            item.st = 'ok'; this.ok++; node.classList.add('correct');

            // 📡 ENVIAR PROGRESO AL RIVAL
            if (Multiplayer && Multiplayer.salaActual) Multiplayer.enviarProgreso(this.ok * 10);

        } else {
            item.st = 'bad'; this.bad++; node.classList.add('wrong');
        }
        this.curr++; this.loadQ();
    },

    pass() { document.getElementById('rn-' + this.curr).classList.remove('active'); this.curr++; this.loadQ(); },
    startTimer() {
        clearInterval(this.timer);
        this.timer = setInterval(() => {
            this.time--; document.getElementById('r-time').innerText = this.time;
            if (this.time <= 0) this.finish();
        }, 1000);
    },
    stop() { clearInterval(this.timer); },
    async finish() {
        this.stop();
        document.getElementById('r-play').classList.add('hidden');
        document.getElementById('r-end').classList.remove('hidden');
        document.getElementById('r-ok').innerText = this.ok;
        document.getElementById('r-bad').innerText = this.bad;
        document.querySelectorAll('.letter-item').forEach(el => el.classList.remove('active'));

        const sum = document.getElementById('r-summary');
        sum.innerHTML = ''; sum.classList.remove('hidden');
        this.data.forEach(d => {
            const div = document.createElement('div'); div.className = 'summary-item';
            div.innerHTML = `<span class="${d.st === 'ok' ? 'sum-correct' : 'sum-wrong'}">${d.id} ${d.st === 'ok' ? '✅' : '❌'}</span> <b>${d.a.toUpperCase()}</b><br><small>${d.q}</small>`;
            sum.appendChild(div);
        });

        const creditosGanados = this.ok * 10;
        document.getElementById('r-credits').innerText = creditosGanados;

        // 📡 AVISAR DE QUE HAS TERMINADO Y COMPROBAR GANADOR
        if (Multiplayer && Multiplayer.salaActual) {
            Multiplayer.enviarSeñal('FINISH', { puntuacion: creditosGanados });
            window.dispatchEvent(new Event('my-game-finished'));
        }

        if (creditosGanados > 0 && AppState.currentUser) {
            AppState.currentUser.creditos += creditosGanados;
            updateUserUI();

            // 📦 NUEVO PAQUETE DE TELEMETRÍA
            const payloadJSON = JSON.stringify({
                creditos: parseInt(creditosGanados),
                modo: "ROSCO",
                aciertos: this.ok,
                totalPalabras: this.data.length,
                puntuacion: creditosGanados // En tu rosco, la puntuación son los créditos x10
            });

            try {
                await apiFetch('/usuarios/guardar-partida', { // 📍 NUEVA RUTA
                    method: 'POST',
                    body: payloadJSON
                });
            } catch(e) {
                console.error("Error guardando el rosco", e);
            }
        }
    }
};