package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    Optional<Producto> findByIdAndTiendaId(Integer id, Integer idTienda);

    boolean existsByTiendaIdAndSkuIgnoreCase(Integer idTienda, String sku);

    @Query("""
            select p from Producto p where p.tienda.id = :t
              and (cast(:categoria as string) is null or p.categoria = cast(:categoria as string))
              and (cast(:q as string) is null or lower(p.nombre) like lower(concat('%', cast(:q as string), '%')) or lower(p.sku) like lower(concat('%', cast(:q as string), '%')))
              and (cast(:estado as string) is null or p.estado = cast(:estado as string))
            order by p.nombre""")
    List<Producto> buscar(@Param("t") Integer idTienda, @Param("categoria") String categoria,
                          @Param("q") String q, @Param("estado") String estado);
}

