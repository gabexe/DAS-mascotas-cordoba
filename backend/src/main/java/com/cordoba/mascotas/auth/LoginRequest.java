package com.cordoba.mascotas.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El CUIL es obligatorio")
    String cuil,

    @NotBlank(message = "La contraseña es obligatoria")
    String clave
) {}
