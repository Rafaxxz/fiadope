package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.Cliente;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByIdAndTiendaId(Integer id, Integer idTienda);

    Optional<Cliente> findByUsuarioId(Integer idUsuario);

    boolean existsByTiendaIdAndDni(Integer idTienda, String dni);

    List<Cliente> findByTiendaIdOrderByNombresAsc(Integer idTienda);

    List<Cliente> findByTiendaIdAndEstadoOrderByNombresAsc(Integer idTienda, String estado);

    @Query("select coalesce(sum(c.limiteCredito), 0) from Cliente c where c.tienda.id = :t and c.estado = 'ACTIVO' and (:excluir is null or c.id <> :excluir)")
    BigDecimal sumaLimites(@Param("t") Integer idTienda, @Param("excluir") Integer excluirCliente);

    @Query("""
            select c from Cliente c where c.tienda.id = :t
              and (cast(:q as string) is null or c.dni like concat('%', cast(:q as string), '%') or lower(c.nombres) like lower(concat('%', cast(:q as string), '%')))
            order by c.nombres""")
    List<Cliente> buscar(@Param("t") Integer idTienda, @Param("q") String q);
}

