package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.auth.CiudadanoRepository;
import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.Mascota;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.cordoba.mascotas.dominio.Refugio;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.cordoba.mascotas.shared.RecursoNoEncontradoException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionAdopcionRepository publicacionRepository;
    private final MascotaRepository mascotaRepository;
    private final RefugioRepository refugioRepository;
    private final CiudadanoRepository ciudadanoRepository;

    // =========================================================================
    // 1. Consultas de Publicaciones (Lectura)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PublicacionResponseDTO> listarPublicaciones(Integer refugioId, EstadoPublicacion estado, String busqueda) {
        String termino = (busqueda != null) ? busqueda.trim() : null;
        return publicacionRepository.buscarPublicaciones(refugioId, estado, termino)
            .stream()
            .map(PublicacionResponseDTO::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public PublicacionResponseDTO obtenerPorId(Integer nroPublicacion) {
        PublicacionAdopcion publicacion = publicacionRepository.findByIdWithDetails(nroPublicacion)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Publicación no encontrada con número: " + nroPublicacion
            ));
        return PublicacionResponseDTO.fromEntity(publicacion);
    }

    // =========================================================================
    // 2. Gestión de Publicaciones (Escritura y Ciclo de Vida)
    // =========================================================================

    @Transactional
    public PublicacionResponseDTO crearPublicacion(NuevaPublicacionDTO dto, Integer refugioIdOverride) {
        Integer refugioId = (refugioIdOverride != null) ? refugioIdOverride : dto.idRefugio();
        if (refugioId == null) {
            throw new IllegalArgumentException("El identificador del refugio es requerido para registrar la publicación");
        }

        Refugio refugio = refugioRepository.findById(refugioId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Refugio no encontrado con ID: " + refugioId));

        if (refugio.getHabilitacionMunicipal() == null || refugio.getHabilitacionMunicipal().isBlank()) {
            throw new IllegalArgumentException("El refugio no cuenta con habilitación municipal vigente para registrar publicaciones");
        }

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

        Mascota nuevaMascota = Mascota.builder()
            .nombre(dto.nombre().trim())
            .especie(dto.especie().trim())
            .sexo(dto.sexo().trim())
            .anioNacimiento(dto.anioNacimiento())
            .raza(dto.raza() != null && !dto.raza().isBlank() ? dto.raza().trim() : "Mestizo")
            .microchip(dto.microchip() != null && !dto.microchip().isBlank() ? dto.microchip().trim() : null)
            .vive(true)
            .refugio(refugio)
            .responsable(responsable)
            .build();
        Mascota mascotaGuardada = mascotaRepository.save(nuevaMascota);

        PublicacionAdopcion nuevaPublicacion = PublicacionAdopcion.builder()
            .mascota(mascotaGuardada)
            .refugio(refugio)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota(dto.caracteristicasMascota().trim())
            .condicionAdopcion(dto.condicionAdopcion().trim())
            .foto(dto.foto().trim())
            .estadoPublicacion(EstadoPublicacion.Activa)
            .build();
        PublicacionAdopcion guardada = publicacionRepository.save(nuevaPublicacion);

        return PublicacionResponseDTO.fromEntity(guardada);
    }

    @Transactional
    public PublicacionResponseDTO cambiarEstado(Integer nroPublicacion, EstadoPublicacion nuevoEstado) {
        return cambiarEstado(nroPublicacion, nuevoEstado, null);
    }

    @Transactional
    public PublicacionResponseDTO cambiarEstado(Integer nroPublicacion, EstadoPublicacion nuevoEstado, Integer solicitanteRefugioId) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado de la publicación no puede ser nulo");
        }

        PublicacionAdopcion publicacion = publicacionRepository.findByIdWithDetails(nroPublicacion)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Publicación no encontrada con número: " + nroPublicacion
            ));

        if (solicitanteRefugioId != null && !publicacion.getRefugio().getIdRefugio().equals(solicitanteRefugioId)) {
            throw new AutenticacionInvalidaException(
                "No posee permisos para administrar publicaciones pertenecientes a otro refugio."
            );
        }

        EstadoPublicacion actual = publicacion.getEstadoPublicacion();

        if (actual == EstadoPublicacion.Finalizada) {
            throw new TransicionEstadoInvalidaException(
                "Una publicación en estado Finalizada no puede cambiar de estado."
            );
        }

        if (!actual.puedeTransicionarA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(
                String.format("Transición no válida de '%s' a '%s'.", actual, nuevoEstado)
            );
        }

        if (nuevoEstado == EstadoPublicacion.Activa) {
            if (!Boolean.TRUE.equals(publicacion.getMascota().getVive())) {
                throw new TransicionEstadoInvalidaException(
                    "No es posible activar la publicación de una mascota no disponible o registrada como fallecida."
                );
            }

            boolean tieneOtraActiva = publicacionRepository.existsOtraPublicacionEnEstado(
                publicacion.getMascota().getNroRegMunicipal(),
                EstadoPublicacion.Activa,
                nroPublicacion
            );
            if (tieneOtraActiva) {
                throw new TransicionEstadoInvalidaException(
                    "La mascota ya posee otra publicación de adopción activa."
                );
            }
        }

        publicacion.setEstadoPublicacion(nuevoEstado);
        PublicacionAdopcion actualizada = publicacionRepository.save(publicacion);
        return PublicacionResponseDTO.fromEntity(actualizada);
    }
}

