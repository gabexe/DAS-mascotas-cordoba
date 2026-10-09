package com.cordoba.mascotas;

import com.cordoba.mascotas.auth.AuthService;
import com.cordoba.mascotas.auth.LoginRequest;
import com.cordoba.mascotas.auth.UsuarioRefugioDTO;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
import com.cordoba.mascotas.publicaciones.PublicacionResponseDTO;
import com.cordoba.mascotas.publicaciones.PublicacionService;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    private AuthService authService;

    @Autowired
    private PublicacionService publicacionService;

    @Test
    @DisplayName("Contexto levanta y autentica con datos reales de la BD SQL Server")
    void testAutenticacionConBaseDeDatosReal() {
        UsuarioRefugioDTO usuario = authService.login(new LoginRequest("20-12345678-9", "1234"));

        assertNotNull(usuario);
        assertEquals("Martín", usuario.nombre());
        assertEquals("Gómez", usuario.apellido());
        assertEquals(101, usuario.idRefugio());
        assertNotNull(usuario.refugio());
        assertEquals("Asociación Protectora Huellitas Cordobesas", usuario.refugio().razonSocial());
    }

    @Test
    @DisplayName("Consulta de publicaciones sembradas en SQL Server retorna datos completos")
    void testListadoPublicacionesSembradas() {
        List<PublicacionResponseDTO> publicaciones = publicacionService.listarPublicaciones(null, null, null);

        assertNotNull(publicaciones);
        assertFalse(publicaciones.isEmpty());
        assertTrue(publicaciones.size() >= 5);

        // Filtrar por refugio Huellitas Cordobesas (101)
        List<PublicacionResponseDTO> huellitas = publicacionService.listarPublicaciones(101, null, null);
        assertFalse(huellitas.isEmpty());

        // Filtrar por término de búsqueda predictiva
        List<PublicacionResponseDTO> busquedaMilo = publicacionService.listarPublicaciones(101, null, "Milo");
        assertEquals(1, busquedaMilo.size());
        assertEquals("Milo", busquedaMilo.get(0).mascota().nombre());
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

        PublicacionResponseDTO creada = publicacionService.crearPublicacion(nueva, 101);
        assertNotNull(creada);
        assertNotNull(creada.nroPublicacion());
        assertNotNull(creada.mascota());
        assertEquals("Rocky Integration", creada.mascota().nombre());
        assertEquals("Activa", creada.estadoPublicacion());

        // Transición: Activa -> Pausada
        PublicacionResponseDTO pausada = publicacionService.cambiarEstado(creada.nroPublicacion(), EstadoPublicacion.Pausada);
        assertEquals("Pausada", pausada.estadoPublicacion());

        // Transición: Pausada -> Activa
        PublicacionResponseDTO reactivada = publicacionService.cambiarEstado(creada.nroPublicacion(), EstadoPublicacion.Activa);
        assertEquals("Activa", reactivada.estadoPublicacion());

        // Transición: Activa -> Finalizada
        PublicacionResponseDTO finalizada = publicacionService.cambiarEstado(creada.nroPublicacion(), EstadoPublicacion.Finalizada);
        assertEquals("Finalizada", finalizada.estadoPublicacion());

        // Transición rechazada: Finalizada -> Activa (Inmutable)
        assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacionService.cambiarEstado(creada.nroPublicacion(), EstadoPublicacion.Activa)
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
            () -> publicacionService.crearPublicacion(duplicado, 101)
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
            com.cordoba.mascotas.shared.AutenticacionInvalidaException.class,
            () -> publicacionService.cambiarEstado(1001, EstadoPublicacion.Pausada, 102)
        );
    }
}

