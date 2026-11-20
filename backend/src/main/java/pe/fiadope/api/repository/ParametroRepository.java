package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.fiadope.api.domain.Parametro;

public interface ParametroRepository extends JpaRepository<Parametro, String> {
}
