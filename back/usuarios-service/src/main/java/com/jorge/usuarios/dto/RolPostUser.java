package com.jorge.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
/**
 * Objeto de Transferencia de Datos (DTO) de utilidad para capturar
 * el identificador de un rol que se desea asignar a un usuario.
 */
public class RolPostUser {
    @NotNull
    @JsonProperty("id_rol")
    private Long idRol;

    public void setIdRol(Long idRol) { this.idRol=idRol; }
    public Long getIdRol() { return idRol; }
}
