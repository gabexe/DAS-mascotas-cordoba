package com.cordoba.mascotas;

import com.cordoba.mascotas.auth.AuthController;
import com.cordoba.mascotas.auth.LoginRequest;
import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.publicaciones.CambioEstadoDTO;
import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
import com.cordoba.mascotas.publicaciones.PublicacionController;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MascotasApplicationIntegrationTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private PublicacionController publicacionController;

    @Test
    @DisplayName("Contexto levanta y autentica con datos reales de la BD SQL Server")
    void testAutenticacionConBaseDeDatosReal() {
        ResponseEntity<Ciudadano> response = authController.login(new LoginRequest("20-12345678-9", "1234"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Ciudadano usuario = response.getBody();
        assertNotNull(usuario);
        assertEquals("Martín", usuario.getNombre());
        assertEquals("Gómez", usuario.getApellido());
        assertEquals(101, usuario.getIdRefugio());
        assertNotNull(usuario.getRefugio());
        assertEquals("Asociación Protectora Huellitas Cordobesas", usuario.getRefugio().getRazonSocial());
    }

    @Test
    @DisplayName("Consulta de publicaciones sembradas en SQL Server retorna datos completos")
    void testListadoPublicacionesSembradas() {
        ResponseEntity<List<PublicacionAdopcion>> response = publicacionController.listar(null, null, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<PublicacionAdopcion> publicaciones = response.getBody();
        assertNotNull(publicaciones);
        assertFalse(publicaciones.isEmpty());
        assertTrue(publicaciones.size() >= 5);

        // Filtrar por refugio Huellitas Cordobesas (101)
        ResponseEntity<List<PublicacionAdopcion>> huellitasResponse = publicacionController.listar(101, null, null, null, null);
        assertEquals(HttpStatus.OK, huellitasResponse.getStatusCode());
        List<PublicacionAdopcion> huellitas = huellitasResponse.getBody();
        assertNotNull(huellitas);
        assertFalse(huellitas.isEmpty());

        // Filtrar por término de búsqueda predictiva
        ResponseEntity<List<PublicacionAdopcion>> busquedaResponse = publicacionController.listar(101, null, null, null, "Milo");
        assertEquals(HttpStatus.OK, busquedaResponse.getStatusCode());
        List<PublicacionAdopcion> busquedaMilo = busquedaResponse.getBody();
        assertNotNull(busquedaMilo);
        assertEquals(1, busquedaMilo.size());
        assertEquals("Milo", busquedaMilo.get(0).getMascota().getNombre());
    }

    @Test
    @Transactional
    @DisplayName("Ciclo completo atómico: Alta de publicación y FSM en SQL Server")
    void testCicloCompletoAltaYTransicionesFSM() {
        NuevaPublicacionDTO nueva = new NuevaPublicacionDTO(
            101,
            "Rocky Integration",
            "Perro",
            "Macho",
            2023,
            "Boxer",
            "MC-IT-001",
            "https://ejemplo.com/rocky.jpg",
            "Muy amigable y activo",
            "Patio seguro y tiempo para paseos"
        );

        ResponseEntity<PublicacionAdopcion> responseCrear = publicacionController.crear(nueva, 101, null, null);
        assertEquals(HttpStatus.CREATED, responseCrear.getStatusCode());

        PublicacionAdopcion creada = responseCrear.getBody();
        assertNotNull(creada);
        assertNotNull(creada.getNroPublicacion());
        assertNotNull(creada.getMascota());
        assertEquals("Rocky Integration", creada.getMascota().getNombre());
        assertEquals("Activa", creada.getEstadoPublicacion().name());

        // Transición: Activa -> Pausada
        ResponseEntity<PublicacionAdopcion> respPausada = publicacionController.cambiarEstado(
            creada.getNroPublicacion(), new CambioEstadoDTO("Pausada"), 101, null, null
        );
        assertEquals(HttpStatus.OK, respPausada.getStatusCode());
        assertNotNull(respPausada.getBody());
        assertEquals("Pausada", respPausada.getBody().getEstadoPublicacion().name());

        // Transición: Pausada -> Activa
        ResponseEntity<PublicacionAdopcion> respActiva = publicacionController.cambiarEstado(
            creada.getNroPublicacion(), new CambioEstadoDTO("Activa"), 101, null, null
        );
        assertEquals(HttpStatus.OK, respActiva.getStatusCode());
        assertNotNull(respActiva.getBody());
        assertEquals("Activa", respActiva.getBody().getEstadoPublicacion().name());

        // Transición: Activa -> Finalizada
        ResponseEntity<PublicacionAdopcion> respFinalizada = publicacionController.cambiarEstado(
            creada.getNroPublicacion(), new CambioEstadoDTO("Finalizada"), 101, null, null
        );
        assertEquals(HttpStatus.OK, respFinalizada.getStatusCode());
        assertNotNull(respFinalizada.getBody());
        assertEquals("Finalizada", respFinalizada.getBody().getEstadoPublicacion().name());

        // Transición rechazada: Finalizada -> Activa (Inmutable)
        assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacionController.cambiarEstado(
                creada.getNroPublicacion(), new CambioEstadoDTO("Activa"), 101, null, null
            )
        );
    }

    @Test
    @Transactional
    @DisplayName("SQL Server rechaza alta con microchip duplicado de mascota ya registrada")
    void testRechazoMicrochipDuplicadoEnBaseDeDatos() {
        NuevaPublicacionDTO duplicado = new NuevaPublicacionDTO(
            101,
            "Clon de Milo",
            "Perro",
            "Macho",
            2023,
            "Mestizo",
            "MC-CBA-99124", // Microchip ya existente en seed de Milo
            "https://ejemplo.com/clon.jpg",
            "Mascota duplicada",
            "Condiciones"
        );

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> publicacionController.crear(duplicado, 101, null, null)
        );

        assertTrue(ex.getMessage().contains("Ya existe una mascota registrada con el microchip"));
    }

    @Test
    @Transactional
    @DisplayName("SQL Server rechaza modificación de publicación por otro refugio")
    void testRechazoModificacionPorOtroRefugio() {
        // Publicación 1001 pertenece a Huellitas Cordobesas (101)
        // Intentar modificar desde Patitas del Sur (102)
        assertThrows(
            AutenticacionInvalidaException.class,
            () -> publicacionController.cambiarEstado(1001, new CambioEstadoDTO("Pausada"), 102, null, null)
        );
    }
}

