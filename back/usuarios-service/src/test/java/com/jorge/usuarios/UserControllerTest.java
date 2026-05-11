package com.jorge.usuarios;

import com.jorge.usuarios.controller.UserController;
import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.entity.Item;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.exceptions.NotFoundException;
import com.jorge.usuarios.repository.ItemRepository;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserServiceImpl userServiceImpl;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private UserController userController;

    @Test
    void getAllUsers() {
        given(userServiceImpl.listarUsuarios()).willReturn(List.of(new UsersAllDTO()));
        List<UsersAllDTO> res = userController.getAllUsers();
        assertEquals(1, res.size());
    }

    @Test
    void getUserId_Long() {
        UsersAllDTO dto = new UsersAllDTO();
        given(userServiceImpl.buscarPorId(1L)).willReturn(dto);
        assertEquals(dto, userController.getUserId(1L));
    }

    @Test
    void getUserId_String() {
        UsersAllDTO dto = new UsersAllDTO();
        given(userServiceImpl.buscarPorEmail("test@test.com")).willReturn(dto);
        assertEquals(dto, userController.getUserId("test@test.com"));
    }

    @Test
    void updateUser() throws BadRequestException {
        Authentication auth = mock(Authentication.class);
        User user = new User();
        given(userServiceImpl.actualizarUsuario(1L, user, auth)).willReturn("OK");
        assertEquals("OK", userController.updateUser(1L, user, auth));
    }

    @Test
    void exportarUsuariosAExcel() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        given(response.getOutputStream()).willReturn(out);

        // Devolvemos lista vacía para que el exportador no falle al mapear datos nulos
        given(userServiceImpl.obtenerTodosLosUsuarios(anyString(), anyString())).willReturn(List.of());

        userController.exportarUsuariosAExcel(response);
        verify(response).setContentType("application/octet-stream");
        verify(response).setHeader(eq("Content-Disposition"), anyString());
    }

    @Test
    void getUsuariosPaginados() {
        Page<UsersAllDTO> page = new PageImpl<>(List.of(new UsersAllDTO()));
        given(userServiceImpl.obtenerTodosLosUsuariosPaginados(0, 10, "nombreUsuario", "asc")).willReturn(page);

        ResponseEntity<Page<UsersAllDTO>> res = userController.getUsuariosPaginados(0, 10, "nombreUsuario", "asc");
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void deleteUser() {
        given(userServiceImpl.desactivarUsuario(1L)).willReturn("OK");
        assertEquals("OK", userController.deleteUser(1L));
    }

    @Test
    void rolesUser() {
        given(userServiceImpl.rolesUser(1L)).willReturn(List.of(new Rol()));
        assertEquals(1, userController.rolesUser(1L).size());
    }

    @Test
    void userAddRol() throws DuplicateException {
        RolPostUser rolPost = new RolPostUser();
        given(userServiceImpl.addRolUser(1L, rolPost)).willReturn("OK");
        assertEquals("OK", userController.userAddRol(1L, rolPost));
    }

    @Test
    void deleteRolUser() {
        given(userServiceImpl.deleteRolUser(1L, 2L)).willReturn("OK");
        assertEquals("OK", userController.deleteRolUser(2L, 1L));
    }

    // --- COMPRAR OBJETO ---
    @Test
    void comprarObjeto_Exito() throws BadRequestException {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        given(userServiceImpl.comprarItem("user@mail.com", 1L)).willReturn(Map.of("ok", "ok"));

        ResponseEntity<?> res = userController.comprarObjeto(1L, auth);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void comprarObjeto_BadRequest() throws BadRequestException {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        doThrow(new BadRequestException("Sin saldo")).when(userServiceImpl).comprarItem("user@mail.com", 1L);

        ResponseEntity<?> res = userController.comprarObjeto(1L, auth);
        assertEquals(400, res.getStatusCode().value());
        assertEquals("Sin saldo", res.getBody());
    }

    @Test
    void comprarObjeto_Exception() throws BadRequestException {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        doThrow(new RuntimeException("Error")).when(userServiceImpl).comprarItem("user@mail.com", 1L);

        ResponseEntity<?> res = userController.comprarObjeto(1L, auth);
        assertEquals(500, res.getStatusCode().value());
    }

    @Test
    void getTienda() {
        given(userServiceImpl.listarTienda()).willReturn(List.of(new Item()));
        assertEquals(1, userController.getTienda().size());
    }

    // --- ADD CREDITOS ADMIN ---
    @Test
    void addCreditos_Exito() throws BadRequestException {
        given(userServiceImpl.sumarCreditosAdmin(1L, 100)).willReturn(Map.of("ok", "ok"));
        ResponseEntity<?> res = userController.addCreditos(1L, 100);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void addCreditos_BadRequest() throws BadRequestException {
        doThrow(new BadRequestException("Error")).when(userServiceImpl).sumarCreditosAdmin(1L, 100);
        ResponseEntity<?> res = userController.addCreditos(1L, 100);
        assertEquals(400, res.getStatusCode().value());
    }

    // --- ACTUALIZAR CREDITOS GANADOS ---
    @Test
    void actualizarCreditosGanados_PayloadNull() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");

        ResponseEntity<String> res = userController.actualizarCreditosGanados(null, auth);
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void actualizarCreditosGanados_SinKey() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");

        ResponseEntity<String> res = userController.actualizarCreditosGanados(new HashMap<>(), auth);
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void actualizarCreditosGanados_CreditosCero() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, Integer> payload = new HashMap<>();
        payload.put("creditosExtra", 0);

        ResponseEntity<String> res = userController.actualizarCreditosGanados(payload, auth);
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void actualizarCreditosGanados_Exito() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, Integer> payload = new HashMap<>();
        payload.put("creditosExtra", 50);

        ResponseEntity<String> res = userController.actualizarCreditosGanados(payload, auth);
        assertEquals(200, res.getStatusCode().value());
        verify(userServiceImpl).sumarCreditosPartida("user@mail.com", 50);
    }

    @Test
    void actualizarCreditosGanados_Exception() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, Integer> payload = new HashMap<>();
        payload.put("creditosExtra", 50);

        doThrow(new RuntimeException("Fallo BD")).when(userServiceImpl).sumarCreditosPartida("user@mail.com", 50);

        ResponseEntity<String> res = userController.actualizarCreditosGanados(payload, auth);
        assertEquals(500, res.getStatusCode().value());
    }

    // --- OBTENER PROPIO PERFIL ---
    @Test
    void getPropioPerfil_Exito() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        given(userServiceImpl.getPropioPerfil("user@mail.com")).willReturn(Map.of("user", "test"));

        ResponseEntity<?> res = userController.getPropioPerfil(auth);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void getPropioPerfil_NotFound() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        doThrow(new NotFoundException("No existe")).when(userServiceImpl).getPropioPerfil("user@mail.com");

        ResponseEntity<?> res = userController.getPropioPerfil(auth);
        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void getPropioPerfil_Exception() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        doThrow(new RuntimeException("Error")).when(userServiceImpl).getPropioPerfil("user@mail.com");

        ResponseEntity<?> res = userController.getPropioPerfil(auth);
        assertEquals(401, res.getStatusCode().value());
    }

    // --- ACTUALIZAR AVATAR ---
    @Test
    void actualizarAvatar_Exito() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        given(userServiceImpl.actualizarAvatar("user@mail.com", "config1")).willReturn(Map.of("ok", "ok"));

        ResponseEntity<?> res = userController.actualizarAvatar(auth, Map.of("config", "config1"));
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void actualizarAvatar_NotFound() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        doThrow(new NotFoundException("No")).when(userServiceImpl).actualizarAvatar("user@mail.com", "config1");

        ResponseEntity<?> res = userController.actualizarAvatar(auth, Map.of("config", "config1"));
        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void actualizarAvatar_Exception() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        doThrow(new RuntimeException("No")).when(userServiceImpl).actualizarAvatar("user@mail.com", "config1");

        ResponseEntity<?> res = userController.actualizarAvatar(auth, Map.of("config", "config1"));
        assertEquals(500, res.getStatusCode().value());
    }

    // --- ACTUALIZAR PROPIO PERFIL ---
    @Test
    void actualizarPropioPerfil_Exito() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, String> payload = Map.of("nombre", "nuevo");
        given(userServiceImpl.actualizarPerfil("user@mail.com", payload)).willReturn(Map.of("ok", "ok"));

        ResponseEntity<?> res = userController.actualizarPropioPerfil(auth, payload);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void actualizarPropioPerfil_NotFound() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, String> payload = Map.of("nombre", "nuevo");
        doThrow(new NotFoundException("No")).when(userServiceImpl).actualizarPerfil("user@mail.com", payload);

        ResponseEntity<?> res = userController.actualizarPropioPerfil(auth, payload);
        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void actualizarPropioPerfil_Exception() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, String> payload = Map.of("nombre", "nuevo");
        doThrow(new RuntimeException("No")).when(userServiceImpl).actualizarPerfil("user@mail.com", payload);

        ResponseEntity<?> res = userController.actualizarPropioPerfil(auth, payload);
        assertEquals(500, res.getStatusCode().value());
    }

    // --- CERRAR SESION ---
    @Test
    void cerrarSesionTotal() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        ResponseEntity<?> res = userController.cerrarSesionTotal(response);
        assertEquals(200, res.getStatusCode().value());
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }

    // --- RANKING ---
    @Test
    void getRankingGlobal() {
        given(userServiceImpl.obtenerRankingGlobal()).willReturn(List.of(Map.of("user", "1")));
        ResponseEntity<List<Map<String, Object>>> res = userController.getRankingGlobal();
        assertEquals(200, res.getStatusCode().value());
    }

    // --- GUARDAR PARTIDA ---
    @Test
    void guardarPartida_Exito() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, Object> payload = Map.of("puntos", 100);

        ResponseEntity<?> res = userController.guardarPartida(auth, payload);
        assertEquals(200, res.getStatusCode().value());
        verify(userServiceImpl).guardarEstadisticasPartida("user@mail.com", payload);
    }

    @Test
    void guardarPartida_Exception() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");
        Map<String, Object> payload = Map.of("puntos", 100);

        doThrow(new RuntimeException("Error BD")).when(userServiceImpl).guardarEstadisticasPartida("user@mail.com", payload);

        ResponseEntity<?> res = userController.guardarPartida(auth, payload);
        assertEquals(500, res.getStatusCode().value());
    }

    // --- ESTADÍSTICAS ---
    @Test
    void getEstadisticasPublicas() {
        given(userServiceImpl.obtenerEstadisticasPublicas(1L)).willReturn(Map.of("user", "test"));
        ResponseEntity<?> res = userController.getEstadisticasPublicas(1L);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void actualizarCreditosGanados_CreditosNull() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");

        Map<String, Integer> payload = new HashMap<>();
        payload.put("creditosExtra", null); // Simula {"creditosExtra": null}

        ResponseEntity<String> res = userController.actualizarCreditosGanados(payload, auth);

        assertEquals(400, res.getStatusCode().value());
        assertEquals("No se han ganado créditos válidos", res.getBody());
    }

    @Test
    void actualizarCreditosGanados_CreditosNegativos() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@mail.com");

        Map<String, Integer> payload = new HashMap<>();
        payload.put("creditosExtra", -10); // Simula un intento de hackeo enviando negativos

        ResponseEntity<String> res = userController.actualizarCreditosGanados(payload, auth);

        assertEquals(400, res.getStatusCode().value());
        assertEquals("No se han ganado créditos válidos", res.getBody());
    }
}