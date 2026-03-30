const token = localStorage.getItem('token');
if (!token) window.top.location.href = 'index.html';

const API_URL = 'http://localhost:8080';

let pagUsuarios = 0;

// --- CONTROL DE PESTAÑAS ---
window.cambiarPestana = function(pestana) {
    const secUsu = document.getElementById('seccion-usuarios');
    const secRol = document.getElementById('seccion-roles');
    const tabUsu = document.getElementById('tab-usuarios');
    const tabRol = document.getElementById('tab-roles');

    if (pestana === 'usuarios') {
        secUsu.classList.remove('hidden');
        secRol.classList.add('hidden');

        tabUsu.classList.add('bg-blue-100', 'text-blue-700');
        tabUsu.classList.remove('text-gray-600', 'hover:bg-gray-100');
        tabRol.classList.remove('bg-purple-100', 'text-purple-700');
        tabRol.classList.add('text-gray-600', 'hover:bg-gray-100');

        cargarUsuarios();
    } else {
        secUsu.classList.add('hidden');
        secRol.classList.remove('hidden');

        tabRol.classList.add('bg-purple-100', 'text-purple-700');
        tabRol.classList.remove('text-gray-600', 'hover:bg-gray-100');
        tabUsu.classList.remove('bg-blue-100', 'text-blue-700');
        tabUsu.classList.add('text-gray-600', 'hover:bg-gray-100');

        cargarRoles();
    }
}

// ==================== LÓGICA DE USUARIOS ====================

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

            if (usuarios.length === 0) {
                tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-gray-500 font-bold">No hay usuarios registrados.</td></tr>`;
                return;
            }

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

                tabla.innerHTML += `
                    <tr class="hover:bg-blue-50 border-b border-gray-100 transition">
                        <td class="px-5 py-3 text-sm font-bold text-gray-800">${nombreCompleto || 'Usuario N/A'}</td>
                        <td class="px-5 py-3 text-sm text-gray-600">${correo}</td>
                        <td class="px-5 py-3 text-center">${rolesHtml}</td>
                        <td class="px-5 py-3 text-center">
                            <button onclick="anadirCreditos(${idUser})" class="text-green-600 hover:text-green-900 font-bold mr-3" title="Añadir Créditos"><i class="fas fa-coins"></i></button>
                            <button onclick="editarUsuario(${idUser})" class="text-blue-600 hover:text-blue-900 font-bold mr-3" title="Editar Usuario"><i class="fas fa-edit"></i></button>
                            <button onclick="abrirGestionRoles(${idUser})" class="text-purple-600 hover:text-purple-900 font-bold mr-3" title="Gestionar Roles"><i class="fas fa-user-tag"></i></button>
                            <button onclick="borrarUsuario(${idUser})" class="text-red-600 hover:text-red-900 font-bold" title="Eliminar Usuario"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        } else {
            tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-red-500 font-bold">Error de permisos o conexión.</td></tr>`;
        }
    } catch (error) {
        tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-red-500 font-bold">Fallo de red al conectar.</td></tr>`;
    }
}

window.exportarExcelUsuarios = async function() {
    try {
        const response = await fetch(`${API_URL}/usuarios/exportar/excel`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 401) {
            alert("Sesión expirada o no autorizada. Por favor, inicia sesión de nuevo.");
            return;
        }

        if (!response.ok) throw new Error("Error al generar el archivo Excel");

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const fecha = new Date().toISOString().split('T')[0];
        a.download = `reporte_usuarios_${fecha}.xlsx`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    } catch (error) {
        alert("No se pudo descargar el archivo.");
    }
}

// ==================== BÚSQUEDA DE USUARIOS ====================

window.buscarUsuarioPorId = function() {
    const id = document.getElementById('inputBusquedaId').value.trim();
    if (!id) { pagUsuarios = 0; return cargarUsuarios(); }
    realizarBusqueda(`${API_URL}/usuarios/${id}`, true);
};

window.buscarUsuarioPorCorreo = function() {
    const correo = document.getElementById('inputBusquedaCorreo').value.trim();
    if (!correo) { pagUsuarios = 0; return cargarUsuarios(); }
    realizarBusqueda(`${API_URL}/usuarios/email/${correo}`, true);
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

            const usuarios = esUnico ? [dataBusqueda] : dataBusqueda;

            const diccRoles = {};
            todosRoles.forEach(r => diccRoles[r.id_rol || r.idRol || r.id] = r.name || r.nombre);

            document.getElementById('infoPagUsuarios').innerText = `Resultados de Búsqueda`;
            document.getElementById('btnAntUsu').disabled = true;
            document.getElementById('btnSigUsu').disabled = true;

            tabla.innerHTML = '';

            if (usuarios.length === 0) {
                tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-gray-500 font-bold">No se encontraron resultados.</td></tr>`;
                return;
            }

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
                    <tr class="hover:bg-blue-50 border-b border-gray-100 transition">
                        <td class="px-5 py-3 text-sm font-bold text-gray-800">${nombreCompleto}</td>
                        <td class="px-5 py-3 text-sm text-gray-600">${correo}</td>
                        <td class="px-5 py-3 text-center">${rolesHtml}</td>
                        <td class="px-5 py-3 text-center">
                            <button onclick="editarUsuario(${idUser})" class="text-blue-600 hover:text-blue-900 font-bold mr-3"><i class="fas fa-edit"></i></button>
                            <button onclick="abrirGestionRoles(${idUser})" class="text-purple-600 hover:text-purple-900 font-bold mr-3"><i class="fas fa-user-tag"></i></button>
                            <button onclick="borrarUsuario(${idUser})" class="text-red-600 hover:text-red-900 font-bold"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        } else {
            tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-gray-500 font-bold">Usuario no encontrado.</td></tr>`;
        }
    } catch (error) {
        tabla.innerHTML = `<tr><td colspan="4" class="text-center py-8 text-red-500 font-bold">Fallo en la ejecución de la búsqueda.</td></tr>`;
    }
}

// ==================== EDITAR Y BORRAR USUARIO ====================

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
        } else { alert("No se pudieron cargar los datos del usuario."); }
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
    const confirmacion = confirm(`¿Dar de baja al usuario con ID: ${id}?\n\n(Borrado lógico)`);
    if (!confirmacion) return;
    try {
        const response = await fetch(`${API_URL}/usuarios/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            alert("Usuario dado de baja.");
            cargarUsuarios();
        } else { alert("Error al intentar borrar al usuario."); }
    } catch (error) { alert("Fallo de red al intentar dar de baja."); }
};


// ==================== GESTIÓN DE ROLES (ASIGNAR/QUITAR A USUARIO) ====================

let idUsuarioRolActual = null;

window.abrirGestionRoles = async function(idUsuario) {
    idUsuarioRolActual = idUsuario;
    document.getElementById('modalGestionRoles').classList.remove('hidden');
    const contenedorRoles = document.getElementById('listaRolesUsuario');
    contenedorRoles.innerHTML = '<p class="text-gray-500">Cargando...</p>';

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
                        <div class="flex justify-between items-center p-2 bg-green-50 border border-green-200 mb-2 rounded">
                            <span class="font-bold text-green-800 uppercase">${nombreRol}</span>
                            <button onclick="quitarRolAUsuario(${idUsuario}, ${idRol})" class="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-xs font-bold transition">Quitar</button>
                        </div>
                    `;
                } else {
                    contenedorRoles.innerHTML += `
                        <div class="flex justify-between items-center p-2 bg-gray-50 border border-gray-200 mb-2 rounded">
                            <span class="font-bold text-gray-600 uppercase">${nombreRol}</span>
                            <button onclick="anadirRolAUsuario(${idUsuario}, ${idRol})" class="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-xs font-bold transition">Añadir</button>
                        </div>
                    `;
                }
            });
        }
    } catch (error) { contenedorRoles.innerHTML = '<p class="text-red-500">Error al cargar la gestión de roles.</p>'; }
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
        if (response.ok) { abrirGestionRoles(idUsuario); } else { alert("Error al asignar el rol."); }
    } catch (error) { alert("Fallo de red."); }
}

window.quitarRolAUsuario = async function(idUsuario, idRol) {
    if (!confirm("¿Seguro que quieres quitarle este rol al usuario?")) return;
    try {
        const response = await fetch(`${API_URL}/usuarios/${idUsuario}/roles/${idRol}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) { abrirGestionRoles(idUsuario); } else { alert("Error al quitar el rol."); }
    } catch (error) { alert("Fallo de red."); }
}

// ==================== LÓGICA DE LA PESTAÑA DE ROLES (CRUD) ====================

async function cargarRoles() {
    const tabla = document.getElementById('tablaRoles');
    tabla.innerHTML = `<tr><td colspan="3" class="text-center py-8 text-purple-500 animate-pulse font-bold">Cargando roles...</td></tr>`;

    try {
        const response = await fetch(`${API_URL}/roles`, { headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) {
            const roles = await response.json();
            tabla.innerHTML = '';
            if (roles.length === 0) { tabla.innerHTML = `<tr><td colspan="3" class="text-center py-8 text-gray-500 font-bold">No hay roles creados.</td></tr>`; return; }

            roles.forEach(r => {
                const id = r.id_rol || r.idRol || r.id;
                tabla.innerHTML += `
                    <tr class="hover:bg-purple-50 border-b border-gray-100 transition">
                        <td class="px-5 py-3 text-sm font-bold text-gray-500">#${id}</td>
                        <td class="px-5 py-3 text-sm font-bold text-purple-700 uppercase">${r.name || r.nombre}</td>
                        <td class="px-5 py-3 text-center">
                            <button onclick="editarRol(${id})" class="text-blue-600 hover:text-blue-900 font-bold mr-3"><i class="fas fa-edit"></i></button>
                            <button onclick="borrarRol(${id})" class="text-red-600 hover:text-red-900 font-bold"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        } else { tabla.innerHTML = `<tr><td colspan="3" class="text-center py-8 text-red-500">Error al cargar roles.</td></tr>`; }
    } catch (error) { tabla.innerHTML = `<tr><td colspan="3" class="text-center py-8 text-red-500 font-bold">Fallo de red al conectar.</td></tr>`; }
}

window.cerrarModalRol = function() {
    document.getElementById('modalGestionarRol').classList.add('hidden');
};

window.crearRol = function() {
    document.getElementById('rolId').value = '';
    document.getElementById('rolNombre').value = '';
    document.getElementById('contenedorRolActivo').classList.add('hidden');
    document.getElementById('cabeceraModalRol').classList.replace('bg-blue-600', 'bg-purple-600');
    document.getElementById('tituloModalRol').innerHTML = '<i class="fas fa-plus-circle mr-3 text-purple-200"></i> Nuevo Rol';
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
            document.getElementById('tituloModalRol').innerHTML = '<i class="fas fa-edit mr-3 text-blue-200"></i> Editar Rol';
            document.getElementById('modalGestionarRol').classList.remove('hidden');
        } else { alert("No se pudo cargar la información del rol."); }
    } catch (error) { alert("Fallo de conexión al intentar cargar el rol."); }
};

// 🔥 AQUÍ ESTÁ EL METODO UNIFICADO DE POST Y PUT PARA GUARDAR ROLES 🔥
window.guardarRol = async function() {
    const id = document.getElementById('rolId').value;
    const nombre = document.getElementById('rolNombre').value.trim().toUpperCase();
    const activo = document.getElementById('rolActivo').checked;

    if (!nombre) {
        alert("El nombre del rol no puede estar vacío.");
        return;
    }

    const esEdicion = id !== ''; // Si hay ID en el input oculto, es un PUT. Si no, POST.
    const endpoint = esEdicion ? `${API_URL}/roles/${id}` : `${API_URL}/roles`;
    const metodoHTTP = esEdicion ? 'PUT' : 'POST';

    // Construimos el Payload (RolPutDTO lleva 'activo', RolDTO normal no)
    const payload = esEdicion ? { name: nombre, activo: activo } : { name: nombre };

    try {
        const response = await fetch(endpoint, {
            method: metodoHTTP,
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert(esEdicion ? "Rol actualizado correctamente." : "Rol creado con éxito.");
            cerrarModalRol();
            cargarRoles();
        } else {
            const errorText = await response.text();
            try {
                const errorJson = JSON.parse(errorText);
                alert(`Error: ${errorJson.message || errorJson.error}`);
            } catch (e) {
                alert(`Error del servidor (${response.status}).`);
            }
        }
    } catch (error) { alert("Fallo de conexión al guardar el rol."); }
};

window.borrarRol = async function(id) {
    const confirmacion = confirm(`¿Estás seguro de que deseas desactivar este rol (ID: ${id})?\n\nSi el rol tiene usuarios asignados actualmente, el sistema no te permitirá borrarlo para evitar inconsistencias.`);
    if (!confirmacion) return;

    try {
        const response = await fetch(`${API_URL}/roles/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            alert("Rol desactivado correctamente.");
            cargarRoles();
        } else {
            const errorText = await response.text();
            try {
                const errorJson = JSON.parse(errorText);
                alert(`Atención: ${errorJson.message || errorJson.error}`);
            } catch (e) { alert(`Error del servidor al desactivar el rol (${response.status}).`); }
        }
    } catch (error) { alert("Fallo de red al intentar desactivar el rol."); }

    window.anadirCreditos = async function(idUsuario) {
        // 1. Preguntamos al administrador la cantidad
        const cantidadStr = prompt("¿Cuántos créditos quieres ingresar en la cuenta de este usuario? (Ej: 100)");

        if (cantidadStr === null) return; // Si le da a cancelar

        const cantidad = parseInt(cantidadStr);
        if (isNaN(cantidad) || cantidad <= 0) {
            alert("Por favor, introduce un número válido mayor que 0.");
            return;
        }

        try {
            // 2. Disparamos la petición PUT a tu nuevo endpoint de Java
            const response = await fetch(`${API_URL}/usuarios/${idUsuario}/creditos?cantidad=${cantidad}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                alert(`¡Éxito! ${data.mensaje}`);
            } else {
                const errorMsg = await response.text();
                alert(`Error: ${errorMsg || 'No se pudieron añadir los créditos. ¿Tienes permisos de ADMIN?'}`);
            }
        } catch (error) {
            console.error("Fallo al añadir créditos:", error);
            alert("Fallo de red al intentar contactar con el servidor.");
        }
    }
};

// Arrancamos en la pestaña por defecto
cargarUsuarios();