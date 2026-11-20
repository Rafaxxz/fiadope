package pe.fiadope.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimiento")
@Getter
@Setter
@NoArgsConstructor
public class Consentimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimiento")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;
    private String versionPolitica;
    private boolean acepta;
    private LocalDateTime fechaHora = LocalDateTime.now();
    @Column(columnDefinition = "inet")
    @ColumnTransformer(read = "host(ip)", write = "?::inet")
    private String ip;
}

