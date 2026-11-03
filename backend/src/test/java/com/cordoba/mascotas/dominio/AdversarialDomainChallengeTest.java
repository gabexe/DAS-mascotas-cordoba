package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Empirical Challenge: Domain Entities Robustness & Invariants")
class AdversarialDomainChallengeTest {

    private Refugio refugioValido;
    private Mascota mascotaViva;
    private PublicacionAdopcion publicacionActiva;

    @BeforeEach
    void setUp() {
        refugioValido = Refugio.builder()
            .idRefugio(100)
            .razonSocial("Refugio Esperanza")
            .habilitacionMunicipal("HAB-MUNIC-2024-001")
            .build();

        mascotaViva = Mascota.builder()
            .nroRegMunicipal(500)
            .nombre("Boby")
            .especie("Canino")
            .sexo("Macho")
            .anioNacimiento(2020)
            .raza("Mestizo")
            .vive(true)
            .refugio(refugioValido)
            .build();

        publicacionActiva = PublicacionAdopcion.builder()
            .nroPublicacion(1)
            .refugio(refugioValido)
            .mascota(mascotaViva)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota("Muy dócil")
            .condicionAdopcion("Casa con patio")
            .foto("data:image/jpeg;base64,sample")
            .estadoPublicacion(EstadoPublicacion.Activa)
            .build();
    }

    // =========================================================================
    // 1. FSM & Terminal State Invariance
    // =========================================================================

    @Nested
    @DisplayName("FSM: State Transition Adversarial Probing")
    class FSMAdversarialTests {

        @Test
        @DisplayName("Terminal State Finalizada is strictly immutable: no transitions allowed under any target")
        void testFinalizadaIsStrictlyImmutable() {
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Finalizada);

            // Attempt transition to Activa
            assertThrows(TransicionEstadoInvalidaException.class,
                () -> publicacionActiva.transicionarA(EstadoPublicacion.Activa));
            assertEquals(EstadoPublicacion.Finalizada, publicacionActiva.getEstadoPublicacion(),
                "State must remain Finalizada after failed transition to Activa");

            // Attempt transition to Pausada
            assertThrows(TransicionEstadoInvalidaException.class,
                () -> publicacionActiva.transicionarA(EstadoPublicacion.Pausada));
            assertEquals(EstadoPublicacion.Finalizada, publicacionActiva.getEstadoPublicacion(),
                "State must remain Finalizada after failed transition to Pausada");

            // Attempt transition to Finalizada (re-finishing)
            assertThrows(TransicionEstadoInvalidaException.class,
                () -> publicacionActiva.transicionarA(EstadoPublicacion.Finalizada));
            assertEquals(EstadoPublicacion.Finalizada, publicacionActiva.getEstadoPublicacion(),
                "State must remain Finalizada after failed transition to Finalizada");

            // Attempt transition to null from Finalizada
            assertThrows(IllegalArgumentException.class,
                () -> publicacionActiva.transicionarA(null));
            assertEquals(EstadoPublicacion.Finalizada, publicacionActiva.getEstadoPublicacion(),
                "State must remain Finalizada after null target");
        }

        @Test
        @DisplayName("Null target state throws IllegalArgumentException and never mutates current state")
        void testNullTransitionTargetDoesNotMutateState() {
            // From Activa
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Activa);
            assertThrows(IllegalArgumentException.class, () -> publicacionActiva.transicionarA(null));
            assertEquals(EstadoPublicacion.Activa, publicacionActiva.getEstadoPublicacion());

            // From Pausada
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Pausada);
            assertThrows(IllegalArgumentException.class, () -> publicacionActiva.transicionarA(null));
            assertEquals(EstadoPublicacion.Pausada, publicacionActiva.getEstadoPublicacion());
        }

        @Test
        @DisplayName("Dead pet cannot transition to Activa from Pausada, and state remains Pausada")
        void testDeadPetCannotTransitionToActiva() {
            mascotaViva.setVive(false);
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Pausada);

            TransicionEstadoInvalidaException ex = assertThrows(
                TransicionEstadoInvalidaException.class,
                () -> publicacionActiva.transicionarA(EstadoPublicacion.Activa)
            );
            assertTrue(ex.getMessage().contains("fallecida") || ex.getMessage().contains("no disponible"));
            assertEquals(EstadoPublicacion.Pausada, publicacionActiva.getEstadoPublicacion(),
                "State must not mutate if dead pet activation is rejected");
        }

        @Test
        @DisplayName("Pet with null 'vive' cannot transition to Activa from Pausada")
        void testNullVivePetCannotTransitionToActiva() {
            mascotaViva.setVive(null);
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Pausada);

            assertThrows(TransicionEstadoInvalidaException.class,
                () -> publicacionActiva.transicionarA(EstadoPublicacion.Activa));
            assertEquals(EstadoPublicacion.Pausada, publicacionActiva.getEstadoPublicacion());
        }

        @Test
        @DisplayName("Dead pet can transition from Pausada to Finalizada (administrative closure)")
        void testDeadPetCanTransitionToFinalizada() {
            mascotaViva.setVive(false);
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Pausada);

            assertDoesNotThrow(() -> publicacionActiva.transicionarA(EstadoPublicacion.Finalizada));
            assertEquals(EstadoPublicacion.Finalizada, publicacionActiva.getEstadoPublicacion());
        }

        @Test
        @DisplayName("Idempotent transitions are safe and preserve current state")
        void testIdempotentTransitions() {
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Activa);
            assertDoesNotThrow(() -> publicacionActiva.transicionarA(EstadoPublicacion.Activa));
            assertEquals(EstadoPublicacion.Activa, publicacionActiva.getEstadoPublicacion());

            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Pausada);
            assertDoesNotThrow(() -> publicacionActiva.transicionarA(EstadoPublicacion.Pausada));
            assertEquals(EstadoPublicacion.Pausada, publicacionActiva.getEstadoPublicacion());
        }

        @Test
        @DisplayName("Stress: Concurrent state transitions maintain terminal invariant")
        void testConcurrentTransitionsDoNotViolateTerminalState() throws InterruptedException {
            final int threads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger finalizedAttempts = new AtomicInteger(0);

            // Set to finalizada
            publicacionActiva.setEstadoPublicacion(EstadoPublicacion.Finalizada);

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        EstadoPublicacion target = (idx % 2 == 0) ? EstadoPublicacion.Activa : EstadoPublicacion.Pausada;
                        publicacionActiva.transicionarA(target);
                    } catch (TransicionEstadoInvalidaException e) {
                        finalizedAttempts.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertEquals(threads, finalizedAttempts.get(), "All concurrent mutation attempts from Finalizada must be rejected");
            assertEquals(EstadoPublicacion.Finalizada, publicacionActiva.getEstadoPublicacion());
        }
    }

    // =========================================================================
    // 2. Ownership / Refugio Authorization
    // =========================================================================

    @Nested
    @DisplayName("Ownership: validarPertenenciaRefugio Adversarial Probing")
    class OwnershipAdversarialTests {

        @Test
        @DisplayName("Matching shelter ID passes without exception")
        void testMatchingShelterIdPasses() {
            assertDoesNotThrow(() -> publicacionActiva.validarPertenenciaRefugio(100));
            assertDoesNotThrow(() -> publicacionActiva.validarPertenenciaRefugio(100L));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 1, 99, 101, 99999, Integer.MAX_VALUE, Integer.MIN_VALUE})
        @DisplayName("Non-matching shelter IDs are strictly rejected with AutenticacionInvalidaException")
        void testNonMatchingShelterIdsRejected(int invalidShelterId) {
            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> publicacionActiva.validarPertenenciaRefugio(invalidShelterId)
            );
            assertTrue(ex.getMessage().contains("No posee permisos"));
        }

        @Test
        @DisplayName("Long overload rejects non-matching IDs")
        void testLongOverloadRejectsNonMatching() {
            assertThrows(AutenticacionInvalidaException.class,
                () -> publicacionActiva.validarPertenenciaRefugio(999L));
        }

        @Test
        @DisplayName("Null solicitante does not throw (allows public queries)")
        void testNullSolicitanteAllowsAccess() {
            assertDoesNotThrow(() -> publicacionActiva.validarPertenenciaRefugio((Integer) null));
            assertDoesNotThrow(() -> publicacionActiva.validarPertenenciaRefugio((Long) null));
        }

        @Test
        @DisplayName("Probing NPE vulnerability when refugio has null idRefugio")
        void testRefugioWithNullIdThrowsNPE() {
            Refugio refugioSinId = Refugio.builder().razonSocial("Refugio Sin ID").build();
            publicacionActiva.setRefugio(refugioSinId);

            // If this throws NullPointerException, it demonstrates missing null-safe equality check
            assertThrows(NullPointerException.class,
                () -> publicacionActiva.validarPertenenciaRefugio(100),
                "Proving empirical NPE when refugio.idRefugio is null");
        }
    }

    // =========================================================================
    // 3. Authentication & Security (Ciudadano.autenticar)
    // =========================================================================

    @Nested
    @DisplayName("Authentication: Ciudadano.autenticar Adversarial Probing")
    class AuthenticationAdversarialTests {

        private Ciudadano ciudadano;

        @BeforeEach
        void initCiudadano() {
            ciudadano = Ciudadano.builder()
                .idCiudadano(10)
                .cuil("20-12345678-9")
                .clave("MiClaveSegura#2024")
                .nombre("Juan")
                .apellido("Gómez")
                .correo("juan@refugio.org")
                .habilitado(true)
                .refugio(refugioValido)
                .build();
        }

        @Test
        @DisplayName("Valid credentials with active shelter pass authentication")
        void testValidAuthentication() {
            assertDoesNotThrow(() -> ciudadano.autenticar("MiClaveSegura#2024"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "miclavesegura#2024", "MiClaveSegura#2024 ", " MiClaveSegura#2024", "wrong"})
        @DisplayName("Wrong, blank, or trimmed passwords fail without leaking other domain states")
        void testWrongPasswordsFail(String candidatePassword) {
            // Even if user is disabled or shelter is null, bad password must report "Credenciales incorrectas"
            ciudadano.setHabilitado(false);
            ciudadano.setRefugio(null);

            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> ciudadano.autenticar(candidatePassword)
            );
            assertEquals("Credenciales incorrectas", ex.getMessage(),
                "Must reject invalid password with generic error before checking status");
        }

        @Test
        @DisplayName("Stored null password rejects any input safely")
        void testStoredNullPasswordRejectsSafely() {
            ciudadano.setClave(null);
            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> ciudadano.autenticar("MiClaveSegura#2024")
            );
            assertEquals("Credenciales incorrectas", ex.getMessage());
        }

        @Test
        @DisplayName("Disabled citizen is rejected with specific authorization message")
        void testDisabledCitizenRejected() {
            ciudadano.setHabilitado(false);
            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> ciudadano.autenticar("MiClaveSegura#2024")
            );
            assertTrue(ex.getMessage().contains("no se encuentra habilitado"));
        }

        @Test
        @DisplayName("Citizen with null habilitado is treated as non-habilitado and rejected")
        void testNullHabilitadoCitizenRejected() {
            ciudadano.setHabilitado(null);
            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> ciudadano.autenticar("MiClaveSegura#2024")
            );
            assertTrue(ex.getMessage().contains("no se encuentra habilitado"));
        }

        @Test
        @DisplayName("Citizen without assigned shelter is rejected")
        void testCitizenWithoutShelterRejected() {
            ciudadano.setRefugio(null);
            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> ciudadano.autenticar("MiClaveSegura#2024")
            );
            assertTrue(ex.getMessage().contains("no pertenece a un refugio"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("Citizen with unhabilitated shelter (null, blank, or spaces) is rejected")
        void testCitizenWithUnhabilitatedShelterRejected(String badHabilitacion) {
            refugioValido.setHabilitacionMunicipal(badHabilitacion);
            AutenticacionInvalidaException ex = assertThrows(
                AutenticacionInvalidaException.class,
                () -> ciudadano.autenticar("MiClaveSegura#2024")
            );
            assertTrue(ex.getMessage().contains("no cuenta con habilitación municipal vigente"));
        }
    }

    // =========================================================================
    // 4. Refugio Habilitation Validation
    // =========================================================================

    @Nested
    @DisplayName("Refugio: Habilitation Invariants Adversarial Probing")
    class RefugioHabilitationAdversarialTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t\n\r"})
        @DisplayName("tieneHabilitacionVigente returns false for blank/null inputs")
        void testTieneHabilitacionVigenteFalseForBlank(String blankHab) {
            Refugio ref = Refugio.builder().habilitacionMunicipal(blankHab).build();
            assertFalse(ref.tieneHabilitacionVigente());
        }

        @Test
        @DisplayName("validarHabilitacionParaPublicar throws IllegalArgumentException on invalid habilitacion")
        void testValidarHabilitacionParaPublicarThrows() {
            Refugio ref = Refugio.builder().habilitacionMunicipal("   ").build();
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                ref::validarHabilitacionParaPublicar
            );
            assertTrue(ex.getMessage().contains("registrar publicaciones"));
        }
    }

    // =========================================================================
    // 5. Mascota & Publicacion Domain Factories & Boundary Inputs
    // =========================================================================

    @Nested
    @DisplayName("Domain Factories: Boundary Invariant Probing")
    class DomainFactoriesAdversarialTests {

        @Test
        @DisplayName("Mascota.crearParaRefugio throws on null refugio")
        void testMascotaFactoryRejectsNullRefugio() {
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                null, "Luna", "Felino", "Hembra", 2022, "Siamés", null, "foto", "carac", "cond"
            );
            assertThrows(IllegalArgumentException.class, () -> Mascota.crearParaRefugio(dto, null));
        }

        @Test
        @DisplayName("Mascota.crearParaRefugio throws on null DTO")
        void testMascotaFactoryRejectsNullDTO() {
            assertThrows(IllegalArgumentException.class, () -> Mascota.crearParaRefugio((NuevaPublicacionDTO) null, refugioValido));
        }

        @Test
        @DisplayName("PublicacionAdopcion.crear canonical factory enforces dead pet prohibition")
        void testPublicacionCanonicalFactoryRejectsDeadPet() {
            mascotaViva.setVive(false);
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PublicacionAdopcion.crear(
                    mascotaViva,
                    refugioValido,
                    "Características",
                    "Condiciones",
                    "foto.jpg"
                )
            );
            assertTrue(ex.getMessage().contains("fallecida"));
        }

        @Test
        @DisplayName("PublicacionAdopcion.crear canonical factory enforces unhabilitated shelter prohibition")
        void testPublicacionCanonicalFactoryRejectsUnhabilitatedShelter() {
            refugioValido.setHabilitacionMunicipal(null);
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PublicacionAdopcion.crear(
                    mascotaViva,
                    refugioValido,
                    "Características",
                    "Condiciones",
                    "foto.jpg"
                )
            );
            assertTrue(ex.getMessage().contains("habilitación municipal"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("PublicacionAdopcion.crear canonical factory rejects empty text fields")
        void testPublicacionCanonicalFactoryRejectsEmptyFields(String emptyVal) {
            // Empty caracteristicas
            assertThrows(IllegalArgumentException.class, () -> PublicacionAdopcion.crear(
                mascotaViva, refugioValido, emptyVal, "Condiciones", "foto"
            ));

            // Empty condiciones
            assertThrows(IllegalArgumentException.class, () -> PublicacionAdopcion.crear(
                mascotaViva, refugioValido, "Carac", emptyVal, "foto"
            ));

            // Empty foto
            assertThrows(IllegalArgumentException.class, () -> PublicacionAdopcion.crear(
                mascotaViva, refugioValido, "Carac", "Cond", emptyVal
            ));
        }

        @Test
        @DisplayName("Disparity probe: crearParaRefugio does NOT enforce dead pet or null validations unlike canonical crear")
        void testCrearParaRefugioLacksValidationComparedToCrear() {
            mascotaViva.setVive(false);
            NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
                100, "Boby", "Canino", "Macho", 2020, "Mestizo", null, "foto", "carac", "cond"
            );

            // Notice: crearParaRefugio creates an Activa publication with a dead pet without throwing!
            PublicacionAdopcion pubConMascotaMuerta = PublicacionAdopcion.crearParaRefugio(dto, mascotaViva, refugioValido);
            assertNotNull(pubConMascotaMuerta);
            assertEquals(EstadoPublicacion.Activa, pubConMascotaMuerta.getEstadoPublicacion());
            assertFalse(pubConMascotaMuerta.getMascota().estaViva(),
                "crearParaRefugio permits building an active publication for a dead pet (inconsistency with domain invariant)");
        }
    }

    // =========================================================================
    // 6. Security & Jackson Serialization Leakage
    // =========================================================================

    @Nested
    @DisplayName("Jackson Serialization: Anti-Leakage & Cycle Immunity")
    class JacksonAdversarialTests {

        private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        @Test
        @DisplayName("Ciudadano serialization strictly hides 'clave' field and its raw value")
        void testCiudadanoClaveNeverSerialized() throws Exception {
            Ciudadano c = Ciudadano.builder()
                .idCiudadano(7)
                .cuil("20-99887766-5")
                .clave("SuperSecretUnleakedPassword123!")
                .nombre("Ana")
                .apellido("López")
                .correo("ana@refugio.org")
                .habilitado(true)
                .refugio(refugioValido)
                .build();

            String json = mapper.writeValueAsString(c);

            assertFalse(json.contains("clave"), "Field 'clave' must never appear in serialized JSON");
            assertFalse(json.contains("SuperSecretUnleakedPassword123!"), "Password value must never appear in serialized JSON");
            assertTrue(json.contains("\"id_refugio\":100"), "Canonical id_refugio must be present");
        }

        @Test
        @DisplayName("Mascota serialization hides lazy relationships to avoid cycles and proxies")
        void testMascotaSerializationHidesLazyRelations() throws Exception {
            Ciudadano resp = Ciudadano.builder().idCiudadano(20).build();
            mascotaViva.setResponsable(resp);

            String json = mapper.writeValueAsString(mascotaViva);

            // Verify no circular reference or heavy nested object for refugio/responsable
            assertFalse(json.contains("\"responsable\":"), "Lazy responsable must be ignored");
            assertTrue(json.contains("\"id_refugio\":100"), "id_refugio must be flatly serialized");
            assertTrue(json.contains("\"nro_reg_municipal\":500"), "nro_reg_municipal must be serialized");
        }

        @Test
        @DisplayName("PublicacionAdopcion serialization flattens required snake_case IDs")
        void testPublicacionAdopcionSerializationSnakeCaseKeys() throws Exception {
            String json = mapper.writeValueAsString(publicacionActiva);

            assertTrue(json.contains("\"nro_publicacion\":1"));
            assertTrue(json.contains("\"id_refugio\":100"));
            assertTrue(json.contains("\"nro_reg_municipal\":500"));
            assertTrue(json.contains("\"estado_publicacion\":\"Activa\""));
        }
    }
}
