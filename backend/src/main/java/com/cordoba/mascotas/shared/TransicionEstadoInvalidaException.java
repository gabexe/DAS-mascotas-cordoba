package com.cordoba.mascotas.shared;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
