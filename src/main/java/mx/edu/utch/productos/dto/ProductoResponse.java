package mx.edu.utch.productos.dto;

import java.math.BigDecimal;
import java.time.Instant;

import mx.edu.utch.productos.model.Producto;

/**
 * Representación pública de un producto devuelta por la API.
 */
public record ProductoResponse(
        String id,
        String codigo,
        String nombre,
        String descripcion,
        String categoria,
        BigDecimal precio,
        Integer stock,
        boolean activo,
        Instant fechaCreacion,
        Instant fechaActualizacion) {

    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria(),
                producto.getPrecio(),
                producto.getStock(),
                producto.isActivo(),
                producto.getFechaCreacion(),
                producto.getFechaActualizacion());
    }
}
