package com.jorge.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
/**
 * Objeto de Transferencia de Datos (DTO) utilizado para la recepción
 * y validación de los datos necesarios al registrar un nuevo usuario en el sistema.
 */
public class UserAddDTO {

    @JsonProperty("nombre_usuario")
    private String nombreUsuario;

    @JsonProperty("apellido_usuario")
    private String apellidoUsuario;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El formato del correo no es válido")
    @JsonProperty("correo_usuario")
    private String emailUsuario;

    @JsonProperty("contrasenha_usuario")
    @Column(name = "contrasenha_usuario")
    private String contrasenhaUsuario;

    // Getters y setters

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario =emailUsuario; }

    public String getApellidoUsuario() { return apellidoUsuario; }
    public void setApellidoUsuario(String apellidoUsuario) { this.apellidoUsuario = apellidoUsuario; }

    public String getContrasenhaUsuario() { return contrasenhaUsuario; }
    public void setContrasenhaUsuario(String contrasenhaUsuario) { this.contrasenhaUsuario = contrasenhaUsuario; }

}
