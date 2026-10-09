package com.cordoba.mascotas.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoPublicacionTest {

    @Test
    @DisplayName("Activa puede transicionar a Pausada y a Finalizada")
    void testTransicionesDesdeActiva() {
        assertTrue(EstadoPublicacion.Activa.puedeTransicionarA(EstadoPublicacion.Pausada));
        assertTrue(EstadoPublicacion.Activa.puedeTransicionarA(EstadoPublicacion.Finalizada));
        assertTrue(EstadoPublicacion.Activa.puedeTransicionarA(EstadoPublicacion.Activa));
        assertFalse(EstadoPublicacion.Activa.puedeTransicionarA(null));
    }

    @Test
    @DisplayName("Pausada puede transicionar a Activa y a Finalizada")
    void testTransicionesDesdePausada() {
        assertTrue(EstadoPublicacion.Pausada.puedeTransicionarA(EstadoPublicacion.Activa));
        assertTrue(EstadoPublicacion.Pausada.puedeTransicionarA(EstadoPublicacion.Finalizada));
        assertTrue(EstadoPublicacion.Pausada.puedeTransicionarA(EstadoPublicacion.Pausada));
        assertFalse(EstadoPublicacion.Pausada.puedeTransicionarA(null));
    }

    @Test
    @DisplayName("Finalizada es terminal e inmutable: no puede transicionar a ningún otro estado")
    void testFinalizadaEsTerminal() {
        assertFalse(EstadoPublicacion.Finalizada.puedeTransicionarA(EstadoPublicacion.Activa));
        assertFalse(EstadoPublicacion.Finalizada.puedeTransicionarA(EstadoPublicacion.Pausada));
        assertFalse(EstadoPublicacion.Finalizada.puedeTransicionarA(EstadoPublicacion.Finalizada));
        assertFalse(EstadoPublicacion.Finalizada.puedeTransicionarA(null));
    }

    @Test
    @DisplayName("fromString parsea correctamente sin importar mayúsculas/minúsculas")
    void testFromStringCaseInsensitive() {
        org.junit.jupiter.api.Assertions.assertEquals(EstadoPublicacion.Activa, EstadoPublicacion.fromString("activa"));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoPublicacion.Activa, EstadoPublicacion.fromString("ACTIVA"));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoPublicacion.Activa, EstadoPublicacion.fromString("Activa"));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoPublicacion.Pausada, EstadoPublicacion.fromString("pausada"));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoPublicacion.Finalizada, EstadoPublicacion.fromString("FINALIZADA"));
        org.junit.jupiter.api.Assertions.assertNull(EstadoPublicacion.fromString("TODAS"));
        org.junit.jupiter.api.Assertions.assertNull(EstadoPublicacion.fromString("todas"));
        org.junit.jupiter.api.Assertions.assertNull(EstadoPublicacion.fromString(""));
        org.junit.jupiter.api.Assertions.assertNull(EstadoPublicacion.fromString(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> EstadoPublicacion.fromString("inexistente"));
    }
}

