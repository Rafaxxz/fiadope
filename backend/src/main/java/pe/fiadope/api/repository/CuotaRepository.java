package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.Cuota;

import java.time.LocalDate;
import java.util.List;

public interface CuotaRepository extends JpaRepository<Cuota, Integer> {

    @Query("""
            select q from Cuota q join fetch q.compra c join fetch c.producto
            where c.cliente.id = :cliente and q.vencimiento = :fp and q.pagada = false
              and not exists (select i from EstadoCuentaItem i where i.cuota = q)
            order by c.fechaHora""")
    List<Cuota> porFacturar(@Param("cliente") Integer idCliente, @Param("fp") LocalDate fechaPago);
}

