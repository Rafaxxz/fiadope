package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.Compra;
import pe.fiadope.api.domain.Modalidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @Query("select c from Compra c join fetch c.producto where c.id = :id and c.cliente.tienda.id = :t")
    Optional<Compra> buscarEnTienda(@Param("id") Integer id, @Param("t") Integer idTienda);

    @Query("select c from Compra c join fetch c.producto where c.cliente.id = :cliente order by c.fechaHora desc")
    List<Compra> deCliente(@Param("cliente") Integer idCliente);

    /** Compras a fin de mes que vencen en una fecha de pago y aún no están en un estado de cuenta. */
    @Query("""
            select c from Compra c join fetch c.producto
            where c.cliente.id = :cliente and c.modalidad = :modalidad and c.fechaPagoAsignada = :fp and c.pagada = false
              and not exists (select i from EstadoCuentaItem i where i.compra = c)
            order by c.fechaHora""")
    List<Compra> finDeMesPorFacturar(@Param("cliente") Integer idCliente, @Param("fp") LocalDate fechaPago,
                                     @Param("modalidad") Modalidad modalidad);

    /** Compras posteriores a un corte dentro del mismo mes (pasan al corte siguiente). */
    @Query("""
            select c from Compra c join fetch c.producto
            where c.cliente.id = :cliente and c.fechaHora >= :desde and c.fechaHora < :hasta
            order by c.fechaHora""")
    List<Compra> entreFechas(@Param("cliente") Integer idCliente, @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta);
}

