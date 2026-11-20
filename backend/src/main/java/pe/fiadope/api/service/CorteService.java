package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.*;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.CompraRepository;
import pe.fiadope.api.repository.EstadoCuentaRepository;
import pe.fiadope.api.security.UsuarioActual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** HU09: corte del mes y generación de los estados de cuenta (F4 y F6). */
@Service
@RequiredArgsConstructor
public class CorteService {

    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private final ClienteRepository clientes;
    private final CompraRepository compras;
    private final EstadoCuentaRepository estados;
    private final CuentaService cuenta;
    private final PdfService pdf;
    private final CorreoService correo;
    private final BitacoraService bitacora;

    @Transactional(readOnly = true)
    public CortePreviewDto preview(int anio, int mes) {
        List<CorteClienteDto> lista = new ArrayList<>();
        for (Cliente c : clientes.findByTiendaIdOrderByNombresAsc(UsuarioActual.tienda())) {
            CorteClienteDto d = corteDe(c, anio, mes);
            if (d.generado() || !d.items().isEmpty() || !d.notas().isEmpty()) {
                lista.add(d);
            }
        }
        BigDecimal total = lista.stream().map(CorteClienteDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        int conSaldo = (int) lista.stream().filter(d -> d.total().signum() > 0).count();
        return new CortePreviewDto(anio, mes, total, conSaldo, lista);
    }

    private CorteClienteDto corteDe(Cliente c, int anio, int mes) {
        LocalDate corte = LocalDate.of(anio, mes, c.getDiaCorte());
        LocalDate pago = MotorFinanciero.fechaPagoDeCorte(corte, c.getDiaPago());
        String email = c.getUsuario() == null ? null : c.getUsuario().getEmail();
        List<String> notas = new ArrayList<>();
        LocalDate finMes = corte.withDayOfMonth(corte.lengthOfMonth());
        String mesSig = corte.plusMonths(1).getMonth().getDisplayName(TextStyle.FULL, ES);
        for (Compra co : compras.entreFechas(c.getId(), corte.plusDays(1).atStartOfDay(), finMes.plusDays(1).atStartOfDay())) {
            notas.add(String.format("%s · %s · S/ %s · pasa al corte de %s",
                    co.getFechaHora().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
                    co.getProducto().getNombre(), co.getCapital().toPlainString(), mesSig));
        }
        var existente = estados.findByClienteIdAndFechaCorte(c.getId(), corte);
        if (existente.isPresent()) {
            EstadoCuenta e = existente.get();
            var items = e.getItems().stream().filter(i -> !"MORA".equals(i.getTipo())).map(Mapper::item).toList();
            return new CorteClienteDto(c.getId(), c.getNombres(), c.getDni(), email, corte, pago, items,
                    e.getTotalPeriodo(), notas, true, e.getId(), e.getEstado(), e.isEnviado());
        }
        var items = cuenta.porFacturar(c, pago);
        return new CorteClienteDto(c.getId(), c.getNombres(), c.getDni(), email, corte, pago,
                items.stream().map(CuentaService.Facturable::dto).toList(), CuentaService.suma(items), notas, false,
                null, null, false);
    }

    @Transactional
    public CorteGeneradoDto generar(CorteGenerarRequest r) {
        Integer t = UsuarioActual.tienda();
        LocalDate hoy = cuenta.hoy();
        int generados = 0;
        int omitidos = 0;
        List<String> detalle = new ArrayList<>();
        for (Integer id : r.idsClientes()) {
            Cliente c = clientes.findByIdAndTiendaId(id, t).orElseThrow(() -> ApiException.noEncontrado("Cliente no encontrado"));
            LocalDate corte = LocalDate.of(r.anio(), r.mes(), c.getDiaCorte());
            if (corte.isAfter(hoy)) {
                omitidos++;
                detalle.add(c.getNombres() + ": el corte del " + corte + " aún no llega");
                continue;
            }
            if (estados.findByClienteIdAndFechaCorte(c.getId(), corte).isPresent()) {
                omitidos++;
                detalle.add(c.getNombres() + ": su estado de cuenta ya fue generado");
                continue;
            }
            EstadoCuenta e = generarPara(c, corte);
            if (e == null) {
                omitidos++;
                detalle.add(c.getNombres() + ": no tiene consumos en este corte");
                continue;
            }
            enviar(e);
            generados++;
            detalle.add(c.getNombres() + ": estado de cuenta por S/ " + e.getTotalPeriodo() + " enviado");
        }
        return new CorteGeneradoDto(generados, omitidos, detalle);
    }

    /** Crea el estado de cuenta de un cliente para una fecha de corte (sin validar la fecha). */
    @Transactional
    public EstadoCuenta generarPara(Cliente c, LocalDate corte) {
        LocalDate pago = MotorFinanciero.fechaPagoDeCorte(corte, c.getDiaPago());
        var items = cuenta.porFacturar(c, pago);
        if (items.isEmpty()) {
            return null;
        }
        EstadoCuenta e = new EstadoCuenta();
        e.setCliente(c);
        e.setFechaCorte(corte);
        e.setFechaPago(pago);
        for (var f : items) {
            EstadoCuentaItem i = new EstadoCuentaItem();
            i.setEstadoCuenta(e);
            i.setTipo(f.tipo());
            i.setCompra(f.compra());
            i.setCuota(f.cuota());
            i.setDetalle(f.detalle());
            i.setFecha(f.fecha());
            i.setDias((short) f.dias());
            i.setMonto(f.monto());
            i.setInteres(f.interes());
            i.setTotal(f.total());
            e.getItems().add(i);
        }
        e.setTotalPeriodo(CuentaService.suma(items));
        estados.save(e);
        bitacora.registrar("CORTE", "estado_cuenta", e.getId(), null,
                Map.of("cliente", c.getDni(), "fecha_corte", corte.toString(), "fecha_pago", pago.toString(),
                        "total", e.getTotalPeriodo()));
        return e;
    }

    private void enviar(EstadoCuenta e) {
        Cliente c = e.getCliente();
        if (c.getUsuario() != null) {
            byte[] archivo = pdf.estadoCuenta(e, null);
            correo.enviar(c.getUsuario().getEmail(), "Tu estado de cuenta FiadoPe al " + e.getFechaCorte(),
                    "<p>Hola " + c.getNombres() + ", adjuntamos tu estado de cuenta. Total a pagar: <b>S/ "
                            + e.getTotalPeriodo() + "</b> hasta el " + e.getFechaPago() + ".</p>",
                    archivo, "estado-cuenta-" + e.getFechaCorte() + ".pdf");
        }
        e.setEnviado(true);
    }

    @Transactional(readOnly = true)
    public byte[] pdfEstado(Integer idEstado) {
        EstadoCuenta e = estados.buscarEnTienda(idEstado, UsuarioActual.tienda())
                .orElseThrow(() -> ApiException.noEncontrado("Estado de cuenta no encontrado"));
        return pdf.estadoCuenta(e, null);
    }
}

