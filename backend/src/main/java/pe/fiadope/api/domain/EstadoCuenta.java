package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estado_cuenta")
@Getter
@Setter
@NoArgsConstructor
public class EstadoCuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;
    private LocalDate fechaCorte;
    private LocalDate fechaPago;
    private BigDecimal totalPeriodo;
    private String estado = "PENDIENTE";
    private LocalDateTime fechaEmision = LocalDateTime.now();
    private boolean enviado;
    @OneToMany(mappedBy = "estadoCuenta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha, id")
    private List<EstadoCuentaItem> items = new ArrayList<>();

    public boolean pendiente() {
        return "PENDIENTE".equals(estado);
    }
}

