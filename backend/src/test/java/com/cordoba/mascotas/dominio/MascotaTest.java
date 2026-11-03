package com.cordoba.mascotas.dominio;

import com.cordoba.mascotas.publicaciones.NuevaPublicacionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MascotaTest {

    @Test
    @DisplayName("estaViva retorna true cuando vive es Boolean.TRUE")
    void testEstaVivaTrue() {
        Mascota mascota = Mascota.builder().vive(true).build();
        assertTrue(mascota.estaViva());
    }

    @Test
    @DisplayName("estaViva retorna false cuando vive es Boolean.FALSE")
    void testEstaVivaFalse() {
        Mascota mascota = Mascota.builder().vive(false).build();
        assertFalse(mascota.estaViva());
    }

    @Test
    @DisplayName("estaViva retorna false de forma segura cuando vive es null")
    void testEstaVivaFalseCuandoViveEsNulo() {
        Mascota mascota = Mascota.builder().vive(null).build();
        assertFalse(mascota.estaViva());
    }

    @Test
    @DisplayName("crearParaRefugio inicializa correctamente la mascota con valores trimeados y estado vive=true")
    void testCrearParaRefugioExitoso() {
        Refugio refugio = Refugio.builder().idRefugio(101).build();
        Ciudadano responsable = Ciudadano.builder().idCiudadano(1).build();

        NuevaPublicacionDTO dto = new NuevaPublicacionDTO(
            101,
            "  Firulais  ",
            "  Canina  ",
            "  Macho  ",
            2021,
            "  Golden Retriever  ",
            "  MC-12345  ",
            "data:image/png;base64,abc",
            "Cariñoso",
            "Patio cerrado"
        );

        Mascota creada = Mascota.crearParaRefugio(dto, refugio, responsable);

        assertNotNull(creada);
        assertEquals("Firulais", creada.getNombre());
        assertEquals("Canina", creada.getEspecie());
        assertEquals("Macho", creada.getSexo());
        assertEquals(2021, creada.getAnioNacimiento());
        assertEquals("Golden Retriever", creada.getRaza());
        assertEquals("MC-12345", creada.getMicrochip());
        assertTrue(creada.estaViva());
        assertEquals(refugio, creada.getRefugio());
        assertEquals(responsable, creada.getResponsable());
    }

    @Test
    @DisplayName("crearParaRefugio asigna raza 'Mestizo' si el DTO contiene raza nula o vacía")
    void testCrearParaRefugioRazaPorDefecto() {
        Refugio refugio = Refugio.builder().idRefugio(101).build();
        NuevaPublicacionDTO dtoConRazaVacia = new NuevaPublicacionDTO(
            101, "Milo", "Perro", "Macho", 2022, "   ", null, "foto", "carac", "cond"
        );

        Mascota creada = Mascota.crearParaRefugio(dtoConRazaVacia, refugio, null);
        assertEquals("Mestizo", creada.getRaza());

        NuevaPublicacionDTO dtoConRazaNula = new NuevaPublicacionDTO(
            101, "Milo", "Perro", "Macho", 2022, null, null, "foto", "carac", "cond"
        );
        Mascota creada2 = Mascota.crearParaRefugio(dtoConRazaNula, refugio, null);
        assertEquals("Mestizo", creada2.getRaza());
    }

    @Test
    @DisplayName("crearParaRefugio deja microchip en null si el DTO no provee uno o viene en blanco")
    void testCrearParaRefugioMicrochipOpcional() {
        Refugio refugio = Refugio.builder().idRefugio(101).build();
        NuevaPublicacionDTO dtoSinChip = new NuevaPublicacionDTO(
            101, "Luna", "Gato", "Hembra", 2023, "Común", "   ", "foto", "carac", "cond"
        );

        Mascota creada = Mascota.crearParaRefugio(dtoSinChip, refugio, null);
        assertNull(creada.getMicrochip());
    }
}
