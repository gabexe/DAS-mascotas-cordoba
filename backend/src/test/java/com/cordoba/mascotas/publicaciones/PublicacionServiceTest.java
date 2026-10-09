package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.RecursoNoEncontradoException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    @Mock
    private PublicacionAdopcionRepository publicacionRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private RefugioRepository refugioRepository;

    @Mock
    private com.cordoba.mascotas.auth.CiudadanoRepository ciudadanoRepository;

    @InjectMocks
    private PublicacionService publicacionService;

    private Refugio refugio;
    private Mascota mascota;
    private PublicacionAdopcion publicacion;

    @BeforeEach
    void setUp() {
        refugio = Refugio.builder()
            .idRefugio(101)
            .razonSocial("Asociación Protectora Huellitas Cordobesas")
            .correo("contacto@huellitascba.org.ar")
            .telefono("0351-4890123")
            .domicilio("Av. Colón 4500, Córdoba Capital")
            .habilitacionMunicipal("HAB-MUNIC-CBA-2024-0412")
            .build();

        mascota = Mascota.builder()
            .nroRegMunicipal(50123)
            .nombre("Milo")
            .especie("Perro")
            .sexo("Macho")
            .anioNacimiento(2023)
            .raza("Mestizo de Labrador")
            .microchip("MC-CBA-99124")
            .vive(true)
            .refugio(refugio)
            .build();

        publicacion = PublicacionAdopcion.builder()
            .nroPublicacion(1001)
            .mascota(mascota)
            .refugio(refugio)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota("Muy cariñoso y sociable")
            .condicionAdopcion("Patio cerrado")
            .foto("https://images.unsplash.com/photo-1543466835")
            .estadoPublicacion(EstadoPublicacion.Activa)
            .build();
    }

    @Test
    @DisplayName("Alta de publicación registra mascota y publicación atómicamente")
    void testCrearPublicacion() {
        NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
            101,
            "Milo",
            "Perro",
            "Macho",
            2023,
            "Labrador",
            "MC-123",
            "https://foto.jpg",
            "Juguetón",
            "Hogar responsable"
        );

        when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
        when(mascotaRepository.save(any(Mascota.class))).thenReturn(mascota);
        when(publicacionRepository.save(any(PublicacionAdopcion.class))).thenReturn(publicacion);

        PublicacionResponseDTO resultado = publicacionService.crearPublicacion(dto, 101);

        assertNotNull(resultado);
        assertEquals(1001, resultado.nroPublicacion());
        assertEquals("Activa", resultado.estadoPublicacion());
        verify(mascotaRepository).save(any(Mascota.class));
        verify(publicacionRepository).save(any(PublicacionAdopcion.class));
    }

    @Test
    @DisplayName("Transición de Activa a Pausada exitosa")
    void testTransicionActivaAPausada() {
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(PublicacionAdopcion.class))).thenAnswer(i -> i.getArgument(0));

        PublicacionResponseDTO res = publicacionService.cambiarEstado(1001, EstadoPublicacion.Pausada);

        assertEquals("Pausada", res.estadoPublicacion());
    }

    @Test
    @DisplayName("Transición de Activa a Finalizada exitosa")
    void testTransicionActivaAFinalizada() {
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(PublicacionAdopcion.class))).thenAnswer(i -> i.getArgument(0));

        PublicacionResponseDTO res = publicacionService.cambiarEstado(1001, EstadoPublicacion.Finalizada);

        assertEquals("Finalizada", res.estadoPublicacion());
    }

    @Test
    @DisplayName("Transición rechazada si la publicación ya está Finalizada")
    void testTransicionDesdeFinalizadaRechazada() {
        publicacion.setEstadoPublicacion(EstadoPublicacion.Finalizada);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(publicacion));

        TransicionEstadoInvalidaException ex = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacionService.cambiarEstado(1001, EstadoPublicacion.Activa)
        );

        assertEquals("Una publicación en estado Finalizada no puede cambiar de estado.", ex.getMessage());
    }

    @Test
    @DisplayName("Búsqueda por ID inexistente lanza RecursoNoEncontradoException")
    void testObtenerPorIdInexistente() {
        when(publicacionRepository.findByIdWithDetails(9999)).thenReturn(Optional.empty());

        assertThrows(
            RecursoNoEncontradoException.class,
            () -> publicacionService.obtenerPorId(9999)
        );
    }

    @Test
    @DisplayName("Alta de publicación falla si ya existe una mascota con el mismo microchip")
    void testCrearPublicacionMicrochipDuplicado() {
        NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
            101,
            "Milo",
            "Perro",
            "Macho",
            2023,
            "Labrador",
            "MC-DUPLICADO",
            "https://foto.jpg",
            "Juguetón",
            "Hogar responsable"
        );

        when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
        when(mascotaRepository.existsByMicrochip("MC-DUPLICADO")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> publicacionService.crearPublicacion(dto, 101)
        );

        assertEquals("Ya existe una mascota registrada con el microchip: MC-DUPLICADO", ex.getMessage());
    }

    @Test
    @DisplayName("Cambio de estado es rechazado si lo intenta un refugio distinto al propietario")
    void testCambiarEstadoPermisosRefugioDistinto() {
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(publicacion));

        com.cordoba.mascotas.shared.AutenticacionInvalidaException ex = assertThrows(
            com.cordoba.mascotas.shared.AutenticacionInvalidaException.class,
            () -> publicacionService.cambiarEstado(1001, EstadoPublicacion.Pausada, 102)
        );

        assertEquals("No posee permisos para administrar publicaciones pertenecientes a otro refugio.", ex.getMessage());
    }

    @Test
    @DisplayName("Transición a Activa es rechazada si la mascota ya tiene otra publicación activa")
    void testCambiarEstadoRechazadoSiYaTieneActiva() {
        publicacion.setEstadoPublicacion(EstadoPublicacion.Pausada);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.existsOtraPublicacionEnEstado(50123, EstadoPublicacion.Activa, 1001)).thenReturn(true);

        TransicionEstadoInvalidaException ex = assertThrows(
            TransicionEstadoInvalidaException.class,
            () -> publicacionService.cambiarEstado(1001, EstadoPublicacion.Activa, 101)
        );

        assertEquals("La mascota ya posee otra publicación de adopción activa.", ex.getMessage());
    }
}

