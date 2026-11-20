package pe.fiadope.api.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pe.fiadope.api.config.AppProperties;
import pe.fiadope.api.domain.*;
import pe.fiadope.api.dto.Dtos.ClienteRequest;
import pe.fiadope.api.dto.Dtos.CompraRequest;
import pe.fiadope.api.dto.Dtos.ProductoRequest;
import pe.fiadope.api.finanzas.MotorFinanciero.TipoTasa;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.TiendaRepository;
import pe.fiadope.api.repository.UsuarioRepository;
import pe.fiadope.api.security.UsuarioPrincipal;
import pe.fiadope.api.service.ClienteService;
import pe.fiadope.api.service.CompraService;
import pe.fiadope.api.service.CorteService;
import pe.fiadope.api.service.ProductoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los juegos de prueba 1 y 2 del informe (sección 6.5) cuando la base está vacía,
 * usando los mismos servicios que la API (las cifras salen del motor financiero, no están escritas a mano).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements ApplicationRunner {

    private final AppProperties props;
    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final CompraService compraService;
    private final CorteService corteService;
    private final TransactionTemplate tx;

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seed().habilitado() || usuarios.count() > 0) {
            return;
        }
        log.info("Base vacía: cargando datos de demostración (juegos de prueba 1 y 2)");
        tx.executeWithoutResult(s -> {
            usuario(null, "70000000", "Administrador FiadoPe", "admin@fiadope.pe", "Admin#Fiado26", Rol.ADMIN_PLATAFORMA);

            Tienda lucho = tienda("10458792011", "Bodega Don Lucho", "Luis Torres Huamán", "Bodega y abarrotes",
                    "San Juan de Lurigancho", "Jr. Los Pinos 245", "987654321", "25000.00");
            Usuario adminLucho = usuario(lucho, "10458792011", "Luis Torres Huamán", "donlucho@fiadope.pe", "Bodega#Lucho26", Rol.ADMIN_NEGOCIO);

            Tienda espiga = tienda("20601234567", "Panadería La Espiga", "Carmen Rojas Vega", "Panadería",
                    "Los Olivos", "Av. Universitaria 3120", "976543210", "10000.00");
            Usuario adminEspiga = usuario(espiga, "20601234567", "Carmen Rojas Vega", "laespiga@fiadope.pe", "Espiga#Pan26", Rol.ADMIN_NEGOCIO);

            Tienda chabuca = tienda("10712345678", "Pollería El Brasero", "Mario Chávez Ríos", "Pollería",
                    "Comas", "Av. Túpac Amaru 1450", "965432109", "8000.00");
            usuario(chabuca, "10712345678", "Mario Chávez Ríos", "elbrasero@fiadope.pe", "Brasero#Pollo26", Rol.ADMIN_NEGOCIO);
            chabuca.setEstado("BAJA");

            como(adminLucho, () -> juegoUno());
            como(adminEspiga, () -> juegoDos());
        });
        SecurityContextHolder.clearContext();
        log.info("Datos de demostración listos. Revisa el README para los usuarios de prueba.");
    }

    private void juegoUno() {
        Map<String, Integer> p = new HashMap<>();
        p.put("combo", producto("Combo arroz y aceite", "Abarrotes y granel", "ABR-001", "Costeño / Primor", "11.00", "12.00", true, false, "assets/productos/arroz-aceite.jpg"));
        p.put("cocina", producto("Cocina de 2 hornillas", "Electro y hogar", "ELH-001", "Surge", "420.00", "450.00", true, true, "assets/productos/cocina.jpg"));
        p.put("pan", producto("Canasta pan y lácteos", "Lácteos y panadería", "LAC-001", "Gloria", "8.00", "8.55", true, false, "assets/productos/pan-lacteos.jpg"));
        p.put("gaseosa", producto("Gaseosa 3 L", "Bebidas y gaseosas", "BEB-001", "Inca Kola", "11.00", "12.00", true, false, "assets/productos/gaseosa.jpg"));
        p.put("leche", producto("Leche evaporada Gloria (tarro azul)", "Lácteos y panadería", "LAC-002", "Gloria", "4.20", "4.50", true, false, "assets/productos/leche.jpg"));
        p.put("arroz", producto("Arroz Costeño x kilo / granel", "Abarrotes y granel", "ABR-002", "Costeño", "4.20", "4.50", true, false, "assets/productos/arroz.jpg"));
        p.put("aceite", producto("Aceite Primor 900 ml", "Abarrotes y granel", "ABR-003", "Primor", "10.50", "11.00", true, false, "assets/productos/aceite.jpg"));
        p.put("licuadora", producto("Licuadora 1.5 L", "Electro y hogar", "ELH-002", "Oster", "180.00", "199.00", false, true, "assets/productos/licuadora.jpg"));

        Integer rosa = cliente("47891230", "Rosa Quispe Mamani", "Manzana B Lote 14", "912345678", "rosa.quispe@fiadope.pe",
                "Rosa#Fiado26", TipoTasa.EFECTIVA, 360, null, "24", TipoTasa.EFECTIVA, "15", "1500.00", 3, 20, 26);
        compra(rosa, p.get("combo"), "10", LocalDateTime.of(2026, 9, 3, 10, 15), Modalidad.FIN_DE_MES, null);
        compra(rosa, p.get("cocina"), "1", LocalDateTime.of(2026, 9, 15, 17, 40), Modalidad.CUOTAS, 3);
        compra(rosa, p.get("pan"), "10", LocalDateTime.of(2026, 9, 18, 8, 5), Modalidad.FIN_DE_MES, null);
        compra(rosa, p.get("gaseosa"), "5", LocalDateTime.of(2026, 9, 22, 19, 30), Modalidad.FIN_DE_MES, null);
        corte(rosa, LocalDate.of(2026, 9, 20));
        corte(rosa, LocalDate.of(2026, 10, 20));

        Integer manuel = cliente("08761234", "Manuel Flores Córdova", "Av. Canto Grande 420", "945678123", "manuel.flores@fiadope.pe",
                "Manuel#Fiado26", TipoTasa.EFECTIVA, 360, null, "24", TipoTasa.EFECTIVA, "15", "1000.00", 3, 20, 26);
        compra(manuel, p.get("arroz"), "20", LocalDateTime.of(2026, 11, 4, 9, 20), Modalidad.FIN_DE_MES, null);
        compra(manuel, p.get("leche"), "12", LocalDateTime.of(2026, 11, 12, 18, 10), Modalidad.FIN_DE_MES, null);

        Integer bertha = cliente("41298455", "Bertha Huamán Alanya", "Jr. Los Claveles 108", "956781234", "bertha.huaman@fiadope.pe",
                "Bertha#Fiado26", TipoTasa.NOMINAL, 360, 30, "22", TipoTasa.EFECTIVA, "12", "1200.00", 6, 15, 22);
        compra(bertha, p.get("aceite"), "4", LocalDateTime.of(2026, 11, 10, 11, 0), Modalidad.FIN_DE_MES, null);
        compra(bertha, p.get("licuadora"), "1", LocalDateTime.of(2026, 11, 13, 16, 45), Modalidad.CUOTAS, 4);
    }

    private void juegoDos() {
        Integer torta = producto("Torta de cumpleaños", "Lácteos y panadería", "PAN-001", "La Espiga", "110.00", "120.00", true, false, "assets/productos/torta.jpg");
        Integer bocaditos = producto("Bocaditos para evento (ciento)", "Lácteos y panadería", "PAN-002", "La Espiga", "280.00", "300.00", true, true, "assets/productos/bocaditos.jpg");
        Integer pan = producto("Pan francés semanal", "Lácteos y panadería", "PAN-003", "La Espiga", "70.00", "75.40", true, false, "assets/productos/pan-frances.jpg");

        Integer jorge = cliente("41234567", "Jorge Huamán Torres", "Calle Las Begonias 233", "923456781", "jorge.huaman@fiadope.pe",
                "Jorge#Fiado26", TipoTasa.NOMINAL, 360, 1, "36", TipoTasa.NOMINAL, "15", "800.00", 3, 25, 5);
        compra(jorge, torta, "1", LocalDateTime.of(2026, 10, 2, 15, 0), Modalidad.FIN_DE_MES, null);
        compra(jorge, bocaditos, "1", LocalDateTime.of(2026, 10, 10, 12, 30), Modalidad.CUOTAS, 2);
        compra(jorge, pan, "1", LocalDateTime.of(2026, 10, 20, 7, 0), Modalidad.FIN_DE_MES, null);
        corte(jorge, LocalDate.of(2026, 10, 25));
    }

    // ------------------------------------------------------------------ util
    private void como(Usuario u, Runnable accion) {
        var principal = new UsuarioPrincipal(u.getId(), u.getLogin(), u.getNombre(), u.getRol(),
                u.getTienda() == null ? null : u.getTienda().getId(), "seed");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()))));
        try {
            accion.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Tienda tienda(String ruc, String nombre, String propietario, String giro, String ubicacion, String direccion,
                          String telefono, String limite) {
        Tienda t = new Tienda();
        t.setRuc(ruc);
        t.setRazonSocial(nombre);
        t.setPropietario(propietario);
        t.setGiro(giro);
        t.setUbicacion(ubicacion);
        t.setDireccion(direccion);
        t.setTelefono(telefono);
        t.setLimiteGlobal(new BigDecimal(limite));
        return tiendas.save(t);
    }

    private Usuario usuario(Tienda t, String login, String nombre, String email, String clave, Rol rol) {
        Usuario u = new Usuario();
        u.setTienda(t);
        u.setLogin(login);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setHashClave(encoder.encode(clave));
        u.setRol(rol);
        return usuarios.save(u);
    }

    private Integer producto(String nombre, String categoria, String sku, String marca, String contado, String lista,
                             boolean finMes, boolean cuotas, String imagen) {
        return productoService.crear(new ProductoRequest(nombre, categoria, sku, marca, null, "UND",
                new BigDecimal(contado), new BigDecimal(lista), finMes, cuotas, imagen)).id();
    }

    private Integer cliente(String dni, String nombres, String direccion, String telefono, String email, String clave,
                            TipoTasa tipo, int plazo, Integer cap, String tasa, TipoTasa tipoMora, String tasaMora,
                            String limite, int maxMeses, int corte, int pago) {
        return clienteService.crear(new ClienteRequest(dni, nombres, direccion, telefono, email, clave, tipo, plazo, cap,
                new BigDecimal(tasa), tipoMora, new BigDecimal(tasaMora), "PEN", new BigDecimal(limite), maxMeses, corte,
                pago, true)).id();
    }

    private void compra(Integer cliente, Integer producto, String cantidad, LocalDateTime fecha, Modalidad modalidad, Integer n) {
        compraService.registrar(new CompraRequest(cliente, producto, new BigDecimal(cantidad), fecha, modalidad, n));
    }

    private void corte(Integer idCliente, LocalDate fechaCorte) {
        corteService.generarPara(clientes.findById(idCliente).orElseThrow(), fechaCorte);
    }
}
