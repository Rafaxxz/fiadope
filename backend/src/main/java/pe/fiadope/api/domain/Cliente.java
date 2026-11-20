package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.fiadope.api.finanzas.MotorFinanciero;
import pe.fiadope.api.finanzas.MotorFinanciero.TipoTasa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tienda")
    private Tienda tienda;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
    private String dni;
    private String nombres;
    private String direccion;
    private String telefono;
    @Enumerated(EnumType.STRING)
    private TipoTasa tipoTasa;
    private short diasPlazoTasa;
    private Short diasCapitalizacion;
    private BigDecimal tasa;
    @Enumerated(EnumType.STRING)
    private TipoTasa tipoTasaMora;
    private BigDecimal tasaMora;
    @Column(columnDefinition = "bpchar(3)")
    private String moneda = "PEN";
    private BigDecimal limiteCredito;
    private BigDecimal deudaVigente = BigDecimal.ZERO;
    private short maxMeses;
    private short diaCorte;
    private short diaPago;
    private String estado = "ACTIVO";
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public boolean activo() {
        return "ACTIVO".equals(estado);
    }

    public BigDecimal ted() {
        return MotorFinanciero.ted(tipoTasa, tasa, diasPlazoTasa, capitalizacion());
    }

    public BigDecimal tedMora() {
        return MotorFinanciero.ted(tipoTasaMora, tasaMora, diasPlazoTasa, capitalizacion());
    }

    private Integer capitalizacion() {
        return diasCapitalizacion == null ? 1 : diasCapitalizacion.intValue();
    }
}

