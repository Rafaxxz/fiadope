package pe.fiadope.api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.fiadope.api.exception.ApiException;

import java.util.Optional;

/** Acceso al usuario autenticado. Cada consulta transaccional se filtra con su id_tienda (multi-tienda). */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Optional<UsuarioPrincipal> opcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static UsuarioPrincipal get() {
        return opcional().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
    }

    public static Integer tienda() {
        Integer t = get().idTienda();
        if (t == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta operación es solo para usuarios de una tienda");
        }
        return t;
    }
}

