package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tienda")
    private Tienda tienda;
    private String login;
    private String nombre;
    private String email;
    private String hashClave;
    @Enumerated(EnumType.STRING)
    private Rol rol;
    private String mfaSecreto;
    private String mfaToken;
    private LocalDateTime mfaExpira;
    private short mfaIntentos;
    private short intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private boolean activo = true;
}

