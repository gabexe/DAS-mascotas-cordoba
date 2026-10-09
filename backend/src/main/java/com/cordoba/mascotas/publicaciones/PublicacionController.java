package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.EstadoPublicacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final PublicacionService publicacionService;

    // =========================================================================
    // 1. Endpoints de Consulta (Lectura)
    // =========================================================================

    @GetMapping
    public ResponseEntity<List<PublicacionResponseDTO>> listar(
        @RequestParam(value = "refugioId", required = false) Integer refugioId,
        @RequestParam(value = "id_refugio", required = false) Integer idRefugioParam,
        @RequestHeader(value = "X-Refugio-Id", required = false) Integer refugioIdHeader,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String q
    ) {
        Integer queryRefugioId = idRefugioParam != null ? idRefugioParam : refugioId;
        Integer idRefugio = resolverIdRefugio(refugioIdHeader, queryRefugioId, null);
        EstadoPublicacion estadoEnum = EstadoPublicacion.fromString(estado);

        List<PublicacionResponseDTO> publicaciones = publicacionService.listarPublicaciones(idRefugio, estadoEnum, q);
        return ResponseEntity.ok(publicaciones);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> obtenerPorId(@PathVariable("id") Integer id) {
        PublicacionResponseDTO publicacion = publicacionService.obtenerPorId(id);
        return ResponseEntity.ok(publicacion);
    }

    // =========================================================================
    // 2. Endpoints de Modificación (Escritura)
    // =========================================================================

    @PostMapping
    public ResponseEntity<PublicacionResponseDTO> crear(
        @Valid @RequestBody NuevaPublicacionDTO dto,
        @RequestParam(value = "refugioId", required = false) Integer refugioId,
        @RequestParam(value = "id_refugio", required = false) Integer idRefugioParam,
        @RequestHeader(value = "X-Refugio-Id", required = false) Integer refugioIdHeader
    ) {
        Integer queryRefugioId = idRefugioParam != null ? idRefugioParam : refugioId;
        Integer idRefugio = resolverIdRefugio(refugioIdHeader, queryRefugioId, dto.idRefugio());
        PublicacionResponseDTO publicacionCreada = publicacionService.crearPublicacion(dto, idRefugio);
        return ResponseEntity.status(HttpStatus.CREATED).body(publicacionCreada);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PublicacionResponseDTO> cambiarEstado(
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
        Integer idRefugio = resolverIdRefugio(refugioIdHeader, queryRefugioId, dto.idRefugio());
        PublicacionResponseDTO publicacionActualizada = publicacionService.cambiarEstado(id, nuevoEstado, idRefugio);
        return ResponseEntity.ok(publicacionActualizada);
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

