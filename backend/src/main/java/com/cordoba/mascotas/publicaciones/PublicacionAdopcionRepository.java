package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.EstadoPublicacion;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicacionAdopcionRepository extends JpaRepository<PublicacionAdopcion, Integer> {

    // =========================================================================
    // 1. Consultas con Carga Temprana de Relaciones (Eager Fetching)
    // =========================================================================

    @Query("""
        SELECT p FROM PublicacionAdopcion p
        JOIN FETCH p.mascota m
        JOIN FETCH p.refugio r
        WHERE (:idRefugio IS NULL OR r.idRefugio = :idRefugio)
          AND (:estado IS NULL OR p.estadoPublicacion = :estado)
          AND (:termino IS NULL OR :termino = ''
               OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(m.raza) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(m.especie) LIKE LOWER(CONCAT('%', :termino, '%')))
        ORDER BY p.nroPublicacion DESC
    """)
    List<PublicacionAdopcion> buscarPublicaciones(
        @Param("idRefugio") Integer idRefugio,
        @Param("estado") EstadoPublicacion estado,
        @Param("termino") String termino
    );

    @Query("""
        SELECT p FROM PublicacionAdopcion p
        JOIN FETCH p.mascota m
        JOIN FETCH p.refugio r
        WHERE p.nroPublicacion = :id
    """)
    Optional<PublicacionAdopcion> findByIdWithDetails(@Param("id") Integer id);

    // =========================================================================
    // 2. Validaciones de Integridad y Reglas de Negocio
    // =========================================================================

    @Query("""
        SELECT COUNT(p) > 0 FROM PublicacionAdopcion p
        WHERE p.mascota.nroRegMunicipal = :nroReg
          AND p.estadoPublicacion = :estado
          AND (:nroPubExcluir IS NULL OR p.nroPublicacion <> :nroPubExcluir)
    """)
    boolean existsOtraPublicacionEnEstado(
        @Param("nroReg") Integer nroRegMunicipal,
        @Param("estado") EstadoPublicacion estado,
        @Param("nroPubExcluir") Integer nroPublicacionExcluir
    );
}

