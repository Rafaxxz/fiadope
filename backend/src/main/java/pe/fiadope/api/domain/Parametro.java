package pe.fiadope.api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametro")
@Getter
@Setter
@NoArgsConstructor
public class Parametro {
    @Id
    private String clave;
    private BigDecimal valor;
    private String descripcion;
    private LocalDateTime actualizado = LocalDateTime.now();
}

