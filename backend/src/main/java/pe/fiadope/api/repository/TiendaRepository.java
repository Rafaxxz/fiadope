package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.Tienda;

import java.util.List;

public interface TiendaRepository extends JpaRepository<Tienda, Integer> {
    boolean existsByRuc(String ruc);

    @Query("""
            select t from Tienda t
            where (cast(:q as string) is null or t.ruc like concat('%', cast(:q as string), '%') or lower(t.razonSocial) like lower(concat('%', cast(:q as string), '%')))
              and (cast(:giro as string) is null or t.giro = cast(:giro as string))
              and (cast(:estado as string) is null or t.estado = cast(:estado as string))
            order by t.fechaAlta desc""")
    List<Tienda> buscar(@Param("q") String q, @Param("giro") String giro, @Param("estado") String estado);
}

