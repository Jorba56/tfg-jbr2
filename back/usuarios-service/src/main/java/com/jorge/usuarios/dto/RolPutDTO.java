package com.jorge.usuarios.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Objeto de Transferencia de Datos (DTO) utilizado para la actualización
 * de un rol existente, permitiendo modificar su nombre o su estado (activo/inactivo).
 */
public class RolPutDTO {

    @NotNull
    private String name;
    private boolean activo;

    public String getName() { return name; }
    public void setName(String name) { this.name= name; }

    public boolean getActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo= activo; }

}

