package pe.fiadope.api.finanzas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.fiadope.api.finanzas.MotorFinanciero.FilaCronograma;
import pe.fiadope.api.finanzas.MotorFinanciero.TipoTasa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.fiadope.api.finanzas.MotorFinanciero.*;

/** Verifica el motor contra los juegos de prueba 1 y 2 del informe (sección 6.5), al céntimo. */
class MotorFinancieroTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static LocalDate f(int d, int m, int a) {
        return LocalDate.of(a, m, d);
    }

    private static BigDecimal r7(BigDecimal tasa) {
        return tasa.setScale(7, RoundingMode.HALF_UP);
    }

    @Test
    @DisplayName("F1: días comerciales 30/360")
    void diasComerciales() {
        assertThat(MotorFinanciero.diasComerciales(f(3, 9, 2026), f(26, 9, 2026))).isEqualTo(23);
        assertThat(MotorFinanciero.diasComerciales(f(22, 9, 2026), f(26, 10, 2026))).isEqualTo(34);
        assertThat(MotorFinanciero.diasComerciales(f(2, 10, 2026), f(5, 11, 2026))).isEqualTo(33);
        assertThat(MotorFinanciero.diasComerciales(f(31, 10, 2026), f(5, 11, 2026))).isEqualTo(5);
    }

    @Nested
    @DisplayName("Juego de prueba 1: TEA 24 %, mora TEA 15 %, corte 20, pago 26")
    class JuegoUno {
        final BigDecimal ted = MotorFinanciero.ted(TipoTasa.EFECTIVA, bd("0.24"), 360, null);
        final BigDecimal tedMor = MotorFinanciero.ted(TipoTasa.EFECTIVA, bd("0.15"), 360, null);
        final BigDecimal tem = MotorFinanciero.tem(ted);

        @Test
        void tasasIntermedias() {
            assertThat(r7(ted)).isEqualByComparingTo("0.0005977");
            assertThat(r7(tedMor)).isEqualByComparingTo("0.0003883");
            assertThat(r7(tem)).isEqualByComparingTo("0.0180876");
            assertThat(MotorFinanciero.tea(ted).setScale(6, RoundingMode.HALF_UP)).isEqualByComparingTo("0.240000");
        }

        @Test
        void fechasDePago() {
            assertThat(fechaPagoAsignada(f(3, 9, 2026), 20, 26)).isEqualTo(f(26, 9, 2026));
            assertThat(fechaPagoAsignada(f(20, 9, 2026), 20, 26)).isEqualTo(f(26, 9, 2026));
            assertThat(fechaPagoAsignada(f(22, 9, 2026), 20, 26)).isEqualTo(f(26, 10, 2026));
        }

        @Test
        void comprasFinDeMes() {
            assertThat(redondear(valorFuturo(bd("120.00"), ted, 23))).isEqualByComparingTo("121.66");
            assertThat(redondear(valorFuturo(bd("85.50"), ted, 8))).isEqualByComparingTo("85.91");
            assertThat(redondear(valorFuturo(bd("60.00"), ted, 34))).isEqualByComparingTo("61.23");
        }

        @Test
        void cocinaEnTresCuotas() {
            BigDecimal cPrima = redondear(valorFuturo(bd("450.00"), ted, 11));
            assertThat(cPrima).isEqualByComparingTo("452.97");
            assertThat(cuotaFrancesa(cPrima, tem, 3)).isEqualByComparingTo("156.48");

            List<FilaCronograma> filas = cronograma(cPrima, tem, 3, f(26, 9, 2026));
            assertThat(filas).extracting(FilaCronograma::vencimiento)
                    .containsExactly(f(26, 10, 2026), f(26, 11, 2026), f(26, 12, 2026));
            assertThat(filas).extracting(FilaCronograma::interes).map(BigDecimal::toPlainString)
                    .containsExactly("8.19", "5.51", "2.78");
            assertThat(filas).extracting(FilaCronograma::amortizacion).map(BigDecimal::toPlainString)
                    .containsExactly("148.29", "150.97", "153.71");
            assertThat(filas).extracting(FilaCronograma::cuota).map(BigDecimal::toPlainString)
                    .containsExactly("156.48", "156.48", "156.49");
            assertThat(filas).extracting(FilaCronograma::saldoFinal).map(BigDecimal::toPlainString)
                    .containsExactly("304.68", "153.71", "0.00");

            BigDecimal total = totalVentaPlazo(filas);
            assertThat(total).isEqualByComparingTo("469.45");
            assertThat(total.subtract(bd("450.00"))).isEqualByComparingTo("19.45");
        }

        @Test
        void pagoConTresDiasDeMora() {
            ResultadoMora r = pagoConMora(bd("207.57"), ted, tedMor, f(26, 9, 2026), f(29, 9, 2026));
            assertThat(r.diasMora()).isEqualTo(3);
            assertThat(r.interesCompensatorio()).isEqualByComparingTo("0.37");
            assertThat(r.interesMoratorio()).isEqualByComparingTo("0.24");
            assertThat(r.total()).isEqualByComparingTo("208.18");
        }

        @Test
        void pagoATiempo() {
            ResultadoMora r = pagoConMora(bd("207.57"), ted, tedMor, f(26, 9, 2026), f(26, 9, 2026));
            assertThat(r.diasMora()).isZero();
            assertThat(r.total()).isEqualByComparingTo("207.57");
        }

        @Test
        void limiteDeCredito() {
            BigDecimal deuda = bd("715.50");
            assertThat(creditoDisponible(bd("1500.00"), deuda)).isEqualByComparingTo("784.50");
            assertThat(dentroDelLimite(bd("900.00"), bd("1500.00"), deuda)).isFalse();
            assertThat(dentroDelLimite(bd("784.50"), bd("1500.00"), deuda)).isTrue();
        }
    }

    @Nested
    @DisplayName("Juego de prueba 2: TNA 36 % cap. diaria, mora TNA 15 %, corte 25, pago 5")
    class JuegoDos {
        final BigDecimal ted = MotorFinanciero.ted(TipoTasa.NOMINAL, bd("0.36"), 360, 1);
        final BigDecimal tedMor = MotorFinanciero.ted(TipoTasa.NOMINAL, bd("0.15"), 360, 1);
        final BigDecimal tem = MotorFinanciero.tem(ted);

        @Test
        void tasasIntermedias() {
            assertThat(r7(ted)).isEqualByComparingTo("0.0010000");
            assertThat(r7(tedMor)).isEqualByComparingTo("0.0004167");
            assertThat(r7(tem)).isEqualByComparingTo("0.0304391");
            assertThat(MotorFinanciero.tea(ted).multiply(bd("100")).setScale(4, RoundingMode.HALF_UP))
                    .isEqualByComparingTo("43.3072");
            assertThat(MotorFinanciero.tea(tedMor).multiply(bd("100")).setScale(2, RoundingMode.HALF_UP))
                    .isEqualByComparingTo("16.18");
        }

        @Test
        void fechasYMontos() {
            assertThat(fechaPagoAsignada(f(2, 10, 2026), 25, 5)).isEqualTo(f(5, 11, 2026));
            assertThat(fechaPagoAsignada(f(20, 10, 2026), 25, 5)).isEqualTo(f(5, 11, 2026));
            assertThat(fechaPagoAsignada(f(26, 10, 2026), 25, 5)).isEqualTo(f(5, 12, 2026));
            assertThat(redondear(valorFuturo(bd("120.00"), ted, 33))).isEqualByComparingTo("124.02");
            assertThat(redondear(valorFuturo(bd("75.40"), ted, 15))).isEqualByComparingTo("76.54");
        }

        @Test
        void bocaditosEnDosCuotas() {
            BigDecimal cPrima = redondear(valorFuturo(bd("300.00"), ted, 25));
            assertThat(cPrima).isEqualByComparingTo("307.59");
            List<FilaCronograma> filas = cronograma(cPrima, tem, 2, f(5, 11, 2026));
            assertThat(filas).extracting(FilaCronograma::vencimiento).containsExactly(f(5, 12, 2026), f(5, 1, 2027));
            assertThat(filas).extracting(FilaCronograma::interes).map(BigDecimal::toPlainString)
                    .containsExactly("9.36", "4.75");
            assertThat(filas).extracting(FilaCronograma::cuota).map(BigDecimal::toPlainString)
                    .containsExactly("160.85", "160.85");
            assertThat(totalVentaPlazo(filas)).isEqualByComparingTo("321.70");
        }

        @Test
        void pagoConCuatroDiasDeMora() {
            ResultadoMora r = pagoConMora(bd("200.56"), ted, tedMor, f(5, 11, 2026), f(9, 11, 2026));
            assertThat(r.diasMora()).isEqualTo(4);
            assertThat(r.interesCompensatorio()).isEqualByComparingTo("0.80");
            assertThat(r.interesMoratorio()).isEqualByComparingTo("0.33");
            assertThat(r.total()).isEqualByComparingTo("201.69");
        }
    }

    @Test
    @DisplayName("Tope BCRP: TNA 18 % cap. diaria equivale a TEA 19.72 %")
    void topeMoratorio() {
        BigDecimal ted = MotorFinanciero.ted(TipoTasa.NOMINAL, bd("0.18"), 360, 1);
        assertThat(MotorFinanciero.tea(ted).multiply(bd("100")).setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("19.72");
    }

    @Test
    @DisplayName("Tasa nominal con capitalización mensual y tasa efectiva mensual")
    void otrasConversiones() {
        // TNA 24 % cap. mensual -> TEM 2 %
        BigDecimal ted = MotorFinanciero.ted(TipoTasa.NOMINAL, bd("0.24"), 360, 30);
        assertThat(MotorFinanciero.tem(ted).setScale(7, RoundingMode.HALF_UP)).isEqualByComparingTo("0.0200000");
        // TEM 2 % -> TEA 26.8242 %
        BigDecimal ted2 = MotorFinanciero.ted(TipoTasa.EFECTIVA, bd("0.02"), 30, null);
        assertThat(MotorFinanciero.tea(ted2).setScale(6, RoundingMode.HALF_UP)).isEqualByComparingTo("0.268242");
    }
}
