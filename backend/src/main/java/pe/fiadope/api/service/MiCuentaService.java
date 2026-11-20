package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Cliente;
import pe.fiadope.api.domain.Compra;
import pe.fiadope.api.domain.EstadoCuenta;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.CompraRepository;
import pe.fiadope.api.repository.EstadoCuentaRepository;
import pe.fiadope.api.repository.PagoRepository;
import pe.fiadope.api.security.UsuarioActual;

import java.util.List;

/** HU11: portal del cliente («Mi libreta vecinal»). */
@Service
@RequiredArgsConstructor
public class MiCuentaService {

    private final ClienteRepository clientes;
    private final CompraRepository compras;
    private final EstadoCuentaRepository estados;
    private final PagoRepository pagos;
    private final CuentaService cuenta;
    private final PdfService pdf;

    private Cliente cliente() {
        return clientes.findByUsuarioId(UsuarioActual.get().idUsuario())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontró tu libreta"));
    }

    @Transactional(readOnly = true)
    public MiCuentaDto miCuenta() {
        Cliente c = cliente();
        ClienteResumenDto r = cuenta.resumen(c);
        List<ItemDto> detalle;
        if (r.idEstadoPendiente() != null) {
            detalle = estados.findById(r.idEstadoPendiente()).orElseThrow().getItems().stream().map(Mapper::item).toList();
        } else {
            detalle = cuenta.porFacturar(c, r.fechaPago()).stream().map(CuentaService.Facturable::dto).toList();
        }
        var lista = compras.deCliente(c.getId()).stream().map(Mapper::compra).toList();
        var historial = estados.findByClienteIdOrderByFechaCorteDesc(c.getId()).stream().map(this::estado).toList();
        return new MiCuentaDto(c.getNombres(), c.getDni(), c.getTienda().getRazonSocial(), c.getMoneda(),
                r.totalACobrar(), r.fechaPago(), r.diasMora(), r.saldoAlCorte(), r.moraAplicada(), c.getLimiteCredito(),
                c.getDeudaVigente(), r.creditoLibre(), r.usoPorcentaje(), Mapper.pct(MotorFinanciero.tea(c.ted()), 4),
                Mapper.pct(MotorFinanciero.tea(c.tedMora()), 4), c.getDiaCorte(), c.getDiaPago(),
                r.idEstadoPendiente(), detalle, lista, historial);
    }

    private EstadoCuentaDto estado(EstadoCuenta e) {
        var pago = pagos.findByEstadoCuentaId(e.getId()).map(p -> Mapper.pago(p, null)).orElse(null);
        return new EstadoCuentaDto(e.getId(), e.getFechaCorte(), e.getFechaPago(), e.getTotalPeriodo(), e.getEstado(),
                e.getItems().stream().map(Mapper::item).toList(), pago);
    }

    @Transactional(readOnly = true)
    public CronogramaDto cronograma(Integer idCompra) {
        Cliente c = cliente();
        Compra co = compras.findById(idCompra).filter(x -> x.getCliente().getId().equals(c.getId()))
                .orElseThrow(() -> ApiException.noEncontrado("Compra no encontrada"));
        return CompraService.cronograma(co);
    }

    @Transactional(readOnly = true)
    public byte[] pdfEstadoCuenta(Integer idEstado) {
        Cliente c = cliente();
        if (idEstado != null) {
            EstadoCuenta e = estados.findById(idEstado).filter(x -> x.getCliente().getId().equals(c.getId()))
                    .orElseThrow(() -> ApiException.noEncontrado("Estado de cuenta no encontrado"));
            return pdf.estadoCuenta(e, null);
        }
        var pendientes = estados.findByClienteIdAndEstadoOrderByFechaCorteAsc(c.getId(), "PENDIENTE");
        if (!pendientes.isEmpty()) {
            return pdf.estadoCuenta(pendientes.get(0), cuenta.hoy());
        }
        var todos = estados.findByClienteIdOrderByFechaCorteDesc(c.getId());
        if (todos.isEmpty()) {
            throw ApiException.badRequest("Aún no tienes estados de cuenta");
        }
        return pdf.estadoCuenta(todos.get(0), null);
    }

    @Transactional(readOnly = true)
    public byte[] pdfCronograma(Integer idCompra) {
        return pdf.cronograma(cronograma(idCompra));
    }
}

