package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CiudadanoRepository ciudadanoRepository;

    // =========================================================================
    // 1. Autenticación de Usuarios de Refugio (RF15)
    // =========================================================================

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ResponseEntity<Ciudadano> login(@Valid @RequestBody LoginRequest request) {
        if (request == null || request.cuil() == null || request.clave() == null) {
            throw new AutenticacionInvalidaException("El CUIL y la contraseña son requeridos");
        }

        String rawCuil = request.cuil().trim();
        String digitsOnly = rawCuil.replaceAll("[^0-9]", "");
        String formattedCuil = digitsOnly.length() == 11
            ? digitsOnly.substring(0, 2) + "-" + digitsOnly.substring(2, 10) + "-" + digitsOnly.substring(10)
            : rawCuil;

        Ciudadano ciudadano = ciudadanoRepository.findByCuil(rawCuil)
            .or(() -> ciudadanoRepository.findByCuil(formattedCuil))
            .or(() -> ciudadanoRepository.findByCuilNormalized(digitsOnly))
            .orElseThrow(() -> new AutenticacionInvalidaException("Credenciales incorrectas"));

        // Invoca el comportamiento rico de la entidad de dominio ("Tell, Don't Ask")
        ciudadano.autenticar(request.clave());

        return ResponseEntity.ok(ciudadano);
    }
}
