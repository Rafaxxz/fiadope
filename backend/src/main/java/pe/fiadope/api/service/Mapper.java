package pe.fiadope.api.service;

import pe.fiadope.api.domain.*;
import pe.fiadope.api.dto.Dtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Conversión de entidades a DTOs. */
public final class Mapper {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Mapper() {
    }

    public static BigDecimal pct(BigDecimal tasa, int decimales) {
        return tasa.multiply(CIEN).setScale(decimales, RoundingMode.HALF_UP);
    }

    public static ProductoDto producto(Producto p) {
        return new ProductoDto(p.getId(), p.getNombre(), p.getCategoria(), p.getSku(), p.getMarca(), p.getProveedor(),
                p.getUnidad(), p.getPrecioContado(), p.getPrecioLista(), p.isPermiteFinMes(), p.isPermiteCuotas(),
                p.getImagenUrl(), p.getEstado());
    }

    public static FilaCronogramaDto fila(Cuota q) {
        return new FilaCronogramaDto(q.getNumero(), q.getVencimiento(), q.getSaldoInicial(), q.getInteres(),
                q.getAmortizacion(), q.getCuota(), q.getSaldoFinal(), q.isPagada());
    }

    public static CompraDto compra(Compra c) {
        int pagadas = (int) c.getCuotas().stream().filter(Cuota::isPagada).count();
        FilaCronogramaDto proxima = c.getCuotas().stream().filter(q -> !q.isPagada()).findFirst().map(Mapper::fila).orElse(null);
        return new CompraDto(c.getId(), c.getCliente().getId(), c.getCliente().getNombres(), c.getProducto().getId(),
                c.getProducto().getNombre(), c.getProducto().getImagenUrl(), c.getFechaHora(), c.getCantidad(),
                c.getCapital(), c.getModalidad(), c.getNCuotas() == null ? null : c.getNCuotas().intValue(),
                c.getFechaCorte(), c.getFechaPagoAsignada(), c.getDias(), c.getMontoS(), c.getInteres(),
                c.getCuotaFija(), c.getTotalPlazo(), c.isPagada(), pagadas, proxima);
    }

    public static ItemDto item(EstadoCuentaItem i) {
        return new ItemDto(i.getTipo(), i.getDetalle(), i.getFecha(), i.getDias(), i.getMonto(), i.getInteres(), i.getTotal());
    }

    public static PagoDto pago(Pago p, BigDecimal saldoLibreta) {
        return new PagoDto(p.getId(), p.getEstadoCuenta().getId(), p.getEstadoCuenta().getCliente().getNombres(),
                p.getFechaReal(), p.getMetodoPago(), p.getNroOperacion(), p.getDiasMora(), p.getIntCompMora(),
                p.getIntMora(), p.getTotalPagado(), p.getRegistrado(), saldoLibreta);
    }

    public static BitacoraDto bitacora(Bitacora b) {
        return new BitacoraDto(b.getId(), b.getFechaHora(), b.getUsuarioLogin(), b.getAccion(), b.getTabla(),
                b.getIdRegistro(), b.getValorAntes(), b.getValorDespues(), b.getIp());
    }
}

