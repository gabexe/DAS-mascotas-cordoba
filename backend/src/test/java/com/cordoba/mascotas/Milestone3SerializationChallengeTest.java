package com.cordoba.mascotas;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.publicaciones.MascotaRepository;
import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
import com.cordoba.mascotas.publicaciones.PublicacionAdopcionRepository;
import com.cordoba.mascotas.publicaciones.RefugioRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Empirical Challenge: Milestone 3 Direct Jackson Serialization & Invariants")
class Milestone3SerializationChallengeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private CiudadanoRepository ciudadanoRepository;

    @Autowired
    private RefugioRepository refugioRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private PublicacionAdopcionRepository publicacionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================================
    // SECTION 1: Ciudadano.clave is NEVER exposed in JSON output
    // =========================================================================
    @Nested
    @DisplayName("Challenge 1: Ciudadano.clave Zero-Exposure Verification")
    class ClaveZeroExposureChallenge {

        @Test
        @DisplayName("POST /api/auth/login response over HTTP wire NEVER contains 'clave' or password value")
        void testLoginEndpointDoesNotExposeClaveInHttpPayload() throws Exception {
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

            String responseBody = result.getResponse().getContentAsString();
            assertNotNull(responseBody, "Response body must not be null");

            JsonNode root = objectMapper.readTree(responseBody);

            // Verificación estructural estricta
            assertFalse(root.has("clave"), "The JSON root must not contain a 'clave' property");
            assertFalse(responseBody.contains("\"clave\""), "Raw response must not contain '\"clave\"'");
            // "1234" is a substring of CUIL "20-12345678-9", so verify it is not present as a password value
            assertFalse(responseBody.contains(":\"1234\""), "Password must not appear as field value");
            assertFalse(responseBody.contains(": \"1234\""), "Password must not appear as field value with whitespace");

            // Verificación de campos legítimos requeridos
            assertTrue(root.has("id_ciudadano"), "Must contain id_ciudadano");
            assertTrue(root.has("cuil"), "Must contain cuil");
            assertTrue(root.has("nombre"), "Must contain nombre");
            assertTrue(root.has("apellido"), "Must contain apellido");
            assertTrue(root.has("correo"), "Must contain correo");
            assertTrue(root.has("id_refugio"), "Must contain id_refugio");
            assertTrue(root.has("refugio"), "Must contain refugio");
        }

        @Test
        @DisplayName("POST /api/auth/login failure with adversarial password NEVER echoes password back")
        void testLoginFailureDoesNotEchoAdversarialPasswords() throws Exception {
            String[] maliciousPasswords = {
                "SecretPass123!@#",
                "' OR '1'='1' --",
                "<script>alert('xss')</script>",
                "SuperLongPasswordToTestBufferLeaksAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            };

            for (String evilPass : maliciousPasswords) {
                String payload = String.format("""
                    {
                        "cuil": "20-12345678-9",
                        "clave": "%s"
                    }
                    """, evilPass);

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                assertFalse(responseBody.contains(evilPass),
                    "Failed login response must never echo the attempted password: " + evilPass);
                assertFalse(responseBody.contains("\"clave\""),
                    "Failed login response must never contain '\"clave\"'");
            }
        }

        @Test
        @DisplayName("Direct serialization of Ciudadano loaded from real SQL Server does NOT serialize clave")
        void testDirectSerializationOfRealCiudadano() throws Exception {
            Ciudadano dbCiudadano = ciudadanoRepository.findByCuil("20-12345678-9")
                .orElseThrow(() -> new AssertionError("Seeded Ciudadano must exist"));

            // La entidad tiene clave no nula
            assertNotNull(dbCiudadano.getClave(), "Ciudadano in DB has a password");

            String json = objectMapper.writeValueAsString(dbCiudadano);
            JsonNode tree = objectMapper.readTree(json);

            assertFalse(tree.has("clave"));
            assertFalse(json.contains("\"clave\""));
            assertFalse(json.contains(":\"" + dbCiudadano.getClave() + "\""));
            assertFalse(json.contains(": \"" + dbCiudadano.getClave() + "\""));
        }

        @Test
        @DisplayName("Nested serialization of Ciudadano in complex maps/lists NEVER exposes clave")
        void testNestedCiudadanoSerialization() throws Exception {
            Ciudadano c = Ciudadano.builder()
                .idCiudadano(42)
                .cuil("20-42424242-4")
                .clave("CriticalSecretKey_XYZ_999")
                .nombre("Admin")
                .apellido("Sistema")
                .correo("admin@cba.gov.ar")
                .build();

            Map<String, Object> nested = Map.of(
                "status", "success",
                "data", Map.of(
                    "actor", c,
                    "history", List.of(c)
                )
            );

            String json = objectMapper.writeValueAsString(nested);
            assertFalse(json.contains("CriticalSecretKey_XYZ_999"));
            assertFalse(json.contains("\"clave\""));
        }

        @Test
        @DisplayName("Jackson BeanPropertyDefinition introspection proves 'clave' is excluded from serialization")
        void testJacksonIntrospectionExcludesClave() {
            JavaType javaType = objectMapper.getTypeFactory().constructType(Ciudadano.class);
            BeanDescription beanDesc = objectMapper.getSerializationConfig().introspect(javaType);

            List<BeanPropertyDefinition> properties = beanDesc.findProperties();
            for (BeanPropertyDefinition prop : properties) {
                assertFalse("clave".equalsIgnoreCase(prop.getName()),
                    "Jackson serialization properties must not include 'clave': found " + prop.getName());
            }
        }

        @Test
        @DisplayName("Ciudadano inside Mascota relation does NOT expose clave (Mascota.responsable is @JsonIgnore)")
        void testCiudadanoInMascotaRelationDoesNotExposeClave() throws Exception {
            Ciudadano responsable = Ciudadano.builder()
                .idCiudadano(77)
                .cuil("20-77777777-7")
                .clave("ResponsablePrivatePassword123")
                .nombre("Responsable")
                .apellido("Refugio")
                .build();

            Mascota mascota = Mascota.builder()
                .nroRegMunicipal(888)
                .nombre("Bobby")
                .especie("Canina")
                .sexo("Macho")
                .anioNacimiento(2022)
                .vive(true)
                .responsable(responsable)
                .build();

            String json = objectMapper.writeValueAsString(mascota);
            assertFalse(json.contains("ResponsablePrivatePassword123"));
            assertFalse(json.contains("responsable"));
            assertFalse(json.contains("clave"));
        }
    }

    // =========================================================================
    // SECTION 2: Zero LazyInitializationException with open-in-view: false
    // =========================================================================
    @Nested
    @DisplayName("Challenge 2: LazyInitializationException Resilience (open-in-view: false)")
    class OpenInViewLazyChallenge {

        @Test
        @DisplayName("Verify spring.jpa.open-in-view is explicitly FALSE")
        void testVerifyOpenInViewIsFalse() {
            String openInView = environment.getProperty("spring.jpa.open-in-view");
            assertEquals("false", openInView, "spring.jpa.open-in-view must be configured as 'false'");
        }

        @Test
        @DisplayName("GET /api/publicaciones serializes all entities cleanly over HTTP without LazyInitializationException")
        void testHttpListarPublicacionesOutsideSession() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/publicaciones"))
                .andExpect(status().isOk())
                .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);

            assertTrue(root.isArray(), "Output must be a JSON array");
            assertTrue(root.size() >= 5, "Seeded database should contain at least 5 publications");

            for (JsonNode item : root) {
                assertTrue(item.has("nro_publicacion"));
                assertTrue(item.has("mascota"));
                assertTrue(item.has("refugio"));
                assertTrue(item.has("nro_reg_municipal"));
                assertTrue(item.has("id_refugio"));
            }
        }

        @Test
        @DisplayName("GET /api/publicaciones/{id} serializes single entity cleanly over HTTP without LazyInitializationException")
        void testHttpGetPublicacionByIdOutsideSession() throws Exception {
            Integer samplePubId = publicacionRepository.findAll().get(0).getNroPublicacion();
            MvcResult result = mockMvc.perform(get("/api/publicaciones/" + samplePubId))
                .andExpect(status().isOk())
                .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);

            assertEquals(samplePubId, root.get("nro_publicacion").asInt());
            assertTrue(root.has("mascota"));
            assertTrue(root.has("refugio"));
            assertTrue(root.has("nro_reg_municipal"));
            assertTrue(root.has("id_refugio"));
        }

        @Test
        @DisplayName("POST /api/publicaciones returns 201 CREATED serialized outside transaction without LazyInitializationException")
        void testHttpPostPublicacionOutsideSession() throws Exception {
            String chipUnique = "MC-CHALLENGE-" + System.currentTimeMillis();
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101,
                "Challenger Dog",
                "Perro",
                "Macho",
                2023,
                "Criollo",
                chipUnique,
                "data:image/jpeg;base64,challengeTestImage",
                "Muy amigable y juguetón",
                "Casa con patio cerrado"
            );

            MvcResult result = mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);

            assertTrue(root.has("nro_publicacion"));
            assertTrue(root.has("mascota"));
            assertTrue(root.has("refugio"));
            assertEquals(101, root.get("id_refugio").asInt());
            assertEquals("Challenger Dog", root.get("mascota").get("nombre").asText());
        }

        @Test
        @DisplayName("PATCH /api/publicaciones/{id}/estado returns 200 OK serialized outside transaction without LazyInitializationException")
        void testHttpPatchPublicacionOutsideSession() throws Exception {
            // First create a new publication to transition safely
            String chip = "MC-PATCH-" + System.currentTimeMillis();
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                101,
                "Transition Dog",
                "Perro",
                "Macho",
                2022,
                "Mestizo",
                chip,
                "data:image/png;base64,image",
                "Tranquilo",
                "Familia responsable"
            );

            MvcResult createResult = mockMvc.perform(post("/api/publicaciones")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

            JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
            int nroPub = createdJson.get("nro_publicacion").asInt();

            // Patch to Pausada
            String patchBody = """
                {
                    "nuevo_estado": "Pausada"
                }
                """;

            MvcResult patchResult = mockMvc.perform(patch("/api/publicaciones/" + nroPub + "/estado")
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(patchBody))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode patchedJson = objectMapper.readTree(patchResult.getResponse().getContentAsString());
            assertEquals("Pausada", patchedJson.get("estado_publicacion").asText());
            assertTrue(patchedJson.has("mascota"));
            assertTrue(patchedJson.has("refugio"));
            assertTrue(patchedJson.has("nro_reg_municipal"));
            assertTrue(patchedJson.has("id_refugio"));
        }

        @Test
        @DisplayName("Mascota with uninitialized LAZY Refugio and Responsable proxies serializes with ZERO LazyInitializationException")
        void testMascotaWithUninitializedProxiesSerializesWithoutException() {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            // Fetch entity in a transaction and close it immediately
            Mascota detachedMascota = txTemplate.execute(status -> {
                List<Mascota> list = mascotaRepository.findAll();
                assertFalse(list.isEmpty(), "Mascota list must not be empty");
                Mascota m = list.get(0);
                // Ensure refugio proxy is not initialized
                assertFalse(Hibernate.isInitialized(m.getRefugio()), "Proxy of Refugio must be uninitialized");
                return m;
            });

            // Transaction is closed, session is terminated.
            // Serialize detached entity outside session.
            String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(detachedMascota),
                "Serializing Mascota with uninitialized proxies outside JPA session must NOT throw LazyInitializationException");

            assertNotNull(json);
            assertDoesNotThrow(() -> {
                JsonNode tree = objectMapper.readTree(json);
                assertTrue(tree.has("id_refugio"), "Must serialize id_refugio from uninitialized proxy");
                assertNotNull(tree.get("id_refugio").asInt());
                assertFalse(tree.has("refugio"), "LAZY refugio object must be ignored");
                assertFalse(tree.has("responsable"), "LAZY responsable object must be ignored");
            });
        }

        @Test
        @DisplayName("PublicacionAdopcion loaded via standard findById (non-fetch-join) serializes with ZERO LazyInitializationException")
        void testPublicacionLoadedViaStandardFindByIdSerializesWithoutException() {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            PublicacionAdopcion detachedPub = txTemplate.execute(status -> {
                List<PublicacionAdopcion> pubs = publicacionRepository.findAll();
                assertFalse(pubs.isEmpty(), "Publications must exist");
                return publicacionRepository.findById(pubs.get(0).getNroPublicacion()).orElseThrow();
            });

            // Outside transaction and session
            String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(detachedPub),
                "Serializing PublicacionAdopcion loaded via findById outside JPA session must NOT throw LazyInitializationException");

            assertNotNull(json);
            assertDoesNotThrow(() -> {
                JsonNode tree = objectMapper.readTree(json);
                assertTrue(tree.has("nro_publicacion"));
                assertTrue(tree.has("nro_reg_municipal"));
                assertTrue(tree.has("id_refugio"));
                assertTrue(tree.has("mascota"));
                assertTrue(tree.has("refugio"));
            });
        }

        @Test
        @DisplayName("Concurrent stress: 10 threads serializing detached PublicacionAdopcion outside JPA session")
        void testConcurrentStressSerializationOutsideSession() throws Exception {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            List<PublicacionAdopcion> detachedList = txTemplate.execute(status ->
                publicacionRepository.buscarPublicaciones(null, null, null)
            );
            assertNotNull(detachedList);
            assertFalse(detachedList.isEmpty());

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Callable<Boolean>> tasks = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                tasks.add(() -> {
                    for (PublicacionAdopcion pub : detachedList) {
                        String json = objectMapper.writeValueAsString(pub);
                        if (!json.contains("\"nro_publicacion\"") || !json.contains("\"id_refugio\"")) {
                            return false;
                        }
                    }
                    return true;
                });
            }

            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            for (Future<Boolean> f : futures) {
                assertTrue(f.get(), "Each thread must complete serialization without error");
            }
        }
    }

    // =========================================================================
    // SECTION 3: Presence of nro_reg_municipal and id_refugio in PublicacionAdopcion JSON
    // =========================================================================
    @Nested
    @DisplayName("Challenge 3: Presence of nro_reg_municipal and id_refugio Contract Parity")
    class FieldPresenceChallenge {

        @Test
        @DisplayName("PublicacionAdopcion JSON has nro_reg_municipal and id_refugio at ROOT level matching nested values")
        void testPublicacionJsonHasRootAndNestedIdentifiers() throws Exception {
            Integer samplePubId = publicacionRepository.findAll().get(0).getNroPublicacion();
            MvcResult result = mockMvc.perform(get("/api/publicaciones/" + samplePubId))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

            // 1. Root level identifiers
            assertTrue(root.has("nro_reg_municipal"), "Root JSON must contain 'nro_reg_municipal'");
            assertTrue(root.has("id_refugio"), "Root JSON must contain 'id_refugio'");

            int rootNroReg = root.get("nro_reg_municipal").asInt();
            int rootIdRefugio = root.get("id_refugio").asInt();
            assertTrue(rootNroReg > 0, "nro_reg_municipal must be positive integer");
            assertTrue(rootIdRefugio > 0, "id_refugio must be positive integer");

            // 2. Nested mascota identifiers
            assertTrue(root.has("mascota"), "Root JSON must contain nested 'mascota'");
            JsonNode mascotaNode = root.get("mascota");
            assertTrue(mascotaNode.has("nro_reg_municipal"), "Nested mascota must contain 'nro_reg_municipal'");
            assertTrue(mascotaNode.has("id_refugio"), "Nested mascota must contain 'id_refugio'");

            assertEquals(rootNroReg, mascotaNode.get("nro_reg_municipal").asInt(),
                "Root 'nro_reg_municipal' must match 'mascota.nro_reg_municipal'");
            assertEquals(rootIdRefugio, mascotaNode.get("id_refugio").asInt(),
                "Root 'id_refugio' must match 'mascota.id_refugio'");

            // 3. Nested refugio identifiers
            assertTrue(root.has("refugio"), "Root JSON must contain nested 'refugio'");
            JsonNode refugioNode = root.get("refugio");
            assertTrue(refugioNode.has("id_refugio"), "Nested refugio must contain 'id_refugio'");
            assertEquals(rootIdRefugio, refugioNode.get("id_refugio").asInt(),
                "Root 'id_refugio' must match 'refugio.id_refugio'");
        }

        @Test
        @DisplayName("Every item in GET /api/publicaciones array contains nro_reg_municipal and id_refugio")
        void testEveryPublicationInListHasIdentifiers() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/publicaciones"))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(root.isArray());

            for (int i = 0; i < root.size(); i++) {
                JsonNode item = root.get(i);
                assertTrue(item.has("nro_reg_municipal"), "Item " + i + " must contain 'nro_reg_municipal'");
                assertTrue(item.has("id_refugio"), "Item " + i + " must contain 'id_refugio'");
                assertFalse(item.get("nro_reg_municipal").isNull(), "nro_reg_municipal must not be null in active publication");
                assertFalse(item.get("id_refugio").isNull(), "id_refugio must not be null in active publication");
            }
        }

        @Test
        @DisplayName("Null safety in getter methods when relationships are null")
        void testNullSafetyInDynamicGetters() {
            // Publicacion with null mascota and refugio
            PublicacionAdopcion emptyPub = new PublicacionAdopcion();
            assertNull(emptyPub.getNroRegMunicipal(), "getNroRegMunicipal() must return null when mascota is null");
            assertNull(emptyPub.getIdRefugio(), "getIdRefugio() must return null when refugio is null");

            assertDoesNotThrow(() -> {
                String json = objectMapper.writeValueAsString(emptyPub);
                JsonNode tree = objectMapper.readTree(json);
                assertTrue(tree.has("nro_reg_municipal"));
                assertTrue(tree.get("nro_reg_municipal").isNull());
                assertTrue(tree.has("id_refugio"));
                assertTrue(tree.get("id_refugio").isNull());
            });

            // Mascota with null refugio
            Mascota emptyMascota = new Mascota();
            assertNull(emptyMascota.getIdRefugio(), "Mascota.getIdRefugio() must return null when refugio is null");
            assertDoesNotThrow(() -> {
                String json = objectMapper.writeValueAsString(emptyMascota);
                JsonNode tree = objectMapper.readTree(json);
                assertTrue(tree.has("id_refugio"));
                assertTrue(tree.get("id_refugio").isNull());
            });

            // Ciudadano with null refugio
            Ciudadano emptyCiudadano = new Ciudadano();
            assertNull(emptyCiudadano.getIdRefugio(), "Ciudadano.getIdRefugio() must return null when refugio is null");
            assertDoesNotThrow(() -> {
                String json = objectMapper.writeValueAsString(emptyCiudadano);
                JsonNode tree = objectMapper.readTree(json);
                assertTrue(tree.has("id_refugio"));
                assertTrue(tree.get("id_refugio").isNull());
            });
        }
    }

    // =========================================================================
    // SECTION 4: Schema Cleanliness, No camelCase, No Duplicates
    // =========================================================================
    @Nested
    @DisplayName("Challenge 4: Strict Schema Cleanliness & snake_case Parity")
    class SchemaCleanlinessChallenge {

        @Test
        @DisplayName("Zero camelCase keys in PublicacionAdopcion JSON tree (root, mascota, refugio)")
        void testZeroCamelCaseKeysInPublicacionJson() throws Exception {
            Integer samplePubId = publicacionRepository.findAll().get(0).getNroPublicacion();
            MvcResult result = mockMvc.perform(get("/api/publicaciones/" + samplePubId))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertNoCamelCaseKeys(root, "$");
        }

        @Test
        @DisplayName("Zero duplicate keys in PublicacionAdopcion serialized JSON")
        void testNoDuplicateKeysInPublicacionSerialization() throws Exception {
            Integer samplePubId = publicacionRepository.findAll().get(0).getNroPublicacion();
            MvcResult result = mockMvc.perform(get("/api/publicaciones/" + samplePubId))
                .andExpect(status().isOk())
                .andReturn();

            String rawJson = result.getResponse().getContentAsString();
            assertNoDuplicateKeysInJson(rawJson);
        }

        @Test
        @DisplayName("Zero duplicate keys in Ciudadano serialized JSON")
        void testNoDuplicateKeysInCiudadanoSerialization() throws Exception {
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

            String rawJson = result.getResponse().getContentAsString();
            assertNoDuplicateKeysInJson(rawJson);
        }

        @Test
        @DisplayName("Hibernate internal proxy properties (hibernateLazyInitializer, handler) are NEVER exposed")
        void testNoHibernateInternalPropertiesExposed() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/publicaciones"))
                .andExpect(status().isOk())
                .andReturn();

            String rawJson = result.getResponse().getContentAsString();
            assertFalse(rawJson.contains("hibernateLazyInitializer"),
                "JSON must never contain 'hibernateLazyInitializer'");
            assertFalse(rawJson.contains("\"handler\""),
                "JSON must never contain '\"handler\"'");
        }

        @Test
        @DisplayName("Date 'fecha_publicacion' is formatted as ISO string (YYYY-MM-DD), NOT timestamp array or number")
        void testDateFormattingIsIsoString() throws Exception {
            Integer samplePubId = publicacionRepository.findAll().get(0).getNroPublicacion();
            MvcResult result = mockMvc.perform(get("/api/publicaciones/" + samplePubId))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(root.has("fecha_publicacion"));
            JsonNode dateNode = root.get("fecha_publicacion");
            assertTrue(dateNode.isTextual(), "fecha_publicacion must be a string");
            assertTrue(dateNode.asText().matches("^\\d{4}-\\d{2}-\\d{2}$"),
                "fecha_publicacion must match format YYYY-MM-DD: " + dateNode.asText());
        }

        private void assertNoCamelCaseKeys(JsonNode node, String path) {
            if (node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    // Chequear que no contenga letras mayúsculas (camelCase)
                    boolean hasUppercase = !key.equals(key.toLowerCase());
                    assertFalse(hasUppercase,
                        String.format("Found forbidden camelCase key '%s' at path '%s'", key, path));
                    assertNoCamelCaseKeys(field.getValue(), path + "." + key);
                }
            } else if (node.isArray()) {
                for (int i = 0; i < node.size(); i++) {
                    assertNoCamelCaseKeys(node.get(i), path + "[" + i + "]");
                }
            }
        }

        private void assertNoDuplicateKeysInJson(String json) throws Exception {
            try (JsonParser parser = objectMapper.createParser(json)) {
                List<Set<String>> scopeStack = new ArrayList<>();

                while (!parser.isClosed()) {
                    JsonToken token = parser.nextToken();
                    if (token == null) break;

                    if (token == JsonToken.START_OBJECT) {
                        scopeStack.add(new HashSet<>());
                    } else if (token == JsonToken.END_OBJECT) {
                        if (!scopeStack.isEmpty()) {
                            scopeStack.remove(scopeStack.size() - 1);
                        }
                    } else if (token == JsonToken.FIELD_NAME) {
                        String fieldName = parser.currentName();
                        Set<String> currentScope = scopeStack.get(scopeStack.size() - 1);
                        assertFalse(currentScope.contains(fieldName),
                            "Duplicate key detected in JSON object: '" + fieldName + "'");
                        currentScope.add(fieldName);
                    }
                }
            }
        }
    }
}
