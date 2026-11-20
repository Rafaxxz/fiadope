package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "estado_cuenta_item")
@Getter
@Setter
@NoArgsConstructor
public class EstadoCuentaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado")
    private EstadoCuenta estadoCuenta;
    private String tipo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuota")
    private Cuota cuota;
    private String detalle;
    private LocalDate fecha;
    private short dias;
    private BigDecimal monto;
    private BigDecimal interes;
    private BigDecimal total;
}

