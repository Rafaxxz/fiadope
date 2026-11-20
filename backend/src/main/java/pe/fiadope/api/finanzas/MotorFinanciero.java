package pe.fiadope.api.finanzas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor financiero de FiadoPe: implementa las fórmulas F1 a F12 del marco conceptual (sección 6.4).
 * <p>
 * Convenciones: año comercial de 360 días y meses de 30 días; las tasas se calculan con 12 decimales
 * internos (el informe exige al menos 7) y los montos se redondean a 2 decimales (HALF_UP) al guardarse
 * o mostrarse. Todas las tasas se reciben y devuelven en tanto por uno (0.24 = 24 %).
 */
public final class MotorFinanciero {

    public static final int DIAS_ANIO = 360;
    public static final int DIAS_MES = 30;
    public static final int ESCALA_TASA = 12;
    public static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

    public enum TipoTasa { NOMINAL, EFECTIVA }

    private MotorFinanciero() {
    }

    // ------------------------------------------------------------------ F1
    /** F1. Días comerciales entre dos fechas (método 30/360). */
    public static int diasComerciales(LocalDate desde, LocalDate hasta) {
        int d1 = Math.min(desde.getDayOfMonth(), 30);
        int d2 = Math.min(hasta.getDayOfMonth(), 30);
        return (hasta.getYear() - desde.getYear()) * DIAS_ANIO
                + (hasta.getMonthValue() - desde.getMonthValue()) * DIAS_MES
                + (d2 - d1);
    }

    // ------------------------------------------------------------- F2 / F3
    /**
     * F2 y F3. Convierte la tasa pactada a tasa efectiva diaria (TED).
     *
     * @param tipo               NOMINAL o EFECTIVA
     * @param tasa               valor de la tasa en tanto por uno
     * @param diasPlazo          días del plazo de la tasa (360 anual, 30 mensual, 1 diaria...)
     * @param diasCapitalizacion días del período de capitalización (solo tasa nominal)
     */
    public static BigDecimal ted(TipoTasa tipo, BigDecimal tasa, int diasPlazo, Integer diasCapitalizacion) {
        if (tasa.signum() == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_TASA);
        }
        if (tipo == TipoTasa.NOMINAL) {
            int cap = diasCapitalizacion == null || diasCapitalizacion <= 0 ? 1 : diasCapitalizacion;
            // m = días de la tasa / días de capitalización; tasa del período de capitalización = TN / m
            BigDecimal m = BigDecimal.valueOf(diasPlazo).divide(BigDecimal.valueOf(cap), MC);
            BigDecimal tasaPeriodo = tasa.divide(m, MC);
            if (cap == 1) {
                return tasaPeriodo.setScale(ESCALA_TASA, RoundingMode.HALF_UP);
            }
            return raiz(BigDecimal.ONE.add(tasaPeriodo), cap).subtract(BigDecimal.ONE)
                    .setScale(ESCALA_TASA, RoundingMode.HALF_UP);
        }
        return raiz(BigDecimal.ONE.add(tasa), diasPlazo).subtract(BigDecimal.ONE)
                .setScale(ESCALA_TASA, RoundingMode.HALF_UP);
    }

    /** F3. Tasa efectiva de n días a partir de la TED: (1 + TED)^n − 1. */
    public static BigDecimal tasaEfectiva(BigDecimal ted, int dias) {
        return BigDecimal.ONE.add(ted).pow(dias, MC).subtract(BigDecimal.ONE)
                .setScale(ESCALA_TASA, RoundingMode.HALF_UP);
    }

    public static BigDecimal tem(BigDecimal ted) {
        return tasaEfectiva(ted, DIAS_MES);
    }

    public static BigDecimal tea(BigDecimal ted) {
        return tasaEfectiva(ted, DIAS_ANIO);
    }

    // ------------------------------------------------------------------ F4
    /** F4 (parte 1). Fecha de corte que le corresponde a una compra: si la compra es posterior al corte, pasa al del mes siguiente. */
    public static LocalDate fechaCorteDeCompra(LocalDate fechaCompra, int diaCorte) {
        LocalDate corteDelMes = fechaCompra.withDayOfMonth(diaCorte);
        return fechaCompra.isAfter(corteDelMes) ? corteDelMes.plusMonths(1) : corteDelMes;
    }

    /** F4 (parte 2). Fecha de pago de un corte: el siguiente día de pago luego del corte. */
    public static LocalDate fechaPagoDeCorte(LocalDate fechaCorte, int diaPago) {
        LocalDate pago = fechaCorte.withDayOfMonth(diaPago);
        return diaPago > fechaCorte.getDayOfMonth() ? pago : pago.plusMonths(1);
    }

    /** F4. Fecha de pago asignada a una compra. */
    public static LocalDate fechaPagoAsignada(LocalDate fechaCompra, int diaCorte, int diaPago) {
        return fechaPagoDeCorte(fechaCorteDeCompra(fechaCompra, diaCorte), diaPago);
    }

    // ----------------------------------------------------------- F5 / F8
    /** F5. Monto de una compra a fin de mes: S = C × (1 + TED)^d. También F8 (C' con d = días de gracia). */
    public static BigDecimal valorFuturo(BigDecimal capital, BigDecimal ted, int dias) {
        return capital.multiply(BigDecimal.ONE.add(ted).pow(dias, MC), MC);
    }

    // ------------------------------------------------------------------ F7
    /** F7. Interés por d días de mora sobre la deuda D: D × [(1 + TED)^dm − 1]. */
    public static BigDecimal interesPorDias(BigDecimal deuda, BigDecimal ted, int dias) {
        if (dias <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return redondear(deuda.multiply(BigDecimal.ONE.add(ted).pow(dias, MC).subtract(BigDecimal.ONE), MC));
    }

    public static ResultadoMora pagoConMora(BigDecimal deuda, BigDecimal ted, BigDecimal tedMora,
                                            LocalDate fechaPagoPactada, LocalDate fechaPagoReal) {
        int dm = Math.max(0, diasComerciales(fechaPagoPactada, fechaPagoReal));
        BigDecimal icomp = interesPorDias(deuda, ted, dm);
        BigDecimal imor = interesPorDias(deuda, tedMora, dm);
        BigDecimal total = redondear(deuda).add(icomp).add(imor);
        return new ResultadoMora(dm, icomp, imor, total);
    }

    public record ResultadoMora(int diasMora, BigDecimal interesCompensatorio, BigDecimal interesMoratorio,
                                BigDecimal total) {
    }

    // ------------------------------------------------------------------ F9
    /** F9. Cuota del método francés vencido ordinario: R = C' × [TEM (1+TEM)^n] / [(1+TEM)^n − 1]. */
    public static BigDecimal cuotaFrancesa(BigDecimal capitalAjustado, BigDecimal tem, int n) {
        if (tem.signum() == 0) {
            return redondear(capitalAjustado.divide(BigDecimal.valueOf(n), MC));
        }
        BigDecimal factor = BigDecimal.ONE.add(tem).pow(n, MC);
        BigDecimal frc = tem.multiply(factor, MC).divide(factor.subtract(BigDecimal.ONE), MC);
        return redondear(capitalAjustado.multiply(frc, MC));
    }

    // ------------------------------------------------------------------ F10
    /**
     * F10. Cronograma francés. El saldo se lleva con precisión interna y la última cuota se ajusta para
     * cubrir la diferencia de redondeo (saldo final = 0.00).
     */
    public static List<FilaCronograma> cronograma(BigDecimal capitalAjustado, BigDecimal tem, int n,
                                                  LocalDate primeraFechaPago) {
        BigDecimal cuota = cuotaFrancesa(capitalAjustado, tem, n);
        List<FilaCronograma> filas = new ArrayList<>();
        BigDecimal saldo = capitalAjustado;
        for (int k = 1; k <= n; k++) {
            BigDecimal interes = saldo.multiply(tem, MC);
            BigDecimal cuotaK;
            BigDecimal amortizacion;
            if (k == n) {
                amortizacion = saldo;
                cuotaK = redondear(saldo.add(interes));
            } else {
                cuotaK = cuota;
                amortizacion = cuota.subtract(interes);
            }
            BigDecimal saldoFinal = k == n ? BigDecimal.ZERO : saldo.subtract(amortizacion);
            filas.add(new FilaCronograma(k, primeraFechaPago.plusMonths(k), redondear(saldo), redondear(interes),
                    redondear(amortizacion), cuotaK, redondear(saldoFinal)));
            saldo = saldoFinal;
        }
        return filas;
    }

    public record FilaCronograma(int numero, LocalDate vencimiento, BigDecimal saldoInicial, BigDecimal interes,
                                 BigDecimal amortizacion, BigDecimal cuota, BigDecimal saldoFinal) {
    }

    // ------------------------------------------------------------------ F11
    /** F11. La compra procede solo si C ≤ límite − deuda vigente. */
    public static boolean dentroDelLimite(BigDecimal capital, BigDecimal limite, BigDecimal deudaVigente) {
        return capital.compareTo(creditoDisponible(limite, deudaVigente)) <= 0;
    }

    public static BigDecimal creditoDisponible(BigDecimal limite, BigDecimal deudaVigente) {
        return redondear(limite.subtract(deudaVigente).max(BigDecimal.ZERO));
    }

    // ------------------------------------------------------------------ F12
    /** F12. Total venta a plazo T = Σ cuotas. El interés total es T − C. */
    public static BigDecimal totalVentaPlazo(List<FilaCronograma> filas) {
        return filas.stream().map(FilaCronograma::cuota).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------------------------------------------------------------- util
    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Raíz n-ésima con precisión de BigDecimal (Newton sobre la aproximación en double). */
    static BigDecimal raiz(BigDecimal x, int n) {
        if (n == 1) {
            return x;
        }
        BigDecimal r = BigDecimal.valueOf(Math.pow(x.doubleValue(), 1.0 / n));
        BigDecimal bn = BigDecimal.valueOf(n);
        for (int i = 0; i < 8; i++) {
            // r = r - (r^n - x) / (n r^(n-1))
            BigDecimal rn1 = r.pow(n - 1, MC);
            BigDecimal f = rn1.multiply(r, MC).subtract(x);
            r = r.subtract(f.divide(bn.multiply(rn1, MC), MC), MC);
        }
        return r;
    }
}
