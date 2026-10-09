package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.auth.RefugioDTO;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.shared.GlobalExceptionHandler;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
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
    private PublicacionService publicacionService;

    private PublicacionResponseDTO fixtureResponse(int nro, String estado) {
        RefugioDTO refugio = new RefugioDTO(
            101,
            "Huellitas",
            "info@h.org",
            "351",
            "Colón",
            "HAB-123"
        );
        MascotaDTO mascota = new MascotaDTO(
            50123,
            "Milo",
            "Perro",
            "Macho",
            2023,
            "Labrador",
            null,
            true,
            101
        );
        return new PublicacionResponseDTO(
            nro,
            50123,
            101,
            LocalDate.now(),
            "Muy dócil",
            "Patio cerrado",
            "https://foto.jpg",
            estado,
            mascota,
            refugio
        );
    }

    @Test
    @DisplayName("GET /api/publicaciones retorna lista con 200 OK")
    void testListarPublicaciones() throws Exception {
        when(publicacionService.listarPublicaciones(any(), any(), any()))
            .thenReturn(List.of(fixtureResponse(1001, "Activa")));

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

        when(publicacionService.crearPublicacion(any(NuevaPublicacionDTO.class), any()))
            .thenReturn(fixtureResponse(1001, "Activa"));

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
        when(publicacionService.cambiarEstado(eq(1001), eq(EstadoPublicacion.Pausada), any()))
            .thenReturn(fixtureResponse(1001, "Pausada"));

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
        when(publicacionService.cambiarEstado(eq(1001), eq(EstadoPublicacion.Activa), any()))
            .thenThrow(new TransicionEstadoInvalidaException("Una publicación en estado Finalizada no puede cambiar de estado."));

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
        when(publicacionService.cambiarEstado(eq(1001), eq(EstadoPublicacion.Pausada), any()))
            .thenReturn(fixtureResponse(1001, "Pausada"));

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
        when(publicacionService.listarPublicaciones(eq(101), any(), any()))
            .thenReturn(List.of(fixtureResponse(1001, "Activa")));

        mockMvc.perform(get("/api/publicaciones").param("id_refugio", "101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nro_publicacion").value(1001));
    }
}

