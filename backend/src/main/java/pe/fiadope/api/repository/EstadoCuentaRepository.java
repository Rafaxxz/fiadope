package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.EstadoCuenta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EstadoCuentaRepository extends JpaRepository<EstadoCuenta, Integer> {
    Optional<EstadoCuenta> findByClienteIdAndFechaCorte(Integer idCliente, LocalDate fechaCorte);

    List<EstadoCuenta> findByClienteIdAndEstadoOrderByFechaCorteAsc(Integer idCliente, String estado);

    List<EstadoCuenta> findByClienteIdOrderByFechaCorteDesc(Integer idCliente);

    @Query("select e from EstadoCuenta e where e.id = :id and e.cliente.tienda.id = :t")
    Optional<EstadoCuenta> buscarEnTienda(@Param("id") Integer id, @Param("t") Integer idTienda);
}

