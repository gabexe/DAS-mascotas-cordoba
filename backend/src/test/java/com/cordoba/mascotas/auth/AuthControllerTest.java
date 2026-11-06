package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CiudadanoRepository ciudadanoRepository;

    private Refugio fixtureRefugio() {
        return Refugio.builder()
            .idRefugio(101)
            .razonSocial("Asociación Protectora Huellitas Cordobesas")
            .correo("contacto@huellitascba.org.ar")
            .telefono("0351-4890123")
            .domicilio("Av. Colón 4500")
            .habilitacionMunicipal("HAB-MUNIC-CBA-2024-0412")
            .build();
    }

    private Ciudadano fixtureCiudadano() {
        return Ciudadano.builder()
            .idCiudadano(1)
            .cuil("20-12345678-9")
            .clave("1234")
            .nombre("Martín")
            .apellido("Gómez")
            .correo("martin@huellitas.org")
            .telefono("351-4567890")
            .habilitado(true)
            .refugio(fixtureRefugio())
            .build();
    }

    @Test
    @DisplayName("POST /api/auth/login retorna 200 con datos del usuario y refugio")
    void testLoginExitoso() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20-12345678-9", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_ciudadano").value(1))
            .andExpect(jsonPath("$.cuil").value("20-12345678-9"))
            .andExpect(jsonPath("$.nombre").value("Martín"))
            .andExpect(jsonPath("$.id_refugio").value(101))
            .andExpect(jsonPath("$.refugio.razon_social").value("Asociación Protectora Huellitas Cordobesas"));
    }

    @Test
    @DisplayName("POST /api/auth/login retorna 401 si las credenciales son inválidas")
    void testLoginInvalido() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20-12345678-9", "error");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    @DisplayName("POST /api/auth/login retorna 400 si faltan campos obligatorios")
    void testLoginCamposIncompletos() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detalles.cuil").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login retorna 400 si el JSON está malformado")
    void testLoginJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ json invalido"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Formato de Solicitud Inválido"));
    }

    @Test
    @DisplayName("POST /api/auth/login exitoso con CUIL sin guiones (normalización)")
    void testLoginExitosoCuilNormalizado() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        when(ciudadanoRepository.findByCuil("20123456789")).thenReturn(Optional.empty());
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20123456789", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_ciudadano").value(1))
            .andExpect(jsonPath("$.cuil").value("20-12345678-9"));
    }

    @Test
    @DisplayName("POST /api/auth/login exitoso con formato de CUIL con puntos")
    void testLoginExitosoCuilConPuntos() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        when(ciudadanoRepository.findByCuil("20.12345678.9")).thenReturn(Optional.empty());
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20.12345678.9", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_ciudadano").value(1));
    }

    @Test
    @DisplayName("POST /api/auth/login falla por CUIL inexistente (401)")
    void testLoginUsuarioNoEncontrado() throws Exception {
        when(ciudadanoRepository.findByCuil(any())).thenReturn(Optional.empty());
        when(ciudadanoRepository.findByCuilNormalized(any())).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("20-99999999-9", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    @DisplayName("POST /api/auth/login falla si el usuario no está habilitado municipalmente (401)")
    void testLoginUsuarioDeshabilitado() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        ciudadano.setHabilitado(false);
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20-12345678-9", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensaje").value("El usuario no se encuentra habilitado por el Programa Municipal"));
    }

    @Test
    @DisplayName("POST /api/auth/login falla si el usuario no pertenece a un refugio habilitado (401)")
    void testLoginUsuarioSinRefugio() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        ciudadano.setRefugio(null);
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20-12345678-9", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensaje").value("El usuario no pertenece a un refugio habilitado"));
    }

    @Test
    @DisplayName("POST /api/auth/login falla si el refugio no tiene habilitación municipal vigente (401)")
    void testLoginRefugioSinHabilitacion() throws Exception {
        Ciudadano ciudadano = fixtureCiudadano();
        ciudadano.getRefugio().setHabilitacionMunicipal("");
        when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

        LoginRequest request = new LoginRequest("20-12345678-9", "1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.mensaje").value("El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente"));
    }
}

