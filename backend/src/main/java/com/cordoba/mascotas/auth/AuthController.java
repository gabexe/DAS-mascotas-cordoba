package com.cordoba.mascotas.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =========================================================================
    // 1. Autenticación de Usuarios de Refugio (RF15)
    // =========================================================================

    @PostMapping("/login")
    public ResponseEntity<UsuarioRefugioDTO> login(@Valid @RequestBody LoginRequest request) {
        UsuarioRefugioDTO usuario = authService.login(request);
        return ResponseEntity.ok(usuario);
    }
}
