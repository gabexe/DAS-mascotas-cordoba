package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicacionController.class)
@Import(GlobalExceptionHandler.class)
class PublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublicacionAdopcionRepository publicacionRepository;

    @MockBean
    private MascotaRepository mascotaRepository;

    @MockBean
    private RefugioRepository refugioRepository;

    @MockBean
    private CiudadanoRepository ciudadanoRepository;

    private Refugio fixtureRefugio() {
        return Refugio.builder()
            .idRefugio(101)
            .razonSocial("Huellitas")
            .correo("info@h.org")
            .telefono("351")
            .domicilio("Colón")
            .habilitacionMunicipal("HAB-123")
            .build();
    }

    private Mascota fixtureMascota(Refugio refugio) {
        return Mascota.builder()
            .nroRegMunicipal(50123)
            .nombre("Milo")
            .especie("Perro")
            .sexo("Macho")
            .anioNacimiento(2023)
            .raza("Labrador")
            .microchip(null)
            .vive(true)
            .refugio(refugio)
            .build();
    }

    private PublicacionAdopcion fixturePublicacion(int nro, EstadoPublicacion estado) {
        Refugio refugio = fixtureRefugio();
        Mascota mascota = fixtureMascota(refugio);
        return PublicacionAdopcion.builder()
            .nroPublicacion(nro)
            .mascota(mascota)
            .refugio(refugio)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota("Muy dócil")
            .condicionAdopcion("Patio cerrado")
            .foto("https://foto.jpg")
            .estadoPublicacion(estado)
            .build();
    }

    @Test
    @DisplayName("GET /api/publicaciones retorna lista con 200 OK")
    void testListarPublicaciones() throws Exception {
        when(publicacionRepository.buscarPublicaciones(any(), any(), any()))
            .thenReturn(List.of(fixturePublicacion(1001, EstadoPublicacion.Activa)));

        mockMvc.perform(get("/api/publicaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nro_publicacion").value(1001))
            .andExpect(jsonPath("$[0].mascota.nombre").value("Milo"));
    }

    @Test
    @DisplayName("POST /api/publicaciones registra publicación y retorna 201 Created")
    void testCrearPublicacion() throws Exception {
        NuevaPublicacionDTO request = new NuevaPublicacionDTO(
            101,
            "Milo",
            "Perro",
            "Macho",
            2023,
            "Labrador",
            null,
            "https://foto.jpg",
            "Muy dócil",
            "Patio cerrado"
        );

        Refugio refugio = fixtureRefugio();
        when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(i -> {
            Mascota m = i.getArgument(0);
            m.setNroRegMunicipal(50123);
            return m;
        });
        when(publicacionRepository.save(any(PublicacionAdopcion.class)))
            .thenReturn(fixturePublicacion(1001, EstadoPublicacion.Activa));

        mockMvc.perform(post("/api/publicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nro_publicacion").value(1001))
            .andExpect(jsonPath("$.estado_publicacion").value("Activa"));
    }

    @Test
    @DisplayName("PATCH /api/publicaciones/{id}/estado actualiza a Pausada y retorna 200")
    void testCambiarEstadoExitoso() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Activa);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));
        when(publicacionRepository.save(any(PublicacionAdopcion.class))).thenAnswer(i -> i.getArgument(0));

        CambioEstadoDTO request = new CambioEstadoDTO("Pausada");

        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado_publicacion").value("Pausada"));
    }

    @Test
    @DisplayName("PATCH /api/publicaciones/{id}/estado rechaza transición desde Finalizada con 409 Conflict")
    void testCambiarEstadoConflictoTerminal() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Finalizada);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));

        CambioEstadoDTO request = new CambioEstadoDTO("Activa");

        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.mensaje").value("Una publicación en estado Finalizada no puede cambiar de estado."));
    }

    @Test
    @DisplayName("PATCH /api/publicaciones/{id}/estado acepta aliases en payload (ej: 'estado' en minúsculas)")
    void testCambiarEstadoConAliasYMinusculas() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Activa);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));
        when(publicacionRepository.save(any(PublicacionAdopcion.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"pausada\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado_publicacion").value("Pausada"));
    }

    @Test
    @DisplayName("JSON malformado retorna 400 Bad Request en lugar de 500")
    void testJsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ malformed json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Formato de Solicitud Inválido"));
    }

    @Test
    @DisplayName("PUT /api/publicaciones/{id}/estado es rechazado con 405 Method Not Allowed")
    void testPutEstadoRechazadoCon405() throws Exception {
        mockMvc.perform(put("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevo_estado\": \"Pausada\"}"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.error").value("Método No Permitido"));
    }

    @Test
    @DisplayName("GET /api/publicaciones?id_refugio=101 resuelve correctamente el parámetro normalizado")
    void testListarPublicacionesConParametroIdRefugio() throws Exception {
        when(publicacionRepository.buscarPublicaciones(eq(101), any(), any()))
            .thenReturn(List.of(fixturePublicacion(1001, EstadoPublicacion.Activa)));

        mockMvc.perform(get("/api/publicaciones").param("id_refugio", "101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nro_publicacion").value(1001));
    }

    @Test
    @DisplayName("GET /api/publicaciones/{id} retorna detalle con 200 OK")
    void testObtenerPorIdExitoso() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Activa);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));

        mockMvc.perform(get("/api/publicaciones/1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nro_publicacion").value(1001))
            .andExpect(jsonPath("$.mascota.nombre").value("Milo"));
    }

    @Test
    @DisplayName("GET /api/publicaciones/{id} retorna 404 si no existe")
    void testObtenerPorIdInexistenteRetorna404() throws Exception {
        when(publicacionRepository.findByIdWithDetails(9999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/publicaciones/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.mensaje").value("Publicación no encontrada con número: 9999"));
    }

    @Test
    @DisplayName("POST /api/publicaciones con microchip duplicado retorna 400 Bad Request")
    void testCrearPublicacionMicrochipDuplicadoRetorna400() throws Exception {
        NuevaPublicacionDTO request = new NuevaPublicacionDTO(
            101,
            "Milo",
            "Perro",
            "Macho",
            2023,
            "Labrador",
            "MC-DUPLICADO",
            "https://foto.jpg",
            "Muy dócil",
            "Patio cerrado"
        );

        Refugio refugio = fixtureRefugio();
        when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
        when(mascotaRepository.existsByMicrochip("MC-DUPLICADO")).thenReturn(true);

        mockMvc.perform(post("/api/publicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje").value("Ya existe una mascota registrada con el microchip: MC-DUPLICADO"));
    }

    @Test
    @DisplayName("PATCH /api/publicaciones/{id}/estado rechaza si solicitante es de otro refugio (401)")
    void testCambiarEstadoPermisosRefugioDistintoRetorna401() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Activa);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));

        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .header("X-Refugio-Id", 102)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambioEstadoDTO("Pausada"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensaje").value("No posee permisos para administrar publicaciones pertenecientes a otro refugio."));
    }

    @Test
    @DisplayName("PATCH /api/publicaciones/{id}/estado rechaza activar si la mascota ya posee otra publicación activa (409)")
    void testCambiarEstadoRechazadoSiYaTieneActivaRetorna409() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Pausada);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));
        when(publicacionRepository.existsOtraPublicacionEnEstado(50123, EstadoPublicacion.Activa, 1001)).thenReturn(true);

        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambioEstadoDTO("Activa"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.mensaje").value("La mascota ya posee otra publicación de adopción activa."));
    }

    @Test
    @DisplayName("PATCH /api/publicaciones/{id}/estado de Activa a Finalizada exitosa")
    void testCambiarEstadoActivaAFinalizada() throws Exception {
        PublicacionAdopcion pub = fixturePublicacion(1001, EstadoPublicacion.Activa);
        when(publicacionRepository.findByIdWithDetails(1001)).thenReturn(Optional.of(pub));
        when(publicacionRepository.save(any(PublicacionAdopcion.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(patch("/api/publicaciones/1001/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambioEstadoDTO("Finalizada"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado_publicacion").value("Finalizada"));
    }
}

