package com.cordoba.mascotas.auth;

import com.cordoba.mascotas.dominio.Ciudadano;
import com.cordoba.mascotas.shared.AutenticacionInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CiudadanoRepository ciudadanoRepository;

    // =========================================================================
    // 1. Autenticación y Validación de Acceso
    // =========================================================================

    @Transactional(readOnly = true)
    public UsuarioRefugioDTO login(LoginRequest request) {
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

        if (!verificarClave(request.clave(), ciudadano.getClave())) {
            throw new AutenticacionInvalidaException("Credenciales incorrectas");
        }

        if (Boolean.FALSE.equals(ciudadano.getHabilitado())) {
            throw new AutenticacionInvalidaException("El usuario no se encuentra habilitado por el Programa Municipal");
        }

        if (ciudadano.getRefugio() == null) {
            throw new AutenticacionInvalidaException("El usuario no pertenece a un refugio habilitado");
        }

        if (ciudadano.getRefugio().getHabilitacionMunicipal() == null 
            || ciudadano.getRefugio().getHabilitacionMunicipal().isBlank()) {
            throw new AutenticacionInvalidaException("El refugio al que pertenece el usuario no cuenta con habilitación municipal vigente");
        }

        return UsuarioRefugioDTO.fromEntity(ciudadano);
    }

    // =========================================================================
    // 2. Métodos Auxiliares de Verificación
    // =========================================================================

    private boolean verificarClave(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        return rawPassword.equals(storedPassword);
    }
}
