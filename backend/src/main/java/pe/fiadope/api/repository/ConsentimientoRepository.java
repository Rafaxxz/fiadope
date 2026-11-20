package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.fiadope.api.domain.Consentimiento;

import java.util.Optional;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Integer> {
    Optional<Consentimiento> findFirstByClienteIdOrderByFechaHoraDesc(Integer idCliente);
}

