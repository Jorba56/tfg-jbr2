const token = localStorage.getItem('jwt_token');
if (!token) window.top.location.href = 'login.html';

const API_URL = 'https://gateway-production-a1f6.up.railway.app';

let usuarioSeleccionadoParaCreditos = null;

window.anadirCreditos = function (idUsuario) {
    usuarioSeleccionadoParaCreditos = idUsuario;
    document.getElementById('modal-nombre-usuario').innerText = "ID #" + idUsuario;
    document.getElementById('modal-input-cantidad').value = '';
    document.getElementById('modal-creditos').classList.remove('hidden');
    setTimeout(() => document.getElementById('modal-input-cantidad').focus(), 100);
};

window.cerrarModalCreditos = function () {
    usuarioSeleccionadoParaCreditos = null;
    document.getElementById('modal-creditos').classList.add('hidden');
};
//dinero
const btnCreditos = document.getElementById('btn-confirmar-creditos');
if (btnCreditos) {
    btnCreditos.addEventListener('click', async () => {
        if (!usuarioSeleccionadoParaCreditos) return;
        const cantidad = parseInt(document.getElementById('modal-input-cantidad').value);
        if (isNaN(cantidad) || cantidad <= 0) return alert("Por favor, introduce un número válido mayor que 0.");

        try {
            btnCreditos.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Cargando...';
            btnCreditos.disabled = true;

            const response = await fetch(`${API_URL}/usuarios/${usuarioSeleccionadoParaCreditos}/creditos?cantidad=${cantidad}`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok) {
                const data = await response.json();
                alert(`¡Éxito! ${data.mensaje}`);
                cerrarModalCreditos();
            } else {
                alert('Error al añadir créditos.');
            }
        } catch (error) {
            alert("Fallo de red al intentar contactar con el servidor.");
        } finally {
            btnCreditos.innerHTML = '<i class="fas fa-check mr-2"></i> Ingresar';
            btnCreditos.disabled = false;
        }
    });
}

let pagUsuarios = 0;

window.cambiarPestana = function(pestana) {
    const secUsu = document.getElementById('seccion-usuarios');
    const secRol = document.getElementById('seccion-roles');
    const tabUsu = document.getElementById('tab-usuarios');
    const tabRol = document.getElementById('tab-roles');

    if (pestana === 'usuarios') {
        secUsu.classList.remove('hidden');
        secRol.classList.add('hidden');
        tabUsu.className = "px-4 md:px-6 py-2 rounded-md font-bold text-sm bg-blue-100 text-blue-700 transition w-1/2 sm:w-auto";
        tabRol.className = "px-4 md:px-6 py-2 rounded-md font-bold text-sm theme-text hover:bg-gray-500/10 transition w-1/2 sm:w-auto";
        cargarUsuarios();
    } else {
        secUsu.classList.add('hidden');
        secRol.classList.remove('hidden');
        tabRol.className = "px-4 md:px-6 py-2 rounded-md font-bold text-sm bg-purple-100 text-purple-700 transition w-1/2 sm:w-auto";
        tabUsu.className = "px-4 md:px-6 py-2 rounded-md font-bold text-sm theme-text hover:bg-gray-500/10 transition w-1/2 sm:w-auto";
        cargarRoles();
    }
}

window.cambiarPaginaUsu = function(dir) {
    pagUsuarios += dir;
    cargarUsuarios();
}

async function cargarUsuarios() {
    const tabla = document.getElementById('tablaUsuarios');
    tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-blue-500 animate-pulse font-bold">Cargando usuarios y roles...</td></tr>`;

    try {
        const [resUsu, resRoles, resMapa] = await Promise.all([
            fetch(`${API_URL}/usuarios/paginados?page=${pagUsuarios}&size=10&sortBy=idUser&sortDir=asc`, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/roles`, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/usuarios_roles`, { headers: { 'Authorization': `Bearer ${token}` } })
        ]);

        if (resUsu.ok && resRoles.ok && resMapa.ok) {
            const dataUsu = await resUsu.json();
            const todosLosRoles = await resRoles.json();
            const mapaUsuariosRoles = await resMapa.json();

            const usuarios = dataUsu.content || [];
            const diccRoles = {};
            todosLosRoles.forEach(r => diccRoles[r.idRol || r.id_rol || r.id] = r.name || r.nombre || 'Desconocido');

            document.getElementById('infoPagUsuarios').innerText = `Página ${dataUsu.number + 1} de ${dataUsu.totalPages || 1}`;
            document.getElementById('btnAntUsu').disabled = dataUsu.number === 0;
            document.getElementById('btnSigUsu').disabled = dataUsu.number >= (dataUsu.totalPages - 1);

            tabla.innerHTML = '';
            if (usuarios.length === 0) return tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 theme-text font-bold">No hay usuarios registrados.</td></tr>`;

            usuarios.forEach(u => {
                const idUser = u.id_usuario || u.id_user || u.idUser || u.id;
                const nombreCompleto = `${u.nombre_usuario || u.nombreUsuario || ''} ${u.apellido_usuario || u.apellidoUsuario || ''}`.trim();
                const correo = u.correo_usuario || u.emailUsuario || 'Sin correo';
                const relacion = mapaUsuariosRoles.find(m => (m.id_usuario || m.idUser) === idUser);
                let rolesHtml = '<span class="text-gray-400 text-xs italic">Sin roles</span>';

                if (relacion && relacion.id_rol && relacion.id_rol.length > 0) {
                    rolesHtml = relacion.id_rol.map(idRol => {
                        const nombreRol = diccRoles[idRol] || `ID:${idRol}`;
                        return `<span class="bg-blue-100 text-blue-800 text-xs font-bold px-2 py-1 rounded border border-blue-200 mr-1 shadow-sm uppercase">${nombreRol}</span>`;
                    }).join('');
                }

                // APLICAMOS LAS CLASES DINÁMICAS theme-row, theme-strong y theme-text
                tabla.innerHTML += `
                    <tr class="theme-row">
                    <td class="px-5 py-4 text-sm theme-text">#${idUser}</td>
                        <td class="px-5 py-4 text-sm font-bold theme-strong">${nombreCompleto || 'Usuario N/A'}</td>
                        <td class="px-5 py-4 text-sm theme-text">${correo}</td>
                        <td class="px-5 py-4 text-center">${rolesHtml}</td>
                        <td class="px-5 py-4 text-center">
                            <button onclick="anadirCreditos(${idUser})" class="text-yellow-500 hover:text-yellow-600 font-bold mr-3" title="Añadir Créditos"><i class="fas fa-coins"></i></button>
                            <button onclick="editarUsuario(${idUser})" class="text-blue-500 hover:text-blue-600 font-bold mr-3" title="Editar Usuario"><i class="fas fa-edit"></i></button>
                            <button onclick="abrirGestionRoles(${idUser})" class="text-purple-500 hover:text-purple-600 font-bold mr-3" title="Gestionar Roles"><i class="fas fa-user-tag"></i></button>
                            <button onclick="borrarUsuario(${idUser})" class="text-red-500 hover:text-red-600 font-bold" title="Eliminar Usuario"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        }
    } catch (error) { tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-red-500 font-bold">Fallo de red al conectar.</td></tr>`; }
}

window.exportarExcelUsuarios = async function() {
    try {
        const response = await fetch(`${API_URL}/usuarios/exportar/excel`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.status === 401) return alert("Sesión expirada.");
        if (!response.ok) throw new Error();

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reporte_usuarios_${new Date().toISOString().split('T')[0]}.xlsx`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    } catch (error) { alert("No se pudo descargar el archivo."); }
}

window.buscarUsuarioPorId = function() {
    const id = document.getElementById('inputBusquedaId').value.trim();
    if (!id) { pagUsuarios = 0; return cargarUsuarios(); }
    realizarBusqueda(`${API_URL}/usuarios/${id}`, true);
};

window.buscarUsuarioPorCorreo = function() {
    const correo = document.getElementById('inputBusquedaCorreo').value.trim();
    if (!correo) { pagUsuarios = 0; return cargarUsuarios(); }

    // 1. Codificamos el correo para que el @ y otros caracteres no rompan la petición HTTP
    const correoCodificado = encodeURIComponent(correo);
    realizarBusqueda(`${API_URL}/usuarios/correo/${correoCodificado}`, true);
};

async function realizarBusqueda(urlFetch, esUnico) {
    const tabla = document.getElementById('tablaUsuarios');
    tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-blue-500 animate-pulse font-bold">Buscando...</td></tr>`;

    try {
        const [resBusqueda, resRoles, resMapa] = await Promise.all([
            fetch(urlFetch, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/roles`, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/usuarios_roles`, { headers: { 'Authorization': `Bearer ${token}` } })
        ]);

        if (resBusqueda.ok && resRoles.ok && resMapa.ok) {
            const dataBusqueda = await resBusqueda.json();
            const todosRoles = await resRoles.json();
            const mapaUsuariosRoles = await resMapa.json();

            // 2. PARSEO INTELIGENTE: Detectamos si Java mandó una lista, un objeto paginado, o un usuario suelto.
            let usuarios = [];
            if (Array.isArray(dataBusqueda)) {
                usuarios = dataBusqueda;
            } else if (dataBusqueda && dataBusqueda.content) {
                usuarios = dataBusqueda.content;
            } else if (dataBusqueda && Object.keys(dataBusqueda).length > 0) {
                usuarios = [dataBusqueda];
            }

            const diccRoles = {};
            todosRoles.forEach(r => diccRoles[r.id_rol || r.idRol || r.id] = r.name || r.nombre);

            document.getElementById('infoPagUsuarios').innerText = `Resultados de Búsqueda`;
            document.getElementById('btnAntUsu').disabled = true;
            document.getElementById('btnSigUsu').disabled = true;

            tabla.innerHTML = '';
            if (usuarios.length === 0) return tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 theme-text font-bold">No se encontraron resultados.</td></tr>`;

            usuarios.forEach(u => {
                const idUser = u.id_usuario || u.idUser || u.id;
                const nombreCompleto = `${u.nombre_usuario || u.nombreUsuario || ''} ${u.apellido_usuario || u.apellidoUsuario || ''}`.trim();
                const correo = u.correo_usuario || u.emailUsuario || u.correo || 'Sin correo';
                const relacion = mapaUsuariosRoles.find(m => (m.id_usuario || m.idUser) === idUser);
                let rolesHtml = '<span class="text-gray-400 text-xs italic">Sin roles</span>';

                if (relacion && relacion.id_rol && relacion.id_rol.length > 0) {
                    rolesHtml = relacion.id_rol.map(idRolMapeado => {
                        const nombreRol = diccRoles[idRolMapeado] || `ID:${idRolMapeado}`;
                        return `<span class="bg-blue-100 text-blue-800 text-xs font-bold px-2 py-1 rounded border border-blue-200 mr-1 shadow-sm uppercase">${nombreRol}</span>`;
                    }).join('');
                }

                tabla.innerHTML += `
                    <tr class="theme-row">
                    <td class="px-5 py-4 text-sm theme-text">#${idUser}</td>
                        <td class="px-5 py-4 text-sm font-bold theme-strong">${nombreCompleto || 'Usuario N/A'}</td>
                        <td class="px-5 py-4 text-sm theme-text">${correo}</td>
                        <td class="px-5 py-4 text-center">${rolesHtml}</td>
                        <td class="px-5 py-4 text-center">
                            <button onclick="anadirCreditos(${idUser})" class="text-yellow-500 hover:text-yellow-600 font-bold mr-3" title="Añadir Créditos"><i class="fas fa-coins"></i></button>
                            <button onclick="editarUsuario(${idUser})" class="text-blue-500 hover:text-blue-600 font-bold mr-3" title="Editar Usuario"><i class="fas fa-edit"></i></button>
                            <button onclick="abrirGestionRoles(${idUser})" class="text-purple-500 hover:text-purple-600 font-bold mr-3" title="Gestionar Roles"><i class="fas fa-user-tag"></i></button>
                            <button onclick="borrarUsuario(${idUser})" class="text-red-500 hover:text-red-600 font-bold" title="Eliminar Usuario"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        } else { tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 theme-text font-bold">Usuario no encontrado.</td></tr>`; }
    } catch (error) { tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-red-500 font-bold">Fallo en la búsqueda.</td></tr>`; }
}

window.editarUsuario = async function(id) {
    try {
        const response = await fetch(`${API_URL}/usuarios/${id}`, { headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) {
            const usuario = await response.json();
            document.getElementById('editUserId').value = id;
            document.getElementById('editNombre').value = usuario.nombre_usuario || usuario.nombreUsuario || '';
            document.getElementById('editApellidos').value = usuario.apellido_usuario || usuario.apellidoUsuario || '';
            document.getElementById('editCorreo').value = usuario.correo_usuario || usuario.emailUsuario || '';
            document.getElementById('editActivo').checked = usuario.activo !== false;
            document.getElementById('modalEditarUsuario').classList.remove('hidden');
        }
    } catch (error) { alert("Fallo de red al intentar conectar."); }
}

window.cerrarModalEditar = function() { document.getElementById('modalEditarUsuario').classList.add('hidden'); }

window.guardarEdicionUsuario = async function() {
    const id = document.getElementById('editUserId').value;
    const payload = {
        nombre_usuario: document.getElementById('editNombre').value,
        apellido_usuario: document.getElementById('editApellidos').value,
        correo_usuario: document.getElementById('editCorreo').value,
        activo: document.getElementById('editActivo').checked
    };
    try {
        const response = await fetch(`${API_URL}/usuarios/${id}`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (response.ok) {
            alert("¡Usuario modificado con éxito!");
            cerrarModalEditar();
            cargarUsuarios();
        } else { alert("Error al modificar usuario."); }
    } catch (error) { alert("Fallo de conexión al guardar."); }
}

window.borrarUsuario = async function(id) {
    if (!confirm(`¿Dar de baja al usuario con ID: ${id}?\n\n(Borrado lógico)`)) return;
    try {
        const response = await fetch(`${API_URL}/usuarios/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) { alert("Usuario dado de baja."); cargarUsuarios(); }
    } catch (error) { alert("Fallo de red al intentar dar de baja."); }
};

let idUsuarioRolActual = null;

window.abrirGestionRoles = async function(idUsuario) {
    idUsuarioRolActual = idUsuario;
    document.getElementById('modalGestionRoles').classList.remove('hidden');
    const contenedorRoles = document.getElementById('listaRolesUsuario');
    contenedorRoles.innerHTML = '<p class="theme-text">Cargando...</p>';

    try {
        const [resTodosRoles, resMapa] = await Promise.all([
            fetch(`${API_URL}/roles`, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/usuarios_roles`, { headers: { 'Authorization': `Bearer ${token}` } })
        ]);

        if (resTodosRoles.ok && resMapa.ok) {
            const todosRoles = await resTodosRoles.json();
            const mapaCompleto = await resMapa.json();
            const relacion = mapaCompleto.find(m => (m.id_usuario || m.idUser) === idUsuario);
            const rolesDelUsuario = relacion && relacion.id_rol ? relacion.id_rol : [];

            contenedorRoles.innerHTML = '';
            todosRoles.forEach(rol => {
                const idRol = rol.idRol || rol.id_rol || rol.id;
                const nombreRol = rol.name || rol.nombre;
                const loTiene = rolesDelUsuario.includes(idRol);

                if (loTiene) {
                    contenedorRoles.innerHTML += `
                        <div class="flex justify-between items-center p-3 bg-green-50 border border-green-200 mb-2 rounded shadow-sm">
                            <span class="font-bold text-green-800 uppercase">${nombreRol}</span>
                            <button onclick="quitarRolAUsuario(${idUsuario}, ${idRol})" class="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-xs font-bold transition">Quitar</button>
                        </div>
                    `;
                } else {
                    contenedorRoles.innerHTML += `
                        <div class="flex justify-between items-center p-3 theme-card border theme-border mb-2 rounded shadow-sm">
                            <span class="font-bold theme-text uppercase">${nombreRol}</span>
                            <button onclick="anadirRolAUsuario(${idUsuario}, ${idRol})" class="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-xs font-bold transition">Añadir</button>
                        </div>
                    `;
                }
            });
        }
    } catch (error) {}
}

window.cerrarGestionRoles = function() {
    document.getElementById('modalGestionRoles').classList.add('hidden');
    idUsuarioRolActual = null;
    cargarUsuarios();
}

window.anadirRolAUsuario = async function(idUsuario, idRol) {
    try {
        const response = await fetch(`${API_URL}/usuarios/${idUsuario}/roles`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({ id_rol: idRol })
        });
        if (response.ok) abrirGestionRoles(idUsuario);
    } catch (error) {}
}

window.quitarRolAUsuario = async function(idUsuario, idRol) {
    try {
        const response = await fetch(`${API_URL}/usuarios/${idUsuario}/roles/${idRol}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) abrirGestionRoles(idUsuario);
    } catch (error) {}
}

async function cargarRoles() {
    const tabla = document.getElementById('tablaRoles');
    tabla.innerHTML = `<tr><td colspan="3" class="text-center py-8 text-purple-500 animate-pulse font-bold">Cargando roles...</td></tr>`;

    try {
        const response = await fetch(`${API_URL}/roles`, { headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) {
            const roles = await response.json();
            tabla.innerHTML = '';
            if (roles.length === 0) return tabla.innerHTML = `<tr><td colspan="3" class="text-center py-8 theme-text font-bold">No hay roles creados.</td></tr>`;

            roles.forEach(r => {
                const id = r.id_rol || r.idRol || r.id;
                tabla.innerHTML += `
                    <tr class="theme-row">
                        <td class="px-5 py-4 text-sm theme-text">#${id}</td>
                        <td class="px-5 py-4 text-sm font-bold text-purple-500 uppercase">${r.name || r.nombre}</td>
                        <td class="px-5 py-4 text-center">
                            <button onclick="editarRol(${id})" class="text-blue-500 hover:text-blue-600 font-bold mr-3"><i class="fas fa-edit"></i></button>
                            <button onclick="borrarRol(${id})" class="text-red-500 hover:text-red-600 font-bold"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        }
    } catch (error) {}
}

window.cerrarModalRol = function() { document.getElementById('modalGestionarRol').classList.add('hidden'); };

window.crearRol = function() {
    document.getElementById('rolId').value = '';
    document.getElementById('rolNombre').value = '';
    document.getElementById('contenedorRolActivo').classList.add('hidden');
    document.getElementById('cabeceraModalRol').classList.replace('bg-blue-600', 'bg-purple-600');
    document.getElementById('tituloModalRol').innerHTML = '<i class="fas fa-plus-circle mr-3"></i> Nuevo Rol';
    document.getElementById('modalGestionarRol').classList.remove('hidden');
};

window.editarRol = async function(id) {
    try {
        const response = await fetch(`${API_URL}/roles/${id}`, { headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) {
            const rol = await response.json();
            document.getElementById('rolId').value = rol.id_rol || rol.idRol || rol.id;
            document.getElementById('rolNombre').value = rol.name || rol.nombre || '';
            document.getElementById('contenedorRolActivo').classList.remove('hidden');
            document.getElementById('rolActivo').checked = rol.activo !== false;
            document.getElementById('cabeceraModalRol').classList.replace('bg-purple-600', 'bg-blue-600');
            document.getElementById('tituloModalRol').innerHTML = '<i class="fas fa-edit mr-3"></i> Editar Rol';
            document.getElementById('modalGestionarRol').classList.remove('hidden');
        }
    } catch (error) {}
};

window.guardarRol = async function() {
    const id = document.getElementById('rolId').value;
    const nombre = document.getElementById('rolNombre').value.trim().toUpperCase();
    const activo = document.getElementById('rolActivo').checked;

    if (!nombre) return alert("El nombre del rol no puede estar vacío.");

    const esEdicion = id !== '';
    const payload = esEdicion ? { name: nombre, activo: activo } : { name: nombre };

    try {
        const response = await fetch(esEdicion ? `${API_URL}/roles/${id}` : `${API_URL}/roles`, {
            method: esEdicion ? 'PUT' : 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert(esEdicion ? "Rol actualizado correctamente." : "Rol creado con éxito.");
            cerrarModalRol();
            cargarRoles();
        } else {
            const errorJson = await response.json();
            alert(`Error: ${errorJson.message || errorJson.error}`);
        }
    } catch (error) {}
};

window.borrarRol = async function(id) {
    if (!confirm(`¿Deseas desactivar este rol (ID: ${id})?`)) return;
    try {
        const response = await fetch(`${API_URL}/roles/${id}`, { method: 'DELETE', headers: {'Authorization': `Bearer ${token}`} });
        if (response.ok) { alert("Rol desactivado."); cargarRoles(); }
    } catch (error) {}
}

cargarUsuarios();