package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicacionAdopcionTest {

    private Refugio refugioPropietario;
    private Mascota mascotaViva;
    private PublicacionAdopcion publicacion;

    @BeforeEach
    void setUp() {
        refugioPropietario = Refugio.builder()
            .idRefugio(101)
            .razonSocial("Refugio Esperanza")
            .build();

        mascotaViva = Mascota.builder()
            .nroRegMunicipal(5001)
            .nombre("Toby")
            .vive(true)
            .refugio(refugioPropietario)
            .build();

        publicacion = PublicacionAdopcion.builder()
            .nroPublicacion(2001)
            .refugio(refugioPropietario)
            .mascota(mascotaViva)
            .fechaPublicacion(LocalDate.now())
            .estadoPublicacion(EstadoPublicacion.Activa)
            .build();
    }

    // =========================================================================
    // 1. Transiciones de Estados FSM
    // =========================================================================

    @Test
    @DisplayName("Transición de Activa a Pausada es exitosa")
    void testTransicionActivaAPausada() {
        publicacion.transicionarA(EstadoPublicacion.Pausada);
        assertEquals(EstadoPublicacion.Pausada, publicacion.getEstadoPublicacion());
    }

    @Test
    @DisplayName("Transición de Pausada a Activa es exitosa cuando la mascota continúa viva")
    void testTransicionPausadaAActiva() {
        publicacion.setEstadoPublicacion(EstadoPublicacion.Pausada);
        publicacion.transicionarA(EstadoPublicacion.Activa);
        assertEquals(EstadoPublicacion.Activa, publicacion.getEstadoPublicacion());
    }

    @Test
    @DisplayName("Transición de Activa a Finalizada es exitosa")
    void testTransicionActivaAFinalizada() {
        publicacion.transicionarA(EstadoPublicacion.Finalizada);
        assertEquals(EstadoPublicacion.Finalizada, publicacion.getEstadoPublicacion());
    }

    @Test
    @DisplayName("Transición de Pausada a Finalizada es exitosa")
    void testTransicionPausadaAFinalizada() {
        publicacion.setEstadoPublicacion(EstadoPublicacion.Pausada);
        publicacion.transicionarA(EstadoPublicacion.Finalizada);
        assertEquals(EstadoPublicacion.Finalizada, publicacion.getEstadoPublicacion());
    }

    @Test
    @DisplayName("Transición desde estado Finalizada es rechazada categóricamente (terminal e inmutable)")
    void testTransicionFinalizadaAAcualquieraRechazada() {
        publicacion.setEstadoPublicacion(EstadoPublicacion.Finalizada);

        TransicionEstadoInvalidaException exActiva = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacion.transicionarA(EstadoPublicacion.Activa)
        );
        assertEquals("Una publicación en estado Finalizada no puede cambiar de estado.", exActiva.getMessage());

        TransicionEstadoInvalidaException exPausada = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacion.transicionarA(EstadoPublicacion.Pausada)
        );
        assertEquals("Una publicación en estado Finalizada no puede cambiar de estado.", exPausada.getMessage());

        TransicionEstadoInvalidaException exFinalizada = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacion.transicionarA(EstadoPublicacion.Finalizada)
        );
        assertEquals("Una publicación en estado Finalizada no puede cambiar de estado.", exFinalizada.getMessage());
    }

    @Test
    @DisplayName("Transición con estado nulo lanza IllegalArgumentException")
    void testTransicionEstadoNuloRechazada() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> publicacion.transicionarA(null)
        );
        assertEquals("El nuevo estado de la publicación no puede ser nulo.", ex.getMessage());
    }

    @Test
    @DisplayName("Transición rechazada si el enum no permite la transición")
    void testTransicionInvalidaSegunEnum() {
        publicacion.setEstadoPublicacion(EstadoPublicacion.Finalizada);
        TransicionEstadoInvalidaException ex = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacion.transicionarA(EstadoPublicacion.Pausada)
        );
        assertEquals("Una publicación en estado Finalizada no puede cambiar de estado.", ex.getMessage());
    }

    @Test
    @DisplayName("Transición a Activa rechazada si la mascota se encuentra fallecida o no disponible")
    void testTransicionAActivaRechazadaSiMascotaFallecida() {
        mascotaViva.setVive(false);
        publicacion.setEstadoPublicacion(EstadoPublicacion.Pausada);

        TransicionEstadoInvalidaException ex = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacion.transicionarA(EstadoPublicacion.Activa)
        );
        assertEquals("No es posible activar la publicación de una mascota no disponible o registrada como fallecida.", ex.getMessage());
    }

    @Test
    @DisplayName("Transición Activa a Activa es idempotente y permitida")
    void testTransicionActivaAActivaPermitida() {
        publicacion.transicionarA(EstadoPublicacion.Activa);
        assertEquals(EstadoPublicacion.Activa, publicacion.getEstadoPublicacion());
    }

    // =========================================================================
    // 2. Validación de Titularidad / Pertenencia a Refugio
    // =========================================================================

    @Test
    @DisplayName("validarPertenenciaRefugio no arroja excepción cuando el ID del solicitante coincide con el refugio titular")
    void testValidarPertenenciaRefugioCoincidente() {
        assertDoesNotThrow(() -> publicacion.validarPertenenciaRefugio(101));
    }

    @Test
    @DisplayName("validarPertenenciaRefugio arroja AutenticacionInvalidaException cuando el ID del solicitante no coincide")
    void testValidarPertenenciaRefugioDistintoRechazado() {
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> publicacion.validarPertenenciaRefugio(102)
        );
        assertEquals("No posee permisos para administrar publicaciones pertenecientes a otro refugio.", ex.getMessage());
    }

    @Test
    @DisplayName("validarPertenenciaRefugio permite operaciones si el solicitanteRefugioId es nulo")
    void testValidarPertenenciaRefugioConSolicitanteNulo() {
        assertDoesNotThrow(() -> publicacion.validarPertenenciaRefugio((Integer) null));
    }
}
