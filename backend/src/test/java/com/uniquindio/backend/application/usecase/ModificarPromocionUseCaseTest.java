package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.PromocionRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModificarPromocionUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    private ProductoRepositoryEnMemoria productoRepository;
    private PromocionRepositoryEnMemoria promocionRepository;
    private ModificarPromocionUseCase useCase;
    private UUID vendedorId;
    private Producto producto;
    private Promocion promocion;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        promocionRepository = new PromocionRepositoryEnMemoria();
        useCase = new ModificarPromocionUseCase(productoRepository, promocionRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        promocion = Promocion.programar(UUID.randomUUID(), producto.getId(), 20, DESDE, HASTA);
        promocionRepository.guardar(promocion);
    }

    @Test
    void cambiaElPorcentajeYLasFechas() {
        Promocion resultado = useCase.ejecutar(promocion.getId(), vendedorId, 35,
                DESDE.plusDays(1), HASTA.plusDays(10));

        assertEquals(promocion.getId(), resultado.getId());
        assertEquals(35, resultado.getPorcentaje());

        Promocion guardada = promocionRepository.obtenerPorId(promocion.getId()).orElseThrow();
        assertEquals(35, guardada.getPorcentaje());
        assertEquals(DESDE.plusDays(1), guardada.getDesde());
        assertEquals(HASTA.plusDays(10), guardada.getHasta());
    }

    @Test
    void actualizaLaMismaPromocionSinCrearOtra() {
        useCase.ejecutar(promocion.getId(), vendedorId, 35, DESDE, HASTA);

        assertEquals(1, promocionRepository.obtenerPorProductoId(producto.getId()).size());
    }

    @Test
    void noCambiaElPrecioBaseDelProducto() {
        useCase.ejecutar(promocion.getId(), vendedorId, 35, DESDE, HASTA);

        assertEquals(100, producto.getPrecio());
    }

    @Test
    void puedeConservarSuMismoRangoDeFechas() {
        // Solo cambia el porcentaje: no debe chocar consigo misma.
        assertDoesNotThrow(() -> useCase.ejecutar(promocion.getId(), vendedorId, 50, DESDE, HASTA));
    }

    @Test
    void puedeExtenderSuRangoSobreSusPropiasFechas() {
        assertDoesNotThrow(() -> useCase.ejecutar(promocion.getId(), vendedorId, 20,
                DESDE.minusDays(5), HASTA.plusDays(5)));
    }

    @Test
    void rechazaSolaparseConOtraPromocionDelMismoProducto() {
        Promocion otra = Promocion.programar(UUID.randomUUID(), producto.getId(), 10,
                HASTA.plusDays(1), HASTA.plusDays(10));
        promocionRepository.guardar(otra);

        assertThrows(ConflictoException.class,
                () -> useCase.ejecutar(promocion.getId(), vendedorId, 20, DESDE, HASTA.plusDays(1)));

        // No se guardó nada: la promoción conserva su rango original.
        assertEquals(HASTA, promocionRepository.obtenerPorId(promocion.getId()).orElseThrow().getHasta());
    }

    @Test
    void permiteMoverseAUnRangoConsecutivoSinSolapar() {
        Promocion otra = Promocion.programar(UUID.randomUUID(), producto.getId(), 10,
                HASTA.plusDays(1), HASTA.plusDays(10));
        promocionRepository.guardar(otra);

        assertDoesNotThrow(() -> useCase.ejecutar(promocion.getId(), vendedorId, 20,
                DESDE.minusDays(10), HASTA));
    }

    @Test
    void soloElDuenoModificaLaPromocion() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(promocion.getId(), UUID.randomUUID(), 35, DESDE, HASTA));

        assertEquals(20, promocionRepository.obtenerPorId(promocion.getId()).orElseThrow().getPorcentaje());
    }

    @Test
    void productoEliminadoNoSeModifica() {
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(promocion.getId(), vendedorId, 35, DESDE, HASTA));
    }

    @Test
    void promocionInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, 35, DESDE, HASTA));
    }

    @Test
    void rechazaPorcentajeFueraDeRangoYNoGuarda() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(promocion.getId(), vendedorId, 0, DESDE, HASTA));
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(promocion.getId(), vendedorId, 100, DESDE, HASTA));

        assertEquals(20, promocionRepository.obtenerPorId(promocion.getId()).orElseThrow().getPorcentaje());
    }

    @Test
    void rechazaFechasInvertidas() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(promocion.getId(), vendedorId, 35, HASTA, DESDE));
    }
}
