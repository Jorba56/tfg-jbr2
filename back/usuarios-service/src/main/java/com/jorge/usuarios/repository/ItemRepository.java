package com.jorge.usuarios.repository;

import com.jorge.usuarios.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio para la gestión de objetos de la tienda (Items).
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Busca items por su tipo (util para filtrar en la tienda: COSMETICO, POWERUP, etc.)
     */
    List<Item> findByTipo(String tipo);

    /**
     * Opcional: Buscar items que el usuario pueda pagar
     */
    List<Item> findByPrecioLessThanEqual(int precio);
}