package com.cordoba.mascotas.publicaciones;

import com.cordoba.mascotas.dominio.Mascota;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MascotaDTO(
    @JsonProperty("nro_reg_municipal") Integer nroRegMunicipal,
    String nombre,
    String especie,
    String sexo,
    @JsonProperty("anio_nacimiento") Integer anioNacimiento,
    String raza,
    String microchip,
    Boolean vive,
    @JsonProperty("id_refugio") Integer idRefugio
) {
    public static MascotaDTO fromEntity(Mascota mascota) {
        if (mascota == null) {
            return null;
        }
        return new MascotaDTO(
            mascota.getNroRegMunicipal(),
            mascota.getNombre(),
            mascota.getEspecie(),
            mascota.getSexo(),
            mascota.getAnioNacimiento(),
            mascota.getRaza(),
            mascota.getMicrochip(),
            mascota.getVive(),
            mascota.getRefugio() != null ? mascota.getRefugio().getIdRefugio() : null
        );
    }
}
