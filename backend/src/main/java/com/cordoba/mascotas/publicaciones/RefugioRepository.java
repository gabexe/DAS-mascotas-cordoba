package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.Refugio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefugioRepository extends JpaRepository<Refugio, Integer> {

    // =========================================================================
    // 1. Acceso a Datos de Entidad Refugio
    // =========================================================================
}
