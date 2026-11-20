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
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto")
    private Producto producto;
    private LocalDateTime fechaHora;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal capital;
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;
    @Column(name = "n_cuotas")
    private Short nCuotas;
    private LocalDate fechaCorte;
    private LocalDate fechaPagoAsignada;
    private short dias;
    @Column(name = "monto_s")
    private BigDecimal montoS;
    private BigDecimal interes;
    private BigDecimal ted;
    private BigDecimal tedMora;
    private BigDecimal tem;
    private BigDecimal tea;
    private BigDecimal cuotaFija;
    private BigDecimal totalPlazo;
    private boolean pagada;
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero")
    private List<Cuota> cuotas = new ArrayList<>();
}

