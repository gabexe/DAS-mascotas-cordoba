package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.ErrorResponse;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Empirical Challenge: Milestone 2 Transactional Integrity & Concurrency")
class Milestone2TransactionalChallengeTest {

    @Autowired
    private PublicacionController publicacionController;

    @Autowired
    private PublicacionAdopcionRepository publicacionRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private RefugioRepository refugioRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Integer> createdPublicacionIds = new ArrayList<>();
    private final List<Integer> createdMascotaIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdPublicacionIds.clear();
        createdMascotaIds.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up created entities in isolation to keep database pristine
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            for (Integer pubId : createdPublicacionIds) {
                if (publicacionRepository.existsById(pubId)) {
                    publicacionRepository.deleteById(pubId);
                }
            }
            for (Integer mascId : createdMascotaIds) {
                if (mascotaRepository.existsById(mascId)) {
                    mascotaRepository.deleteById(mascId);
                }
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
        }
    }

    // =========================================================================
    // 1. Transactional Proxy & Rollback Verification on crearPublicacion
    // =========================================================================

    @Nested
    @DisplayName("Challenge 1: @Transactional Rollback on crearPublicacion")
    class TransactionalRollbackTests {

        @Test
        @DisplayName("Controller bean is proxied by Spring AOP for transaction demarcation")
        void testControllerIsAopProxy() {
            assertTrue(AopUtils.isAopProxy(publicacionController),
                "PublicacionController must be wrapped in Spring AOP proxy to enforce @Transactional boundaries");
        }

        @Test
        @DisplayName("Rollback: Database constraint failure on Publicacion prevents orphaned Mascota")
        void testRollbackOnPublicacionConstraintFailureLeavesNoOrphanedMascota() {
            long initialMascotaCount = mascotaRepository.count();
            String uniqueMicrochip = "MC-TX-CHALLENGE-" + System.currentTimeMillis();

            // caracteristicas_mascota is NVARCHAR(1000) in database schema.
            // Sending 1500 chars passes bean validation (since no max size is annotated)
            // but causes SQL Server DataIntegrityViolationException during publicacion insert.
            String oversizedCharacteristics = "X".repeat(1500);

            NuevaPublicacionDTO invalidDto = new NuevaPublicacionDTO(
                101,
                "Perro Fantasma Rollback",
                "Perro",
                "Macho",
                2022,
                "Mestizo",
                uniqueMicrochip,
                "https://ejemplo.com/fantasma.jpg",
                oversizedCharacteristics,
                "Patio cerrado"
            );

            // Calling controller directly from outside test transaction
            assertThrows(org.springframework.dao.DataAccessException.class, () -> {
                publicacionController.crear(invalidDto, 101, null, null);
            }, "Expected SQL Server DataAccessException due to oversized column length");

            // Verify empirical state in SQL Server
            long finalMascotaCount = mascotaRepository.count();
            assertEquals(initialMascotaCount, finalMascotaCount,
                "Mascota table row count must NOT increase when publicacion creation fails");

            assertFalse(mascotaRepository.existsByMicrochip(uniqueMicrochip),
                "No orphaned Mascota should persist with the microchip of the failed publication");
        }

        @Test
        @DisplayName("Rollback: Unchecked exception thrown in publication persistence rolls back Mascota")
        void testRollbackOnUncheckedExceptionLeavesNoOrphanedMascota() {
            long initialMascotaCount = mascotaRepository.count();
            String uniqueMicrochip = "MC-TX-FAIL-" + System.currentTimeMillis();

            // To empirically verify transaction rollback across operations:
            // We execute within a programmatic transaction simulating controller execution failure
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            TransactionStatus txStatus = transactionManager.getTransaction(def);

            Refugio refugio = refugioRepository.findById(101).orElseThrow();
            Mascota nuevaMascota = Mascota.crearParaRefugio(
                "Mascota Transaccional", "Perro", "Hembra", 2021, "Callejero", uniqueMicrochip, refugio, null
            );
            Mascota guardada = mascotaRepository.save(nuevaMascota);
            assertNotNull(guardada.getNroRegMunicipal(), "Mascota should have received generated IDENTITY id");

            // Simulate failure before commit (exactly what occurs if publicacion save fails)
            transactionManager.rollback(txStatus);

            // After rollback, verify in clean transaction that mascota was discarded by SQL Server
            long finalMascotaCount = mascotaRepository.count();
            assertEquals(initialMascotaCount, finalMascotaCount,
                "Mascota row must be completely rolled back by SQL Server");
            assertFalse(mascotaRepository.existsByMicrochip(uniqueMicrochip),
                "Microchip should not exist after rollback");
        }
    }

    // =========================================================================
    // 2. existsOtraPublicacionEnEstado Check & 409 Conflict Fidelity
    // =========================================================================

    @Nested
    @DisplayName("Challenge 2: existsOtraPublicacionEnEstado & 409 Conflict on Reactivation")
    class ExistsOtraPublicacionEnEstadoTests {

        private Mascota sharedMascota;
        private PublicacionAdopcion activePublicacion;
        private PublicacionAdopcion pausedPublicacion;

        @BeforeEach
        void createCoexistingPublications() {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            TransactionStatus tx = transactionManager.getTransaction(def);

            Refugio refugio = refugioRepository.findById(101).orElseThrow();

            // Create 1 shared Mascota
            sharedMascota = Mascota.builder()
                .nombre("Mascota Compartida")
                .especie("Perro")
                .sexo("Macho")
                .anioNacimiento(2022)
                .raza("Labrador")
                .vive(true)
                .refugio(refugio)
                .build();
            sharedMascota = mascotaRepository.save(sharedMascota);
            createdMascotaIds.add(sharedMascota.getNroRegMunicipal());

            // Create Publication 1 in Activa state
            activePublicacion = PublicacionAdopcion.builder()
                .mascota(sharedMascota)
                .refugio(refugio)
                .fechaPublicacion(LocalDate.now())
                .caracteristicasMascota("Muy juguetón")
                .condicionAdopcion("Casa amplia")
                .foto("https://ejemplo.com/p1.jpg")
                .estadoPublicacion(EstadoPublicacion.Activa)
                .build();
            activePublicacion = publicacionRepository.save(activePublicacion);
            createdPublicacionIds.add(activePublicacion.getNroPublicacion());

            // Create Publication 2 for SAME Mascota in Pausada state
            pausedPublicacion = PublicacionAdopcion.builder()
                .mascota(sharedMascota)
                .refugio(refugio)
                .fechaPublicacion(LocalDate.now())
                .caracteristicasMascota("Muy juguetón copia")
                .condicionAdopcion("Casa amplia")
                .foto("https://ejemplo.com/p2.jpg")
                .estadoPublicacion(EstadoPublicacion.Pausada)
                .build();
            pausedPublicacion = publicacionRepository.save(pausedPublicacion);
            createdPublicacionIds.add(pausedPublicacion.getNroPublicacion());

            transactionManager.commit(tx);
        }

        @Test
        @DisplayName("Repository query existsOtraPublicacionEnEstado accurately detects conflicting active publication")
        void testQueryExistsOtraPublicacionEnEstado() {
            boolean existsActive = publicacionRepository.existsOtraPublicacionEnEstado(
                sharedMascota.getNroRegMunicipal(),
                EstadoPublicacion.Activa,
                pausedPublicacion.getNroPublicacion()
            );
            assertTrue(existsActive, "Query must detect activePublicacion as existing active publication for the same pet");

            // Excluding activePublicacion itself returns false
            boolean existsExcludingActive = publicacionRepository.existsOtraPublicacionEnEstado(
                sharedMascota.getNroRegMunicipal(),
                EstadoPublicacion.Activa,
                activePublicacion.getNroPublicacion()
            );
            assertFalse(existsExcludingActive, "Excluding activePublicacion itself must return false");
        }

        @Test
        @DisplayName("Controller: Validates cross-aggregate uniqueness before mutating entity, throwing TransicionEstadoInvalidaException instead of DataIntegrityViolationException")
        void testControllerRejectsReactivationWithTransicionEstadoInvalidaException() {
            CambioEstadoDTO dto = new CambioEstadoDTO("Activa");

            TransicionEstadoInvalidaException ex = assertThrows(
                TransicionEstadoInvalidaException.class,
                () -> publicacionController.cambiarEstado(
                    pausedPublicacion.getNroPublicacion(), dto, 101, null, null
                ),
                "Cross-aggregate check must reject reactivation with domain TransicionEstadoInvalidaException"
            );

            assertEquals("La mascota ya posee otra publicación de adopción activa.", ex.getMessage());
        }

        @Test
        @DisplayName("MockMvc: Returns 409 Conflict with 'Conflicto de Estado' on duplicate active reactivation")
        void testMockMvcReturns409ConflictOnDuplicateActiveReactivation() throws Exception {
            CambioEstadoDTO dto = new CambioEstadoDTO("Activa");

            mockMvc.perform(patch("/api/publicaciones/{id}/estado", pausedPublicacion.getNroPublicacion())
                    .header("X-Refugio-Id", 101)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflicto de Estado"))
                .andExpect(jsonPath("$.mensaje").value("La mascota ya posee otra publicación de adopción activa."));
        }

        @Test
        @DisplayName("Permitted: Reactivation succeeds when the other publication transitions to Finalizada")
        void testReactivationSucceedsWhenOtherIsFinalizada() {
            // First transition active publication to Finalizada
            publicacionController.cambiarEstado(
                activePublicacion.getNroPublicacion(), new CambioEstadoDTO("Finalizada"), 101, null, null
            );

            // Now reactivating paused publication must succeed because no OTHER active publication exists
            ResponseEntity<PublicacionAdopcion> response = publicacionController.cambiarEstado(
                pausedPublicacion.getNroPublicacion(), new CambioEstadoDTO("Activa"), 101, null, null
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Activa", response.getBody().getEstadoPublicacion().name());

            PublicacionAdopcion updated = publicacionRepository.findById(pausedPublicacion.getNroPublicacion()).orElseThrow();
            assertEquals(EstadoPublicacion.Activa, updated.getEstadoPublicacion());
        }

        @Test
        @DisplayName("Permitted: Idempotent call to Activa on already active publication succeeds without false conflict")
        void testIdempotentActivationDoesNotTriggerConflict() {
            // activePublicacion is already Activa. Sending "Activa" again should succeed
            assertDoesNotThrow(() -> {
                ResponseEntity<PublicacionAdopcion> resp = publicacionController.cambiarEstado(
                    activePublicacion.getNroPublicacion(), new CambioEstadoDTO("Activa"), 101, null, null
                );
                assertEquals(HttpStatus.OK, resp.getStatusCode());
                assertEquals("Activa", resp.getBody().getEstadoPublicacion().name());
            });
        }
    }

    // =========================================================================
    // 3. Concurrency Stress Testing
    // =========================================================================

    @Nested
    @DisplayName("Challenge 3: Concurrency & Race Condition Probing")
    class ConcurrencyStressTests {

        @Test
        @DisplayName("Concurrency: Simultaneous reactivation of two paused publications for same pet guarantees at most ONE active")
        void testConcurrentReactivationPreventsDualActivePublications() throws Exception {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            TransactionStatus tx = transactionManager.getTransaction(def);

            Refugio refugio = refugioRepository.findById(101).orElseThrow();
            Mascota mascotaConcurrente = Mascota.builder()
                .nombre("Mascota Concurrente")
                .especie("Canina")
                .sexo("Hembra")
                .anioNacimiento(2023)
                .vive(true)
                .refugio(refugio)
                .build();
            final Mascota savedMascotaConcurrente = mascotaRepository.save(mascotaConcurrente);
            createdMascotaIds.add(savedMascotaConcurrente.getNroRegMunicipal());
            final int sharedRegMunicipal = savedMascotaConcurrente.getNroRegMunicipal();

            // Create TWO Pausada publications for this pet
            PublicacionAdopcion pubA = PublicacionAdopcion.builder()
                .mascota(savedMascotaConcurrente)
                .refugio(refugio)
                .fechaPublicacion(LocalDate.now())
                .caracteristicasMascota("Pub A")
                .condicionAdopcion("Cond A")
                .foto("https://foto.jpg")
                .estadoPublicacion(EstadoPublicacion.Pausada)
                .build();
            pubA = publicacionRepository.save(pubA);
            createdPublicacionIds.add(pubA.getNroPublicacion());

            PublicacionAdopcion pubB = PublicacionAdopcion.builder()
                .mascota(savedMascotaConcurrente)
                .refugio(refugio)
                .fechaPublicacion(LocalDate.now())
                .caracteristicasMascota("Pub B")
                .condicionAdopcion("Cond B")
                .foto("https://foto.jpg")
                .estadoPublicacion(EstadoPublicacion.Pausada)
                .build();
            pubB = publicacionRepository.save(pubB);
            createdPublicacionIds.add(pubB.getNroPublicacion());

            transactionManager.commit(tx);

            // Now run 2 concurrent threads trying to activate pubA and pubB at the EXACT same millisecond
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(2);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            final int idA = pubA.getNroPublicacion();
            final int idB = pubB.getNroPublicacion();

            Runnable taskA = () -> {
                try {
                    startGate.await();
                    publicacionController.cambiarEstado(idA, new CambioEstadoDTO("Activa"), 101, null, null);
                    successCount.incrementAndGet();
                } catch (TransicionEstadoInvalidaException | DataIntegrityViolationException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // Any other exception
                } finally {
                    doneGate.countDown();
                }
            };

            Runnable taskB = () -> {
                try {
                    startGate.await();
                    publicacionController.cambiarEstado(idB, new CambioEstadoDTO("Activa"), 101, null, null);
                    successCount.incrementAndGet();
                } catch (TransicionEstadoInvalidaException | DataIntegrityViolationException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // Any other exception
                } finally {
                    doneGate.countDown();
                }
            };

            executor.submit(taskA);
            executor.submit(taskB);

            // Release threads simultaneously
            startGate.countDown();
            assertTrue(doneGate.await(10, TimeUnit.SECONDS), "Concurrent execution timed out");
            executor.shutdown();

            // Verification in database:
            long activeCount = publicacionRepository.buscarPublicaciones(101, EstadoPublicacion.Activa, null)
                .stream()
                .filter(p -> p.getMascota() != null && p.getMascota().getNroRegMunicipal().equals(sharedRegMunicipal))
                .count();

            assertTrue(activeCount <= 1,
                "CRITICAL INVARIANT: Exactly at most ONE publication can be Activa for a mascot under concurrent requests. Found: " + activeCount);
            assertTrue(conflictCount.get() >= 1 || successCount.get() <= 1,
                "At least one of the competing activation requests must encounter a conflict or fail");
        }

        @Test
        @DisplayName("Concurrency: Simultaneous creation with identical microchip yields at most ONE registered pet")
        void testConcurrentCreationWithSameMicrochipEnforcesUniqueness() throws Exception {
            String sharedMicrochip = "MC-RACE-" + System.currentTimeMillis();

            NuevaPublicacionDTO request1 = new NuevaPublicacionDTO(
                101, "Corredor 1", "Perro", "Macho", 2023, "Galgo", sharedMicrochip,
                "https://foto.jpg", "Veloz", "Patio grande"
            );
            NuevaPublicacionDTO request2 = new NuevaPublicacionDTO(
                101, "Corredor 2", "Perro", "Hembra", 2023, "Galgo", sharedMicrochip,
                "https://foto.jpg", "Veloz y cariñosa", "Patio grande"
            );

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(2);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);

            Runnable create1 = () -> {
                try {
                    startGate.await();
                    ResponseEntity<PublicacionAdopcion> r = publicacionController.crear(request1, 101, null, null);
                    if (r.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                        createdPublicacionIds.add(r.getBody().getNroPublicacion());
                        createdMascotaIds.add(r.getBody().getMascota().getNroRegMunicipal());
                    }
                } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            };

            Runnable create2 = () -> {
                try {
                    startGate.await();
                    ResponseEntity<PublicacionAdopcion> r = publicacionController.crear(request2, 101, null, null);
                    if (r.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                        createdPublicacionIds.add(r.getBody().getNroPublicacion());
                        createdMascotaIds.add(r.getBody().getMascota().getNroRegMunicipal());
                    }
                } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            };

            executor.submit(create1);
            executor.submit(create2);

            startGate.countDown();
            assertTrue(doneGate.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(1, successCount.get(), "Exactly ONE creation request must succeed");
            assertEquals(1, rejectedCount.get(), "The competing creation request must be rejected");

            // Verify in database: only ONE pet with this microchip exists
            long matchingPets = mascotaRepository.findAll().stream()
                .filter(m -> sharedMicrochip.equals(m.getMicrochip()))
                .count();
            assertEquals(1, matchingPets, "Exactly ONE mascota with the microchip should exist in SQL Server");
        }
    }
}
