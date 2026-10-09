package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
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
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login retorna 200 con datos del usuario y refugio")
    void testLoginExitoso() throws Exception {
        RefugioDTO refugioDTO = new RefugioDTO(
            101,
            "Asociación Protectora Huellitas Cordobesas",
            "contacto@huellitascba.org.ar",
            "0351-4890123",
            "Av. Colón 4500",
            "HAB-MUNIC-CBA-2024-0412"
        );

        UsuarioRefugioDTO usuarioDTO = new UsuarioRefugioDTO(
            1,
            "20-12345678-9",
            "Martín",
            "Gómez",
            "martin@huellitas.org",
            "351-4567890",
            101,
            refugioDTO
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(usuarioDTO);

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
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new AutenticacionInvalidaException("Credenciales incorrectas"));

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
}

