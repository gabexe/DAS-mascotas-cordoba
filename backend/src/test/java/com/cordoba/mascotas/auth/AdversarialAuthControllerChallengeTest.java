package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Adversarial Empirical Challenge: AuthController Direct Wiring & GlobalExceptionHandler")
class AdversarialAuthControllerChallengeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CiudadanoRepository ciudadanoRepository;

    private Refugio fixtureRefugioHabilitado() {
        return Refugio.builder()
            .idRefugio(101)
            .razonSocial("Refugio Esperanza Municipal")
            .correo("contacto@esperanza.org.ar")
            .telefono("0351-4890123")
            .domicilio("Av. Colón 4500")
            .habilitacionMunicipal("HAB-MUNIC-CBA-2024-0412")
            .build();
    }

    private Ciudadano fixtureCiudadanoActivo() {
        return Ciudadano.builder()
            .idCiudadano(1)
            .cuil("20-12345678-9")
            .clave("Secret123*")
            .nombre("Martín")
            .apellido("Gómez")
            .correo("martin@huellitas.org")
            .telefono("351-4567890")
            .habilitado(true)
            .refugio(fixtureRefugioHabilitado())
            .build();
    }

    @Nested
    @DisplayName("1. Cuil Format and Non-Existent User Challenges (401 Unauthorized)")
    class CuilAndUserChallenges {

        @ParameterizedTest(name = "CUIL malformado o anómalo \"{0}\" rechaza con 401 Unauthorized")
        @ValueSource(strings = {
            "20-ABCDEF-9",
            "12345",
            "20123456789012345",
            "00-00000000-0",
            "!@#$%^&*()",
            "CUIL-INVENTADO-123",
            "20.XX.YY.ZZ"
        })
        void testLoginCuilInvalidoOInexistenteRetorna401(String invalidCuil) throws Exception {
            when(ciudadanoRepository.findByCuil(any())).thenReturn(Optional.empty());
            when(ciudadanoRepository.findByCuilNormalized(any())).thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest(invalidCuil, "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No Autorizado"))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
        }

        @Test
        @DisplayName("CUIL bien formateado pero inexistente retorna 401 Unauthorized")
        void testLoginCuilValidoPeroInexistenteRetorna401() throws Exception {
            when(ciudadanoRepository.findByCuil("20-99999999-9")).thenReturn(Optional.empty());
            when(ciudadanoRepository.findByCuilNormalized("20999999999")).thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest("20-99999999-9", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
        }
    }

    @Nested
    @DisplayName("2. Password and Domain Invariant Challenges (401 Unauthorized)")
    class PasswordAndInvariantsChallenges {

        @Test
        @DisplayName("Contraseña incorrecta para usuario existente retorna 401 Unauthorized")
        void testLoginClaveIncorrectaRetorna401() throws Exception {
            Ciudadano ciudadano = fixtureCiudadanoActivo();
            when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

            LoginRequest request = new LoginRequest("20-12345678-9", "WrongPassword999");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
        }

        @Test
        @DisplayName("Usuario deshabilitado municipalmente (habilitado = false) retorna 401 Unauthorized")
        void testLoginUsuarioDeshabilitadoRetorna401() throws Exception {
            Ciudadano ciudadano = fixtureCiudadanoActivo();
            ciudadano.setHabilitado(false);
            when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

            LoginRequest request = new LoginRequest("20-12345678-9", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("El usuario no se encuentra habilitado por el Programa Municipal"));
        }

        @Test
        @DisplayName("Usuario con habilitado = null retorna 401 Unauthorized")
        void testLoginUsuarioHabilitadoNullRetorna401() throws Exception {
            Ciudadano ciudadano = fixtureCiudadanoActivo();
            ciudadano.setHabilitado(null);
            when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

            LoginRequest request = new LoginRequest("20-12345678-9", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("El usuario no se encuentra habilitado por el Programa Municipal"));
        }

        @Test
        @DisplayName("Usuario sin refugio asignado (refugio = null) retorna 401 Unauthorized")
        void testLoginUsuarioSinRefugioRetorna401() throws Exception {
            Ciudadano ciudadano = fixtureCiudadanoActivo();
            ciudadano.setRefugio(null);
            when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

            LoginRequest request = new LoginRequest("20-12345678-9", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("El usuario no pertenece a un refugio habilitado"));
        }

        @ParameterizedTest(name = "Refugio con habilitación municipal inválida \"{0}\" retorna 401")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        void testLoginRefugioSinHabilitacionVigenteRetorna401(String invalidHabilitacion) throws Exception {
            Ciudadano ciudadano = fixtureCiudadanoActivo();
            ciudadano.getRefugio().setHabilitacionMunicipal(invalidHabilitacion);
            when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

            LoginRequest request = new LoginRequest("20-12345678-9", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente"));
        }
    }

    @Nested
    @DisplayName("3. Payload Validation and Bad Request Challenges (400 Bad Request)")
    class PayloadValidationChallenges {

        @Test
        @DisplayName("Login con payload JSON vacío {} retorna 400 Bad Request con detalles de validación")
        void testLoginPayloadVacioRetorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validación Fallida"))
                .andExpect(jsonPath("$.detalles.cuil").exists())
                .andExpect(jsonPath("$.detalles.clave").exists());
        }

        @Test
        @DisplayName("Login con CUIL en blanco retorna 400 Bad Request")
        void testLoginCuilEnBlancoRetorna400() throws Exception {
            LoginRequest request = new LoginRequest("   ", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.cuil").exists());
        }

        @Test
        @DisplayName("Login con contraseña en blanco retorna 400 Bad Request")
        void testLoginClaveEnBlancoRetorna400() throws Exception {
            LoginRequest request = new LoginRequest("20-12345678-9", "   ");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.clave").exists());
        }

        @Test
        @DisplayName("Login con JSON malformado retorna 400 Bad Request con mensaje explicativo")
        void testLoginJsonMalformadoRetorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cuil\": \"20-12345678-9\", \"clave\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Formato de Solicitud Inválido"));
        }

        @Test
        @DisplayName("Login con body completamente vacío retorna 400 Bad Request")
        void testLoginBodyCompletamenteVacioRetorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Formato de Solicitud Inválido"));
        }
    }

    @Nested
    @DisplayName("4. HTTP Method Fidelity and Security Sanitization")
    class MethodFidelityAndSanitization {

        @Test
        @DisplayName("Métodos HTTP GET, PUT, PATCH, DELETE en /api/auth/login retornan 405 Method Not Allowed")
        void testMetodosHttpNoSoportadosRetornan405() throws Exception {
            mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Método No Permitido"));

            mockMvc.perform(put("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

            mockMvc.perform(delete("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

            mockMvc.perform(patch("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        }

        @Test
        @DisplayName("Respuesta de login exitoso nunca expone el hash ni el campo 'clave'")
        void testLoginExitosoNoFiltraClave() throws Exception {
            Ciudadano ciudadano = fixtureCiudadanoActivo();
            when(ciudadanoRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(ciudadano));

            LoginRequest request = new LoginRequest("20-12345678-9", "Secret123*");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clave").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.id_ciudadano").value(1))
                .andExpect(jsonPath("$.cuil").value("20-12345678-9"));
        }
    }
}
