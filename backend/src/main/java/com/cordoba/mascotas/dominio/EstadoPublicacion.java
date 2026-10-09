package com.cordoba.mascotas.dominio;

/**
 * Estados válidos para una publicación de adopción.
 * Maneja las reglas de transición (FSM):
 * - Activa: Mascota disponible. Puede transicionar a Pausada o Finalizada.
 * - Pausada: Disponibilidad suspendida temporalmente. Puede transicionar a Activa o Finalizada.
 * - Finalizada: Proceso concluido. Estado terminal e inmutable.
 */
public enum EstadoPublicacion {
    Activa,
    Pausada,
    Finalizada;

    /**
     * Valida si la transición hacia el nuevo estado es permitida según las reglas de negocio.
     *
     * @param nuevoEstado Estado al que se desea transicionar.
     * @return true si la transición es válida, false en caso contrario.
     */
    public boolean puedeTransicionarA(EstadoPublicacion nuevoEstado) {
        if (nuevoEstado == null) {
            return false;
        }

        // Estado terminal: Una vez finalizada, ninguna modificación de estado es permitida
        if (this == Finalizada) {
            return false;
        }

        // Si es el mismo estado, no hay transición
        if (this == nuevoEstado) {
            return true;
        }

        return switch (this) {
            case Activa -> nuevoEstado == Pausada || nuevoEstado == Finalizada;
            case Pausada -> nuevoEstado == Activa || nuevoEstado == Finalizada;
            case Finalizada -> false;
        };
    }

    /**
     * Parsea un valor textual a EstadoPublicacion ignorando diferencias de mayúsculas y minúsculas.
     * Retorna null si el valor es nulo, vacío o "TODAS".
     * Lanza IllegalArgumentException si el valor especificado no corresponde a ningún estado válido.
     */
    public static EstadoPublicacion fromString(String valor) {
        if (valor == null || valor.isBlank() || "TODAS".equalsIgnoreCase(valor.trim())) {
            return null;
        }
        for (EstadoPublicacion estado : values()) {
            if (estado.name().equalsIgnoreCase(valor.trim())) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + valor);
    }
}

