package com.cordoba.mascotas.publicaciones;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CambioEstadoDTO(
    @NotBlank(message = "El nuevo estado de la publicación es obligatorio")
    @JsonProperty("nuevo_estado")
    @JsonAlias("estado")
    String nuevoEstado,

    @JsonProperty("id_refugio")
    Integer idRefugio
) {
    public CambioEstadoDTO(String nuevoEstado) {
        this(nuevoEstado, null);
    }
}

