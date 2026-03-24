package com.jorge.usuarios.mapping;

import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UsersAllDTO mappingADTO(User usuario);

    UserIdDTo userToIdDTO(User usuario);

    User userAddDTO(UserAddDTO usuario);

    UserByRol mappingRoles(User usuario);

    LoginDTO mapeoLogin(User usuario);

    LoginDTO mappeoLogin(User usuario);
}
