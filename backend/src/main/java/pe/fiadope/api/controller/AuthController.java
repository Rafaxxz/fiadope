package pe.fiadope.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.security.CaptchaService;
import pe.fiadope.api.service.AuthService;

@Tag(name = "P01-P02 Autenticación")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @Operation(summary = "Genera un captcha de 4 dígitos")
    @PostMapping("/captcha")
    public CaptchaService.Captcha captcha() {
        return auth.nuevoCaptcha();
    }

    @Operation(summary = "HU01: usuario, contraseña y captcha. Envía el código MFA al correo")
    @PostMapping("/login")
    public LoginRespuesta login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @Operation(summary = "HU02: confirma el código de 6 dígitos y devuelve el JWT")
    @PostMapping("/mfa/verificar")
    public SesionRespuesta verificar(@Valid @RequestBody MfaRequest req) {
        return auth.verificar(req);
    }

    @Operation(summary = "HU02: reenvía un código nuevo (el anterior deja de funcionar)")
    @PostMapping("/mfa/reenviar")
    public LoginRespuesta reenviar(@Valid @RequestBody MfaReenvioRequest req) {
        return auth.reenviar(req);
    }

    @Operation(summary = "Perfil del usuario autenticado (renueva la actividad de la sesión)")
    @GetMapping("/yo")
    public PerfilDto yo() {
        return auth.yo();
    }

    @Operation(summary = "Cierra la sesión en el servidor")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        auth.logout();
        return ResponseEntity.noContent().build();
    }
}

