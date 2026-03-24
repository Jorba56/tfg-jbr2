package com.jorge.usuarios.repository;

import com.jorge.usuarios.entity.User;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findUsersByActivoIs(boolean activo);
    User findUserByEmailUsuario(String correo);
}




