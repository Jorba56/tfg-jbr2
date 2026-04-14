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

        // --- 1. CONSTRUCCIÓN DEL PODIO (Top 5) ---
        let podiumHTML = '<div class="podium-layout">';

        // Recorremos solo hasta los 5 primeros (o los que haya si son menos de 5)
        const topCount = Math.min(ranking.length, 5);

        for (let i = 0; i < topCount; i++) {
            const piloto = ranking[i];
            const pos = i + 1; // 1, 2, 3, 4, 5

            let medalla = `#${pos}`;
            let bgStyle = 'background: rgba(0,0,0,0.4);';

            // Tamaños de avatar para el podio (El 1º es el rey)
            let avatarSize = 65;

            if (pos === 1) { medalla = '🥇'; bgStyle = 'background: rgba(255, 215, 0, 0.1);'; avatarSize = 90; }
            if (pos === 2) { medalla = '🥈'; bgStyle = 'background: rgba(192, 192, 192, 0.05);'; }
            if (pos === 3) { medalla = '🥉'; bgStyle = 'background: rgba(205, 127, 50, 0.05);'; }

            const avatarUrl = piloto.avatar ? `https://api.dicebear.com/9.x/avataaars/svg?seed=${piloto.avatar}` : 'static/images/favicon.png';

            podiumHTML += `
                <div class="shop-item podium-card pos-${pos}" style="${bgStyle}" onclick="app.openPlayerStats(${piloto.id})">
                    <div style="font-size: 2rem; margin-top: -10px; margin-bottom: 5px; text-shadow: 0 0 15px rgba(255,255,255,0.2);">${medalla}</div>
                    
                    <img src="${avatarUrl}" alt="Avatar" style="width: ${avatarSize}px; height: ${avatarSize}px; border-radius: 50%; border: 3px solid var(--primary); margin: 0 auto 10px auto; background: #1e293b;">
                    
                    <div style="flex-grow: 1;">
                        <h4 style="margin: 0; font-size: 1.1rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${piloto.username}</h4>
                        <div style="font-weight: bold; color: #ffd700; font-size: 1.2rem; margin-top: 5px;">🪙 ${piloto.creditos}</div>
                    </div>
                </div>
            `;
        }
        podiumHTML += '</div>';

        // --- 2. CONSTRUCCIÓN DE LA COLA (Puestos 6 en adelante) ---
        let restHTML = '<div class="ranking-rest">';

        for (let i = 5; i < ranking.length; i++) {
            const piloto = ranking[i];
            const avatarUrl = piloto.avatar ? `https://api.dicebear.com/9.x/avataaars/svg?seed=${piloto.avatar}` : 'static/images/favicon.png';

            // Tarjetas mini horizontales
            restHTML += `
                <div class="shop-item small-rank-card" onclick="app.openPlayerStats(${piloto.id})">
                    <div style="display: flex; align-items: center; gap: 15px;">
                        <span style="font-size: 0.9rem; font-weight: bold; width: 25px; text-align: center; color: var(--text-secondary);">#${i + 1}</span>
                        <img src="${avatarUrl}" alt="Avatar" style="width: 35px; height: 35px; border-radius: 50%; border: 1px solid var(--primary);">
                        <h4 style="margin: 0; font-size: 0.9rem;">${piloto.username}</h4>
                    </div>
                    <div style="font-weight: bold; color: #ffd700; font-size: 0.9rem;">
                        🪙 ${piloto.creditos}
                    </div>
                </div>
            `;
        }
        restHTML += '</div>';

        // Inyectamos todo de golpe
        list.innerHTML = podiumHTML + restHTML;

    } catch (error) {
        list.innerHTML = '<p class="text-center text-red-500">Error al contactar con boxes.</p>';
    }
}

export async function openPlayerStats(id) {
    const modal = document.getElementById('modal-stats');
    showScreen('player-stats-screen');

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