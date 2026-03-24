package com.jorge.usuarios.repository;

import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuariosRolesRepository extends JpaRepository<Rol, User> {

}
