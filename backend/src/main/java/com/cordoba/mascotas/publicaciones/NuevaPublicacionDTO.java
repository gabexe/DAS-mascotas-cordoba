package com.cordoba.mascotas.publicaciones;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NuevaPublicacionDTO(
    @JsonProperty("id_refugio")
    Integer idRefugio,

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    String nombre,

    @NotBlank(message = "La especie es obligatoria (ej: Perro, Gato, Otro)")
    String especie,

    @NotBlank(message = "El sexo es obligatorio (ej: Macho, Hembra)")
    String sexo,

    @NotNull(message = "El año aproximado de nacimiento es obligatorio")
    @Min(value = 2000, message = "El año de nacimiento debe ser mayor o igual a 2000")
    @Max(value = 2030, message = "El año de nacimiento no puede ser futuro")
    @JsonProperty("anio_nacimiento")
    Integer anioNacimiento,

    String raza,

    String microchip,

    @NotBlank(message = "La fotografía de la mascota es obligatoria")
    String foto,

    @NotBlank(message = "Las características de la mascota son obligatorias")
    @JsonProperty("caracteristicas_mascota")
    String caracteristicasMascota,

    @NotBlank(message = "Las condiciones de adopción son obligatorias")
    @JsonProperty("condicion_adopcion")
    String condicionAdopcion
) {}
