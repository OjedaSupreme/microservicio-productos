package mx.edu.utch.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.edu.utch.productos.dto.ProductoRequest;
import mx.edu.utch.productos.dto.ProductoResponse;
import mx.edu.utch.productos.exception.DatoDuplicadoException;
import mx.edu.utch.productos.exception.RecursoNoEncontradoException;
import mx.edu.utch.productos.model.Producto;
import mx.edu.utch.productos.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de ProductoService (Mockito)")
class ProductoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-22T18:00:00Z");
    private static final String ID = "665f1c2ab9d3a14e8c0f0001";

    @Mock
    private ProductoRepository repositorio;

    private ProductoService servicio;

    @BeforeEach
    void configurar() {
        servicio = new ProductoService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static ProductoRequest solicitud(String codigo, String descripcion, Boolean activo) {
        return new ProductoRequest(codigo, "  Multímetro digital ", descripcion, " Electrónica ",
                new BigDecimal("499.90"), 25, activo);
    }

    private static Producto existente() {
        Producto p = new Producto();
        p.setId(ID);
        p.setCodigo("ELE-0001");
        p.setNombre("Multímetro digital");
        p.setCategoria("Electrónica");
        p.setPrecio(new BigDecimal("499.90"));
        p.setStock(25);
        p.setActivo(true);
        p.setFechaCreacion(Instant.parse("2026-11-01T10:00:00Z"));
        p.setFechaActualizacion(Instant.parse("2026-11-01T10:00:00Z"));
        return p;
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("guarda el producto normalizado y devuelve la respuesta")
        void creaProducto() {
            when(repositorio.existsByCodigo("ELE-0001")).thenReturn(false);
            when(repositorio.save(any(Producto.class))).thenAnswer(inv -> {
                Producto p = inv.getArgument(0);
                p.setId(ID);
                return p;
            });

            ProductoResponse r = servicio.crear(solicitud("ELE-0001", "  Mide voltaje ", null));

            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(repositorio).save(captor.capture());
            Producto guardado = captor.getValue();
            assertThat(guardado.getNombre()).isEqualTo("Multímetro digital");
            assertThat(guardado.getDescripcion()).isEqualTo("Mide voltaje");
            assertThat(guardado.getCategoria()).isEqualTo("Electrónica");
            assertThat(guardado.isActivo()).isTrue();
            assertThat(guardado.getFechaCreacion()).isEqualTo(AHORA);
            assertThat(guardado.getFechaActualizacion()).isEqualTo(AHORA);
            assertThat(r.id()).isEqualTo(ID);
            assertThat(r.codigo()).isEqualTo("ELE-0001");
            assertThat(r.precio()).isEqualByComparingTo("499.90");
        }

        @Test
        @DisplayName("permite descripción nula y producto inactivo")
        void creaSinDescripcionEInactivo() {
            when(repositorio.existsByCodigo("ELE-0002")).thenReturn(false);
            when(repositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductoResponse r = servicio.crear(solicitud("ELE-0002", null, false));

            assertThat(r.descripcion()).isNull();
            assertThat(r.activo()).isFalse();
        }

        @Test
        @DisplayName("lanza DatoDuplicadoException si el código ya existe")
        void rechazaCodigoDuplicado() {
            when(repositorio.existsByCodigo("ELE-0001")).thenReturn(true);
            ProductoRequest req = solicitud("ELE-0001", null, true);

            assertThatThrownBy(() -> servicio.crear(req))
                    .isInstanceOf(DatoDuplicadoException.class)
                    .hasMessageContaining("ELE-0001");
            verify(repositorio, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @ParameterizedTest(name = "categoría = [{0}] devuelve todos")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void sinFiltroDevuelveTodos(String categoria) {
            when(repositorio.findAll()).thenReturn(List.of(existente()));

            List<ProductoResponse> r = servicio.listar(categoria);

            assertThat(r).hasSize(1).first().extracting(ProductoResponse::codigo).isEqualTo("ELE-0001");
            verify(repositorio, never()).findByCategoriaIgnoreCase(anyString());
        }

        @Test
        @DisplayName("con categoría filtra ignorando mayúsculas y espacios")
        void conFiltroPorCategoria() {
            when(repositorio.findByCategoriaIgnoreCase("electrónica")).thenReturn(List.of(existente()));

            List<ProductoResponse> r = servicio.listar("  electrónica ");

            assertThat(r).hasSize(1);
            verify(repositorio, never()).findAll();
        }
    }

    @Nested
    @DisplayName("obtenerPorId")
    class Obtener {

        @Test
        @DisplayName("devuelve el producto cuando existe")
        void obtieneExistente() {
            when(repositorio.findById(ID)).thenReturn(Optional.of(existente()));

            assertThat(servicio.obtenerPorId(ID).nombre()).isEqualTo("Multímetro digital");
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException cuando no existe")
        void noExiste() {
            when(repositorio.findById("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.obtenerPorId("x"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Producto con id 'x' no encontrado");
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("aplica los cambios y conserva la fecha de creación")
        void actualizaExistente() {
            Producto p = existente();
            when(repositorio.findById(ID)).thenReturn(Optional.of(p));
            when(repositorio.existsByCodigoAndIdNot("ELE-0009", ID)).thenReturn(false);
            when(repositorio.save(p)).thenReturn(p);

            ProductoRequest req = new ProductoRequest("ELE-0009", "Osciloscopio", "Dos canales", "Medición",
                    new BigDecimal("8999.00"), 3, true);
            ProductoResponse r = servicio.actualizar(ID, req);

            assertThat(r.codigo()).isEqualTo("ELE-0009");
            assertThat(r.nombre()).isEqualTo("Osciloscopio");
            assertThat(r.stock()).isEqualTo(3);
            assertThat(r.fechaCreacion()).isEqualTo(Instant.parse("2026-11-01T10:00:00Z"));
            assertThat(r.fechaActualizacion()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si el id no existe")
        void noExiste() {
            when(repositorio.findById(ID)).thenReturn(Optional.empty());
            ProductoRequest req = solicitud("ELE-0001", null, true);

            assertThatThrownBy(() -> servicio.actualizar(ID, req))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(repositorio, never()).save(any());
        }

        @Test
        @DisplayName("lanza DatoDuplicadoException si el nuevo código pertenece a otro producto")
        void codigoDeOtroProducto() {
            when(repositorio.findById(ID)).thenReturn(Optional.of(existente()));
            when(repositorio.existsByCodigoAndIdNot("ELE-0002", ID)).thenReturn(true);
            ProductoRequest req = solicitud("ELE-0002", null, true);

            assertThatThrownBy(() -> servicio.actualizar(ID, req))
                    .isInstanceOf(DatoDuplicadoException.class);
            verify(repositorio, never()).save(any());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina cuando el producto existe")
        void eliminaExistente() {
            when(repositorio.existsById(ID)).thenReturn(true);

            servicio.eliminar(ID);

            verify(repositorio).deleteById(ID);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si no existe")
        void noExiste() {
            when(repositorio.existsById(ID)).thenReturn(false);

            assertThatThrownBy(() -> servicio.eliminar(ID))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(repositorio, never()).deleteById(anyString());
        }
    }
}
