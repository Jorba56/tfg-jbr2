package com.jorge.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
/**
 * Objeto de Transferencia de Datos (DTO) utilizado para listar
 * la información pública y esencial de los usuarios, ocultando datos sensibles como contraseñas.
 */
public class UsersAllDTO {

    @NotNull
    @JsonProperty("id_usuario")
    private Long idUser;

    @JsonProperty("nombre_usuario")
    private String nombreUsuario;

    @JsonProperty("apellido_usuario")
    private String apellidoUsuario;

    @JsonProperty("correo_usuario")
    private String emailUsuario;

    // Getters y setters
    public void setIdUser(Long idUser) { this.idUser = idUser; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario =emailUsuario; }

    public String getApellidoUsuario() { return apellidoUsuario; }
    public void setApellidoUsuario(String apellidoUsuario) { this.apellidoUsuario = apellidoUsuario; }

    }

