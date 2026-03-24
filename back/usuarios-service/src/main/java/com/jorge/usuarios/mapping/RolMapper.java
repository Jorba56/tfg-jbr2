package com.jorge.usuarios.mapping;


import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RolMapper {
    RolDTO mappingADTO(Rol rol);

    RolPutDTO mappingPut(Rol rol);

    Rol mappingPutReverse(RolPutDTO rolNuevo);
    RolPostUser mappingPost(Rol rol);
}
