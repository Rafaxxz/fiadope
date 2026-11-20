package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tienda")
@Getter
@Setter
@NoArgsConstructor
public class Tienda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tienda")
    private Integer id;
    @Column(columnDefinition = "bpchar(11)")
    private String ruc;
    private String razonSocial;
    private String propietario;
    private String giro;
    private String ubicacion;
    private String direccion;
    private String telefono;
    private BigDecimal limiteGlobal;
    private String estado = "ACTIVA";
    private LocalDateTime fechaAlta = LocalDateTime.now();

    public boolean activa() {
        return "ACTIVA".equals(estado);
    }
}

