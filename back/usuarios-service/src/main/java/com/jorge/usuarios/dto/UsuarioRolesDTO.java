package com.jorge.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
/**
 * Objeto de Transferencia de Datos (DTO) diseñado para proporcionar
 * una vista plana que relaciona directamente el ID de un usuario con los IDs de sus roles.
 */
public class UsuarioRolesDTO {
    @NotNull
    @JsonProperty("id_rol")
    private List<Long> idRoles;

    @NotNull
    @JsonProperty("id_usuario")
    private Long idUser;

    public List<Long> getIdRoles() { return idRoles; }
    public void setIdRoles(List<Long> idRoles ) { this.idRoles=idRoles; }

    public Long getIdUser() { return idUser; }
    public void setIdUser (Long idUser ) { this.idUser=idUser; }
}
