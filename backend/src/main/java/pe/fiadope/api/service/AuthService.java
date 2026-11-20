package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.config.AppProperties;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.domain.Usuario;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.UsuarioRepository;
import pe.fiadope.api.security.CaptchaService;
import pe.fiadope.api.security.JwtService;
import pe.fiadope.api.security.SesionService;
import pe.fiadope.api.security.UsuarioActual;
import pe.fiadope.api.security.UsuarioPrincipal;
import pe.fiadope.api.util.Validaciones;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;
    private final CaptchaService captcha;
    private final JwtService jwt;
    private final SesionService sesiones;
    private final CorreoService correo;
    private final BitacoraService bitacora;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService.Captcha nuevoCaptcha() {
        return captcha.generar();
    }

    /** HU01: usuario + contraseña segura + captcha. Si es correcto, envía el código MFA al correo (HU02). */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginRespuesta login(LoginRequest req) {
        if (!captcha.validar(req.captchaId(), req.captcha())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El código captcha no coincide. Intenta con uno nuevo.",
                    Map.of("captcha", "El código no coincide"));
        }
        Usuario u = usuarios.findByLogin(req.usuario().trim()).orElse(null);
        if (u == null) {
            bitacora.registrarDe(null, req.usuario(), "LOGIN_FALLIDO", Map.of("motivo", "usuario no registrado"));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(ahora)) {
            long min = Math.max(1, Duration.between(ahora, u.getBloqueadoHasta()).toMinutes() + 1);
            throw new ApiException(HttpStatus.LOCKED,
                    "Cuenta bloqueada por intentos fallidos. Intenta de nuevo en " + min + " minuto(s).");
        }
        if (!u.isActivo()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La cuenta está deshabilitada");
        }
        if (u.getTienda() != null && !u.getTienda().activa()) {
            bitacora.registrarDe(u, null, "LOGIN_RECHAZADO", Map.of("motivo", "tienda dada de baja"));
            throw new ApiException(HttpStatus.FORBIDDEN, "La tienda está dada de baja. Comunícate con FiadoPe.");
        }
        if (!encoder.matches(req.contrasena(), u.getHashClave())) {
            int intentos = u.getIntentosFallidos() + 1;
            int max = props.seguridad().maxIntentos();
            if (intentos >= max) {
                u.setIntentosFallidos((short) 0);
                u.setBloqueadoHasta(ahora.plusMinutes(props.seguridad().minutosBloqueo()));
                usuarios.save(u);
                bitacora.registrarDe(u, null, "BLOQUEO_CUENTA",
                        Map.of("intentos", max, "hasta", u.getBloqueadoHasta().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                throw new ApiException(HttpStatus.LOCKED, "Cuenta bloqueada por " + props.seguridad().minutosBloqueo()
                        + " minutos tras " + max + " intentos fallidos.");
            }
            u.setIntentosFallidos((short) intentos);
            usuarios.save(u);
            bitacora.registrarDe(u, null, "LOGIN_FALLIDO", Map.of("intento", intentos));
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Usuario o contraseña incorrectos. Te quedan " + (max - intentos) + " intento(s).");
        }
        u.setIntentosFallidos((short) 0);
        u.setBloqueadoHasta(null);
        byte[] semilla = new byte[24];
        random.nextBytes(semilla);
        u.setMfaToken(HexFormat.of().formatHex(semilla));
        String codigo = emitirCodigo(u);
        usuarios.save(u);
        return new LoginRespuesta(u.getMfaToken(), Validaciones.ocultarCorreo(u.getEmail()),
                props.seguridad().minutosMfa() * 60, props.mfa().mostrarCodigo() ? codigo : null);
    }

    private String emitirCodigo(Usuario u) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        u.setMfaSecreto(encoder.encode(codigo));
        u.setMfaExpira(LocalDateTime.now().plusMinutes(props.seguridad().minutosMfa()));
        u.setMfaIntentos((short) 0);
        correo.enviarCodigoMfa(u.getEmail(), u.getNombre(), codigo);
        return codigo;
    }

    /** HU02: reenvía un código nuevo; el anterior deja de funcionar. */
    @Transactional
    public LoginRespuesta reenviar(MfaReenvioRequest req) {
        Usuario u = porMfaToken(req.mfaToken());
        String codigo = emitirCodigo(u);
        usuarios.save(u);
        bitacora.registrarDe(u, null, "MFA_REENVIO", null);
        return new LoginRespuesta(u.getMfaToken(), Validaciones.ocultarCorreo(u.getEmail()),
                props.seguridad().minutosMfa() * 60, props.mfa().mostrarCodigo() ? codigo : null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SesionRespuesta verificar(MfaRequest req) {
        Usuario u = porMfaToken(req.mfaToken());
        if (u.getMfaExpira() == null || u.getMfaExpira().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El código venció. Pide uno nuevo.", Map.of("codigo", "Código vencido"));
        }
        if (u.getMfaIntentos() >= props.seguridad().maxIntentosMfa()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Superaste los intentos. Pide un código nuevo.",
                    Map.of("codigo", "Pide un código nuevo"));
        }
        if (u.getMfaSecreto() == null || !encoder.matches(req.codigo(), u.getMfaSecreto())) {
            u.setMfaIntentos((short) (u.getMfaIntentos() + 1));
            usuarios.save(u);
            bitacora.registrarDe(u, null, "MFA_FALLIDO", Map.of("intento", u.getMfaIntentos()));
            int quedan = props.seguridad().maxIntentosMfa() - u.getMfaIntentos();
            throw new ApiException(HttpStatus.BAD_REQUEST, "Código incorrecto. Te quedan " + quedan + " intento(s).",
                    Map.of("codigo", "Código incorrecto"));
        }
        u.setMfaSecreto(null);
        u.setMfaToken(null);
        u.setMfaExpira(null);
        u.setMfaIntentos((short) 0);
        usuarios.save(u);
        JwtService.TokenEmitido t = jwt.emitir(u);
        sesiones.abrir(t.jti(), u.getId(), u.getTienda() == null ? null : u.getTienda().getId());
        bitacora.registrarDe(u, null, "LOGIN", Map.of("rol", u.getRol().name()));
        return new SesionRespuesta(t.token(), props.seguridad().minutosInactividad() * 60, perfil(u));
    }

    private Usuario porMfaToken(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest("Vuelve a iniciar sesión");
        }
        return usuarios.findByMfaToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La verificación ya no es válida. Vuelve a iniciar sesión."));
    }

    @Transactional(readOnly = true)
    public PerfilDto yo() {
        UsuarioPrincipal p = UsuarioActual.get();
        return perfil(usuarios.findById(p.idUsuario()).orElseThrow());
    }

    @Transactional
    public void logout() {
        UsuarioPrincipal p = UsuarioActual.get();
        sesiones.cerrar(p.jti());
        bitacora.registrar("LOGOUT", "usuario", p.idUsuario(), null, null);
    }

    private PerfilDto perfil(Usuario u) {
        Integer idCliente = u.getRol() == Rol.CLIENTE
                ? clientes.findByUsuarioId(u.getId()).map(c -> c.getId()).orElse(null) : null;
        var t = u.getTienda();
        return new PerfilDto(u.getId(), u.getLogin(), u.getNombre(), Validaciones.ocultarCorreo(u.getEmail()), u.getRol(),
                t == null ? null : new TiendaResumen(t.getId(), t.getRazonSocial(), t.getRuc(), t.getGiro(),
                        t.getUbicacion(), t.getPropietario(), t.getLimiteGlobal()), idCliente);
    }
}
