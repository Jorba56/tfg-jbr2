document.getElementById('registroForm').addEventListener('submit', async function(event) {
    event.preventDefault(); // Evita que la página se recargue

    const btnRegistro = document.getElementById('btnRegistro');
    const mensajeError = document.getElementById('mensajeError');

    // Cogemos los valores de los inputs
    const nombre = document.getElementById('nombre').value;
    const apellido = document.getElementById('apellido').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // Cambiamos el texto del botón
    btnRegistro.textContent = 'Registrando...';
    btnRegistro.disabled = true;
    mensajeError.style.color = 'black';
    mensajeError.textContent = '';

    // montamos el json con formato snake_case para que spring boot lo entienda
    const usuarioNuevo = {
        nombre_usuario: nombre,
        apellido_usuario: apellido,
        email_usuario: email,
        contrasenha_usuario: password
    };

    try {
        // Hacemos la petición al API Gateway (revisa si tu ruta exacta es /auth/register)
        const response = await fetch('https://gateway-production-a1f6.up.railway.app/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(usuarioNuevo)
        });

        if (response.ok) {
            mensajeError.style.color = 'green';
            mensajeError.textContent = '¡Registro completado! Redirigiendo...';

            setTimeout(() => {
                window.location.href = 'login.html'; // Pon aquí el nombre de tu página de login
            }, 2000);
        } else {
            // Si el correo ya existe, tu backend lanza DuplicateException (Error 400 o 409)
            const errorData = await response.text();
            mensajeError.style.color = 'red';
            mensajeError.textContent = 'Error: ' + (errorData || 'No se pudo crear la cuenta.');
            btnRegistro.textContent = 'Registrarse';
            btnRegistro.disabled = false;
        }

    } catch (error) {
        console.error('Error de red:', error);
        mensajeError.style.color = 'red';
        mensajeError.textContent = 'Error de conexión con el servidor.';
        btnRegistro.textContent = 'Registrarse';
        btnRegistro.disabled = false;
    }
});