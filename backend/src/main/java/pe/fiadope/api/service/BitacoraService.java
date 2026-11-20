package pe.fiadope.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Bitacora;
import pe.fiadope.api.domain.Usuario;
import pe.fiadope.api.repository.BitacoraRepository;
import pe.fiadope.api.security.UsuarioActual;
import pe.fiadope.api.security.UsuarioPrincipal;
import pe.fiadope.api.util.Red;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final BitacoraRepository repo;
    private final ObjectMapper objectMapper;

    /** Registra un evento a nombre del usuario autenticado. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(String accion, String tabla, Integer idRegistro, Object antes, Object despues) {
        UsuarioPrincipal p = UsuarioActual.opcional().orElse(null);
        guardar(p == null ? null : p.idUsuario(), p == null ? null : p.idTienda(), p == null ? null : p.login(),
                accion, tabla, idRegistro, antes, despues);
    }

    /** Registra un evento de un usuario aún no autenticado (login, bloqueo, MFA). Se guarda aunque la operación falle. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarDe(Usuario u, String login, String accion, Object detalle) {
        guardar(u == null ? null : u.getId(), u == null || u.getTienda() == null ? null : u.getTienda().getId(),
                u == null ? login : u.getLogin(), accion, "usuario", u == null ? null : u.getId(), null, detalle);
    }

    /** Registra un evento con tienda explícita (usado por el administrador de plataforma). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrarEnTienda(Integer idTienda, String accion, String tabla, Integer idRegistro, Object antes, Object despues) {
        UsuarioPrincipal p = UsuarioActual.opcional().orElse(null);
        guardar(p == null ? null : p.idUsuario(), idTienda, p == null ? null : p.login(), accion, tabla, idRegistro, antes, despues);
    }

    private void guardar(Integer idUsuario, Integer idTienda, String login, String accion, String tabla,
                         Integer idRegistro, Object antes, Object despues) {
        Bitacora b = new Bitacora();
        b.setIdUsuario(idUsuario);
        b.setIdTienda(idTienda);
        b.setUsuarioLogin(login);
        b.setAccion(accion);
        b.setTabla(tabla);
        b.setIdRegistro(idRegistro);
        b.setValorAntes(json(antes));
        b.setValorDespues(json(despues));
        b.setIp(Red.ipActual());
        repo.save(b);
    }

    private String json(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

