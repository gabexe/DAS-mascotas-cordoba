package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CiudadanoRepository ciudadanoRepository;

    @InjectMocks
    private AuthService authService;

    private Refugio refugioMock;
    private Ciudadano ciudadanoMock;

    @BeforeEach
    void setUp() {
        refugioMock = Refugio.builder()
            .idRefugio(101)
            .razonSocial("Asociación Protectora Huellitas Cordobesas")
            .correo("contacto@huellitascba.org.ar")
            .telefono("0351-4890123")
            .domicilio("Av. Colón 4500, Córdoba Capital")
            .habilitacionMunicipal("HAB-MUNIC-CBA-2024-0412")
            .build();

        ciudadanoMock = Ciudadano.builder()
            .idCiudadano(1)
            .cuil("20-12345678-9")
            .clave("1234")
            .nombre("Martín")
            .apellido("Gómez")
            .correo("martin.gomez@huellitascba.org.ar")
            .telefono("351-4567890")
            .habilitado(true)
            .refugio(refugioMock)
            .build();
    }

    @Test
    @DisplayName("Login exitoso con CUIL exacto y contraseña correcta")
    void testLoginExitoso() {
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadanoMock));

        UsuarioRefugioDTO response = authService.login(new LoginRequest("20-12345678-9", "1234"));

        assertNotNull(response);
        assertEquals("Martín", response.nombre());
        assertEquals("Gómez", response.apellido());
        assertEquals(101, response.idRefugio());
        assertNotNull(response.refugio());
        assertEquals("HAB-MUNIC-CBA-2024-0412", response.refugio().habilitacionMunicipal());
    }

    @Test
    @DisplayName("Login exitoso con CUIL sin guiones (normalización)")
    void testLoginExitosoCuilNormalizado() {
        when(ciudadanoRepository.findByCuil("20123456789")).thenReturn(Optional.empty());
        when(ciudadanoRepository.findByCuilNormalized("20123456789")).thenReturn(Optional.of(ciudadanoMock));

        UsuarioRefugioDTO response = authService.login(new LoginRequest("20123456789", "1234"));

        assertNotNull(response);
        assertEquals(1, response.idCiudadano());
    }

    @Test
    @DisplayName("Login falla por contraseña incorrecta")
    void testLoginPasswordIncorrecta() {
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadanoMock));

        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> authService.login(new LoginRequest("20-12345678-9", "wrongpass"))
        );

        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    @Test
    @DisplayName("Login falla por CUIL inexistente")
    void testLoginUsuarioNoEncontrado() {
        when(ciudadanoRepository.findByCuil(anyString())).thenReturn(Optional.empty());
        when(ciudadanoRepository.findByCuilNormalized(anyString())).thenReturn(Optional.empty());

        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> authService.login(new LoginRequest("20-99999999-9", "1234"))
        );

        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    @Test
    @DisplayName("Login falla si el usuario no está habilitado municipalmente")
    void testLoginUsuarioDeshabilitado() {
        ciudadanoMock.setHabilitado(false);
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadanoMock));

        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> authService.login(new LoginRequest("20-12345678-9", "1234"))
        );

        assertEquals("El usuario no se encuentra habilitado por el Programa Municipal", ex.getMessage());
    }

    @Test
    @DisplayName("Login falla si el usuario no pertenece a un refugio habilitado")
    void testLoginUsuarioSinRefugio() {
        ciudadanoMock.setRefugio(null);
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadanoMock));

        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> authService.login(new LoginRequest("20-12345678-9", "1234"))
        );

        assertEquals("El usuario no pertenece a un refugio habilitado", ex.getMessage());
    }

    @Test
    @DisplayName("Login falla si el refugio no tiene habilitación municipal vigente")
    void testLoginRefugioSinHabilitacionMunicipal() {
        refugioMock.setHabilitacionMunicipal("");
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadanoMock));

        AutenticacionInvalidaException ex = assertThrows(
            AutenticacionInvalidaException.class,
            () -> authService.login(new LoginRequest("20-12345678-9", "1234"))
        );

        assertEquals("El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente", ex.getMessage());
    }

    @Test
    @DisplayName("Login exitoso con formato de CUIL con puntos")
    void testLoginCuilConPuntos() {
        when(ciudadanoRepository.findByCuil("20.12345678.9")).thenReturn(Optional.empty());
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadanoMock));

        UsuarioRefugioDTO response = authService.login(new LoginRequest("20.12345678.9", "1234"));

        assertNotNull(response);
        assertEquals(1, response.idCiudadano());
    }
}

