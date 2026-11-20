package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Cliente;
import pe.fiadope.api.domain.Compra;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.repository.ClienteRepository;
import pe.fiadope.api.repository.CompraRepository;
import pe.fiadope.api.repository.TiendaRepository;
import pe.fiadope.api.security.UsuarioActual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** HU04: panel del negocio (total por cobrar, próximo corte y clientes en mora). */
@Service
@RequiredArgsConstructor
public class PanelService {

    private final TiendaRepository tiendas;
    private final ClienteRepository clientes;
    private final CompraRepository compras;
    private final CuentaService cuenta;

    @Transactional(readOnly = true)
    public PanelDto panel() {
        Integer idTienda = UsuarioActual.tienda();
        var t = tiendas.findById(idTienda).orElseThrow();
        LocalDate hoy = cuenta.hoy();
        List<Cliente> lista = clientes.findByTiendaIdOrderByNombresAsc(idTienda);
        List<ClienteResumenDto> resumenes = new ArrayList<>();
        BigDecimal porCobrar = BigDecimal.ZERO;
        BigDecimal enMora = BigDecimal.ZERO;
        int conSaldo = 0;
        int nMora = 0;
        LocalDate proximoCorte = null;
        LocalDate proximoPago = null;
        BigDecimal montoProximo = BigDecimal.ZERO;
        BigDecimal sumaTea = BigDecimal.ZERO;
        BigDecimal sumaTeaMor = BigDecimal.ZERO;
        Map<String, BigDecimal> top = new HashMap<>();

        for (Cliente c : lista) {
            ClienteResumenDto r = cuenta.resumen(c);
            resumenes.add(r);
            sumaTea = sumaTea.add(MotorFinanciero.tea(c.ted()));
            sumaTeaMor = sumaTeaMor.add(MotorFinanciero.tea(c.tedMora()));
            if (r.deudaVigente().signum() > 0 || r.totalACobrar().signum() > 0) {
                conSaldo++;
            }
            porCobrar = porCobrar.add(r.totalACobrar());
            if (r.enMora()) {
                nMora++;
                enMora = enMora.add(r.totalACobrar());
            }
            if (!c.activo()) {
                continue;
            }
            LocalDate corte = MotorFinanciero.fechaCorteDeCompra(hoy, c.getDiaCorte());
            LocalDate pago = MotorFinanciero.fechaPagoDeCorte(corte, c.getDiaPago());
            BigDecimal monto = CuentaService.suma(cuenta.porFacturar(c, pago));
            if (proximoCorte == null || corte.isBefore(proximoCorte)) {
                proximoCorte = corte;
                proximoPago = pago;
                montoProximo = monto;
            } else if (corte.equals(proximoCorte)) {
                montoProximo = montoProximo.add(monto);
            }
            for (Compra co : compras.entreFechas(c.getId(), hoy.minusDays(30).atStartOfDay(), hoy.plusDays(1).atStartOfDay())) {
                top.merge(co.getProducto().getNombre(), co.getCapital(), BigDecimal::add);
            }
        }
        resumenes.sort(Comparator.comparing(ClienteResumenDto::enMora).reversed()
                .thenComparing(ClienteResumenDto::diasMora, Comparator.reverseOrder())
                .thenComparing(ClienteResumenDto::nombres));
        var topProductos = top.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).limit(3)
                .map(e -> new ProductoTopDto(e.getKey(), e.getValue())).toList();
        int n = lista.isEmpty() ? 1 : lista.size();
        BigDecimal teaProm = sumaTea.divide(BigDecimal.valueOf(n), MotorFinanciero.MC);
        BigDecimal teaMorProm = sumaTeaMor.divide(BigDecimal.valueOf(n), MotorFinanciero.MC);
        int dias = proximoCorte == null ? 0 : (int) ChronoUnit.DAYS.between(hoy, proximoCorte);
        return new PanelDto(t.getRazonSocial(), t.getPropietario(), t.getUbicacion(), t.getLimiteGlobal(), porCobrar,
                conSaldo, proximoCorte, proximoPago, dias, montoProximo, nMora, enMora, Mapper.pct(teaProm, 4),
                Mapper.pct(teaMorProm, 4), lista.size(), resumenes, topProductos);
    }
}

