package com.jorge.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
/**
 * Objeto de Transferencia de Datos (DTO) utilizado para mostrar
 * un resumen de la información de un usuario cuando se filtra el listado por roles.
 */
public class UserByRol{

    @NotNull
    @JsonProperty("nombre_usuario")
    private String nombreUsuario;

    @NotNull
    private boolean activo;

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public boolean getActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo= activo; }

}

