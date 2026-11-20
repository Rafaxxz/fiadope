package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.fiadope.api.domain.*;
import pe.fiadope.api.dto.Dtos.ClienteResumenDto;
import pe.fiadope.api.dto.Dtos.ItemDto;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.repository.CompraRepository;
import pe.fiadope.api.repository.CuotaRepository;
import pe.fiadope.api.repository.EstadoCuentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cuenta corriente del cliente: saldo al corte (F6), mora a la fecha (F7) y crédito disponible (F11). */
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CompraRepository compras;
    private final CuotaRepository cuotas;
    private final EstadoCuentaRepository estados;
    private final Clock clock;

    public LocalDate hoy() {
        return LocalDate.now(clock);
    }

    /** Ítems que entran en el estado de cuenta de una fecha de pago y aún no se facturaron (F6). */
    public List<Facturable> porFacturar(Cliente c, LocalDate fechaPago) {
        List<Facturable> lista = new ArrayList<>();
        for (Compra co : compras.finDeMesPorFacturar(c.getId(), fechaPago, Modalidad.FIN_DE_MES)) {
            lista.add(new Facturable("COMPRA", co, null, co.getProducto().getNombre(), co.getFechaHora().toLocalDate(),
                    co.getDias(), co.getCapital(), co.getInteres(), co.getMontoS()));
        }
        for (Cuota q : cuotas.porFacturar(c.getId(), fechaPago)) {
            Compra co = q.getCompra();
            lista.add(new Facturable("CUOTA", co, q, co.getProducto().getNombre() + " · cuota " + q.getNumero() + " de " + co.getNCuotas(),
                    co.getFechaHora().toLocalDate(), 0, q.getAmortizacion(), q.getInteres(), q.getCuota()));
        }
        lista.sort(Comparator.comparing(Facturable::fecha));
        return lista;
    }

    public record Facturable(String tipo, Compra compra, Cuota cuota, String detalle, LocalDate fecha, int dias,
                             BigDecimal monto, BigDecimal interes, BigDecimal total) {
        public ItemDto dto() {
            return new ItemDto(tipo, detalle, fecha, dias, monto, interes, total);
        }
    }

    public static BigDecimal suma(List<Facturable> items) {
        return items.stream().map(Facturable::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Resumen de la libreta del cliente a la fecha de hoy. */
    public ClienteResumenDto resumen(Cliente c) {
        LocalDate hoy = hoy();
        List<EstadoCuenta> pendientes = estados.findByClienteIdAndEstadoOrderByFechaCorteAsc(c.getId(), "PENDIENTE");
        BigDecimal saldo;
        BigDecimal mora = BigDecimal.ZERO;
        int diasMora = 0;
        LocalDate fechaCorte;
        LocalDate fechaPago;
        Integer idPendiente = null;
        if (!pendientes.isEmpty()) {
            EstadoCuenta e = pendientes.get(0);
            idPendiente = e.getId();
            fechaCorte = e.getFechaCorte();
            fechaPago = e.getFechaPago();
            saldo = pendientes.stream().map(EstadoCuenta::getTotalPeriodo).reduce(BigDecimal.ZERO, BigDecimal::add);
            for (EstadoCuenta p : pendientes) {
                var r = MotorFinanciero.pagoConMora(p.getTotalPeriodo(), c.ted(), c.tedMora(), p.getFechaPago(), hoy);
                mora = mora.add(r.interesCompensatorio()).add(r.interesMoratorio());
                diasMora = Math.max(diasMora, r.diasMora());
            }
        } else {
            fechaCorte = MotorFinanciero.fechaCorteDeCompra(hoy, c.getDiaCorte());
            fechaPago = MotorFinanciero.fechaPagoDeCorte(fechaCorte, c.getDiaPago());
            saldo = suma(porFacturar(c, fechaPago));
        }
        BigDecimal libre = MotorFinanciero.creditoDisponible(c.getLimiteCredito(), c.getDeudaVigente());
        BigDecimal uso = c.getLimiteCredito().signum() == 0 ? BigDecimal.ZERO
                : c.getDeudaVigente().multiply(BigDecimal.valueOf(100)).divide(c.getLimiteCredito(), 1, RoundingMode.HALF_UP);
        return new ClienteResumenDto(c.getId(), c.getDni(), c.getNombres(), c.getDireccion(), c.getTelefono(),
                c.getEstado(), c.getLimiteCredito(), c.getDeudaVigente(), libre, uso, saldo, fechaCorte, fechaPago,
                diasMora, mora, saldo.add(mora), idPendiente, diasMora > 0);
    }
}

