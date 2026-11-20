package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.*;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.EstadoCuentaRepository;
import pe.fiadope.api.repository.PagoRepository;
import pe.fiadope.api.security.UsuarioActual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HU10: pago total del estado de cuenta, con interés compensatorio y moratorio por los días de atraso (F7). */
@Service
@RequiredArgsConstructor
public class PagoService {

    private final EstadoCuentaRepository estados;
    private final ClienteRepository clientes;
    private final PagoRepository pagos;
    private final CuentaService cuenta;
    private final BitacoraService bitacora;

    /** Estado de cuenta pendiente más antiguo del cliente, con la mora calculada a la fecha indicada. */
    @Transactional(readOnly = true)
    public PagoPreviewDto previewCliente(Integer idCliente, LocalDate fecha) {
        Cliente c = clientes.findByIdAndTiendaId(idCliente, UsuarioActual.tienda())
                .orElseThrow(() -> ApiException.noEncontrado("Cliente no encontrado"));
        List<EstadoCuenta> pendientes = estados.findByClienteIdAndEstadoOrderByFechaCorteAsc(c.getId(), "PENDIENTE");
        if (pendientes.isEmpty()) {
            throw ApiException.badRequest(c.getNombres() + " no tiene estados de cuenta pendientes. Genera primero el corte del mes.");
        }
        return preview(pendientes.get(0), fecha == null ? cuenta.hoy() : fecha, pendientes.size());
    }

    @Transactional(readOnly = true)
    public PagoPreviewDto previewEstado(Integer idEstado, LocalDate fecha) {
        EstadoCuenta e = buscar(idEstado);
        int n = estados.findByClienteIdAndEstadoOrderByFechaCorteAsc(e.getCliente().getId(), "PENDIENTE").size();
        return preview(e, fecha == null ? cuenta.hoy() : fecha, n);
    }

    private PagoPreviewDto preview(EstadoCuenta e, LocalDate fechaReal, int pendientes) {
        Cliente c = e.getCliente();
        var r = MotorFinanciero.pagoConMora(e.getTotalPeriodo(), c.ted(), c.tedMora(), e.getFechaPago(), fechaReal);
        var items = e.getItems().stream().map(Mapper::item).toList();
        return new PagoPreviewDto(e.getId(), c.getId(), c.getNombres(), c.getDni(), e.getFechaCorte(), e.getFechaPago(),
                fechaReal, items, e.getTotalPeriodo(), r.diasMora(), r.interesCompensatorio(), r.interesMoratorio(),
                r.interesCompensatorio().add(r.interesMoratorio()), r.total(),
                Mapper.pct(MotorFinanciero.tea(c.ted()), 4), Mapper.pct(MotorFinanciero.tea(c.tedMora()), 4),
                c.getMoneda(), pendientes);
    }

    @Transactional
    public PagoDto registrar(PagoRequest r) {
        EstadoCuenta e = buscar(r.idEstado());
        if (!e.pendiente()) {
            throw ApiException.badRequest("Este estado de cuenta ya fue pagado");
        }
        Cliente c = e.getCliente();
        var pendientes = estados.findByClienteIdAndEstadoOrderByFechaCorteAsc(c.getId(), "PENDIENTE");
        if (!pendientes.get(0).getId().equals(e.getId())) {
            throw ApiException.badRequest("Primero se debe pagar el estado de cuenta del corte " + pendientes.get(0).getFechaCorte());
        }
        if (r.fechaPagoReal().isAfter(cuenta.hoy())) {
            throw ApiException.campo("fechaPagoReal", "La fecha de pago no puede ser futura");
        }
        LocalDate primeraCompra = e.getItems().stream().map(EstadoCuentaItem::getFecha).min(LocalDate::compareTo).orElse(e.getFechaCorte());
        if (r.fechaPagoReal().isBefore(primeraCompra)) {
            throw ApiException.campo("fechaPagoReal", "La fecha de pago no puede ser anterior a la compra");
        }
        if (!"Efectivo".equals(r.metodoPago()) && (r.nroOperacion() == null || r.nroOperacion().isBlank())) {
            throw ApiException.campo("nroOperacion", "El número de operación es obligatorio");
        }
        var mora = MotorFinanciero.pagoConMora(e.getTotalPeriodo(), c.ted(), c.tedMora(), e.getFechaPago(), r.fechaPagoReal());
        if (r.montoPagado().setScale(2, RoundingMode.HALF_UP).compareTo(mora.total()) != 0) {
            throw ApiException.campo("montoPagado", "Solo se acepta el pago total de S/ " + mora.total().toPlainString()
                    + ". No se aceptan pagos parciales.");
        }

        if (mora.diasMora() > 0) {
            EstadoCuentaItem i = new EstadoCuentaItem();
            i.setEstadoCuenta(e);
            i.setTipo("MORA");
            i.setDetalle("Intereses por mora (" + mora.diasMora() + " días): compensatorio "
                    + mora.interesCompensatorio() + " + moratorio " + mora.interesMoratorio());
            i.setFecha(r.fechaPagoReal());
            i.setDias((short) mora.diasMora());
            i.setMonto(e.getTotalPeriodo());
            i.setInteres(mora.interesCompensatorio().add(mora.interesMoratorio()));
            i.setTotal(i.getInteres());
            e.getItems().add(i);
        }

        BigDecimal deudaAntes = c.getDeudaVigente();
        BigDecimal liberado = BigDecimal.ZERO;
        for (EstadoCuentaItem i : e.getItems()) {
            if ("COMPRA".equals(i.getTipo()) && i.getCompra() != null) {
                i.getCompra().setPagada(true);
                liberado = liberado.add(i.getCompra().getCapital());
            } else if ("CUOTA".equals(i.getTipo()) && i.getCuota() != null) {
                liberado = liberado.add(capitalDeCuota(i.getCuota()));
                i.getCuota().setPagada(true);
                Compra co = i.getCuota().getCompra();
                co.setPagada(co.getCuotas().stream().allMatch(Cuota::isPagada));
            }
        }
        c.setDeudaVigente(c.getDeudaVigente().subtract(liberado).max(BigDecimal.ZERO));
        e.setEstado("PAGADO");

        Pago p = new Pago();
        p.setEstadoCuenta(e);
        p.setMetodoPago(r.metodoPago());
        p.setNroOperacion("Efectivo".equals(r.metodoPago()) ? null : r.nroOperacion().trim());
        p.setFechaReal(r.fechaPagoReal());
        p.setDiasMora((short) mora.diasMora());
        p.setIntCompMora(mora.interesCompensatorio());
        p.setIntMora(mora.interesMoratorio());
        p.setTotalPagado(mora.total());
        pagos.save(p);

        Map<String, Object> despues = new LinkedHashMap<>();
        despues.put("cliente", c.getDni());
        despues.put("fecha_corte", e.getFechaCorte().toString());
        despues.put("dias_mora", mora.diasMora());
        despues.put("int_comp_mora", mora.interesCompensatorio());
        despues.put("int_mora", mora.interesMoratorio());
        despues.put("total_pagado", mora.total());
        despues.put("metodo", r.metodoPago());
        despues.put("deuda_vigente", c.getDeudaVigente());
        bitacora.registrar("PAGO", "pago", p.getId(), Map.of("estado", "PENDIENTE", "deuda_vigente", deudaAntes), despues);
        return Mapper.pago(p, BigDecimal.ZERO.setScale(2));
    }

    /** Capital (en términos del precio C) que libera una cuota: proporción de la amortización sobre C'. */
    static BigDecimal capitalDeCuota(Cuota q) {
        Compra co = q.getCompra();
        BigDecimal cPrima = co.getMontoS();
        BigDecimal acumuladoHasta = cPrima.subtract(q.getSaldoFinal());
        BigDecimal acumuladoAntes = cPrima.subtract(q.getSaldoInicial());
        BigDecimal hasta = q.getNumero() == co.getNCuotas() ? co.getCapital()
                : co.getCapital().multiply(acumuladoHasta).divide(cPrima, 2, RoundingMode.HALF_UP);
        BigDecimal antes = q.getNumero() == 1 ? BigDecimal.ZERO
                : co.getCapital().multiply(acumuladoAntes).divide(cPrima, 2, RoundingMode.HALF_UP);
        return hasta.subtract(antes);
    }

    private EstadoCuenta buscar(Integer id) {
        return estados.buscarEnTienda(id, UsuarioActual.tienda())
                .orElseThrow(() -> ApiException.noEncontrado("Estado de cuenta no encontrado"));
    }
}

