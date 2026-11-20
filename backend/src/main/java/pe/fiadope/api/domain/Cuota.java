package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuota")
@Getter
@Setter
@NoArgsConstructor
public class Cuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuota")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra")
    private Compra compra;
    private short numero;
    private LocalDate vencimiento;
    private BigDecimal saldoInicial;
    private BigDecimal interes;
    private BigDecimal amortizacion;
    private BigDecimal cuota;
    private BigDecimal saldoFinal;
    private boolean pagada;
}

