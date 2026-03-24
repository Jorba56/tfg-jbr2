package com.jorge.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
/**
 * Objeto de Transferencia de Datos (DTO) utilizado para capturar
 * y validar las credenciales del usuario durante el inicio de sesión.
 */
public class LoginDTO {

    @JsonProperty("correo_usuario")
    @NotBlank(message = "El correo no puede estar vacío")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,8}$",message = "El formato del correo no es válido")
    private String emailUsuario;

    @JsonProperty("contrasenha_usuario")
    @NotBlank(message = "La contraseña no puede estar vacía")
    private String contrasenhaUsuario;

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }
    public String getContrasenhaUsuario() { return contrasenhaUsuario; }
    public void setContrasenhaUsuario(String contrasenhaUsuario) { this.contrasenhaUsuario = contrasenhaUsuario; }
}
