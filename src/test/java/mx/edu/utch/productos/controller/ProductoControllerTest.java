package mx.edu.utch.productos.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import mx.edu.utch.productos.dto.ProductoRequest;
import mx.edu.utch.productos.dto.ProductoResponse;
import mx.edu.utch.productos.exception.DatoDuplicadoException;
import mx.edu.utch.productos.exception.RecursoNoEncontradoException;
import mx.edu.utch.productos.service.ProductoService;

@WebMvcTest(ProductoController.class)
@DisplayName("Pruebas de la capa web y del manejador global de errores (MockMvc + Mockito)")
class ProductoControllerTest {

    private static final String URL = "/api/v1/productos";
    private static final String ID = "665f1c2ab9d3a14e8c0f0001";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ProductoService servicio;

    private static ProductoRequest valido() {
        return new ProductoRequest("ELE-0001", "Multímetro digital", "Mide voltaje", "Electrónica",
                new BigDecimal("499.90"), 25, true);
    }

    private static ProductoResponse respuesta() {
        Instant t = Instant.parse("2026-11-22T18:00:00Z");
        return new ProductoResponse(ID, "ELE-0001", "Multímetro digital", "Mide voltaje", "Electrónica",
                new BigDecimal("499.90"), 25, true, t, t);
    }

    private String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    @Test
    @DisplayName("POST válido -> 201 con encabezado Location")
    void crear201() throws Exception {
        when(servicio.crear(any())).thenReturn(respuesta());

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(valido())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(URL + "/" + ID)))
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.precio").value(499.90))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-11-22T18:00:00Z"));
    }

    @Test
    @DisplayName("POST con campos inválidos -> 400 con detalle por campo")
    void crear400Validacion() throws Exception {
        ProductoRequest invalido = new ProductoRequest("abc", "", null, "", new BigDecimal("-5"), -1, null);

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.ruta").value(URL))
                .andExpect(jsonPath("$.detalles.codigo").exists())
                .andExpect(jsonPath("$.detalles.nombre").exists())
                .andExpect(jsonPath("$.detalles.categoria").value("La categoría es obligatoria"))
                .andExpect(jsonPath("$.detalles.precio").value("El precio debe ser mayor a 0"))
                .andExpect(jsonPath("$.detalles.stock").value("El stock no puede ser negativo"));
        verify(servicio, never()).crear(any());
    }

    @Test
    @DisplayName("POST con JSON mal formado -> 400")
    void crear400JsonMalFormado() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\": \"ELE-0001\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos"));
    }

    @Test
    @DisplayName("POST con código duplicado -> 409")
    void crear409() throws Exception {
        when(servicio.crear(any())).thenThrow(new DatoDuplicadoException("código", "ELE-0001"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(valido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un registro con código 'ELE-0001'"));
    }

    @Test
    @DisplayName("Violación del índice único en MongoDB -> 409")
    void crear409IndiceUnico() throws Exception {
        when(servicio.crear(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(valido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El registro viola una restricción de unicidad"));
    }

    @Test
    @DisplayName("GET lista -> 200 con arreglo")
    void listar200() throws Exception {
        when(servicio.listar(null)).thenReturn(List.of(respuesta()));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("ELE-0001"));
    }

    @Test
    @DisplayName("GET por id inexistente -> 404")
    void obtener404() throws Exception {
        when(servicio.obtenerPorId("nada")).thenThrow(new RecursoNoEncontradoException("Producto", "nada"));

        mvc.perform(get(URL + "/nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Producto con id 'nada' no encontrado"))
                .andExpect(jsonPath("$.detalles").doesNotExist());
    }

    @Test
    @DisplayName("PUT válido -> 200")
    void actualizar200() throws Exception {
        when(servicio.actualizar(eq(ID), any())).thenReturn(respuesta());

        mvc.perform(put(URL + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(json(valido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Multímetro digital"));
    }

    @Test
    @DisplayName("DELETE existente -> 204; inexistente -> 404")
    void eliminar() throws Exception {
        mvc.perform(delete(URL + "/" + ID)).andExpect(status().isNoContent());

        doThrow(new RecursoNoEncontradoException("Producto", "nada")).when(servicio).eliminar("nada");
        mvc.perform(delete(URL + "/nada")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Método no soportado -> 405")
    void metodoNoPermitido() throws Exception {
        mvc.perform(patch(URL + "/" + ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensaje").value("El método PATCH no está permitido en esta ruta"));
    }

    @Test
    @DisplayName("Ruta inexistente -> 404 en formato JSON")
    void rutaInexistente() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La ruta solicitada no existe"));
    }

    @Test
    @DisplayName("Error inesperado -> 500 sin exponer detalles internos")
    void errorInterno() throws Exception {
        when(servicio.listar(null)).thenThrow(new IllegalStateException("conexión perdida"));

        mvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno; intente más tarde"));
    }
}
