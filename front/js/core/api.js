// js/core/api.js

export const API_URL = ''; // Vacío porque usamos rutas relativas (Proxy/Monolito)

// Función ayudante para no escribir 'credentials: include' 100 veces
export async function apiFetch(endpoint, options = {}) {
    const defaultOptions = {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    // Fusionamos las opciones por defecto con las que enviemos
    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: { ...defaultOptions.headers, ...options.headers }
    };

    // Si es GET, no enviamos Content-Type para evitar fallos
    if (finalOptions.method === 'GET' || !options.body) {
        delete finalOptions.headers['Content-Type'];
    }

    return fetch(`${API_URL}${endpoint}`, finalOptions);
}