package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Refugio;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RefugioDTO(
    @JsonProperty("id_refugio") Integer idRefugio,
    @JsonProperty("razon_social") String razonSocial,
    String correo,
    String telefono,
    String domicilio,
    @JsonProperty("habilitacion_municipal") String habilitacionMunicipal
) {
    public static RefugioDTO fromEntity(Refugio refugio) {
        if (refugio == null) {
            return null;
        }
        return new RefugioDTO(
            refugio.getIdRefugio(),
            refugio.getRazonSocial(),
            refugio.getCorreo(),
            refugio.getTelefono(),
            refugio.getDomicilio(),
            refugio.getHabilitacionMunicipal()
        );
    }
}
