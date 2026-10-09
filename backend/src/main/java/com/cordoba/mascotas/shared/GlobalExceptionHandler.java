package com.cordoba.mascotas.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. Manejo de Errores de Validación y Parámetros (400 Bad Request)
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Validación Fallida",
            "Existen campos inválidos en la solicitud",
            errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Solicitud Inválida",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Formato de Solicitud Inválido",
            "El cuerpo de la solicitud no tiene un formato JSON válido o contiene tipos de datos incompatibles"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = String.format("El parámetro '%s' debe ser de tipo válido", ex.getName());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Parámetro Inválido",
            mensaje
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // =========================================================================
    // 2. Manejo de Autenticación y Autorización (401 Unauthorized)
    // =========================================================================

    @ExceptionHandler(AutenticacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacionInvalida(AutenticacionInvalidaException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "No Autorizado",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // =========================================================================
    // 3. Manejo de Recursos No Encontrados (404 Not Found)
    // =========================================================================

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "No Encontrado",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // =========================================================================
    // 4. Manejo de Conflictos de Estado e Integridad (409 Conflict)
    // =========================================================================

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransicionEstadoInvalida(TransicionEstadoInvalidaException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflicto de Estado",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflicto de Integridad",
            "La operación entra en conflicto con las restricciones de datos del sistema municipal"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // =========================================================================
    // 5. Manejo de Métodos HTTP No Soportados (405 Method Not Allowed)
    // =========================================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "Método No Permitido",
            String.format("El método HTTP '%s' no está soportado para este endpoint", ex.getMethod())
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // =========================================================================
    // 6. Manejo de Errores No Controlados (500 Internal Server Error)
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Error Interno del Servidor",
            ex.getMessage() != null ? ex.getMessage() : "Ocurrió un error inesperado al procesar la solicitud"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
