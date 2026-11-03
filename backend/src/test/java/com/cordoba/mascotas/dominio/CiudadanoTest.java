package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CiudadanoTest {

    private Refugio refugioValido;
    private Ciudadano ciudadanoValido;

    @BeforeEach
    void setUp() {
        refugioValido = Refugio.builder()
            .idRefugio(101)
            .razonSocial("Refugio Esperanza")
            .habilitacionMunicipal("HAB-MUNIC-2024-001")
            .build();

        ciudadanoValido = Ciudadano.builder()
            .idCiudadano(1)
            .cuil("20-12345678-9")
            .clave("SecretoSeguro123*")
            .nombre("Carlos")
            .apellido("Pérez")
            .correo("carlos@refugioesperanza.org")
            .telefono("3514445566")
            .habilitado(true)
            .refugio(refugioValido)
            .build();
    }

    @Test
    @DisplayName("Autenticación exitosa cuando contraseña, usuario y refugio son válidos")
    void testAutenticacionExitosa() {
        assertDoesNotThrow(() -> ciudadanoValido.autenticar("SecretoSeguro123*"));
    }

    @Test
    @DisplayName("Autenticación rechazada por contraseña incorrecta")
    void testAutenticacionFallaClaveIncorrecta() {
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar("PasswordErronea")
        );
        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    @Test
    @DisplayName("Autenticación rechazada cuando la contraseña ingresada es nula")
    void testAutenticacionFallaClaveIngresadaNula() {
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar(null)
        );
        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    @Test
    @DisplayName("Autenticación rechazada cuando la contraseña almacenada es nula")
    void testAutenticacionFallaClaveAlmacenadaNula() {
        ciudadanoValido.setClave(null);
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar("SecretoSeguro123*")
        );
        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    @Test
    @DisplayName("Autenticación rechazada si el usuario no está habilitado municipalmente")
    void testAutenticacionFallaUsuarioDeshabilitado() {
        ciudadanoValido.setHabilitado(false);
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar("SecretoSeguro123*")
        );
        assertEquals("El usuario no se encuentra habilitado por el Programa Municipal", ex.getMessage());
    }

    @Test
    @DisplayName("Autenticación rechazada si el indicador de habilitación es nulo")
    void testAutenticacionFallaUsuarioHabilitadoNulo() {
        ciudadanoValido.setHabilitado(null);
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar("SecretoSeguro123*")
        );
        assertEquals("El usuario no se encuentra habilitado por el Programa Municipal", ex.getMessage());
    }

    @Test
    @DisplayName("Autenticación rechazada si el usuario no pertenece a ningún refugio")
    void testAutenticacionFallaUsuarioSinRefugio() {
        ciudadanoValido.setRefugio(null);
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar("SecretoSeguro123*")
        );
        assertEquals("El usuario no pertenece a un refugio habilitado", ex.getMessage());
    }

    @Test
    @DisplayName("Autenticación rechazada si el refugio carece de habilitación municipal vigente")
    void testAutenticacionFallaRefugioSinHabilitacionMunicipal() {
        refugioValido.setHabilitacionMunicipal("   ");
        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> ciudadanoValido.autenticar("SecretoSeguro123*")
        );
        assertEquals("El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente", ex.getMessage());
    }

    @Test
    @DisplayName("Serialización Jackson de Ciudadano excluye estrictamente el campo 'clave' vía @JsonIgnore")
    void testClaveOcultaEnSerializacionJackson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(ciudadanoValido);
        assertFalse(json.contains("clave"), "El JSON serializado no debe contener la propiedad 'clave'");
        assertFalse(json.contains("SecretoSeguro123*"), "El JSON serializado no debe contener el valor de la contraseña");
    }
}
