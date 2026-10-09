package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Integer> {

    // =========================================================================
    // 1. Validaciones de Registro Municipal
    // =========================================================================

    boolean existsByMicrochip(String microchip);
}

