package com.cordoba.mascotas.shared;

public class AutenticacionInvalidaException extends RuntimeException {
    public AutenticacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
