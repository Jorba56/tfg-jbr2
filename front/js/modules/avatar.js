// js/modules/avatar.js
import { AppState } from '../core/state.js';
import { apiFetch } from '../core/api.js';
import { updateUserUI } from '../core/ui.js';

export function updateAvatarPreview() {
    const DICEBEAR_API = 'https://api.dicebear.com/9.x/avataaars/svg';
    if (!AppState.currentUser) return '';

    const base = AppState.currentUser.username;
    const top = document.getElementById('av-top')?.value || 'shortHair';
    const acc = document.getElementById('av-acc')?.value || 'none';

    let config = `${encodeURIComponent(base)}&top=${top}`;

    // Reparación de accesorios
    if (acc !== 'none') {
        config += `&accessories=${acc}&accessoriesProbability=100`;
    } else {
        config += `&accessoriesProbability=0`;
    }

    const finalUrl = `${DICEBEAR_API}?seed=${config}`;

    const avatarImg = document.getElementById('avatar-preview');
    if (avatarImg) {
        avatarImg.src = finalUrl;
    }

    return config; // Devolvemos la config por si queremos guardarla
}

export async function guardarPerfil(e) {
    e.preventDefault();
    const btnSubmit = e.target.querySelector('button[type="submit"]');

    try {
        if(btnSubmit) {
            btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
            btnSubmit.disabled = true;
        }

        const configAvatar = updateAvatarPreview();

        // Petición al backend para guardar los cambios
        const response = await apiFetch(`/usuarios/avatar`, {
            method: 'PUT',
            body: JSON.stringify({ avatarConfig: configAvatar })
        });

        if (response.ok) {
            alert("¡Cambios de chapa y pintura guardados!");
            AppState.currentUser.avatar = configAvatar;
            updateUserUI();
        } else {
            throw new Error("El taller rechazó los cambios.");
        }

    } catch (error) {
        alert("Fallo en boxes: " + error.message);
    } finally {
        if(btnSubmit) {
            btnSubmit.innerHTML = '<div>GUARDAR CAMBIOS</div>';
            btnSubmit.disabled = false;
        }
    }
}