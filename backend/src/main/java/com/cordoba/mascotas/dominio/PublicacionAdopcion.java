package com.cordoba.mascotas.dominio;

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
public class PublicacionAdopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nro_publicacion")
    private Integer nroPublicacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nro_reg_municipal", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_refugio", nullable = false)
    private Refugio refugio;

    @Column(name = "fecha_publicacion", nullable = false)
    @Builder.Default
    private LocalDate fechaPublicacion = LocalDate.now();

    @Column(name = "caracteristicas_mascota", nullable = false, length = 1000)
    private String caracteristicasMascota;

    @Column(name = "condicion_adopcion", nullable = false, length = 1000)
    private String condicionAdopcion;

    @Column(name = "foto", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String foto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_publicacion", nullable = false, length = 50)
    @Builder.Default
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.Activa;
}
