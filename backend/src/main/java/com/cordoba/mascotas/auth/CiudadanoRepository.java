package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CiudadanoRepository extends JpaRepository<Ciudadano, Integer> {

    // =========================================================================
    // 1. Consultas de Autenticación por CUIL
    // =========================================================================

    Optional<Ciudadano> findByCuil(String cuil);

    @Query("SELECT c FROM Ciudadano c WHERE REPLACE(REPLACE(REPLACE(c.cuil, '-', ''), ' ', ''), '.', '') = :cleanCuil")
    Optional<Ciudadano> findByCuilNormalized(@Param("cleanCuil") String cleanCuil);
}
