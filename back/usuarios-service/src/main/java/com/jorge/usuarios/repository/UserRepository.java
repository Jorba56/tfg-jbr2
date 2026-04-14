package com.jorge.usuarios.repository;

import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findUsersByActivoIs(boolean activo);
    User findUserByEmailUsuario(String correo) throws NotFoundException;
    List<User> findByActivoTrue(Sort sort);
    Page<User> findByActivoTrue(Pageable pageable);
    List<User> findTop10ByOrderByCreditosDesc();
}






