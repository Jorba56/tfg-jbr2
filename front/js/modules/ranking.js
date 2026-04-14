import { apiFetch } from '../core/api.js';
import { showScreen } from '../core/ui.js';

export async function openRanking() {
    showScreen('ranking-screen');
    const list = document.getElementById('ranking-list');
    list.innerHTML = '<p class="text-center animate-pulse">Recuperando telemetría global...</p>';

    try {
        const response = await apiFetch('/usuarios/ranking/global', { method: 'GET' });
        if (!response.ok) throw new Error("Fallo de red");

        const ranking = await response.json();
        list.innerHTML = '';

        if (ranking.length === 0) {
            list.innerHTML = '<p class="text-center">Aún no hay pilotos en la pista.</p>';
            return;
        }

        ranking.forEach((piloto, index) => {
            let medalla = `#${index + 1}`;
            if (index === 0) medalla = '🥇';
            if (index === 1) medalla = '🥈';
            if (index === 2) medalla = '🥉';

            const esGanador = index === 0 ? 'border: 2px solid #ffd700; background: rgba(255, 215, 0, 0.1);' : '';
            const avatarUrl = piloto.avatar ? `https://api.dicebear.com/9.x/avataaars/svg?seed=${piloto.avatar}` : 'static/images/favicon.png';

            // ⚡ La magia está en el onclick: pasamos el ID del piloto
            list.innerHTML += `
                <div class="shop-item flex items-center justify-between" style="flex-direction: row; padding: 15px; cursor: pointer; ${esGanador}" onclick="app.openPlayerStats(${piloto.id})">
                    <div style="display: flex; align-items: center; gap: 15px;">
                        <span style="font-size: 1.5rem; font-weight: bold; width: 30px; text-align: center;">${medalla}</span>
                        <img src="${avatarUrl}" alt="Avatar" style="width: 40px; height: 40px; border-radius: 50%; border: 2px solid var(--primary);">
                        <h4 style="margin: 0; font-size: 1.1rem;">${piloto.username}</h4>
                    </div>
                    <div style="font-weight: bold; color: #ffd700; font-size: 1.1rem;">
                        🪙 ${piloto.creditos}
                    </div>
                </div>
            `;
        });
    } catch (error) {
        list.innerHTML = '<p class="text-center text-red-500">Error al contactar con boxes.</p>';
    }
}

export async function openPlayerStats(id) {
    const modal = document.getElementById('modal-stats');
    modal.classList.remove('hidden'); // Mostramos el modal en blanco al instante

    // Ponemos estado de carga
    document.getElementById('modal-username').innerText = "Cargando...";
    document.getElementById('modal-credits').innerText = "...";
    document.getElementById('modal-record-rosco').innerText = "-";
    document.getElementById('modal-record-frases').innerText = "-";
    document.getElementById('modal-bar-frases').style.width = "0%";
    document.getElementById('modal-pct-frases').innerText = "0%";

    try {
        // Pedimos los datos a tu nuevo Endpoint de Java
        const response = await apiFetch(`/usuarios/estadisticas/${id}`, { method: 'GET' });
        if (!response.ok) throw new Error("No se pudo obtener la telemetría.");

        const stats = await response.json();

        // Pintamos los datos recibidos
        document.getElementById('modal-avatar').src = `https://api.dicebear.com/9.x/avataaars/svg?seed=${stats.avatar}`;
        document.getElementById('modal-username').innerText = stats.username;
        document.getElementById('modal-credits').innerText = stats.creditos;
        document.getElementById('modal-record-rosco').innerText = stats.mejorRosco;
        document.getElementById('modal-record-frases').innerText = stats.mejorContrarreloj;

        // Animamos la barra de precisión
        setTimeout(() => {
            document.getElementById('modal-bar-frases').style.width = `${stats.pctFrases}%`;
            document.getElementById('modal-pct-frases').innerText = `${stats.pctFrases}%`;
        }, 100); // Pequeño retraso para que la animación CSS se dispare

    } catch (error) {
        document.getElementById('modal-username').innerText = "Piloto Desconocido";
        console.error("Error:", error);
    }
}

export function closeStatsModal() {
    document.getElementById('modal-stats').classList.add('hidden');
}