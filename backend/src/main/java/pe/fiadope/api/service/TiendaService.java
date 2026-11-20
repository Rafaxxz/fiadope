package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.domain.Tienda;
import pe.fiadope.api.domain.Usuario;
import pe.fiadope.api.dto.Dtos.TiendaDto;
import pe.fiadope.api.dto.Dtos.TiendaRequest;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.TiendaRepository;
import pe.fiadope.api.repository.UsuarioRepository;
import pe.fiadope.api.security.SesionService;
import pe.fiadope.api.util.Validaciones;

import java.util.List;
import java.util.Map;

/** HU03: alta, baja y reactivación de tiendas (administrador de plataforma). */
@Service
@RequiredArgsConstructor
public class TiendaService {

    private final TiendaRepository tiendas;
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;
    private final SesionService sesiones;
    private final BitacoraService bitacora;

    @Transactional(readOnly = true)
    public List<TiendaDto> listar(String q, String giro, String estado) {
        return tiendas.buscar(Validaciones.vacioANull(q), Validaciones.vacioANull(giro), Validaciones.vacioANull(estado))
                .stream().map(this::dto).toList();
    }

    @Transactional
    public TiendaDto crear(TiendaRequest r) {
        if (tiendas.existsByRuc(r.ruc())) {
            throw ApiException.campo("ruc", "El RUC ya existe");
        }
        if (usuarios.existsByLogin(r.ruc())) {
            throw ApiException.campo("ruc", "El RUC ya está registrado como usuario");
        }
        if (usuarios.existsByEmailIgnoreCase(r.adminEmail())) {
            throw ApiException.campo("adminEmail", "El correo ya está registrado");
        }
        if (!Validaciones.claveSegura(r.adminClave())) {
            throw ApiException.campo("adminClave", "La contraseña necesita 8 caracteres, una mayúscula, un número y un símbolo");
        }
        Tienda t = new Tienda();
        copiar(r, t);
        tiendas.save(t);

        Usuario admin = new Usuario();
        admin.setTienda(t);
        admin.setLogin(r.ruc());
        admin.setNombre(r.propietario());
        admin.setEmail(r.adminEmail().trim().toLowerCase());
        admin.setHashClave(encoder.encode(r.adminClave()));
        admin.setRol(Rol.ADMIN_NEGOCIO);
        usuarios.save(admin);

        TiendaDto dto = dto(t);
        bitacora.registrarEnTienda(t.getId(), "ALTA_TIENDA", "tienda", t.getId(), null, dto);
        return dto;
    }

    @Transactional
    public TiendaDto actualizar(Integer id, TiendaRequest r) {
        Tienda t = buscar(id);
        if (!t.getRuc().equals(r.ruc())) {
            throw ApiException.campo("ruc", "El RUC no se puede modificar");
        }
        TiendaDto antes = dto(t);
        if (r.limiteGlobal().compareTo(clientes.sumaLimites(t.getId(), null)) < 0) {
            throw ApiException.campo("limiteGlobal", "El límite global no puede ser menor a la suma de los límites de sus clientes");
        }
        copiar(r, t);
        TiendaDto dto = dto(t);
        bitacora.registrarEnTienda(t.getId(), "EDITA_TIENDA", "tienda", t.getId(), antes, dto);
        return dto;
    }

    @Transactional
    public TiendaDto cambiarEstado(Integer id, boolean activar) {
        Tienda t = buscar(id);
        String antes = t.getEstado();
        t.setEstado(activar ? "ACTIVA" : "BAJA");
        if (!activar) {
            sesiones.cerrarPorTienda(t.getId());
        }
        bitacora.registrarEnTienda(t.getId(), activar ? "REACTIVA_TIENDA" : "BAJA_TIENDA", "tienda", t.getId(),
                Map.of("estado", antes), Map.of("estado", t.getEstado()));
        return dto(t);
    }

    private Tienda buscar(Integer id) {
        return tiendas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Tienda no encontrada"));
    }

    private void copiar(TiendaRequest r, Tienda t) {
        t.setRuc(r.ruc());
        t.setRazonSocial(r.razonSocial().trim());
        t.setPropietario(r.propietario().trim());
        t.setGiro(r.giro());
        t.setUbicacion(r.ubicacion().trim());
        t.setDireccion(r.direccion().trim());
        t.setTelefono(r.telefono());
        t.setLimiteGlobal(r.limiteGlobal());
    }

    private TiendaDto dto(Tienda t) {
        var admin = usuarios.findFirstByTiendaIdAndRol(t.getId(), Rol.ADMIN_NEGOCIO).orElse(null);
        long nClientes = clientes.findByTiendaIdOrderByNombresAsc(t.getId()).size();
        return new TiendaDto(t.getId(), t.getRuc(), t.getRazonSocial(), t.getPropietario(), t.getGiro(),
                t.getUbicacion(), t.getDireccion(), t.getTelefono(), t.getLimiteGlobal(),
                clientes.sumaLimites(t.getId(), null), t.getEstado(), t.getFechaAlta(), nClientes,
                admin == null ? null : admin.getLogin(), admin == null ? null : Validaciones.ocultarCorreo(admin.getEmail()));
    }
}

