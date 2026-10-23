package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.port.out.ReporteMasVendidosPort;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CatalogoRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CompraEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PreventaEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ResenaEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ProductoPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({
        ProductoRepositoryJpa.class,
        ProductoPersistenceMapper.class,
        ReporteMasVendidosJpaAdapter.class
})
class ConsultasCatalogoJpaTest {

    @Autowired
    private CatalogoRepository catalogoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ReporteMasVendidosPort reporteMasVendidosPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    void buscaFiltraYPaginaProductosEnLaBaseDeDatos() {
        UUID vendedorId = UUID.randomUUID();
        Producto alfa = producto(vendedorId, "Mouse Alfa", "Perifericos", 30);
        Producto beta = producto(vendedorId, "Mouse Beta", "Perifericos", 45);
        producto(vendedorId, "Mouse Delta", "Mobiliario", 40);
        producto(vendedorId, "Teclado", "Perifericos", 40).eliminar(false);
        entityManager.flush();
        entityManager.clear();

        var pagina = catalogoRepository.buscar(
                new FiltroProducto("MOUSE", "perifericos", Gama.MEDIA, 20.0, 50.0, false),
                1,
                1,
                LocalDate.of(2026, 10, 1));

        assertEquals(List.of(beta.getId()), pagina.contenido().stream().map(Producto::getId).toList());
        assertEquals(2, pagina.totalElementos());
        assertEquals(2, pagina.totalPaginas());
        assertEquals(1, pagina.pagina());
    }

    @Test
    void soloPreventaBuscaLanzamientosFuturosYExcluyeLosYaDisponibles() {
        UUID vendedorId = UUID.randomUUID();
        Producto futuro = producto(vendedorId, "Consola futura", "Consolas", 500);
        Producto disponible = producto(vendedorId, "Consola lanzada", "Consolas", 400);
        guardarPreventa(futuro.getId(), LocalDate.of(2026, 10, 10));
        guardarPreventa(disponible.getId(), LocalDate.of(2026, 10, 1));
        entityManager.flush();
        entityManager.clear();

        var pagina = catalogoRepository.buscar(
                new FiltroProducto(null, null, null, null, null, true),
                0,
                20,
                LocalDate.of(2026, 10, 1));

        assertEquals(List.of(futuro.getId()), pagina.contenido().stream().map(Producto::getId).toList());
    }

    @Test
    void rankingAgrupaVentasCompletadasSinMultiplicarUnidadesPorResenas() {
        UUID vendedorId = UUID.randomUUID();
        Producto alfa = producto(vendedorId, "Alfa", "Perifericos", 30);
        Producto beta = producto(vendedorId, "Beta", "Perifericos", 45);
        guardarCompra(alfa.getId(), 2, EstadoCompra.COMPLETADA);
        guardarCompra(alfa.getId(), 1, EstadoCompra.COMPLETADA);
        guardarCompra(beta.getId(), 4, EstadoCompra.COMPLETADA);
        guardarCompra(beta.getId(), 50, EstadoCompra.PENDIENTE);
        guardarResena(alfa.getId(), 4);
        guardarResena(alfa.getId(), 5);
        guardarResena(beta.getId(), 5);
        entityManager.flush();
        entityManager.clear();

        List<ProductoVendidoResponse> resultado = reporteMasVendidosPort.obtener(vendedorId, 10);

        assertEquals(List.of(beta.getId(), alfa.getId()),
                resultado.stream().map(ProductoVendidoResponse::productoId).toList());
        assertEquals(4L, resultado.get(0).unidades());
        assertEquals(5.0, resultado.get(0).calificacionPromedio());
        assertEquals(4.5, resultado.get(1).calificacionPromedio());

        List<ProductoVendidoResponse> topUno = reporteMasVendidosPort.obtener(vendedorId, 1);
        assertEquals(List.of(beta.getId()), topUno.stream()
                .map(ProductoVendidoResponse::productoId).toList());
    }

    private Producto producto(UUID vendedorId, String nombre, String categoria, double precio) {
        Producto producto = Producto.publicar(UUID.randomUUID(), vendedorId, nombre, "Descripción",
                precio, 10, Gama.MEDIA, Exclusividad.NORMAL, categoria);
        catalogoGuardar(producto);
        return producto;
    }

    private void catalogoGuardar(Producto producto) {
        productoRepository.guardar(producto);
    }

    private void guardarPreventa(UUID productoId, LocalDate lanzamiento) {
        PreventaEntity entity = new PreventaEntity();
        entity.setId(UUID.randomUUID());
        entity.setProductoId(productoId);
        entity.setFechaLanzamiento(lanzamiento);
        entity.setDescripcion("Lanzamiento programado");
        entityManager.persist(entity);
    }

    private void guardarCompra(UUID productoId, int cantidad, EstadoCompra estado) {
        CompraEntity entity = new CompraEntity();
        entity.setId(UUID.randomUUID());
        entity.setUsuarioId(UUID.randomUUID());
        entity.setProductoId(productoId);
        entity.setCantidad(cantidad);
        entity.setFecha(LocalDateTime.of(2026, 10, 1, 10, 0));
        entity.setPrecioUnitario(100);
        entity.setEstado(estado);
        entity.setResenada(false);
        entity.setPuntosAsignados(false);
        entity.setDescargado(false);
        entity.setCuponAplicado(false);
        entityManager.persist(entity);
    }

    private void guardarResena(UUID productoId, int estrellas) {
        ResenaEntity entity = new ResenaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setIdCompra(UUID.randomUUID().toString());
        entity.setIdProducto(productoId.toString());
        entity.setIdUsuario(UUID.randomUUID().toString());
        entity.setCalificacion(estrellas);
        entity.setComentario("Reseña de prueba");
        entity.setFechaCreacion(LocalDateTime.of(2026, 10, 1, 10, 0));
        entityManager.persist(entity);
    }
}
