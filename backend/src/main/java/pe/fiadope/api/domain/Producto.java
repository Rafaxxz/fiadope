package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tienda")
    private Tienda tienda;
    private String nombre;
    private String marca;
    private String proveedor;
    private String unidad = "UND";
    private String categoria = "Abarrotes y granel";
    private String sku;
    private BigDecimal precioContado;
    private BigDecimal precioLista;
    private boolean permiteFinMes = true;
    private boolean permiteCuotas;
    private String imagenUrl;
    private String estado = "ACTIVO";

    public boolean activo() {
        return "ACTIVO".equals(estado);
    }
}

