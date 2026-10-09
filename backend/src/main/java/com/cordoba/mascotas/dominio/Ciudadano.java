package com.cordoba.mascotas.dominio;

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
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciudadano")
    private Integer idCiudadano;

    @Column(name = "cuil", nullable = false, unique = true, length = 20)
    private String cuil;

    @Column(name = "clave", nullable = false, length = 255)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "domicilio", length = 255)
    private String domicilio;

    @Column(name = "habilitado", nullable = false)
    @Builder.Default
    private Boolean habilitado = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_refugio")
    private Refugio refugio;
}
