package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.fiadope.api.domain.Pago;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    Optional<Pago> findByEstadoCuentaId(Integer idEstado);
}

