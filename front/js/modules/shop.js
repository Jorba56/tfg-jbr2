import { AppState } from '../core/state.js';
import { showScreen, updateUserUI } from '../core/ui.js';
import { apiFetch } from '../core/api.js';

export function openShop() {
    updateUserUI(); showScreen('shop-screen'); loadTienda();
}

export async function loadTienda() {
    try {
        const response = await apiFetch('/usuarios/tienda', { method: 'GET' });
        const items = await response.json();
        const shopList = document.getElementById('shop-list');
        if (!shopList) return; shopList.innerHTML = "";

        const inv = AppState.currentUser.inventario || [];
        const tieneHielo1 = inv.some(i => i.nombre === 'Tanque de Nitrógeno');
        const tieneMulti1 = (AppState.currentUser.habilidadesEquipadas || []).includes(inv.find(i => i.nombre === 'Contrato VIP')?.id_item || inv.find(i => i.nombre === 'Contrato VIP')?.idItem);

        items.forEach(item => {
            let mostrar = true;
            if (item.nombre === 'Nitrógeno Criogénico' && !tieneHielo1) mostrar = false;
            if (item.nombre === 'Socio de Honor' && !tieneMulti1) mostrar = false;
            if (item.nombre === 'Tanque de Nitrógeno' && tieneHielo1) mostrar = false;
            if (item.nombre === 'Contrato VIP' && tieneMulti1) mostrar = false;
            if (inv.some(i => (i.id_item || i.idItem) === (item.id_item || item.idItem))) mostrar = false;

            if (mostrar) {
                const idDelObjeto = item.id_item || item.idItem;
                shopList.innerHTML += `
                    <div class="shop-item group">
                        <div class="item-type-tag">MEJORA</div>
                        <h4>${item.nombre}</h4><span class="desc">${item.descripcion}</span>
                        <div class="price">🪙 ${item.precio}</div>
                        <button class="btn-item-action btn-buy" onclick="app.comprarObjeto(${idDelObjeto}, ${item.precio}, '${item.nombre}', '${item.descripcion}')">
                            <div>COMPRAR</div>
                        </button>
                    </div>`;
            }
        });
        if (shopList.innerHTML === "") shopList.innerHTML = '<p class="empty-msg">¡Has comprado todas las mejoras disponibles!</p>';
    } catch (error) {}
}

export async function comprarObjeto(idItemParam, precio, nombreItem, descItem) {
    if (!AppState.currentUser.inventario) AppState.currentUser.inventario = [];
    if (AppState.currentUser.creditos < precio) return alert("Créditos insuficientes 😔");

    try {
        const response = await apiFetch(`/usuarios/buy/${idItemParam}`, { method: 'POST' });
        let data = {}; try { data = await response.json(); } catch(e){}

        if (response.ok) {
            AppState.currentUser.creditos -= precio;
            AppState.currentUser.inventario.push({ id_item: idItemParam, nombre: nombreItem, descripcion: descItem });
            alert("¡Compra exitosa! Has adquirido: " + nombreItem);
            updateUserUI(); loadTienda();
        } else alert(data.mensaje || "El servidor rechazó la compra.");
    } catch (error) { alert("Fallo de conexión con la tienda."); }
}