package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicacionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Adversarial Empirical Challenge: PublicacionController Direct Wiring & GlobalExceptionHandler")
class AdversarialPublicacionControllerChallengeTest {

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

    private Refugio fixtureRefugio(int id, String hab) {
        return Refugio.builder()
            .idRefugio(id)
            .razonSocial("Refugio Esperanza " + id)
            .correo("contacto@refugio" + id + ".org")
            .telefono("351-111222")
            .domicilio("Av. Colón 1234")
            .habilitacionMunicipal(hab)
            .build();
    }

    private Mascota fixtureMascota(Refugio refugio, boolean vive, String microchip) {
        return Mascota.builder()
            .nroRegMunicipal(77001)
            .nombre("Furia")
            .especie("Canina")
            .sexo("Hembra")
            .anioNacimiento(2022)
            .raza("Galgo")
            .microchip(microchip)
            .vive(vive)
            .refugio(refugio)
            .build();
    }

    private PublicacionAdopcion fixturePublicacion(int nro, EstadoPublicacion estado, Refugio refugio, Mascota mascota) {
        return PublicacionAdopcion.builder()
            .nroPublicacion(nro)
            .mascota(mascota)
            .refugio(refugio)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota("Muy cariñosa y tranquila")
            .condicionAdopcion("Espacio amplio y compromiso de castración")
            .foto("https://imagenes.cordoba.gob.ar/mascotas/furia.jpg")
            .estadoPublicacion(estado)
            .build();
    }

    // =========================================================================
    // 1. Desafíos de POST /api/publicaciones
    // =========================================================================

    @Nested
    @DisplayName("1. Desafíos en POST /api/publicaciones (Creación de Publicación)")
    class PostPublicacionChallenges {

        @Test
        @DisplayName("POST sin X-Refugio-Id ni parámetro ni body rechaza con 400 Bad Request")
        void testCrearPublicacionSinIdentificadorRefugioRetorna400() throws Exception {
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                null, "Furia", "Canina", "Hembra", 2022, "Galgo",
                "MC-9812", "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("El identificador del refugio es requerido para registrar la publicación"));
        }

        @Test
        @DisplayName("POST con refugio inexistente retorna 404 Not Found")
        void testCrearPublicacionRefugioInexistenteRetorna404() throws Exception {
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                9999, "Furia", "Canina", "Hembra", 2022, "Galgo",
                "MC-9812", "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            when(refugioRepository.findById(9999)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 9999)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Refugio no encontrado con ID: 9999"));
        }

        @ParameterizedTest(name = "POST con refugio sin habilitación municipal vigente \"{0}\" rechaza con 400")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void testCrearPublicacionRefugioSinHabilitacionRetorna400(String habInvalida) throws Exception {
            Refugio refugioSinHab = fixtureRefugio(101, habInvalida);
            when(refugioRepository.findById(101)).thenReturn(Optional.of(refugioSinHab));

            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 2022, "Galgo",
                "MC-9812", "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("El refugio no cuenta con habilitación municipal vigente para registrar publicaciones"));
        }

        @Test
        @DisplayName("POST con microchip duplicado rechaza con 400 Bad Request")
        void testCrearPublicacionMicrochipDuplicadoRetorna400() throws Exception {
            Refugio refugio = fixtureRefugio(101, "HAB-OK-001");
            when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
            when(mascotaRepository.existsByMicrochip("MC-DUPLICADO-99")).thenReturn(true);

            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 2022, "Galgo",
                "MC-DUPLICADO-99", "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Ya existe una mascota registrada con el microchip: MC-DUPLICADO-99"));
        }

        @Test
        @DisplayName("POST con microchip con espacios circundantes es sanitizado y validado correctamente")
        void testCrearPublicacionMicrochipConEspaciosSanitizado() throws Exception {
            Refugio refugio = fixtureRefugio(101, "HAB-OK-001");
            when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
            when(mascotaRepository.existsByMicrochip("MC-TRIMMED-123")).thenReturn(true);

            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 2022, "Galgo",
                "   MC-TRIMMED-123   ", "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una mascota registrada con el microchip: MC-TRIMMED-123"));
        }

        @Test
        @DisplayName("POST con violación de integridad en BD (DataIntegrityViolationException) retorna 409 Conflict")
        void testCrearPublicacionViolacionIntegridadRetorna409() throws Exception {
            Refugio refugio = fixtureRefugio(101, "HAB-OK-001");
            when(refugioRepository.findById(101)).thenReturn(Optional.of(refugio));
            when(mascotaRepository.existsByMicrochip(any())).thenReturn(false);
            when(mascotaRepository.save(any())).thenThrow(new DataIntegrityViolationException("FK conflict"));

            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 2022, "Galgo",
                "MC-UNIQUE", "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflicto de Integridad"));
        }

        @Test
        @DisplayName("POST con año de nacimiento futuro (>2030) o antiguo (<2000) retorna 400 Bad Request")
        void testCrearPublicacionAnioNacimientoInvalidoRetorna400() throws Exception {
            NuevaPublicacionDTO dtoAntiguo = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 1990, "Galgo",
                null, "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoAntiguo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.anioNacimiento").value("El año de nacimiento debe ser mayor o igual a 2000"));

            NuevaPublicacionDTO dtoFuturo = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 2050, "Galgo",
                null, "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoFuturo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.anioNacimiento").value("El año de nacimiento no puede ser futuro"));
        }

        @Test
        @DisplayName("POST con campos obligatorios vacíos retorna 400 con mapa de errores completo")
        void testCrearPublicacionCamposObligatoriosVaciosRetorna400() throws Exception {
            NuevaPublicacionDTO dtoVacio = new NuevaPublicacionDTO(
                101, "", "", "", null, "",
                null, "", "", ""
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoVacio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.nombre").exists())
                .andExpect(jsonPath("$.detalles.especie").exists())
                .andExpect(jsonPath("$.detalles.sexo").exists())
                .andExpect(jsonPath("$.detalles.anioNacimiento").exists())
                .andExpect(jsonPath("$.detalles.foto").exists())
                .andExpect(jsonPath("$.detalles.caracteristicasMascota").exists())
                .andExpect(jsonPath("$.detalles.condicionAdopcion").exists());
        }

        @Test
        @DisplayName("POST con cabecera X-Refugio-Id no numérica retorna 400 Bad Request")
        void testCrearPublicacionCabeceraNoNumericaRetorna400() throws Exception {
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101, "Furia", "Canina", "Hembra", 2022, "Galgo",
                null, "https://foto.jpg", "Cariñosa", "Patio cerrado"
            );

            mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", "invalido-abc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro Inválido"));
        }
    }

    // =========================================================================
    // 2. Desafíos de PATCH /api/publicaciones/{id}/estado
    // =========================================================================

    @Nested
    @DisplayName("2. Desafíos en PATCH /api/publicaciones/{id}/estado (FSM y Titularidad)")
    class PatchEstadoChallenges {

        @Test
        @DisplayName("PATCH sobre publicación inexistente retorna 404 Not Found")
        void testPatchEstadoPublicacionInexistenteRetorna404() throws Exception {
            when(publicacionRepository.findByIdWithDetails(99999)).thenReturn(Optional.empty());

            mockMvc.perform(patch("/api/publicaciones/99999/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Publicación no encontrada con número: 99999"));
        }

        @Test
        @DisplayName("PATCH con ID de publicación no numérico retorna 400 Bad Request")
        void testPatchEstadoIdNoNumericoRetorna400() throws Exception {
            mockMvc.perform(patch("/api/publicaciones/no-es-numero/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro Inválido"));
        }

        @Test
        @DisplayName("PATCH con titularidad de refugio ajeno (header X-Refugio-Id: 102 != 101) retorna 401 Unauthorized")
        void testPatchEstadoRefugioAjenoViaHeaderRetorna401() throws Exception {
            Refugio refugio101 = fixtureRefugio(101, "HAB-001");
            Mascota mascota = fixtureMascota(refugio101, true, null);
            PublicacionAdopcion pub = fixturePublicacion(5001, EstadoPublicacion.Activa, refugio101, mascota);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pub));

            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 102)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("No posee permisos para administrar publicaciones pertenecientes a otro refugio."));
        }

        @Test
        @DisplayName("PATCH con titularidad de refugio ajeno vía query param (?refugioId=102) retorna 401")
        void testPatchEstadoRefugioAjenoViaQueryParamRetorna401() throws Exception {
            Refugio refugio101 = fixtureRefugio(101, "HAB-001");
            Mascota mascota = fixtureMascota(refugio101, true, null);
            PublicacionAdopcion pub = fixturePublicacion(5001, EstadoPublicacion.Activa, refugio101, mascota);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pub));

            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .param("refugioId", "102")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("PATCH con titularidad de refugio ajeno vía body (id_refugio: 102) retorna 401")
        void testPatchEstadoRefugioAjenoViaBodyRetorna401() throws Exception {
            Refugio refugio101 = fixtureRefugio(101, "HAB-001");
            Mascota mascota = fixtureMascota(refugio101, true, null);
            PublicacionAdopcion pub = fixturePublicacion(5001, EstadoPublicacion.Activa, refugio101, mascota);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pub));

            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\", \"id_refugio\": 102}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Precedencia de resolución: X-Refugio-Id de cabecera prevalece sobre body incompatible")
        void testPatchEstadoPrecedenciaCabeceraSobreBody() throws Exception {
            Refugio refugio101 = fixtureRefugio(101, "HAB-001");
            Mascota mascota = fixtureMascota(refugio101, true, null);
            PublicacionAdopcion pub = fixturePublicacion(5001, EstadoPublicacion.Activa, refugio101, mascota);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pub));
            when(publicacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Cabecera legítima (101), pero body engañoso (102): debe mandar cabecera y tener éxito
            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\", \"id_refugio\": 102}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado_publicacion").value("Pausada"));
        }

        @Test
        @DisplayName("Transición rechazada: desde Finalizada a cualquier otro estado retorna 409 Conflict")
        void testPatchEstadoTransicionDesdeFinalizadaRetorna409() throws Exception {
            Refugio refugio = fixtureRefugio(101, "HAB-001");
            Mascota mascota = fixtureMascota(refugio, true, null);
            PublicacionAdopcion pubFinalizada = fixturePublicacion(5001, EstadoPublicacion.Finalizada, refugio, mascota);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pubFinalizada));

            // Intentar reabrir a Activa
            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Activa\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Una publicación en estado Finalizada no puede cambiar de estado."));

            // Intentar cambiar a Pausada
            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Una publicación en estado Finalizada no puede cambiar de estado."));
        }

        @Test
        @DisplayName("Transición rechazada: reactivación de mascota fallecida (vive = false) retorna 409 Conflict")
        void testPatchEstadoReactivacionMascotaFallecidaRetorna409() throws Exception {
            Refugio refugio = fixtureRefugio(101, "HAB-001");
            Mascota mascotaMuerta = fixtureMascota(refugio, false, null);
            PublicacionAdopcion pubPausada = fixturePublicacion(5001, EstadoPublicacion.Pausada, refugio, mascotaMuerta);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pubPausada));

            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Activa\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("No es posible activar la publicación de una mascota no disponible o registrada como fallecida."));
        }

        @Test
        @DisplayName("Transición rechazada: reactivación cuando la mascota ya posee otra publicación Activa retorna 409")
        void testPatchEstadoReactivacionMascotaConOtraActivaRetorna409() throws Exception {
            Refugio refugio = fixtureRefugio(101, "HAB-001");
            Mascota mascota = fixtureMascota(refugio, true, null);
            PublicacionAdopcion pubPausada = fixturePublicacion(5001, EstadoPublicacion.Pausada, refugio, mascota);

            when(publicacionRepository.findByIdWithDetails(5001)).thenReturn(Optional.of(pubPausada));
            when(publicacionRepository.existsOtraPublicacionEnEstado(77001, EstadoPublicacion.Activa, 5001)).thenReturn(true);

            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Activa\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("La mascota ya posee otra publicación de adopción activa."));
        }

        @ParameterizedTest(name = "Estado desconocido o inválido \"{0}\" rechaza con 400 Bad Request")
        @ValueSource(strings = {"Eliminada", "Cancelada", "Borrador", "Inexistente", "123", "ARCHIVADA"})
        void testPatchEstadoDesconocidoRetorna400(String estadoInvalido) throws Exception {
            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"" + estadoInvalido + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Estado no válido: " + estadoInvalido));
        }

        @Test
        @DisplayName("PATCH con nuevo_estado en blanco retorna 400 Bad Request por validación Bean")
        void testPatchEstadoEnBlancoRetorna400() throws Exception {
            mockMvc.perform(patch("/api/publicaciones/5001/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.nuevoEstado").exists());
        }

        @Test
        @DisplayName("PATCH con métodos HTTP PUT o DELETE retorna 405 Method Not Allowed")
        void testPatchEstadoMetodosNoPermitidosRetorna405() throws Exception {
            mockMvc.perform(put("/api/publicaciones/5001/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nuevo_estado\": \"Pausada\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

            mockMvc.perform(delete("/api/publicaciones/5001/estado"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        }
    }

    // =========================================================================
    // 3. Desafíos de GET /api/publicaciones y GET /api/publicaciones/{id}
    // =========================================================================

    @Nested
    @DisplayName("3. Desafíos en GET (Lectura, Filtros y Formatos)")
    class GetChallenges {

        @Test
        @DisplayName("GET /api/publicaciones con filtro de estado inválido retorna 400 Bad Request")
        void testGetPublicacionesFiltroEstadoInvalidoRetorna400() throws Exception {
            mockMvc.perform(get("/api/publicaciones").param("estado", "INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Estado no válido: INEXISTENTE"));
        }

        @Test
        @DisplayName("GET /api/publicaciones con estado 'TODAS' traduce a filtro nulo (retorna 200)")
        void testGetPublicacionesEstadoTodasTraduceANull() throws Exception {
            when(publicacionRepository.buscarPublicaciones(null, null, null)).thenReturn(List.of());

            mockMvc.perform(get("/api/publicaciones").param("estado", "TODAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("GET /api/publicaciones con refugioId no numérico retorna 400 Bad Request")
        void testGetPublicacionesRefugioIdNoNumericoRetorna400() throws Exception {
            mockMvc.perform(get("/api/publicaciones").param("refugioId", "invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro Inválido"));
        }

        @Test
        @DisplayName("GET /api/publicaciones/{id} con ID no numérico retorna 400 Bad Request")
        void testGetPorIdNoNumericoRetorna400() throws Exception {
            mockMvc.perform(get("/api/publicaciones/xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro Inválido"));
        }
    }
}
