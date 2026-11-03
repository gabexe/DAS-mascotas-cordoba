package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.publicaciones.MascotaRepository;
import com.cordoba.mascotas.publicaciones.PublicacionAdopcionRepository;
import com.cordoba.mascotas.publicaciones.RefugioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Empirical Verification Challenge Test:
 * Validates API contracts and frontend integration parity between Spring Boot backend
 * and Angular TypeScript interfaces in frontend/src/app/core/models/.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Empirical Challenge: API Contract & Frontend Integration Parity")
class FrontendParityChallengeTest {

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CiudadanoRepository ciudadanoRepository;

    @Autowired
    private RefugioRepository refugioRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private PublicacionAdopcionRepository publicacionRepository;

    // =========================================================================
    // Helper: Recursively assert NO camelCase keys exist in JSON node
    // =========================================================================
    private void assertNoCamelCaseKeys(JsonNode node, String path) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String fullPath = path.isEmpty() ? key : path + "." + key;

                // Check for camelCase violation: contains lowercase followed by uppercase
                boolean isCamelCase = Pattern.compile("[a-z][A-Z]").matcher(key).find();
                assertFalse(isCamelCase, "Violación de convención snake_case detectada en '" + fullPath + "'");

                // Check for sensitive fields
                assertFalse("clave".equalsIgnoreCase(key), "Campo sensible 'clave' expuesto en '" + fullPath + "'");
                assertFalse("password".equalsIgnoreCase(key), "Campo sensible 'password' expuesto en '" + fullPath + "'");

                assertNoCamelCaseKeys(entry.getValue(), fullPath);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                assertNoCamelCaseKeys(node.get(i), path + "[" + i + "]");
            }
        }
    }

    // =========================================================================
    // 1. Auth Contract Parity: POST /api/auth/login <-> UsuarioRefugio
    // =========================================================================
    @Nested
    @DisplayName("1. Auth Contract Parity (UsuarioRefugio & Refugio)")
    class AuthContractParityTests {

        @Test
        @DisplayName("POST /api/auth/login produce JSON compatible 100% con TypeScript UsuarioRefugio y Refugio")
        void testLoginResponseMatchesUsuarioRefugioTypeScriptModel() throws Exception {
            String loginPayload = """
                {
                    "cuil": "20-12345678-9",
                    "clave": "1234"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(responseJson);

            // Assert NO camelCase keys anywhere in response
            assertNoCamelCaseKeys(root, "");

            // Verify TypeScript UsuarioRefugio fields:
            // export interface UsuarioRefugio {
            //   id_ciudadano: number;
            //   cuil: string;
            //   nombre: string;
            //   apellido: string;
            //   correo: string;
            //   telefono: string;
            //   id_refugio: number;
            //   refugio: Refugio;
            // }
            assertTrue(root.hasNonNull("id_ciudadano"), "UsuarioRefugio requiere id_ciudadano no nulo");
            assertTrue(root.get("id_ciudadano").isNumber());
            assertEquals(1, root.get("id_ciudadano").asInt());

            assertTrue(root.hasNonNull("cuil"), "UsuarioRefugio requiere cuil");
            assertTrue(root.get("cuil").isTextual());
            assertEquals("20-12345678-9", root.get("cuil").asText());

            assertTrue(root.hasNonNull("nombre"), "UsuarioRefugio requiere nombre");
            assertTrue(root.get("nombre").isTextual());
            assertEquals("Martín", root.get("nombre").asText());

            assertTrue(root.hasNonNull("apellido"), "UsuarioRefugio requiere apellido");
            assertTrue(root.get("apellido").isTextual());
            assertEquals("Gómez", root.get("apellido").asText());

            assertTrue(root.hasNonNull("correo"), "UsuarioRefugio requiere correo");
            assertTrue(root.get("correo").isTextual());

            assertTrue(root.hasNonNull("telefono"), "UsuarioRefugio requiere telefono");
            assertTrue(root.get("telefono").isTextual());

            assertTrue(root.hasNonNull("id_refugio"), "UsuarioRefugio requiere id_refugio en nivel raíz");
            assertTrue(root.get("id_refugio").isNumber());
            assertEquals(101, root.get("id_refugio").asInt());

            assertTrue(root.hasNonNull("refugio"), "UsuarioRefugio requiere objeto refugio anidado");
            JsonNode refugio = root.get("refugio");
            assertTrue(refugio.isObject());

            // Verify TypeScript Refugio fields:
            // export interface Refugio {
            //   id_refugio: number;
            //   razon_social: string;
            //   correo: string;
            //   telefono: string;
            //   domicilio: string;
            //   habilitacion_municipal: string;
            // }
            assertTrue(refugio.hasNonNull("id_refugio"), "Refugio requiere id_refugio");
            assertTrue(refugio.get("id_refugio").isNumber());
            assertEquals(101, refugio.get("id_refugio").asInt());

            assertTrue(refugio.hasNonNull("razon_social"), "Refugio requiere razon_social");
            assertTrue(refugio.get("razon_social").isTextual());
            assertEquals("Asociación Protectora Huellitas Cordobesas", refugio.get("razon_social").asText());

            assertTrue(refugio.hasNonNull("correo"), "Refugio requiere correo");
            assertTrue(refugio.get("correo").isTextual());

            assertTrue(refugio.hasNonNull("telefono"), "Refugio requiere telefono");
            assertTrue(refugio.get("telefono").isTextual());

            assertTrue(refugio.hasNonNull("domicilio"), "Refugio requiere domicilio");
            assertTrue(refugio.get("domicilio").isTextual());

            assertTrue(refugio.hasNonNull("habilitacion_municipal"), "Refugio requiere habilitacion_municipal");
            assertTrue(refugio.get("habilitacion_municipal").isTextual());

            // Strict check: 'clave' is NEVER present in JSON keys or values
            assertFalse(root.has("clave"), "El campo 'clave' no debe existir");
            assertFalse(responseJson.contains("\"clave\":"), "La propiedad 'clave' no debe estar serializada");
        }
    }

    // =========================================================================
    // 2. Publicaciones API Contract Parity: GET /api/publicaciones
    // =========================================================================
    @Nested
    @DisplayName("2. Publicaciones List Contract Parity (PublicacionAdopcion, Mascota, Refugio)")
    class PublicacionesListParityTests {

        @Test
        @DisplayName("GET /api/publicaciones produce array donde cada elemento cumple PublicacionAdopcion")
        void testPublicacionesListMatchesTypeScriptModel() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/publicaciones"))
                .andExpect(status().isOk())
                .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            JsonNode arrayNode = objectMapper.readTree(responseJson);

            assertTrue(arrayNode.isArray(), "La respuesta debe ser un arreglo JSON");
            assertFalse(arrayNode.isEmpty(), "Debe contener al menos una publicación sembrada");

            // Assert NO camelCase keys anywhere in entire array
            assertNoCamelCaseKeys(arrayNode, "");

            for (JsonNode pub : arrayNode) {
                // Verify PublicacionAdopcion TypeScript interface:
                // export interface PublicacionAdopcion {
                //   nro_publicacion: number;
                //   nro_reg_municipal: number;
                //   id_refugio: number;
                //   fecha_publicacion: string; // ISO date string
                //   caracteristicas_mascota: string;
                //   condicion_adopcion: string;
                //   foto: string;
                //   estado_publicacion: EstadoPublicacion;
                //   mascota?: Mascota;
                //   refugio?: Refugio;
                // }
                assertTrue(pub.hasNonNull("nro_publicacion"));
                assertTrue(pub.get("nro_publicacion").isNumber());

                assertTrue(pub.hasNonNull("nro_reg_municipal"), "PublicacionAdopcion requiere nro_reg_municipal");
                assertTrue(pub.get("nro_reg_municipal").isNumber());

                assertTrue(pub.hasNonNull("id_refugio"), "PublicacionAdopcion requiere id_refugio");
                assertTrue(pub.get("id_refugio").isNumber());

                assertTrue(pub.hasNonNull("fecha_publicacion"), "PublicacionAdopcion requiere fecha_publicacion");
                assertTrue(pub.get("fecha_publicacion").isTextual());
                assertTrue(ISO_DATE_PATTERN.matcher(pub.get("fecha_publicacion").asText()).matches(),
                    "fecha_publicacion debe cumplir formato ISO-8601 YYYY-MM-DD: " + pub.get("fecha_publicacion").asText());

                assertTrue(pub.hasNonNull("caracteristicas_mascota"));
                assertTrue(pub.get("caracteristicas_mascota").isTextual());

                assertTrue(pub.hasNonNull("condicion_adopcion"));
                assertTrue(pub.get("condicion_adopcion").isTextual());

                assertTrue(pub.hasNonNull("foto"));
                assertTrue(pub.get("foto").isTextual());

                assertTrue(pub.hasNonNull("estado_publicacion"));
                String estado = pub.get("estado_publicacion").asText();
                assertTrue(List.of("Activa", "Pausada", "Finalizada").contains(estado),
                    "estado_publicacion debe ser Activa, Pausada o Finalizada pero fue: " + estado);

                // Verify nested Mascota
                assertTrue(pub.hasNonNull("mascota"), "PublicacionAdopcion contiene relacion mascota");
                JsonNode mascota = pub.get("mascota");
                // export interface Mascota {
                //   nro_reg_municipal: number;
                //   nombre: string;
                //   especie: EspecieMascota;
                //   sexo: SexoMascota;
                //   anio_nacimiento: number;
                //   microchip?: string | null;
                //   vive: boolean;
                //   id_refugio: number;
                //   raza?: string;
                // }
                assertTrue(mascota.hasNonNull("nro_reg_municipal"));
                assertEquals(pub.get("nro_reg_municipal").asInt(), mascota.get("nro_reg_municipal").asInt());

                assertTrue(mascota.hasNonNull("nombre"));
                assertTrue(mascota.get("nombre").isTextual());

                assertTrue(mascota.hasNonNull("especie"));
                assertTrue(mascota.get("especie").isTextual());

                assertTrue(mascota.hasNonNull("sexo"));
                String sexo = mascota.get("sexo").asText();
                assertTrue(List.of("Macho", "Hembra").contains(sexo), "sexo debe ser Macho o Hembra pero fue: " + sexo);

                assertTrue(mascota.hasNonNull("anio_nacimiento"));
                assertTrue(mascota.get("anio_nacimiento").isNumber());

                assertTrue(mascota.hasNonNull("vive"));
                assertTrue(mascota.get("vive").isBoolean());

                assertTrue(mascota.hasNonNull("id_refugio"));
                assertTrue(mascota.get("id_refugio").isNumber());
                assertEquals(pub.get("id_refugio").asInt(), mascota.get("id_refugio").asInt());

                // microchip and raza can be null or string
                assertTrue(mascota.has("microchip"));
                assertTrue(mascota.has("raza"));

                // Verify nested Refugio
                assertTrue(pub.hasNonNull("refugio"), "PublicacionAdopcion contiene relacion refugio");
                JsonNode refugio = pub.get("refugio");
                assertTrue(refugio.hasNonNull("id_refugio"));
                assertEquals(pub.get("id_refugio").asInt(), refugio.get("id_refugio").asInt());
                assertTrue(refugio.hasNonNull("razon_social"));
                assertTrue(refugio.hasNonNull("correo"));
                assertTrue(refugio.hasNonNull("telefono"));
                assertTrue(refugio.hasNonNull("domicilio"));
                assertTrue(refugio.hasNonNull("habilitacion_municipal"));
            }
        }
    }

    // =========================================================================
    // 3. Detail Endpoint Parity: GET /api/publicaciones/{id}
    // =========================================================================
    @Nested
    @DisplayName("3. Publicacion Detalle Contract Parity")
    class PublicacionDetalleParityTests {

        @Test
        @DisplayName("GET /api/publicaciones/{id} retorna contrato exacto para visualización en frontend")
        void testObtenerPorIdMatchesModel() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/publicaciones/1001"))
                .andExpect(status().isOk())
                .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            JsonNode pub = objectMapper.readTree(responseJson);

            assertNoCamelCaseKeys(pub, "");

            assertEquals(1001, pub.get("nro_publicacion").asInt());
            assertEquals(101, pub.get("id_refugio").asInt());
            assertEquals("Activa", pub.get("estado_publicacion").asText());
            assertTrue(ISO_DATE_PATTERN.matcher(pub.get("fecha_publicacion").asText()).matches());

            JsonNode mascota = pub.get("mascota");
            assertNotNull(mascota);
            assertEquals("Milo", mascota.get("nombre").asText());
            assertEquals(2023, mascota.get("anio_nacimiento").asInt());
            assertEquals(101, mascota.get("id_refugio").asInt());
            assertTrue(mascota.get("vive").asBoolean());

            JsonNode refugio = pub.get("refugio");
            assertNotNull(refugio);
            assertEquals(101, refugio.get("id_refugio").asInt());
            assertEquals("Asociación Protectora Huellitas Cordobesas", refugio.get("razon_social").asText());
        }
    }

    // =========================================================================
    // 4. Creation API Contract Parity: POST /api/publicaciones
    // =========================================================================
    @Nested
    @DisplayName("4. Creation API Contract Parity (NuevaPublicacionDTO -> PublicacionAdopcion)")
    class CreationParityTests {

        @Test
        @Transactional
        @DisplayName("POST /api/publicaciones con payload idéntico a frontend crea y retorna modelo consistente")
        void testCrearPublicacionConPayloadFrontend() throws Exception {
            // Frontend NuevaPublicacionDTO format:
            // {
            //   nombre: string;
            //   especie: 'Perro' | 'Gato' | 'Otro';
            //   sexo: 'Macho' | 'Hembra';
            //   anio_nacimiento: number;
            //   raza: string;
            //   microchip?: string;
            //   foto: string;
            //   caracteristicas_mascota: string;
            //   condicion_adopcion: string;
            // }
            String frontendPayload = """
                {
                    "nombre": "Simba",
                    "especie": "Gato",
                    "sexo": "Macho",
                    "anio_nacimiento": 2022,
                    "raza": "Siamés",
                    "microchip": "MC-PARITY-99001",
                    "foto": "https://img.mascotas.cba/simba.jpg",
                    "caracteristicas_mascota": "Curioso y ronroneador",
                    "condicion_adopcion": "Departamento con redes de protección"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", "101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(frontendPayload))
                .andExpect(status().isCreated())
                .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            JsonNode created = objectMapper.readTree(responseJson);

            assertNoCamelCaseKeys(created, "");

            assertTrue(created.hasNonNull("nro_publicacion"));
            assertTrue(created.get("nro_publicacion").asInt() > 0);
            assertEquals(101, created.get("id_refugio").asInt());
            assertEquals("Activa", created.get("estado_publicacion").asText());
            assertEquals("Curioso y ronroneador", created.get("caracteristicas_mascota").asText());
            assertEquals("Departamento con redes de protección", created.get("condicion_adopcion").asText());
            assertEquals("https://img.mascotas.cba/simba.jpg", created.get("foto").asText());

            JsonNode mascota = created.get("mascota");
            assertNotNull(mascota);
            assertEquals("Simba", mascota.get("nombre").asText());
            assertEquals("Gato", mascota.get("especie").asText());
            assertEquals("Macho", mascota.get("sexo").asText());
            assertEquals(2022, mascota.get("anio_nacimiento").asInt());
            assertEquals("Siamés", mascota.get("raza").asText());
            assertEquals("MC-PARITY-99001", mascota.get("microchip").asText());
            assertTrue(mascota.get("vive").asBoolean());
            assertEquals(101, mascota.get("id_refugio").asInt());

            JsonNode refugio = created.get("refugio");
            assertNotNull(refugio);
            assertEquals(101, refugio.get("id_refugio").asInt());
        }

        @Test
        @Transactional
        @DisplayName("POST /api/publicaciones sin microchip opcional asigna null y serializa sin fallos")
        void testCrearPublicacionSinMicrochip() throws Exception {
            String frontendPayload = """
                {
                    "nombre": "Luna",
                    "especie": "Perro",
                    "sexo": "Hembra",
                    "anio_nacimiento": 2024,
                    "raza": "",
                    "foto": "https://img.mascotas.cba/luna.jpg",
                    "caracteristicas_mascota": "Cachorra juguetona",
                    "condicion_adopcion": "Compromiso de vacunación"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", "101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(frontendPayload))
                .andExpect(status().isCreated())
                .andReturn();

            JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
            assertNoCamelCaseKeys(created, "");

            JsonNode mascota = created.get("mascota");
            assertTrue(mascota.get("microchip").isNull() || !mascota.has("microchip"));
            assertEquals("Mestizo", mascota.get("raza").asText(), "Raza vacía debe defaulted a 'Mestizo'");
        }
    }

    // =========================================================================
    // 5. State Transition API Contract Parity: PATCH /api/publicaciones/{id}/estado
    // =========================================================================
    @Nested
    @DisplayName("5. State Transition Contract Parity (CambioEstadoDTO -> EstadoPublicacion)")
    class StateTransitionParityTests {

        @Test
        @Transactional
        @DisplayName("PATCH /api/publicaciones/{id}/estado con payload { nuevo_estado } ejecuta transiciones y retorna respuesta válida")
        void testPatchEstadoLifecycleParity() throws Exception {
            // 1. Pausar la publicación 1001 (Activa -> Pausada)
            String patchPausar = "{\"nuevo_estado\": \"Pausada\"}";
            MvcResult respPausada = mockMvc.perform(patch("/api/publicaciones/1001/estado")
                    .header("X-Refugio-Id", "101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(patchPausar))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode jsonPausada = objectMapper.readTree(respPausada.getResponse().getContentAsString());
            assertNoCamelCaseKeys(jsonPausada, "");
            assertEquals("Pausada", jsonPausada.get("estado_publicacion").asText());

            // 2. Reactivar la publicación 1001 (Pausada -> Activa)
            String patchActivar = "{\"nuevo_estado\": \"Activa\"}";
            MvcResult respActiva = mockMvc.perform(patch("/api/publicaciones/1001/estado")
                    .header("X-Refugio-Id", "101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(patchActivar))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode jsonActiva = objectMapper.readTree(respActiva.getResponse().getContentAsString());
            assertNoCamelCaseKeys(jsonActiva, "");
            assertEquals("Activa", jsonActiva.get("estado_publicacion").asText());

            // 3. Finalizar la publicación 1001 (Activa -> Finalizada)
            String patchFinalizar = "{\"nuevo_estado\": \"Finalizada\"}";
            MvcResult respFinalizada = mockMvc.perform(patch("/api/publicaciones/1001/estado")
                    .header("X-Refugio-Id", "101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(patchFinalizar))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode jsonFinalizada = objectMapper.readTree(respFinalizada.getResponse().getContentAsString());
            assertNoCamelCaseKeys(jsonFinalizada, "");
            assertEquals("Finalizada", jsonFinalizada.get("estado_publicacion").asText());
        }

        @Test
        @Transactional
        @DisplayName("PATCH acepta alias 'estado' alternativo a 'nuevo_estado'")
        void testPatchEstadoConAlias() throws Exception {
            String patchAlias = "{\"estado\": \"Pausada\"}";
            MvcResult resp = mockMvc.perform(patch("/api/publicaciones/1001/estado")
                    .header("X-Refugio-Id", "101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(patchAlias))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode json = objectMapper.readTree(resp.getResponse().getContentAsString());
            assertEquals("Pausada", json.get("estado_publicacion").asText());
        }
    }

    // =========================================================================
    // 6. Cross-Entity Edge Cases and Deserialization Resiliency
    // =========================================================================
    @Nested
    @DisplayName("6. Edge Cases & Resilience")
    class ResilienceParityTests {

        @Test
        @DisplayName("Deserializar entidades completas en Cliente HTTP TypeScript emulado no lanza errores")
        void testRoundTripDeserializationFidelity() throws Exception {
            PublicacionAdopcion pub = publicacionRepository.findByIdWithDetails(1001).orElseThrow();
            String json = objectMapper.writeValueAsString(pub);

            // Verify standard generic Map parse
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            assertEquals(1001, map.get("nro_publicacion"));
            assertEquals(101, map.get("id_refugio"));
            assertNotNull(map.get("fecha_publicacion"));
            assertNotNull(map.get("mascota"));
            assertNotNull(map.get("refugio"));

            // Verify nested map
            @SuppressWarnings("unchecked")
            Map<String, Object> mascotaMap = (Map<String, Object>) map.get("mascota");
            assertEquals("Milo", mascotaMap.get("nombre"));
            assertEquals(101, mascotaMap.get("id_refugio"));
            assertEquals(2023, mascotaMap.get("anio_nacimiento"));
            assertTrue((Boolean) mascotaMap.get("vive"));
        }
    }
}
