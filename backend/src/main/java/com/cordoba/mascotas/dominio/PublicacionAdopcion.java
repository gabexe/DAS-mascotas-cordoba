package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import com.cordoba.mascotas.shared.TransicionEstadoInvalidaException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDate;

@Entity
@Table(name = "publicaciones_adopcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PublicacionAdopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nro_publicacion")
    @JsonProperty("nro_publicacion")
    private Integer nroPublicacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nro_reg_municipal", nullable = false)
    @JsonProperty("mascota")
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_refugio", nullable = false)
    @JsonProperty("refugio")
    private Refugio refugio;

    @Column(name = "fecha_publicacion", nullable = false)
    @Builder.Default
    @JsonProperty("fecha_publicacion")
    private LocalDate fechaPublicacion = LocalDate.now();

    @Column(name = "caracteristicas_mascota", nullable = false, length = 1000)
    @JsonProperty("caracteristicas_mascota")
    private String caracteristicasMascota;

    @Column(name = "condicion_adopcion", nullable = false, length = 1000)
    @JsonProperty("condicion_adopcion")
    private String condicionAdopcion;

    @Column(name = "foto", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    @JsonProperty("foto")
    private String foto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_publicacion", nullable = false, length = 50)
    @Builder.Default
    @JsonProperty("estado_publicacion")
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.Activa;

    // =========================================================================
    // 1. Métodos de Serialización Jackson
    // =========================================================================

    /**
     * Expone el número de registro municipal de la mascota para compatibilidad con PublicacionResponseDTO.
     */
    @JsonProperty("nro_reg_municipal")
    public Integer getNroRegMunicipal() {
        return this.mascota != null ? this.mascota.getNroRegMunicipal() : null;
    }

    /**
     * Expone el identificador del refugio para compatibilidad con PublicacionResponseDTO.
     */
    @JsonProperty("id_refugio")
    public Integer getIdRefugio() {
        return this.refugio != null ? this.refugio.getIdRefugio() : null;
    }

    // =========================================================================
    // 2. Lógica de Dominio y Máquina de Estados Finita (Tell, Don't Ask)
    // =========================================================================

    /**
     * Valida que la publicación pertenezca al refugio del usuario solicitante.
     * Aplica Tell, Don't Ask evitando que controladores o capas superiores inspeccionen atributos internos.
     * Si solicitanteRefugioId es nulo, no se aplica restricción.
     *
     * @param solicitanteRefugioId ID del refugio autenticado que ejecuta la operación.
     * @throws AutenticacionInvalidaException si la publicación pertenece a un refugio diferente.
     */
    public void validarPertenenciaRefugio(Integer solicitanteRefugioId) {
        if (solicitanteRefugioId != null && this.refugio != null 
                && !this.refugio.getIdRefugio().equals(solicitanteRefugioId)) {
            throw new AutenticacionInvalidaException(
                "No posee permisos para administrar publicaciones pertenecientes a otro refugio."
            );
        }
    }

    /**
     * Sobrecarga de conveniencia que acepta Long para compatibilidad con firmas de seguridad.
     */
    public void validarPertenenciaRefugio(Long solicitanteRefugioId) {
        if (solicitanteRefugioId != null) {
            validarPertenenciaRefugio(solicitanteRefugioId.intValue());
        }
    }

    /**
     * Ejecuta la transición de la publicación hacia un nuevo estado cumpliendo las reglas canónicas de la FSM:
     * - El nuevo estado no puede ser nulo.
     * - El estado Finalizada es terminal e inmutable.
     * - La transición debe ser permitida por la matriz de adyacencia de EstadoPublicacion.
     * - No es posible transicionar a Activa si la mascota no está registrada como viva.
     *
     * @param nuevoEstado Estado de destino.
     * @throws IllegalArgumentException si el nuevo estado es nulo.
     * @throws TransicionEstadoInvalidaException si la transición viola las invariantes de negocio.
     */
    public void transicionarA(EstadoPublicacion nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado de la publicación no puede ser nulo.");
        }
        if (this.estadoPublicacion == EstadoPublicacion.Finalizada) {
            throw new TransicionEstadoInvalidaException(
                "Una publicación en estado Finalizada no puede cambiar de estado."
            );
        }
        if (!this.estadoPublicacion.puedeTransicionarA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(
                String.format("Transición no válida de '%s' a '%s'.", this.estadoPublicacion, nuevoEstado)
            );
        }
        if (nuevoEstado == EstadoPublicacion.Activa && this.mascota != null && !this.mascota.estaViva()) {
            throw new TransicionEstadoInvalidaException(
                "No es posible activar la publicación de una mascota no disponible o registrada como fallecida."
            );
        }
        this.estadoPublicacion = nuevoEstado;
    }

    /**
     * Fábrica de dominio para construir una nueva instancia de PublicacionAdopcion en estado Activa.
     */
    public static PublicacionAdopcion crearParaRefugio(
        NuevaPublicacionDTO dto,
        Mascota mascota,
        Refugio refugio
    ) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la publicación son requeridos");
        }
        return PublicacionAdopcion.builder()
            .mascota(mascota)
            .refugio(refugio)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota(dto.caracteristicasMascota() != null ? dto.caracteristicasMascota().trim() : null)
            .condicionAdopcion(dto.condicionAdopcion() != null ? dto.condicionAdopcion().trim() : null)
            .foto(dto.foto() != null ? dto.foto().trim() : null)
            .estadoPublicacion(EstadoPublicacion.Activa)
            .build();
    }

    /**
     * Sobrecarga de método fábrica.
     */
    public static PublicacionAdopcion crear(NuevaPublicacionDTO dto, Mascota mascota, Refugio refugio) {
        return crearParaRefugio(dto, mascota, refugio);
    }

    /**
     * Método fábrica de dominio canónico independiente de DTOs.
     */
    public static PublicacionAdopcion crear(
        Mascota mascota,
        Refugio refugio,
        String caracteristicasMascota,
        String condicionAdopcion,
        String foto
    ) {
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota es obligatoria para la publicación.");
        }
        if (!mascota.estaViva()) {
            throw new IllegalArgumentException("No es posible publicar una mascota registrada como fallecida.");
        }
        if (refugio == null) {
            throw new IllegalArgumentException("El refugio es obligatorio para la publicación.");
        }
        if (!refugio.tieneHabilitacionVigente()) {
            throw new IllegalArgumentException("El refugio no cuenta con habilitación municipal vigente para registrar publicaciones.");
        }
        if (caracteristicasMascota == null || caracteristicasMascota.isBlank()) {
            throw new IllegalArgumentException("Las características de la mascota son obligatorias.");
        }
        if (condicionAdopcion == null || condicionAdopcion.isBlank()) {
            throw new IllegalArgumentException("Las condiciones de adopción son obligatorias.");
        }
        if (foto == null || foto.isBlank()) {
            throw new IllegalArgumentException("La fotografía de la mascota es obligatoria.");
        }

        return PublicacionAdopcion.builder()
            .mascota(mascota)
            .refugio(refugio)
            .fechaPublicacion(LocalDate.now())
            .caracteristicasMascota(caracteristicasMascota.trim())
            .condicionAdopcion(condicionAdopcion.trim())
            .foto(foto.trim())
            .estadoPublicacion(EstadoPublicacion.Activa)
            .build();
    }
}
