package mx.edu.utch.productos.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import mx.edu.utch.productos.model.Producto;

/**
 * Acceso a datos de productos. Spring Data MongoDB genera la implementación.
 */
public interface ProductoRepository extends MongoRepository<Producto, String> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, String id);

    List<Producto> findByCategoriaIgnoreCase(String categoria);
}
