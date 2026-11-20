package pe.fiadope.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.fiadope.api.config.AppProperties;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.domain.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration validez;

    public JwtService(AppProperties props) {
        byte[] bytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.validez = Duration.ofHours(props.jwt().horasValidez());
    }

    public record TokenEmitido(String token, String jti, Instant expira) {
    }

    public TokenEmitido emitir(Usuario u) {
        String jti = UUID.randomUUID().toString();
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(validez);
        String token = Jwts.builder()
                .id(jti)
                .subject(u.getLogin())
                .claim("uid", u.getId())
                .claim("nombre", u.getNombre())
                .claim("rol", u.getRol().name())
                .claim("tienda", u.getTienda() == null ? null : u.getTienda().getId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
        return new TokenEmitido(token, jti, expira);
    }

    public Optional<UsuarioPrincipal> leer(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioPrincipal(
                    c.get("uid", Integer.class), c.getSubject(), c.get("nombre", String.class),
                    Rol.valueOf(c.get("rol", String.class)), c.get("tienda", Integer.class), c.getId()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

