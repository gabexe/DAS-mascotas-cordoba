package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefugioTest {

    @Test
    @DisplayName("tieneHabilitacionVigente retorna true cuando la matrícula es válida")
    void testTieneHabilitacionVigenteTrue() {
        Refugio refugio = Refugio.builder()
            .habilitacionMunicipal("HAB-MUNIC-CBA-2024-0412")
            .build();
        assertTrue(refugio.tieneHabilitacionVigente());
    }

    @Test
    @DisplayName("tieneHabilitacionVigente retorna false cuando la matrícula es nula")
    void testTieneHabilitacionVigenteFalseCuandoNula() {
        Refugio refugio = Refugio.builder()
            .habilitacionMunicipal(null)
            .build();
        assertFalse(refugio.tieneHabilitacionVigente());
    }

    @Test
    @DisplayName("tieneHabilitacionVigente retorna false cuando la matrícula es una cadena vacía")
    void testTieneHabilitacionVigenteFalseCuandoVacia() {
        Refugio refugio = Refugio.builder()
            .habilitacionMunicipal("")
            .build();
        assertFalse(refugio.tieneHabilitacionVigente());
    }

    @Test
    @DisplayName("tieneHabilitacionVigente retorna false cuando la matrícula contiene solo espacios en blanco")
    void testTieneHabilitacionVigenteFalseCuandoEspaciosEnBlanco() {
        Refugio refugio = Refugio.builder()
            .habilitacionMunicipal("   ")
            .build();
        assertFalse(refugio.tieneHabilitacionVigente());
    }

    @Test
    @DisplayName("validarHabilitacionVigente no arroja excepción si la habilitación es válida")
    void testValidarHabilitacionVigenteExitosa() {
        Refugio refugio = Refugio.builder()
            .habilitacionMunicipal("HAB-2024-OK")
            .build();
        assertDoesNotThrow(refugio::validarHabilitacionVigente);
    }

    @Test
    @DisplayName("validarHabilitacionVigente lanza AutenticacionInvalidaException si no posee matrícula")
    void testValidarHabilitacionVigenteLanzaExcepcion() {
        Refugio refugio = Refugio.builder()
            .habilitacionMunicipal(null)
            .build();
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            refugio::validarHabilitacionVigente
        );
        assertEquals("El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente", ex.getMessage());
    }
}
