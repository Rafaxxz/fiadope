package pe.fiadope.api.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.fiadope.api.config.AppProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesiones activas en el servidor. Permite invalidar el JWT al cerrar sesión, al dar de baja una tienda
 * y tras 15 minutos sin actividad (MIN_INACTIVIDAD), aunque el token aún no haya expirado.
 */
@Service
public class SesionService {

    private record Sesion(Integer idUsuario, Integer idTienda, Instant ultimaActividad) {
    }

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final Duration inactividad;

    public SesionService(AppProperties props) {
        this.inactividad = Duration.ofMinutes(props.seguridad().minutosInactividad());
    }

    public void abrir(String jti, Integer idUsuario, Integer idTienda) {
        sesiones.put(jti, new Sesion(idUsuario, idTienda, Instant.now()));
    }

    /** Devuelve true si la sesión sigue viva y renueva su última actividad. */
    public boolean tocar(String jti) {
        Sesion s = sesiones.get(jti);
        if (s == null) {
            return false;
        }
        if (s.ultimaActividad().plus(inactividad).isBefore(Instant.now())) {
            sesiones.remove(jti);
            return false;
        }
        sesiones.put(jti, new Sesion(s.idUsuario(), s.idTienda(), Instant.now()));
        return true;
    }

    public void cerrar(String jti) {
        sesiones.remove(jti);
    }

    public void cerrarPorTienda(Integer idTienda) {
        sesiones.entrySet().removeIf(e -> idTienda.equals(e.getValue().idTienda()));
    }

    public void cerrarPorUsuario(Integer idUsuario) {
        sesiones.entrySet().removeIf(e -> idUsuario.equals(e.getValue().idUsuario()));
    }

    @Scheduled(fixedRate = 60_000)
    void limpiar() {
        Instant limite = Instant.now().minus(inactividad);
        sesiones.entrySet().removeIf(e -> e.getValue().ultimaActividad().isBefore(limite));
    }
}

