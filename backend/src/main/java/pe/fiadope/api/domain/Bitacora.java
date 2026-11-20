package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Registro inalterable: Hibernate no genera UPDATE y la base bloquea UPDATE/DELETE con un trigger. */
@Entity
@Immutable
@Table(name = "bitacora")
@Getter
@Setter
@NoArgsConstructor
public class Bitacora {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Integer id;
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @Column(name = "id_tienda")
    private Integer idTienda;
    private String usuarioLogin;
    private String accion;
    private String tabla;
    private Integer idRegistro;
    @JdbcTypeCode(SqlTypes.JSON)
    private String valorAntes;
    @JdbcTypeCode(SqlTypes.JSON)
    private String valorDespues;
    private LocalDateTime fechaHora = LocalDateTime.now();
    @Column(columnDefinition = "inet")
    @ColumnTransformer(read = "host(ip)", write = "?::inet")
    private String ip;
}

