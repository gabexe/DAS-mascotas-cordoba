package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.RecursoNoEncontradoException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionAdopcionRepository publicacionRepository;
    private final MascotaRepository mascotaRepository;
    private final RefugioRepository refugioRepository;
    private final CiudadanoRepository ciudadanoRepository;

    // =========================================================================
    // 1. Endpoints de Consulta (Lectura)
    // =========================================================================

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PublicacionAdopcion>> listar(
        @RequestParam(value = "refugioId", required = false) Integer refugioId,
        @RequestParam(value = "id_refugio", required = false) Integer idRefugioParam,
        @RequestHeader(value = "X-Refugio-Id", required = false) Integer refugioIdHeader,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String q
    ) {
        Integer queryRefugioId = idRefugioParam != null ? idRefugioParam : refugioId;
        Integer idRefugio = resolverIdRefugio(refugioIdHeader, queryRefugioId, null);
        EstadoPublicacion estadoEnum = EstadoPublicacion.fromString(estado);
        String termino = (q != null && !q.isBlank()) ? q.trim() : null;

        List<PublicacionAdopcion> publicaciones = publicacionRepository.buscarPublicaciones(idRefugio, estadoEnum, termino);
        return ResponseEntity.ok(publicaciones);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<PublicacionAdopcion> obtenerPorId(@PathVariable("id") Integer id) {
        PublicacionAdopcion publicacion = publicacionRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Publicación no encontrada con número: " + id
            ));
        return ResponseEntity.ok(publicacion);
    }

    // =========================================================================
    // 2. Endpoints de Modificación (Escritura y Transacciones Atómicas)
    // =========================================================================

    @PostMapping
    @Transactional
    public ResponseEntity<PublicacionAdopcion> crear(
        @Valid @RequestBody NuevaPublicacionDTO dto,
        @RequestParam(value = "refugioId", required = false) Integer refugioId,
        @RequestParam(value = "id_refugio", required = false) Integer idRefugioParam,
        @RequestHeader(value = "X-Refugio-Id", required = false) Integer refugioIdHeader
    ) {
        Integer queryRefugioId = idRefugioParam != null ? idRefugioParam : refugioId;
        Integer idRefugio = resolverIdRefugio(refugioIdHeader, queryRefugioId, dto.idRefugio());
        if (idRefugio == null) {
            throw new IllegalArgumentException("El identificador del refugio es requerido para registrar la publicación");
        }

        Refugio refugio = refugioRepository.findById(idRefugio)
            .orElseThrow(() -> new RecursoNoEncontradoException("Refugio no encontrado con ID: " + idRefugio));

        // Tell, Don't Ask: validación de habilitación en la entidad
        refugio.validarHabilitacionParaPublicar();

        // Validación de unicidad de microchip
        if (dto.microchip() != null && !dto.microchip().isBlank()) {
            String chip = dto.microchip().trim();
            if (mascotaRepository.existsByMicrochip(chip)) {
                throw new IllegalArgumentException("Ya existe una mascota registrada con el microchip: " + chip);
            }
        }

        Ciudadano responsable = null;
        if (refugio.getIdResponsable() != null) {
            responsable = ciudadanoRepository.findById(refugio.getIdResponsable()).orElse(null);
        }

        // Fábrica de dominio y persistencia coordinada
        Mascota nuevaMascota = Mascota.crearParaRefugio(dto, refugio, responsable);
        Mascota mascotaGuardada = mascotaRepository.save(nuevaMascota);

        PublicacionAdopcion nuevaPublicacion = PublicacionAdopcion.crearParaRefugio(dto, mascotaGuardada, refugio);
        PublicacionAdopcion guardada = publicacionRepository.save(nuevaPublicacion);

        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PatchMapping("/{id}/estado")
    @Transactional
    public ResponseEntity<PublicacionAdopcion> cambiarEstado(
        @PathVariable("id") Integer id,
        @Valid @RequestBody CambioEstadoDTO dto,
        @RequestParam(value = "refugioId", required = false) Integer refugioId,
        @RequestParam(value = "id_refugio", required = false) Integer idRefugioParam,
        @RequestHeader(value = "X-Refugio-Id", required = false) Integer refugioIdHeader
    ) {
        EstadoPublicacion nuevoEstado = EstadoPublicacion.fromString(dto.nuevoEstado());
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado de la publicación no puede ser nulo o vacío");
        }

        Integer queryRefugioId = idRefugioParam != null ? idRefugioParam : refugioId;
        Integer solicitanteRefugioId = resolverIdRefugio(refugioIdHeader, queryRefugioId, dto.idRefugio());

        PublicacionAdopcion publicacion = publicacionRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Publicación no encontrada con número: " + id
            ));

        // Tell, Don't Ask: la entidad valida su titularidad
        publicacion.validarPertenenciaRefugio(solicitanteRefugioId);

        // Validación Cross-Aggregate: Invariante de publicación única activa para la mascota.
        // Se valida ANTES de transicionar la entidad para evitar que Hibernate dispare
        // un auto-flush prematuro de la entidad sucia que colisione con el índice único UQ_mascota_publicacion_activa.
        if (nuevoEstado == EstadoPublicacion.Activa &&
                publicacionRepository.existsOtraPublicacionEnEstado(
                    publicacion.getMascota().getNroRegMunicipal(),
                    EstadoPublicacion.Activa,
                    id
                )) {
            throw new TransicionEstadoInvalidaException(
                "La mascota ya posee otra publicación de adopción activa."
            );
        }

        // Tell, Don't Ask: la entidad ejecuta la transición FSM y valida supervivencia
        publicacion.transicionarA(nuevoEstado);

        PublicacionAdopcion actualizada = publicacionRepository.save(publicacion);
        return ResponseEntity.ok(actualizada);
    }

    // =========================================================================
    // 3. Métodos Auxiliares
    // =========================================================================

    private Integer resolverIdRefugio(Integer refugioIdHeader, Integer refugioIdParam, Integer refugioIdBody) {
        if (refugioIdHeader != null) {
            return refugioIdHeader;
        }
        if (refugioIdParam != null) {
            return refugioIdParam;
        }
        return refugioIdBody;
    }
}

