package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UsuarioRefugioDTO(
    @JsonProperty("id_ciudadano") Integer idCiudadano,
    String cuil,
    String nombre,
    String apellido,
    String correo,
    String telefono,
    @JsonProperty("id_refugio") Integer idRefugio,
    RefugioDTO refugio
) {
    public static UsuarioRefugioDTO fromEntity(Ciudadano ciudadano) {
        if (ciudadano == null) {
            return null;
        }
        return new UsuarioRefugioDTO(
            ciudadano.getIdCiudadano(),
            ciudadano.getCuil(),
            ciudadano.getNombre(),
            ciudadano.getApellido(),
            ciudadano.getCorreo(),
            ciudadano.getTelefono(),
            ciudadano.getRefugio() != null ? ciudadano.getRefugio().getIdRefugio() : null,
            RefugioDTO.fromEntity(ciudadano.getRefugio())
        );
    }
}
