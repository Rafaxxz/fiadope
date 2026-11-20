package pe.fiadope.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.fiadope.api.repository.TiendaRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SesionService sesionService;
    private final TiendaRepository tiendaRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        var principal = jwtService.leer(header.substring(7)).orElse(null);
        if (principal == null) {
            rechazar(res, "Token inválido o vencido");
            return;
        }
        if (!sesionService.tocar(principal.jti())) {
            rechazar(res, "Tu sesión se cerró por 15 minutos de inactividad. Ingresa nuevamente.");
            return;
        }
        if (principal.idTienda() != null) {
            boolean activa = tiendaRepository.findById(principal.idTienda()).map(t -> t.activa()).orElse(false);
            if (!activa) {
                sesionService.cerrar(principal.jti());
                rechazar(res, "La tienda está dada de baja");
                return;
            }
        }
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.rol().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(req, res);
    }

    private void rechazar(HttpServletResponse res, String mensaje) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), Map.of("status", 401, "mensaje", mensaje, "errores", Map.of()));
    }
}

