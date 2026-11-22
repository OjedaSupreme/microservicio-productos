package mx.edu.utch.productos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mx.edu.utch.productos.dto.ProductoRequest;
import mx.edu.utch.productos.model.Producto;
import mx.edu.utch.productos.repository.ProductoRepository;

/**
 * Pruebas de integración: contexto completo de Spring + MongoDB embebido (Flapdoodle).
 * Verifican el flujo HTTP -> controlador -> servicio -> repositorio -> MongoDB y la serialización JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Pruebas de integración del CRUD con MongoDB embebido")
class ProductoIntegrationTest {

    private static final String URL = "/api/v1/productos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ProductoRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll();
    }

    private static ProductoRequest solicitud(String codigo, String nombre, String categoria) {
        return new ProductoRequest(codigo, nombre, "Producto de prueba", categoria, new BigDecimal("150.50"), 10,
                null);
    }

    private String crear(ProductoRequest req) throws Exception {
        MvcResult r = mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode nodo = mapper.readTree(r.getResponse().getContentAsString());
        return nodo.get("id").asText();
    }

    @Test
    @DisplayName("Flujo CRUD completo: crear, leer, listar, actualizar y eliminar")
    void flujoCrudCompleto() throws Exception {
        // Crear
        MvcResult creado = mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(solicitud("HER-0001", "Taladro inalámbrico", "Herramientas"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.codigo").value("HER-0001"))
                .andExpect(jsonPath("$.precio").value(150.50))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andReturn();
        String id = mapper.readTree(creado.getResponse().getContentAsString()).get("id").asText();

        // Persistido realmente en MongoDB
        Producto enBd = repositorio.findById(id).orElseThrow();
        assertThat(enBd.getNombre()).isEqualTo("Taladro inalámbrico");
        assertThat(enBd.getPrecio()).isEqualByComparingTo("150.50");

        // Leer por id
        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Taladro inalámbrico"));

        // Listar
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        // Actualizar
        ProductoRequest cambio = new ProductoRequest("HER-0001", "Taladro percutor", null, "Herramientas",
                new BigDecimal("1299.00"), 4, false);
        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cambio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Taladro percutor"))
                .andExpect(jsonPath("$.stock").value(4))
                .andExpect(jsonPath("$.activo").value(false));
        assertThat(repositorio.findById(id).orElseThrow().getPrecio()).isEqualByComparingTo("1299.00");

        // Eliminar
        mvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());
        assertThat(repositorio.existsById(id)).isFalse();
        mvc.perform(get(URL + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Filtra por categoría sin distinguir mayúsculas")
    void filtraPorCategoria() throws Exception {
        crear(solicitud("HER-0001", "Taladro", "Herramientas"));
        crear(solicitud("HER-0002", "Llave inglesa", "Herramientas"));
        crear(solicitud("ELE-0001", "Multímetro", "Electrónica"));

        mvc.perform(get(URL).param("categoria", "herramientas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get(URL)).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Código duplicado al crear y al actualizar -> 409")
    void codigoDuplicado() throws Exception {
        crear(solicitud("ELE-0001", "Multímetro", "Electrónica"));
        String otro = crear(solicitud("ELE-0002", "Osciloscopio", "Electrónica"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(solicitud("ELE-0001", "Copia", "Electrónica"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mvc.perform(put(URL + "/" + otro).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(solicitud("ELE-0001", "Osciloscopio", "Electrónica"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("El índice único de MongoDB impide códigos repetidos aun sin pasar por el servicio")
    void indiceUnicoEnMongo() {
        Producto a = new Producto();
        a.setCodigo("IDX-0001");
        repositorio.save(a);
        Producto b = new Producto();
        b.setCodigo("IDX-0001");

        assertThatThrownBy(() -> repositorio.save(b)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("Datos inválidos -> 400 y no se persiste nada")
    void validacion() throws Exception {
        String cuerpo = "{\"codigo\":\"mal\",\"nombre\":\"ab\",\"categoria\":\"X\",\"precio\":0,\"stock\":5}";

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.codigo").value("El código debe tener el formato AAA-0000 (ej. ELE-0001)"))
                .andExpect(jsonPath("$.detalles.nombre").value("El nombre debe tener entre 3 y 80 caracteres"))
                .andExpect(jsonPath("$.detalles.precio").value("El precio debe ser mayor a 0"));
        assertThat(repositorio.count()).isZero();
    }

    @Test
    @DisplayName("Tipo de dato incorrecto en el JSON -> 400")
    void tipoIncorrecto() throws Exception {
        String cuerpo = "{\"codigo\":\"ELE-0001\",\"nombre\":\"Multímetro\",\"categoria\":\"Electrónica\","
                + "\"precio\":\"caro\",\"stock\":5}";

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Actualizar o eliminar un id inexistente -> 404")
    void inexistente() throws Exception {
        mvc.perform(put(URL + "/no-existe").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(solicitud("ELE-0009", "Nada", "Nada"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete(URL + "/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La documentación OpenAPI está disponible")
    void openApiDisponible() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Microservicio de Productos (HU-01)"));
    }
}
