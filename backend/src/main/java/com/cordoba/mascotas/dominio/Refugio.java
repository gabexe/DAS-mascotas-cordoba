package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refugios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Refugio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refugio")
    @JsonProperty("id_refugio")
    private Integer idRefugio;

    @Column(name = "razon_social", nullable = false, length = 150)
    @JsonProperty("razon_social")
    private String razonSocial;

    @Column(name = "correo", nullable = false, length = 150)
    @JsonProperty("correo")
    private String correo;

    @Column(name = "telefono", nullable = false, length = 50)
    @JsonProperty("telefono")
    private String telefono;

    @Column(name = "domicilio", nullable = false, length = 255)
    @JsonProperty("domicilio")
    private String domicilio;

    @Column(name = "habilitacion_municipal", nullable = false, length = 100)
    @JsonProperty("habilitacion_municipal")
    private String habilitacionMunicipal;

    @Column(name = "id_responsable")
    @JsonProperty("id_responsable")
    private Integer idResponsable;

    // =========================================================================
    // 1. Lógica de Dominio y Reglas de Negocio
    // =========================================================================

    /**
     * Comprueba si el refugio cuenta con una habilitación municipal registrada y no vacía.
     */
    public boolean tieneHabilitacionVigente() {
        return this.habilitacionMunicipal != null && !this.habilitacionMunicipal.isBlank();
    }

    /**
     * Valida que el refugio posea habilitación municipal vigente para operar en el sistema.
     * Empleado en el flujo de autenticación de usuarios de refugio.
     *
     * @throws AutenticacionInvalidaException si la habilitación municipal es nula o vacía.
     */
    public void validarHabilitacionVigente() {
        if (!tieneHabilitacionVigente()) {
            throw new AutenticacionInvalidaException(
                "El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente"
            );
        }
    }

    /**
     * Valida que el refugio posea habilitación municipal vigente para registrar publicaciones.
     * Empleado en el flujo de alta de publicaciones de adopción.
     *
     * @throws IllegalArgumentException si la habilitación municipal es nula o vacía.
     */
    public void validarHabilitacionParaPublicar() {
        if (!tieneHabilitacionVigente()) {
            throw new IllegalArgumentException(
                "El refugio no cuenta con habilitación municipal vigente para registrar publicaciones"
            );
        }
    }
}
