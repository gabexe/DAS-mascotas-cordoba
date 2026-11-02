package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciudadanos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciudadano")
    @JsonProperty("id_ciudadano")
    private Integer idCiudadano;

    @Column(name = "cuil", nullable = false, unique = true, length = 20)
    @JsonProperty("cuil")
    private String cuil;

    @Column(name = "clave", nullable = false, length = 255)
    @JsonIgnore
    private String clave;

    @Column(name = "nombre", nullable = false, length = 100)
    @JsonProperty("nombre")
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    @JsonProperty("apellido")
    private String apellido;

    @Column(name = "correo", nullable = false, length = 150)
    @JsonProperty("correo")
    private String correo;

    @Column(name = "telefono", length = 50)
    @JsonProperty("telefono")
    private String telefono;

    @Column(name = "domicilio", length = 255)
    @JsonProperty("domicilio")
    private String domicilio;

    @Column(name = "habilitado", nullable = false)
    @Builder.Default
    @JsonProperty("habilitado")
    private Boolean habilitado = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_refugio")
    @JsonProperty("refugio")
    private Refugio refugio;

    // =========================================================================
    // 1. Métodos de Serialización Jackson y Acceso Seguro
    // =========================================================================

    /**
     * Garantiza que la contraseña no se exponga jamás en la serialización JSON saliente.
     */
    @JsonIgnore
    public String getClave() {
        return this.clave;
    }

    /**
     * Expone el identificador del refugio directamente para concordancia con el contrato JSON.
     */
    @JsonProperty("id_refugio")
    public Integer getIdRefugio() {
        return this.refugio != null ? this.refugio.getIdRefugio() : null;
    }

    // =========================================================================
    // 2. Lógica de Dominio y Reglas de Negocio (Tell, Don't Ask)
    // =========================================================================

    /**
     * Evalúa si el ciudadano se encuentra habilitado por el Programa Municipal.
     */
    public boolean estaHabilitado() {
        return Boolean.TRUE.equals(this.habilitado);
    }

    /**
     * Autentica al ciudadano validando contraseña, habilitación municipal y pertenencia a refugio.
     * Aplica Tell, Don't Ask evitando que controladores o servicios inspeccionen atributos internos.
     *
     * @param claveIngresada Contraseña en texto plano a verificar.
     * @throws AutenticacionInvalidaException si la clave no coincide, el usuario está inhabilitado,
     *                                        no posee refugio asignado o el refugio no tiene habilitación vigente.
     */
    public void autenticar(String claveIngresada) {
        if (this.clave == null || !this.clave.equals(claveIngresada)) {
            throw new AutenticacionInvalidaException("Credenciales incorrectas");
        }
        if (!Boolean.TRUE.equals(this.habilitado)) {
            throw new AutenticacionInvalidaException("El usuario no se encuentra habilitado por el Programa Municipal");
        }
        if (this.refugio == null) {
            throw new AutenticacionInvalidaException("El usuario no pertenece a un refugio habilitado");
        }
        // Oculta delegado (Hide Delegate) validando el refugio
        this.refugio.validarHabilitacionVigente();
    }
}
