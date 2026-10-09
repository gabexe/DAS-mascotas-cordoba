package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.auth.RefugioDTO;
import com.cordoba.mascotas.dominio.PublicacionAdopcion;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record PublicacionResponseDTO(
    @JsonProperty("nro_publicacion") Integer nroPublicacion,
    @JsonProperty("nro_reg_municipal") Integer nroRegMunicipal,
    @JsonProperty("id_refugio") Integer idRefugio,
    @JsonProperty("fecha_publicacion") LocalDate fechaPublicacion,
    @JsonProperty("caracteristicas_mascota") String caracteristicasMascota,
    @JsonProperty("condicion_adopcion") String condicionAdopcion,
    String foto,
    @JsonProperty("estado_publicacion") String estadoPublicacion,
    MascotaDTO mascota,
    RefugioDTO refugio
) {
    public static PublicacionResponseDTO fromEntity(PublicacionAdopcion pub) {
        if (pub == null) {
            return null;
        }
        return new PublicacionResponseDTO(
            pub.getNroPublicacion(),
            pub.getMascota() != null ? pub.getMascota().getNroRegMunicipal() : null,
            pub.getRefugio() != null ? pub.getRefugio().getIdRefugio() : null,
            pub.getFechaPublicacion(),
            pub.getCaracteristicasMascota(),
            pub.getCondicionAdopcion(),
            pub.getFoto(),
            pub.getEstadoPublicacion().name(),
            MascotaDTO.fromEntity(pub.getMascota()),
            RefugioDTO.fromEntity(pub.getRefugio())
        );
    }
}
