package mx.edu.utch.productos.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import mx.edu.utch.productos.dto.ProductoRequest;
import mx.edu.utch.productos.dto.ProductoResponse;
import mx.edu.utch.productos.exception.DatoDuplicadoException;
import mx.edu.utch.productos.exception.RecursoNoEncontradoException;
import mx.edu.utch.productos.model.Producto;
import mx.edu.utch.productos.repository.ProductoRepository;

/**
 * Lógica de negocio del CRUD de productos.
 */
@Service
public class ProductoService {

    static final String RECURSO = "Producto";
    static final String CAMPO_CODIGO = "código";

    private final ProductoRepository repositorio;
    private final Clock reloj;

    public ProductoService(ProductoRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public ProductoResponse crear(ProductoRequest solicitud) {
        String codigo = normalizarCodigo(solicitud.codigo());
        if (repositorio.existsByCodigo(codigo)) {
            throw new DatoDuplicadoException(CAMPO_CODIGO, codigo);
        }
        Producto producto = new Producto();
        Instant ahora = Instant.now(reloj);
        producto.setFechaCreacion(ahora);
        aplicarCambios(producto, solicitud, codigo, ahora);
        return ProductoResponse.desde(repositorio.save(producto));
    }

    public List<ProductoResponse> listar(String categoria) {
        List<Producto> productos = (categoria == null || categoria.isBlank())
                ? repositorio.findAll()
                : repositorio.findByCategoriaIgnoreCase(categoria.trim());
        return productos.stream().map(ProductoResponse::desde).toList();
    }

    public ProductoResponse obtenerPorId(String id) {
        return ProductoResponse.desde(buscar(id));
    }

    public ProductoResponse actualizar(String id, ProductoRequest solicitud) {
        Producto producto = buscar(id);
        String codigo = normalizarCodigo(solicitud.codigo());
        if (repositorio.existsByCodigoAndIdNot(codigo, id)) {
            throw new DatoDuplicadoException(CAMPO_CODIGO, codigo);
        }
        aplicarCambios(producto, solicitud, codigo, Instant.now(reloj));
        return ProductoResponse.desde(repositorio.save(producto));
    }

    public void eliminar(String id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException(RECURSO, id);
        }
        repositorio.deleteById(id);
    }

    private Producto buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(RECURSO, id));
    }

    private static void aplicarCambios(Producto producto, ProductoRequest solicitud, String codigo, Instant ahora) {
        producto.setCodigo(codigo);
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(solicitud.descripcion() == null ? null : solicitud.descripcion().trim());
        producto.setCategoria(solicitud.categoria().trim());
        producto.setPrecio(solicitud.precio());
        producto.setStock(solicitud.stock());
        producto.setActivo(!Boolean.FALSE.equals(solicitud.activo()));
        producto.setFechaActualizacion(ahora);
    }

    private static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
}
