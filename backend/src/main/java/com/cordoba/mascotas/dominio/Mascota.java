package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
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
@Table(name = "mascotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nro_reg_municipal")
    @JsonProperty("nro_reg_municipal")
    private Integer nroRegMunicipal;

    @Column(name = "nombre", nullable = false, length = 100)
    @JsonProperty("nombre")
    private String nombre;

    @Column(name = "especie", nullable = false, length = 50)
    @JsonProperty("especie")
    private String especie;

    @Column(name = "sexo", nullable = false, length = 20)
    @JsonProperty("sexo")
    private String sexo;

    @Column(name = "año_nacimiento", nullable = false)
    @JsonProperty("anio_nacimiento")
    private Integer anioNacimiento;

    @Column(name = "raza", length = 100)
    @JsonProperty("raza")
    private String raza;

    @Column(name = "microchip", length = 50)
    @JsonProperty("microchip")
    private String microchip;

    @Column(name = "vive", nullable = false)
    @Builder.Default
    @JsonProperty("vive")
    private Boolean vive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_refugio", nullable = false)
    @JsonIgnore
    private Refugio refugio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    @JsonIgnore
    private Ciudadano responsable;

    // =========================================================================
    // 1. Métodos de Serialización Jackson
    // =========================================================================

    /**
     * Oculta la relación perezosa con Refugio en la serialización JSON para prevenir
     * LazyInitializationException y duplicación de datos con respecto a la publicación.
     */
    @JsonIgnore
    public Refugio getRefugio() {
        return this.refugio;
    }

    /**
     * Oculta la relación perezosa con Ciudadano en la serialización JSON para prevenir
     * LazyInitializationException y referencias circulares.
     */
    @JsonIgnore
    public Ciudadano getResponsable() {
        return this.responsable;
    }

    /**
     * Expone únicamente el identificador numérico del refugio para coincidencia estricta con MascotaDTO.
     */
    @JsonProperty("id_refugio")
    public Integer getIdRefugio() {
        return this.refugio != null ? this.refugio.getIdRefugio() : null;
    }

    // =========================================================================
    // 2. Lógica de Dominio y Fábrica (Tell, Don't Ask)
    // =========================================================================

    /**
     * Evalúa si la mascota continúa viva y disponible.
     */
    public boolean estaViva() {
        return Boolean.TRUE.equals(this.vive);
    }

    /**
     * Fábrica de dominio para construir una instancia de Mascota asociada a un Refugio y Responsable.
     */
    public static Mascota crearParaRefugio(
        String nombre,
        String especie,
        String sexo,
        Integer anioNacimiento,
        String raza,
        String microchip,
        Refugio refugio,
        Ciudadano responsable
    ) {
        if (refugio == null) {
            throw new IllegalArgumentException("El refugio es obligatorio para registrar la mascota");
        }
        return Mascota.builder()
            .nombre(nombre != null ? nombre.trim() : null)
            .especie(especie != null ? especie.trim() : null)
            .sexo(sexo != null ? sexo.trim() : null)
            .anioNacimiento(anioNacimiento)
            .raza(raza != null && !raza.isBlank() ? raza.trim() : "Mestizo")
            .microchip(microchip != null && !microchip.isBlank() ? microchip.trim() : null)
            .vive(true)
            .refugio(refugio)
            .responsable(responsable)
            .build();
    }

    /**
     * Sobrecarga de fábrica a partir del parameter object NuevaPublicacionDTO.
     */
    public static Mascota crearParaRefugio(NuevaPublicacionDTO dto, Refugio refugio, Ciudadano responsable) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la mascota no pueden ser nulos");
        }
        if (refugio == null) {
            throw new IllegalArgumentException("El refugio es obligatorio para registrar la mascota");
        }
        return crearParaRefugio(
            dto.nombre(),
            dto.especie(),
            dto.sexo(),
            dto.anioNacimiento(),
            dto.raza(),
            dto.microchip(),
            refugio,
            responsable
        );
    }

    /**
     * Sobrecarga de fábrica sin responsable explícito.
     */
    public static Mascota crearParaRefugio(NuevaPublicacionDTO dto, Refugio refugio) {
        return crearParaRefugio(dto, refugio, null);
    }
}
