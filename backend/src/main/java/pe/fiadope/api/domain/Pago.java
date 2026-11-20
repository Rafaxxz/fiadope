package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado")
    private EstadoCuenta estadoCuenta;
    private String metodoPago;
    private String nroOperacion;
    private LocalDate fechaReal;
    private short diasMora;
    private BigDecimal intCompMora;
    private BigDecimal intMora;
    private BigDecimal totalPagado;
    private LocalDateTime registrado = LocalDateTime.now();
}

