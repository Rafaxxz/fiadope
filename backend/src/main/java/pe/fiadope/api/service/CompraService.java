package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.*;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.finanzas.MotorFinanciero.FilaCronograma;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.CompraRepository;
import pe.fiadope.api.repository.ProductoRepository;
import pe.fiadope.api.security.UsuarioActual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HU07 y HU08: simulación y registro de compras fiadas (fin de mes o cuotas) y su cronograma francés. */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final CompraRepository compras;
    private final BitacoraService bitacora;
    private final Clock clock;

    /** Resultado interno del cálculo (F1, F4, F5, F8, F9, F10, F11, F12). */
    private record Calculo(Cliente cliente, Producto producto, BigDecimal cantidad, BigDecimal capital,
                           LocalDateTime fechaHora, Modalidad modalidad, Integer nCuotas, LocalDate fechaCorte,
                           LocalDate fechaPago, int dias, BigDecimal ted, BigDecimal tedMora, BigDecimal tem,
                           BigDecimal tea, BigDecimal montoS, BigDecimal interes, BigDecimal cuota,
                           List<FilaCronograma> filas, BigDecimal totalPlazo, BigDecimal disponible,
                           boolean excede, String mensaje) {
    }

    private Calculo calcular(CompraRequest r) {
        Integer t = UsuarioActual.tienda();
        Cliente c = clientes.findByIdAndTiendaId(r.idCliente(), t)
                .orElseThrow(() -> ApiException.campo("idCliente", "Cliente no encontrado"));
        if (!c.activo()) {
            throw ApiException.campo("idCliente", "El cliente está dado de baja");
        }
        Producto p = productos.findByIdAndTiendaId(r.idProducto(), t)
                .orElseThrow(() -> ApiException.campo("idProducto", "Producto no encontrado"));
        if (!p.activo()) {
            throw ApiException.campo("idProducto", "El producto está dado de baja");
        }
        if (r.modalidad() == Modalidad.FIN_DE_MES && !p.isPermiteFinMes()) {
            throw ApiException.campo("modalidad", "Este producto no se puede fiar a fin de mes");
        }
        if (r.modalidad() == Modalidad.CUOTAS && !p.isPermiteCuotas()) {
            throw ApiException.campo("modalidad", "Este producto no se puede fiar en cuotas");
        }
        Integer n = null;
        if (r.modalidad() == Modalidad.CUOTAS) {
            n = r.nCuotas() == null ? 2 : r.nCuotas();
            if (n < 2 || n > c.getMaxMeses()) {
                throw ApiException.campo("nCuotas", "El número de cuotas debe estar entre 2 y " + c.getMaxMeses()
                        + " (plazo máximo del cliente)");
            }
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime fechaHora = r.fechaHora() == null ? ahora : r.fechaHora();
        if (fechaHora.isAfter(ahora.plusMinutes(1))) {
            throw ApiException.campo("fechaHora", "No se permiten fechas futuras");
        }
        BigDecimal capital = MotorFinanciero.redondear(p.getPrecioLista().multiply(r.cantidad()));
        LocalDate fecha = fechaHora.toLocalDate();
        LocalDate corte = MotorFinanciero.fechaCorteDeCompra(fecha, c.getDiaCorte());
        LocalDate pago = MotorFinanciero.fechaPagoDeCorte(corte, c.getDiaPago());
        int dias = MotorFinanciero.diasComerciales(fecha, pago);
        BigDecimal ted = c.ted();
        BigDecimal tedMora = c.tedMora();
        BigDecimal tem = MotorFinanciero.tem(ted);
        BigDecimal tea = MotorFinanciero.tea(ted);
        BigDecimal montoS = MotorFinanciero.redondear(MotorFinanciero.valorFuturo(capital, ted, dias));
        BigDecimal interes = montoS.subtract(capital);
        BigDecimal cuota = null;
        List<FilaCronograma> filas = List.of();
        BigDecimal total = montoS;
        if (n != null) {
            filas = MotorFinanciero.cronograma(montoS, tem, n, pago);
            cuota = filas.get(0).cuota();
            total = MotorFinanciero.totalVentaPlazo(filas);
        }
        BigDecimal disponible = MotorFinanciero.creditoDisponible(c.getLimiteCredito(), c.getDeudaVigente());
        boolean excede = !MotorFinanciero.dentroDelLimite(capital, c.getLimiteCredito(), c.getDeudaVigente());
        String mensaje = excede ? "Supera su crédito disponible de " + disponible.toPlainString() : null;
        return new Calculo(c, p, r.cantidad(), capital, fechaHora, r.modalidad(), n, corte, pago, dias, ted, tedMora,
                tem, tea, montoS, interes, cuota, filas, total, disponible, excede, mensaje);
    }

    @Transactional(readOnly = true)
    public SimulacionDto simular(CompraRequest r) {
        Calculo k = calcular(r);
        var filas = k.filas().stream().map(f -> new FilaCronogramaDto(f.numero(), f.vencimiento(), f.saldoInicial(),
                f.interes(), f.amortizacion(), f.cuota(), f.saldoFinal(), false)).toList();
        boolean despues = k.fechaHora().toLocalDate().getDayOfMonth() > k.cliente().getDiaCorte();
        return new SimulacionDto(k.cliente().getId(), k.cliente().getNombres(), k.producto().getId(),
                k.producto().getNombre(), k.cantidad(), k.producto().getPrecioLista(), k.capital(), k.modalidad(),
                k.nCuotas(), k.fechaHora().toLocalDate(), k.fechaCorte(), k.fechaPago(), despues, k.dias(),
                Mapper.pct(k.ted(), 7), Mapper.pct(k.tem(), 7), Mapper.pct(k.tea(), 4),
                k.modalidad() == Modalidad.FIN_DE_MES ? k.montoS() : null,
                k.interes(), k.modalidad() == Modalidad.CUOTAS ? k.montoS() : null, k.cuota(), filas,
                k.totalPlazo(), k.totalPlazo().subtract(k.capital()), k.cliente().getLimiteCredito(),
                k.cliente().getDeudaVigente(), k.disponible(), k.excede(), k.mensaje(), k.cliente().getMaxMeses());
    }

    @Transactional
    public CompraDto registrar(CompraRequest r) {
        Calculo k = calcular(r);
        if (k.excede()) {
            throw ApiException.campo("cantidad", k.mensaje());
        }
        Compra c = new Compra();
        c.setCliente(k.cliente());
        c.setProducto(k.producto());
        c.setFechaHora(k.fechaHora());
        c.setCantidad(k.cantidad());
        c.setPrecioUnitario(k.producto().getPrecioLista());
        c.setCapital(k.capital());
        c.setModalidad(k.modalidad());
        c.setNCuotas(k.nCuotas() == null ? null : k.nCuotas().shortValue());
        c.setFechaCorte(k.fechaCorte());
        c.setFechaPagoAsignada(k.fechaPago());
        c.setDias((short) k.dias());
        c.setMontoS(k.montoS());
        c.setInteres(k.interes());
        c.setTed(k.ted());
        c.setTedMora(k.tedMora());
        c.setTem(k.tem());
        c.setTea(k.tea().setScale(10, RoundingMode.HALF_UP));
        c.setCuotaFija(k.cuota());
        c.setTotalPlazo(k.totalPlazo());
        for (FilaCronograma f : k.filas()) {
            Cuota q = new Cuota();
            q.setCompra(c);
            q.setNumero((short) f.numero());
            q.setVencimiento(f.vencimiento());
            q.setSaldoInicial(f.saldoInicial());
            q.setInteres(f.interes());
            q.setAmortizacion(f.amortizacion());
            q.setCuota(f.cuota());
            q.setSaldoFinal(f.saldoFinal());
            c.getCuotas().add(q);
        }
        compras.save(c);
        Cliente cl = k.cliente();
        BigDecimal antes = cl.getDeudaVigente();
        cl.setDeudaVigente(antes.add(k.capital()));

        Map<String, Object> despues = new LinkedHashMap<>();
        despues.put("cliente", cl.getDni());
        despues.put("producto", k.producto().getNombre());
        despues.put("capital", k.capital());
        despues.put("modalidad", k.modalidad());
        despues.put("n_cuotas", k.nCuotas());
        despues.put("fecha_pago", k.fechaPago().toString());
        despues.put("monto_s", k.montoS());
        despues.put("deuda_vigente", cl.getDeudaVigente());
        bitacora.registrar("COMPRA", "compra", c.getId(), Map.of("deuda_vigente", antes), despues);
        return Mapper.compra(c);
    }

    @Transactional(readOnly = true)
    public List<CompraDto> deCliente(Integer idCliente) {
        clientes.findByIdAndTiendaId(idCliente, UsuarioActual.tienda())
                .orElseThrow(() -> ApiException.noEncontrado("Cliente no encontrado"));
        return compras.deCliente(idCliente).stream().map(Mapper::compra).toList();
    }

    @Transactional(readOnly = true)
    public CronogramaDto cronograma(Integer idCompra) {
        Compra c = compras.buscarEnTienda(idCompra, UsuarioActual.tienda())
                .orElseThrow(() -> ApiException.noEncontrado("Compra no encontrada"));
        return cronograma(c);
    }

    public static CronogramaDto cronograma(Compra c) {
        if (c.getModalidad() != Modalidad.CUOTAS) {
            throw ApiException.badRequest("La compra es a fin de mes: no tiene cronograma de cuotas");
        }
        var filas = c.getCuotas().stream().map(Mapper::fila).toList();
        BigDecimal totInteres = c.getCuotas().stream().map(Cuota::getInteres).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totAmort = c.getCuotas().stream().map(Cuota::getAmortizacion).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CronogramaDto(Mapper.compra(c), c.getCliente().getNombres(), c.getCliente().getDni(),
                Mapper.pct(c.getTea(), 4), Mapper.pct(c.getTem(), 7), Mapper.pct(c.getTed(), 7), c.getDias(),
                c.getCapital(), c.getInteres(), c.getMontoS(), c.getCuotaFija(), filas, totInteres, totAmort,
                c.getTotalPlazo(), c.getTotalPlazo().subtract(c.getCapital()));
    }
}

