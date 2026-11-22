package mx.edu.utch.productos.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mx.edu.utch.productos.dto.ProductoRequest;
import mx.edu.utch.productos.dto.ProductoResponse;
import mx.edu.utch.productos.service.ProductoService;

/**
 * Endpoints REST del recurso /api/v1/productos.
 */
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Operaciones CRUD sobre productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crear producto", description = "201 si se crea, 400 si hay datos inválidos, 409 si el código ya existe")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest solicitud) {
        ProductoResponse creado = servicio.crear(solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Filtro opcional por categoría")
    public List<ProductoResponse> listar(@RequestParam(required = false) String categoria) {
        return servicio.listar(categoria);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar producto por id", description = "404 si no existe")
    public ProductoResponse obtener(@PathVariable String id) {
        return servicio.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "400, 404 o 409 según el caso")
    public ProductoResponse actualizar(@PathVariable String id, @Valid @RequestBody ProductoRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto", description = "204 si se elimina, 404 si no existe")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
