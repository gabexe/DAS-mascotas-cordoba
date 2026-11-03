package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.publicaciones.MascotaRepository;
import com.cordoba.mascotas.publicaciones.PublicacionAdopcionRepository;
import com.cordoba.mascotas.publicaciones.RefugioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DomainSerializationChallengeTest {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================================
    // CHALLENGE 1: Ciudadano.clave is NEVER serialized in JSON
    // =========================================================================
    @Nested
    @DisplayName("Challenge 1: Invariante de Seguridad - Sanitización de 'clave' en Ciudadano")
    class ClaveSanitizationChallenge {

        @Test
        @DisplayName("Ciudadano.clave no se serializa jamás con ObjectMapper inyectado (Spring MVC)")
        void testClaveNeverSerializedWithSpringObjectMapper() throws Exception {
            Ciudadano ciudadano = Ciudadano.builder()
                .idCiudadano(99)
                .cuil("20-99999999-9")
                .clave("SuperSecretPassword123#!")
                .nombre("Juan")
                .apellido("Pérez")
                .correo("juan@test.com")
                .telefono("351111222")
                .domicilio("Calle Falsa 123")
                .habilitado(true)
                .build();

            String json = objectMapper.writeValueAsString(ciudadano);
            JsonNode root = objectMapper.readTree(json);

            assertFalse(root.has("clave"), "El campo 'clave' no debe existir en el JSON");
            assertFalse(json.contains("SuperSecretPassword123#!"), "El valor de la clave no debe aparecer en el JSON");
        }

        @Test
        @DisplayName("Ciudadano.clave no se serializa jamás con vanilla new ObjectMapper()")
        void testClaveNeverSerializedWithVanillaObjectMapper() throws Exception {
            ObjectMapper vanillaMapper = new ObjectMapper();
            Ciudadano ciudadano = Ciudadano.builder()
                .idCiudadano(99)
                .cuil("20-99999999-9")
                .clave("VanillaSecretPassword456$")
                .nombre("María")
                .apellido("Gómez")
                .correo("maria@test.com")
                .habilitado(true)
                .build();

            String json = vanillaMapper.writeValueAsString(ciudadano);
            JsonNode root = vanillaMapper.readTree(json);

            assertFalse(root.has("clave"), "El campo 'clave' no debe existir en vanilla ObjectMapper");
            assertFalse(json.contains("VanillaSecretPassword456$"), "El valor de la clave no debe aparecer en el JSON");
        }

        @Test
        @DisplayName("Ciudadano real cargado desde SQL Server no expone clave al serializarse")
        void testRealCiudadanoFromDatabaseDoesNotExposeClave() throws Exception {
            Optional<Ciudadano> opt = ciudadanoRepository.findByCuil("20-12345678-9");
            assertTrue(opt.isPresent(), "Debe existir el usuario sembrado con CUIL 20-12345678-9");
            Ciudadano ciudadano = opt.get();

            // La clave existe en la entidad
            // Jackson NUNCA la serializa
            String json = objectMapper.writeValueAsString(ciudadano);
            JsonNode root = objectMapper.readTree(json);

            assertFalse(root.has("clave"), "El campo 'clave' no debe existir en el JSON");
            assertFalse(json.contains("\"clave\""), "La clave JSON 'clave' no debe existir en el texto serializado");
            // Verificar que ningún valor en el JSON coincida con un campo de clave
            assertFalse(json.contains(":\"" + ciudadano.getClave() + "\""), "El valor de la clave no debe ser serializado como valor de campo");
        }

        @Test
        @DisplayName("Serialización de lista o estructura anidada que contiene Ciudadano no expone clave")
        void testNestedAndListSerializationDoesNotExposeClave() throws Exception {
            Ciudadano c1 = Ciudadano.builder().idCiudadano(1).clave("SecretA").cuil("20-1").build();
            Ciudadano c2 = Ciudadano.builder().idCiudadano(2).clave("SecretB").cuil("20-2").build();

            Map<String, Object> wrapper = Map.of("usuarios", List.of(c1, c2));
            String json = objectMapper.writeValueAsString(wrapper);

            assertFalse(json.contains("clave"));
            assertFalse(json.contains("SecretA"));
            assertFalse(json.contains("SecretB"));
        }
    }

    // =========================================================================
    // CHALLENGE 2: LazyInitializationException resilience outside JPA session
    // =========================================================================
    @Nested
    @DisplayName("Challenge 2: Resiliencia a LazyInitializationException (open-in-view: false)")
    class LazyInitializationResilienceChallenge {

        @Test
        @DisplayName("Serializar Mascota con proxy LAZY no inicializado fuera de sesión NO lanza LazyInitializationException")
        void testSerializingMascotaOutsideSessionWithUninitializedProxy() throws Exception {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            // 1. En una transacción aislada, recuperamos o creamos una mascota
            Integer nroReg = txTemplate.execute(status -> {
                List<Mascota> mascotas = mascotaRepository.findAll();
                if (!mascotas.isEmpty()) {
                    return mascotas.get(0).getNroRegMunicipal();
                }
                return null;
            });

            assertNotNull(nroReg, "Debe existir al menos una mascota en la base de datos");

            // 2. Cargamos Mascota en una transacción que se cierra inmediatamente
            Mascota mascotaDetached = txTemplate.execute(status -> {
                Mascota m = mascotaRepository.findById(nroReg).orElseThrow();
                // Verificamos que las relaciones LAZY NO estén inicializadas
                assertFalse(Hibernate.isInitialized(m.getRefugio()), "El proxy de Refugio debe estar sin inicializar");
                return m;
            });

            // 3. La transacción ya finalizó y la sesión de Hibernate está CERRADA.
            // Ahora serializamos Mascota con Jackson directamente
            String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(mascotaDetached),
                "Serializar Mascota fuera de sesión JPA NO debe arrojar LazyInitializationException");

            assertNotNull(json);
            JsonNode root = objectMapper.readTree(json);

            // 4. Verificamos que id_refugio se haya serializado correctamente
            assertTrue(root.has("id_refugio"));
            assertNotNull(root.get("id_refugio").asInt());

            // 5. Verificamos que las relaciones LAZY no se hayan serializado (ocultas vía @JsonIgnore)
            assertFalse(root.has("refugio"), "El objeto complejo 'refugio' no debe estar presente en Mascota serializada");
            assertFalse(root.has("responsable"), "El objeto 'responsable' no debe estar presente en Mascota serializada");
        }

        @Test
        @DisplayName("Serializar PublicacionAdopcion fuera de sesión JPA NO lanza LazyInitializationException")
        void testSerializingPublicacionOutsideSession() throws Exception {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            // 1. Obtener ID de una publicación sembrada
            Integer nroPub = txTemplate.execute(status -> {
                List<PublicacionAdopcion> pubs = publicacionRepository.findAll();
                if (!pubs.isEmpty()) {
                    return pubs.get(0).getNroPublicacion();
                }
                return null;
            });

            assertNotNull(nroPub, "Debe existir al menos una publicación en la base de datos");

            // 2. Cargar publicación en una transacción que se cierra
            PublicacionAdopcion pubDetached = txTemplate.execute(status -> {
                return publicacionRepository.findById(nroPub).orElseThrow();
            });

            // 3. Serializar publicación fuera de sesión
            String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(pubDetached),
                "Serializar PublicacionAdopcion fuera de sesión JPA NO debe arrojar LazyInitializationException");

            assertNotNull(json);
            JsonNode root = objectMapper.readTree(json);
            assertTrue(root.has("nro_publicacion"));
            assertTrue(root.has("mascota"));
            assertTrue(root.has("refugio"));
            assertTrue(root.has("id_refugio"));
            assertTrue(root.has("nro_reg_municipal"));
        }

        @Test
        @DisplayName("Serializar lista de Publicaciones obtenidas por buscarPublicaciones fuera de sesión")
        void testSerializingPublicacionesListOutsideSession() throws Exception {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

            List<PublicacionAdopcion> listDetached = txTemplate.execute(status -> {
                return publicacionRepository.buscarPublicaciones(null, null, null);
            });

            assertNotNull(listDetached);
            assertFalse(listDetached.isEmpty());

            String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(listDetached),
                "Serializar lista de publicaciones fuera de sesión NO debe fallar");

            JsonNode arrayNode = objectMapper.readTree(json);
            assertTrue(arrayNode.isArray());
            assertTrue(arrayNode.size() >= 1);
        }
    }

    // =========================================================================
    // CHALLENGE 3: snake_case parity across all 4 entities
    // =========================================================================
    @Nested
    @DisplayName("Challenge 3: Paridad Estricta de Serialización snake_case")
    class SnakeCaseParityChallenge {

        @Test
        @DisplayName("Ciudadano serializa todas las propiedades en snake_case canónico sin duplicados camelCase")
        void testCiudadanoSnakeCaseParity() throws Exception {
            Ciudadano ciudadano = Ciudadano.builder()
                .idCiudadano(1)
                .cuil("20-12345678-9")
                .clave("hidden")
                .nombre("Carlos")
                .apellido("Pérez")
                .correo("carlos@refugio.org")
                .telefono("351111222")
                .domicilio("Colón 100")
                .habilitado(true)
                .refugio(Refugio.builder().idRefugio(10).razonSocial("Refugio Test").build())
                .build();

            String json = objectMapper.writeValueAsString(ciudadano);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            // Claves esperadas en snake_case
            assertTrue(map.containsKey("id_ciudadano"), "Debe contener id_ciudadano");
            assertTrue(map.containsKey("cuil"), "Debe contener cuil");
            assertTrue(map.containsKey("nombre"), "Debe contener nombre");
            assertTrue(map.containsKey("apellido"), "Debe contener apellido");
            assertTrue(map.containsKey("correo"), "Debe contener correo");
            assertTrue(map.containsKey("telefono"), "Debe contener telefono");
            assertTrue(map.containsKey("domicilio"), "Debe contener domicilio");
            assertTrue(map.containsKey("habilitado"), "Debe contener habilitado");
            assertTrue(map.containsKey("id_refugio"), "Debe contener id_refugio");
            assertTrue(map.containsKey("refugio"), "Debe contener refugio");

            // PROHIBICIONES: ningún camelCase o clave interna
            assertFalse(map.containsKey("idCiudadano"), "NO debe contener camelCase idCiudadano");
            assertFalse(map.containsKey("idRefugio"), "NO debe contener camelCase idRefugio");
            assertFalse(map.containsKey("estaHabilitado"), "NO debe contener método de dominio estaHabilitado");
            assertFalse(map.containsKey("clave"), "NO debe contener clave");
        }

        @Test
        @DisplayName("Refugio serializa todas las propiedades en snake_case canónico sin duplicados camelCase")
        void testRefugioSnakeCaseParity() throws Exception {
            Refugio refugio = Refugio.builder()
                .idRefugio(101)
                .razonSocial("Refugio Esperanza")
                .correo("contacto@esperanza.org")
                .telefono("3514445566")
                .domicilio("Av. Colón 1234")
                .habilitacionMunicipal("HAB-2024-001")
                .idResponsable(1)
                .build();

            String json = objectMapper.writeValueAsString(refugio);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            assertTrue(map.containsKey("id_refugio"), "Debe contener id_refugio");
            assertTrue(map.containsKey("razon_social"), "Debe contener razon_social");
            assertTrue(map.containsKey("correo"), "Debe contener correo");
            assertTrue(map.containsKey("telefono"), "Debe contener telefono");
            assertTrue(map.containsKey("domicilio"), "Debe contener domicilio");
            assertTrue(map.containsKey("habilitacion_municipal"), "Debe contener habilitacion_municipal");
            assertTrue(map.containsKey("id_responsable"), "Debe contener id_responsable");

            // PROHIBICIONES
            assertFalse(map.containsKey("idRefugio"), "NO debe contener camelCase idRefugio");
            assertFalse(map.containsKey("razonSocial"), "NO debe contener camelCase razonSocial");
            assertFalse(map.containsKey("habilitacionMunicipal"), "NO debe contener camelCase habilitacionMunicipal");
            assertFalse(map.containsKey("idResponsable"), "NO debe contener camelCase idResponsable");
            assertFalse(map.containsKey("tieneHabilitacionVigente"), "NO debe exponer método tieneHabilitacionVigente");
        }

        @Test
        @DisplayName("Mascota serializa todas las propiedades en snake_case canónico sin duplicados camelCase")
        void testMascotaSnakeCaseParity() throws Exception {
            Mascota mascota = Mascota.builder()
                .nroRegMunicipal(5001)
                .nombre("Toby")
                .especie("Canina")
                .sexo("Macho")
                .anioNacimiento(2022)
                .raza("Mestizo")
                .microchip("MC-999")
                .vive(true)
                .refugio(Refugio.builder().idRefugio(101).build())
                .responsable(Ciudadano.builder().idCiudadano(1).build())
                .build();

            String json = objectMapper.writeValueAsString(mascota);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            assertTrue(map.containsKey("nro_reg_municipal"), "Debe contener nro_reg_municipal");
            assertTrue(map.containsKey("nombre"), "Debe contener nombre");
            assertTrue(map.containsKey("especie"), "Debe contener especie");
            assertTrue(map.containsKey("sexo"), "Debe contener sexo");
            assertTrue(map.containsKey("anio_nacimiento"), "Debe contener anio_nacimiento");
            assertTrue(map.containsKey("raza"), "Debe contener raza");
            assertTrue(map.containsKey("microchip"), "Debe contener microchip");
            assertTrue(map.containsKey("vive"), "Debe contener vive");
            assertTrue(map.containsKey("id_refugio"), "Debe contener id_refugio");

            // PROHIBICIONES
            assertFalse(map.containsKey("nroRegMunicipal"), "NO debe contener camelCase nroRegMunicipal");
            assertFalse(map.containsKey("anioNacimiento"), "NO debe contener camelCase anioNacimiento");
            assertFalse(map.containsKey("idRefugio"), "NO debe contener camelCase idRefugio");
            assertFalse(map.containsKey("refugio"), "NO debe contener objeto refugio");
            assertFalse(map.containsKey("responsable"), "NO debe contener objeto responsable");
            assertFalse(map.containsKey("estaViva"), "NO debe exponer método de negocio estaViva");
        }

        @Test
        @DisplayName("PublicacionAdopcion serializa todas las propiedades en snake_case canónico sin duplicados camelCase")
        void testPublicacionAdopcionSnakeCaseParity() throws Exception {
            Refugio ref = Refugio.builder().idRefugio(101).razonSocial("Refugio Test").build();
            Mascota masc = Mascota.builder().nroRegMunicipal(5001).nombre("Toby").vive(true).build();
            PublicacionAdopcion pub = PublicacionAdopcion.builder()
                .nroPublicacion(2001)
                .mascota(masc)
                .refugio(ref)
                .fechaPublicacion(LocalDate.of(2026, 11, 10))
                .caracteristicasMascota("Tranquilo")
                .condicionAdopcion("Espacio verde")
                .foto("data:image/jpeg;base64,abc")
                .estadoPublicacion(EstadoPublicacion.Activa)
                .build();

            String json = objectMapper.writeValueAsString(pub);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            assertTrue(map.containsKey("nro_publicacion"), "Debe contener nro_publicacion");
            assertTrue(map.containsKey("mascota"), "Debe contener mascota");
            assertTrue(map.containsKey("refugio"), "Debe contener refugio");
            assertTrue(map.containsKey("fecha_publicacion"), "Debe contener fecha_publicacion");
            assertTrue(map.containsKey("caracteristicas_mascota"), "Debe contener caracteristicas_mascota");
            assertTrue(map.containsKey("condicion_adopcion"), "Debe contener condicion_adopcion");
            assertTrue(map.containsKey("foto"), "Debe contener foto");
            assertTrue(map.containsKey("estado_publicacion"), "Debe contener estado_publicacion");
            assertTrue(map.containsKey("nro_reg_municipal"), "Debe contener nro_reg_municipal");
            assertTrue(map.containsKey("id_refugio"), "Debe contener id_refugio");

            // PROHIBICIONES
            assertFalse(map.containsKey("nroPublicacion"), "NO debe contener camelCase nroPublicacion");
            assertFalse(map.containsKey("fechaPublicacion"), "NO debe contener camelCase fechaPublicacion");
            assertFalse(map.containsKey("caracteristicasMascota"), "NO debe contener camelCase caracteristicasMascota");
            assertFalse(map.containsKey("condicionAdopcion"), "NO debe contener camelCase condicionAdopcion");
            assertFalse(map.containsKey("estadoPublicacion"), "NO debe contener camelCase estadoPublicacion");
            assertFalse(map.containsKey("nroRegMunicipal"), "NO debe contener camelCase nroRegMunicipal");
            assertFalse(map.containsKey("idRefugio"), "NO debe contener camelCase idRefugio");
        }
    }

    // =========================================================================
    // CHALLENGE 4: Exact Contract Parity DTO vs Direct Rich Entity
    // =========================================================================
    @Nested
    @DisplayName("Challenge 4: Paridad de Contrato JSON (Entidad Directa vs Contrato REST)")
    class DtoParityChallenge {

        @Test
        @DisplayName("PublicacionAdopcion serializada directamente produce JSON compatible campo por campo con el contrato REST canónico")
        void testPublicacionEntityMatchesCanonicalJsonContract() throws Exception {
            Refugio ref = Refugio.builder()
                .idRefugio(101)
                .razonSocial("Huellitas Cordobesas")
                .correo("contacto@huellitas.org")
                .telefono("0351-4890123")
                .domicilio("Av. Colón 4500")
                .habilitacionMunicipal("HAB-2024-0412")
                .idResponsable(1)
                .build();

            Mascota masc = Mascota.builder()
                .nroRegMunicipal(5001)
                .nombre("Milo")
                .especie("Canina")
                .sexo("Macho")
                .anioNacimiento(2021)
                .raza("Mestizo")
                .microchip("MC-1234")
                .vive(true)
                .refugio(ref)
                .build();

            PublicacionAdopcion pub = PublicacionAdopcion.builder()
                .nroPublicacion(2001)
                .mascota(masc)
                .refugio(ref)
                .fechaPublicacion(LocalDate.of(2026, 11, 10))
                .caracteristicasMascota("Cariñoso y tranquilo")
                .condicionAdopcion("Patio cerrado")
                .foto("data:image/png;base64,sample")
                .estadoPublicacion(EstadoPublicacion.Activa)
                .build();

            String jsonEntity = objectMapper.writeValueAsString(pub);
            JsonNode treeEntity = objectMapper.readTree(jsonEntity);

            // Validar campos raíz canónicos
            assertEquals(2001, treeEntity.get("nro_publicacion").asInt());
            assertEquals(5001, treeEntity.get("nro_reg_municipal").asInt());
            assertEquals(101, treeEntity.get("id_refugio").asInt());
            assertEquals("2026-11-10", treeEntity.get("fecha_publicacion").asText());
            assertEquals("Cariñoso y tranquilo", treeEntity.get("caracteristicas_mascota").asText());
            assertEquals("Patio cerrado", treeEntity.get("condicion_adopcion").asText());
            assertEquals("data:image/png;base64,sample", treeEntity.get("foto").asText());
            assertEquals("Activa", treeEntity.get("estado_publicacion").asText());

            // Validar sub-objeto anidado mascota
            JsonNode mEntity = treeEntity.get("mascota");
            assertNotNull(mEntity, "El sub-objeto 'mascota' debe estar presente");
            assertEquals(5001, mEntity.get("nro_reg_municipal").asInt());
            assertEquals("Milo", mEntity.get("nombre").asText());
            assertEquals("Canina", mEntity.get("especie").asText());
            assertEquals("Macho", mEntity.get("sexo").asText());
            assertEquals(2021, mEntity.get("anio_nacimiento").asInt());
            assertEquals("Mestizo", mEntity.get("raza").asText());
            assertEquals("MC-1234", mEntity.get("microchip").asText());
            assertTrue(mEntity.get("vive").asBoolean());
            assertEquals(101, mEntity.get("id_refugio").asInt());

            // Validar sub-objeto anidado refugio
            JsonNode rEntity = treeEntity.get("refugio");
            assertNotNull(rEntity, "El sub-objeto 'refugio' debe estar presente");
            assertEquals(101, rEntity.get("id_refugio").asInt());
            assertEquals("Huellitas Cordobesas", rEntity.get("razon_social").asText());
            assertEquals("contacto@huellitas.org", rEntity.get("correo").asText());
            assertEquals("0351-4890123", rEntity.get("telefono").asText());
            assertEquals("Av. Colón 4500", rEntity.get("domicilio").asText());
            assertEquals("HAB-2024-0412", rEntity.get("habilitacion_municipal").asText());
            assertEquals(1, rEntity.get("id_responsable").asInt());
        }
    }

    // =========================================================================
    // CHALLENGE 5: Deserialization and Clave Injection Resistance
    // =========================================================================
    @Nested
    @DisplayName("Challenge 5: Resistencia a Inyección y Deserialización de 'clave'")
    class DeserializationChallenge {

        @Test
        @DisplayName("Deserializar JSON con campo 'clave' malicioso no sobreescribe ni asigna la clave vía @JsonIgnore")
        void testDeserializationIgnoresClaveField() throws Exception {
            String maliciousJson = """
                {
                    "id_ciudadano": 50,
                    "cuil": "20-55555555-5",
                    "clave": "MaliciousInjectedPassword!",
                    "nombre": "Pedro",
                    "apellido": "García",
                    "habilitado": true
                }
                """;

            Ciudadano ciudadano = objectMapper.readValue(maliciousJson, Ciudadano.class);

            assertNotNull(ciudadano);
            assertEquals("Pedro", ciudadano.getNombre());
            assertEquals("20-55555555-5", ciudadano.getCuil());
            // @JsonIgnore en el campo previene la inyección de clave desde JSON
            assertNull(ciudadano.getClave(), "Jackson no debe asignar el campo clave ignorado al deserializar");
        }
    }

    // =========================================================================
    // CHALLENGE 6: Exception Consistency and HTTP Status Mapping
    // =========================================================================
    @Nested
    @DisplayName("Challenge 6: Consistencia de Excepciones de Dominio y Mapeo HTTP")
    class ExceptionConsistencyChallenge {

        @Autowired
        private com.cordoba.mascotas.shared.GlobalExceptionHandler exceptionHandler;

        @Test
        @DisplayName("AutenticacionInvalidaException mapea a HTTP 401 Unauthorized con formato estándar")
        void testAutenticacionInvalidaMapping() {
            var ex = new com.cordoba.mascotas.shared.AutenticacionInvalidaException("Credenciales incorrectas");
            var response = exceptionHandler.handleAutenticacionInvalida(ex);

            assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(401, response.getBody().status());
            assertEquals("No Autorizado", response.getBody().error());
            assertEquals("Credenciales incorrectas", response.getBody().mensaje());
        }

        @Test
        @DisplayName("TransicionEstadoInvalidaException mapea a HTTP 409 Conflict con formato estándar")
        void testTransicionInvalidaMapping() {
            var ex = new com.cordoba.mascotas.shared.TransicionEstadoInvalidaException("Una publicación en estado Finalizada no puede cambiar de estado.");
            var response = exceptionHandler.handleTransicionEstadoInvalida(ex);

            assertEquals(org.springframework.http.HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().status());
            assertEquals("Conflicto de Estado", response.getBody().error());
            assertEquals("Una publicación en estado Finalizada no puede cambiar de estado.", response.getBody().mensaje());
        }

        @Test
        @DisplayName("IllegalArgumentException mapea a HTTP 400 Bad Request con formato estándar")
        void testIllegalArgumentMapping() {
            var ex = new IllegalArgumentException("El nuevo estado de la publicación no puede ser nulo.");
            var response = exceptionHandler.handleIllegalArgument(ex);

            assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().status());
            assertEquals("Solicitud Inválida", response.getBody().error());
            assertEquals("El nuevo estado de la publicación no puede ser nulo.", response.getBody().mensaje());
        }

        @Test
        @DisplayName("RecursoNoEncontradoException mapea a HTTP 404 Not Found con formato estándar")
        void testRecursoNoEncontradoMapping() {
            var ex = new com.cordoba.mascotas.shared.RecursoNoEncontradoException("Publicación no encontrada con ID: 9999");
            var response = exceptionHandler.handleRecursoNoEncontrado(ex);

            assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().status());
            assertEquals("No Encontrado", response.getBody().error());
            assertEquals("Publicación no encontrada con ID: 9999", response.getBody().mensaje());
        }
    }
}
