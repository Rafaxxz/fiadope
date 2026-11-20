package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Cliente;
import pe.fiadope.api.domain.Consentimiento;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.domain.Usuario;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.finanzas.MotorFinanciero.TipoTasa;
import pe.fiadope.api.repository.*;
import pe.fiadope.api.security.UsuarioActual;
import pe.fiadope.api.util.Red;
import pe.fiadope.api.util.Validaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HU06: registro de clientes con sus condiciones de crédito y su consentimiento (Ley N.° 29733). */
@Service
@RequiredArgsConstructor
public class ClienteService {

    public static final String VERSION_POLITICA = "v1.0";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;
    private final ConsentimientoRepository consentimientos;
    private final CompraRepository compras;
    private final CuentaService cuenta;
    private final ParametroService parametros;
    private final PasswordEncoder encoder;
    private final BitacoraService bitacora;

    @Transactional(readOnly = true)
    public List<ClienteResumenDto> listar(String q) {
        return clientes.buscar(UsuarioActual.tienda(), Validaciones.vacioANull(q)).stream().map(cuenta::resumen).toList();
    }

    @Transactional(readOnly = true)
    public ClienteFichaDto ficha(Integer id) {
        return ficha(buscar(id));
    }

    @Transactional
    public ClienteFichaDto crear(ClienteRequest r) {
        Integer idTienda = UsuarioActual.tienda();
        if (!r.aceptaPolitica()) {
            throw ApiException.campo("aceptaPolitica", "El cliente debe aceptar la Política de Privacidad para registrarlo");
        }
        if (clientes.existsByTiendaIdAndDni(idTienda, r.dni())) {
            throw ApiException.campo("dni", "Ya tienes un cliente con ese documento");
        }
        if (usuarios.existsByLogin(r.dni())) {
            throw ApiException.campo("dni", "Ese documento ya tiene una cuenta en FiadoPe");
        }
        if (usuarios.existsByEmailIgnoreCase(r.email())) {
            throw ApiException.campo("email", "El correo ya está registrado");
        }
        if (!Validaciones.claveSegura(r.clave())) {
            throw ApiException.campo("clave", "La contraseña del cliente necesita 8 caracteres, una mayúscula, un número y un símbolo");
        }
        if (r.dni().length() != 8 && r.dni().length() != 11) {
            // el login exige 8 u 11 dígitos; un CE de otra longitud no podría ingresar a «Mi cuenta»
            throw ApiException.campo("dni", "Para crear su acceso, el documento debe tener 8 dígitos (DNI)");
        }
        validarCondiciones(r, idTienda, null);

        var tienda = tiendas.getReferenceById(idTienda);
        Usuario u = new Usuario();
        u.setTienda(tienda);
        u.setLogin(r.dni());
        u.setNombre(r.nombres().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setHashClave(encoder.encode(r.clave()));
        u.setRol(Rol.CLIENTE);
        usuarios.save(u);

        Cliente c = new Cliente();
        c.setTienda(tienda);
        c.setUsuario(u);
        c.setDni(r.dni());
        copiar(r, c);
        clientes.save(c);

        Consentimiento con = new Consentimiento();
        con.setCliente(c);
        con.setVersionPolitica(VERSION_POLITICA);
        con.setAcepta(true);
        con.setIp(Red.ipActual());
        consentimientos.save(con);

        bitacora.registrar("ALTA_CLIENTE", "cliente", c.getId(), null, condiciones(c));
        bitacora.registrar("CONSENTIMIENTO", "consentimiento", con.getId(), null,
                Map.of("cliente", c.getDni(), "version", VERSION_POLITICA, "acepta", true));
        return ficha(c);
    }

    @Transactional
    public ClienteFichaDto actualizar(Integer id, ClienteRequest r) {
        Cliente c = buscar(id);
        if (!c.getDni().equals(r.dni())) {
            throw ApiException.campo("dni", "El documento no se puede modificar");
        }
        validarCondiciones(r, c.getTienda().getId(), c.getId());
        if (r.limiteCredito().compareTo(c.getDeudaVigente()) < 0) {
            throw ApiException.campo("limiteCredito", "El límite no puede ser menor a la deuda vigente (S/ " + c.getDeudaVigente() + ")");
        }
        Map<String, Object> antes = condiciones(c);
        boolean cambioTasa = c.getTipoTasa() != r.tipoTasa() || c.getTasa().compareTo(porUno(r.tasaPorcentaje())) != 0
                || c.getTasaMora().compareTo(porUno(r.tasaMoraPorcentaje())) != 0 || c.getDiasPlazoTasa() != r.diasPlazoTasa()
                || c.getTipoTasaMora() != r.tipoTasaMora();
        copiar(r, c);
        Usuario u = c.getUsuario();
        if (u != null) {
            if (!u.getEmail().equalsIgnoreCase(r.email()) && usuarios.existsByEmailIgnoreCase(r.email())) {
                throw ApiException.campo("email", "El correo ya está registrado");
            }
            u.setEmail(r.email().trim().toLowerCase());
            u.setNombre(r.nombres().trim());
            if (r.clave() != null && !r.clave().isBlank()) {
                if (!Validaciones.claveSegura(r.clave())) {
                    throw ApiException.campo("clave", "La contraseña necesita 8 caracteres, una mayúscula, un número y un símbolo");
                }
                u.setHashClave(encoder.encode(r.clave()));
            }
        }
        // Una compra ya registrada conserva su tasa (Ley 29571, arts. 49-50): el cambio solo aplica a compras futuras
        bitacora.registrar(cambioTasa ? "CAMBIO_TASA" : "EDITA_CLIENTE", "cliente", c.getId(), antes, condiciones(c));
        return ficha(c);
    }

    @Transactional
    public ClienteFichaDto cambiarEstado(Integer id, boolean activar) {
        Cliente c = buscar(id);
        if (!activar && c.getDeudaVigente().signum() > 0) {
            throw ApiException.badRequest("No puedes dar de baja a un cliente con deuda vigente");
        }
        String antes = c.getEstado();
        c.setEstado(activar ? "ACTIVO" : "BAJA");
        if (c.getUsuario() != null) {
            c.getUsuario().setActivo(activar);
        }
        bitacora.registrar(activar ? "REACTIVA_CLIENTE" : "BAJA_CLIENTE", "cliente", c.getId(),
                Map.of("estado", antes), Map.of("estado", c.getEstado()));
        return ficha(c);
    }

    private void validarCondiciones(ClienteRequest r, Integer idTienda, Integer idCliente) {
        if (r.diaCorte().equals(r.diaPago())) {
            throw ApiException.campo("diaPago", "El día de corte y el día de pago deben ser distintos");
        }
        if (r.tipoTasa() == TipoTasa.NOMINAL && r.diasCapitalizacion() == null) {
            throw ApiException.campo("diasCapitalizacion", "Indica la capitalización de la tasa nominal");
        }
        if (r.diasCapitalizacion() != null && r.diasCapitalizacion() > r.diasPlazoTasa()) {
            throw ApiException.campo("diasCapitalizacion", "La capitalización no puede ser mayor al plazo de la tasa");
        }
        Integer cap = r.diasCapitalizacion() == null ? 1 : r.diasCapitalizacion();
        BigDecimal tea = MotorFinanciero.tea(MotorFinanciero.ted(r.tipoTasa(), porUno(r.tasaPorcentaje()), r.diasPlazoTasa(), cap));
        BigDecimal teaMor = MotorFinanciero.tea(MotorFinanciero.ted(r.tipoTasaMora(), porUno(r.tasaMoraPorcentaje()), r.diasPlazoTasa(), cap));
        BigDecimal topeComp = parametros.valor(ParametroService.TOPE_COMP);
        BigDecimal topeMor = parametros.valor(ParametroService.TOPE_MOR);
        if (tea.compareTo(topeComp) > 0) {
            throw ApiException.campo("tasaPorcentaje", "Supera el tope compensatorio del BCRP de " + Mapper.pct(topeComp, 2)
                    + " % (TEA equivalente " + Mapper.pct(tea, 2) + " %)");
        }
        if (teaMor.compareTo(topeMor) > 0) {
            throw ApiException.campo("tasaMoraPorcentaje", "Supera el tope moratorio del BCRP de " + Mapper.pct(topeMor, 2)
                    + " % (TEA equivalente " + Mapper.pct(teaMor, 2) + " %)");
        }
        var tienda = tiendas.findById(idTienda).orElseThrow();
        BigDecimal otros = clientes.sumaLimites(idTienda, idCliente);
        if (otros.add(r.limiteCredito()).compareTo(tienda.getLimiteGlobal()) > 0) {
            throw ApiException.campo("limiteCredito", "La suma de límites de tus clientes superaría el límite global de la tienda (S/ "
                    + tienda.getLimiteGlobal() + "). Disponible para asignar: S/ " + tienda.getLimiteGlobal().subtract(otros).max(BigDecimal.ZERO));
        }
    }

    private static BigDecimal porUno(BigDecimal porcentaje) {
        return porcentaje.divide(CIEN, 9, RoundingMode.HALF_UP);
    }

    private void copiar(ClienteRequest r, Cliente c) {
        c.setNombres(r.nombres().trim());
        c.setDireccion(r.direccion().trim());
        c.setTelefono(r.telefono());
        c.setTipoTasa(r.tipoTasa());
        c.setDiasPlazoTasa(r.diasPlazoTasa().shortValue());
        c.setDiasCapitalizacion(r.diasCapitalizacion() == null ? null : r.diasCapitalizacion().shortValue());
        c.setTasa(porUno(r.tasaPorcentaje()).setScale(7, RoundingMode.HALF_UP));
        c.setTipoTasaMora(r.tipoTasaMora());
        c.setTasaMora(porUno(r.tasaMoraPorcentaje()).setScale(7, RoundingMode.HALF_UP));
        c.setMoneda(r.moneda());
        c.setLimiteCredito(r.limiteCredito());
        c.setMaxMeses(r.maxMeses().shortValue());
        c.setDiaCorte(r.diaCorte().shortValue());
        c.setDiaPago(r.diaPago().shortValue());
    }

    private Map<String, Object> condiciones(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dni", c.getDni());
        m.put("nombres", c.getNombres());
        m.put("tipo_tasa", c.getTipoTasa());
        m.put("tasa", c.getTasa());
        m.put("tipo_tasa_mora", c.getTipoTasaMora());
        m.put("tasa_mora", c.getTasaMora());
        m.put("dias_plazo_tasa", c.getDiasPlazoTasa());
        m.put("dias_capitalizacion", c.getDiasCapitalizacion());
        m.put("limite_credito", c.getLimiteCredito());
        m.put("max_meses", c.getMaxMeses());
        m.put("dia_corte", c.getDiaCorte());
        m.put("dia_pago", c.getDiaPago());
        return m;
    }

    Cliente buscar(Integer id) {
        Integer t = UsuarioActual.tienda();
        return clientes.findByIdAndTiendaId(id, t).orElseThrow(() -> {
            // CP15: intento de ver un cliente de otra tienda
            if (clientes.existsById(id)) {
                bitacora.registrarDe(usuarios.findById(UsuarioActual.get().idUsuario()).orElse(null), null,
                        "ACCESO_DENEGADO", Map.of("tabla", "cliente", "id", id));
            }
            return ApiException.noEncontrado("Cliente no encontrado");
        });
    }

    ClienteFichaDto ficha(Cliente c) {
        BigDecimal ted = c.ted();
        BigDecimal tedMor = c.tedMora();
        var con = consentimientos.findFirstByClienteIdOrderByFechaHoraDesc(c.getId())
                .map(x -> new ConsentimientoDto(x.getVersionPolitica(), x.getFechaHora(), x.getIp())).orElse(null);
        var lista = compras.deCliente(c.getId()).stream().map(Mapper::compra).toList();
        return new ClienteFichaDto(c.getId(), c.getDni(), c.getNombres(), c.getDireccion(), c.getTelefono(),
                c.getUsuario() == null ? null : c.getUsuario().getEmail(),
                c.getUsuario() == null ? null : c.getUsuario().getLogin(),
                c.getTipoTasa(), c.getDiasPlazoTasa(),
                c.getDiasCapitalizacion() == null ? null : c.getDiasCapitalizacion().intValue(),
                Mapper.pct(c.getTasa(), 4), c.getTipoTasaMora(), Mapper.pct(c.getTasaMora(), 4),
                Mapper.pct(MotorFinanciero.tea(ted), 4), Mapper.pct(MotorFinanciero.tea(tedMor), 4),
                Mapper.pct(ted, 7), Mapper.pct(MotorFinanciero.tem(ted), 7), c.getMoneda(), c.getLimiteCredito(),
                c.getMaxMeses(), c.getDiaCorte(), c.getDiaPago(), c.getEstado(), c.getFechaRegistro(), con,
                cuenta.resumen(c), lista);
    }
}
