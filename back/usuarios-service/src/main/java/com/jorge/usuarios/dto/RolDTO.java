package com.jorge.usuarios.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Objeto de Transferencia de Datos (DTO) utilizado para la creación
 * y recepción de la información básica de un nuevo rol.
 */
public class RolDTO {

    @NotNull
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name= name; }

}