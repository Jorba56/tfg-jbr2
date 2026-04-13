package com.jorge.usuarios.controller;

import com.jorge.usuarios.entity.Item;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UserIdDTo;
import com.jorge.usuarios.dto.UsersAllDTO;

import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.repository.ItemRepository;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import com.jorge.usuarios.utils.UsuarioExcelExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Controlador REST encargado de gestionar las peticiones HTTP relacionadas con los Usuarios.
 * Define los endpoints para el CRUD de usuarios y la gestión de sus roles asignados.
 */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para el CRUD de usuarios y la gestión de sus roles asignados.")
public class UserController {

    private final UserServiceImpl userServiceImpl;
    private final ItemRepository itemRepository;

    public UserController(UserServiceImpl userServiceImpl, ItemRepository itemRepository) {
        this.userServiceImpl = userServiceImpl;
        this.itemRepository = itemRepository;
    }

    /**
     * Obtiene la lista completa de usuarios activos en el sistema.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Listar todos los usuarios", description = "Obtiene una lista con la información pública de todos los usuarios activos en el sistema.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public List<UsersAllDTO> getAllUsers() {
        return userServiceImpl.listarUsuarios();
    }

    @Operation(summary = "Obtener propio perfil", description = "Devuelve los datos del usuario logueado basándose en su Cookie/Token.")
    @GetMapping("/perfil")
    public ResponseEntity<?> getPropioPerfil(Authentication authentication) {
        try {
            // El nombre en el token es el email
            String emailLogueado = authentication.getName();

            // Buscamos todos los datos del piloto en BBDD
            UsersAllDTO pilotoActual = userServiceImpl.buscarPorEmail(emailLogueado);

            // Devolvemos el piloto (Spring Boot lo convierte a JSON automáticamente)
            return ResponseEntity.ok(pilotoActual);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Sesión inválida o caducada");
        }
    }

    /**
     * Busca y devuelve los datos de un usuario específico mediante su ID.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Buscar usuario por ID", description = "Devuelve los detalles completos de un usuario específico. Solo accesible para administradores.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}")
    public UsersAllDTO getUserId(@PathVariable Long id) {
        return userServiceImpl.buscarPorId(id);
    }

    @Operation(summary = "Actualizar propio perfil", description = "El usuario actualiza sus datos con su Token, sin enviar ID.")
    @PutMapping("/perfil") // Usamos "mi-perfil" para evitar conflictos con la ruta "/{id}"
    public ResponseEntity<?> updatePropioPerfil(@RequestBody User usuario, Authentication authentication) {
        try {
            // 1. La aduana saca tu email del Token automáticamente
            String emailLogueado = authentication.getName();

            UsersAllDTO pilotoActual = userServiceImpl.buscarPorEmail(emailLogueado);

            // 🚨 ATENCIÓN MECÁNICO: Revisa tu DTO. Puede que el getter sea getIdUsuario() o getId_user()
            Long idReal = pilotoActual.getIdUser();

            // 3. Reutilizamos tu motor de actualización (que ahora sí pedirá todos los datos)
            String resultado = userServiceImpl.actualizarUsuario(idReal, usuario, authentication);

            return ResponseEntity.ok(resultado);

        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fallo interno en boxes al actualizar: " + e.getMessage());
        }
    }

    /**
     * Busca y devuelve los datos de un usuario específico mediante su correo.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Buscar usuario por correo", description = "Devuelve los detalles completos de un usuario específico. Solo accesible para administradores.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/correo/{email}")
    public UsersAllDTO getUserId(@PathVariable String email) {
        return userServiceImpl.buscarPorEmail(email);
    }

    /**
     * Endpoint para actualizar los datos de un usuario en el sistema.
     * Protegido por autenticación JWT. La lógica de negocio determina los permisos exactos basándose en el token.
     *
     * @param id             Identificador del usuario a modificar, obtenido de la ruta (URL).
     * @param usuario        Objeto JSON recibido en el cuerpo de la petición con los nuevos datos.
     * @param authentication Información de sesión inyectada automáticamente por Spring Security.
     * @return Cadena de texto confirmando la edición exitosa.
     */
    @Operation(summary = "Actualizar usuario", description = "Modifica los datos de un usuario. Bloquea la edición de contraseñas para admins y roles para usuarios normales.")
    @PutMapping("/{id}")
    public String updateUser(@PathVariable Long id, @RequestBody User usuario, Authentication authentication) throws BadRequestException {
        return userServiceImpl.actualizarUsuario(id, usuario, authentication);
    }

    /**
     * Exporta el registro completo de usuarios del sistema a un archivo Excel (.xlsx).
     *
     * @param response Objeto {@link HttpServletResponse} para escribir el archivo de salida.
     * @throws IOException Si ocurre un error durante la generación del documento.
     */
    @Operation(summary = "Exportar lista de todos los usuarios a Excel", description = "Descarga un archivo .xlsx con el registro completo de usuarios del sistema.")
    @PreAuthorize("hasAuthority('ADMIN')") // Tiene sentido que esto solo lo haga el admin general
    @GetMapping("/exportar/excel")
    public void exportarUsuariosAExcel(HttpServletResponse response) throws IOException {

        response.setContentType("application/octet-stream");
        DateFormat formateador = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
        String fechaActual = formateador.format(new Date());

        String cabeceraClave = "Content-Disposition";
        String cabeceraValor = "attachment; filename=usuarios_sistema_" + fechaActual + ".xlsx";
        response.setHeader(cabeceraClave, cabeceraValor);

        List<UsersAllDTO> usuarios = userServiceImpl.obtenerTodosLosUsuarios("nombreUsuario", "asc");

        UsuarioExcelExporter exportador = new UsuarioExcelExporter(usuarios);
        exportador.exportar(response);
    }
        /**
         * Recupera una lista paginada de todos los usuarios del sistema, con opciones de ordenación.
         *
         * @param page Número de la página (comienza en 0).
         * @param size Cantidad de usuarios por página.
         * @param sortBy Campo de la entidad para ordenar los resultados.
         * @param sortDir Dirección de ordenación ("asc" o "desc").
         * @return {@link ResponseEntity} con una página de {@link UsersAllDTO}.
         */
    @Operation(summary = "Listar usuarios paginados", description = "Devuelve los usuarios en páginas.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/paginados")
    public ResponseEntity<Page<UsersAllDTO>> getUsuariosPaginados(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "10", required = false) int size,
            @RequestParam(value = "sortBy", defaultValue = "nombreUsuario", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc", required = false) String sortDir) {

        return ResponseEntity.ok(userServiceImpl.obtenerTodosLosUsuariosPaginados(page, size, sortBy, sortDir));
    }

    /**
     * Realiza un borrado lógico del usuario especificado.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Desactivar usuario", description = "Realiza un borrado lógico del usuario especificado cambiando su estado activo a false.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        return userServiceImpl.desactivarUsuario(id);
    }

    /**
     * Obtiene la lista de roles que tiene asignados un usuario en concreto.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Ver roles de un usuario", description = "Obtiene la lista de los roles de seguridad que tiene asignados un usuario en concreto.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/roles")
    public List<Rol> rolesUser(@PathVariable Long id) {
        return userServiceImpl.rolesUser(id);
    }

    /**
     * Asigna un nuevo rol a un usuario existente.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Añadir rol a un usuario", description = "Asigna un nuevo rol a la lista de roles del usuario.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{id}/roles")
    public String userAddRol(@PathVariable Long id, @RequestBody RolPostUser idRol) throws DuplicateException {
        return userServiceImpl.addRolUser(id, idRol);
    }

    /**
     * Revoca (elimina) un rol específico de un usuario.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Revocar rol a un usuario", description = "Elimina la asociación de un rol específico con un usuario.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{idUsuario}/roles/{idRol}") //Quitarle un rol a un usuario
    public String deleteRolUser(@PathVariable Long idRol, @PathVariable Long idUsuario) {
        return userServiceImpl.deleteRolUser(idUsuario, idRol);
    }

    @Operation(summary = "Comprar objeto", description = "El usuario autenticado compra un objeto y se le restan los créditos.")
    @PostMapping("/buy/{itemId}")
    public ResponseEntity<?> comprarObjeto(@PathVariable Long itemId, Authentication authentication) {
        try {
            // Spring Security ya sabe quién eres gracias al Token, sacamos el email directamente
            String emailUsuario = authentication.getName();

            // Delegamos la lógica al servicio
            return ResponseEntity.ok(userServiceImpl.comprarItem(emailUsuario, itemId));

        } catch (BadRequestException e) {
            // Si no tiene saldo o ya tiene el objeto, devolvemos un 400 con el motivo
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al procesar la compra");
        }
    }

    @Operation(summary = "Listar tienda", description = "Devuelve todos los items disponibles para comprar.")
    @GetMapping("/tienda")
    public List<Item> getTienda() {
        return userServiceImpl.listarTienda();
    }

    @Operation(summary = "Añadir créditos", description = "Un administrador añade créditos a un usuario.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}/creditos")
    public ResponseEntity<?> addCreditos(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            return ResponseEntity.ok(userServiceImpl.sumarCreditosAdmin(id, cantidad));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public static class RecompensaPayload {
        public Integer creditosExtra;
        // Getter y Setter necesarios para que Spring lea el JSON
        public Integer getCreditosExtra() { return creditosExtra; }
        public void setCreditosExtra(Integer creditosExtra) { this.creditosExtra = creditosExtra; }
    }

    /**
     * Endpoint para que el juego (script.js) envíe los créditos ganados al acabar una contrarreloj o modo IA.
     */
    @Operation(summary = "Guardar recompensa de partida", description = "Suma créditos al terminar la partida.")
    @PostMapping("/actualizar-creditos")
    public ResponseEntity<String> actualizarCreditosGanados(@RequestBody java.util.Map<String, Integer> payload, Authentication authentication) {
        System.out.println("\n--- 🚥 RECEPCIÓN DE TELEMETRÍA ---");
        try {
            String emailUsuario = authentication.getName();

            // 1. Comprobamos si el paquete llegó roto o vacío
            if (payload == null || !payload.containsKey("creditosExtra")) {
                System.out.println("❌ ERROR: El JSON no contiene la palabra 'creditosExtra'");
                return ResponseEntity.badRequest().body("Formato JSON incorrecto");
            }

            // 2. Extraemos el número con total seguridad
            Integer creditosExtra = payload.get("creditosExtra");
            System.out.println("Piloto: " + emailUsuario + " | Créditos a sumar: " + creditosExtra);

            // 3. Filtro antitrámite
            if (creditosExtra == null || creditosExtra <= 0) {
                System.out.println("❌ ERROR: Créditos nulos o a cero.");
                return ResponseEntity.badRequest().body("No se han ganado créditos válidos");
            }

            // 4. Guardamos en base de datos
            userServiceImpl.sumarCreditosPartida(emailUsuario, creditosExtra);

            System.out.println("🏁 ÉXITO: +" + creditosExtra + " créditos guardados en BBDD.\n");
            return ResponseEntity.ok("Telemetría guardada correctamente");

        } catch (Exception e) {
            System.err.println("💥 ERROR INTERNO AL GUARDAR: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Fallo catastrófico en el motor de guardado");
        }
    }
}
