package com.cordoba.mascotas.dominio;

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
public class Refugio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refugio")
    private Integer idRefugio;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", nullable = false, length = 50)
    private String telefono;

    @Column(name = "domicilio", nullable = false, length = 255)
    private String domicilio;

    @Column(name = "habilitacion_municipal", nullable = false, length = 100)
    private String habilitacionMunicipal;

    @Column(name = "id_responsable")
    private Integer idResponsable;
}
