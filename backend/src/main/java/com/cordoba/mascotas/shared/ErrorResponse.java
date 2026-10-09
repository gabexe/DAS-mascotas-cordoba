package com.cordoba.mascotas.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String mensaje,
    Map<String, String> detalles
) {
    public static ErrorResponse of(int status, String error, String mensaje) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, null);
    }

    public static ErrorResponse of(int status, String error, String mensaje, Map<String, String> detalles) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, detalles);
    }
}
