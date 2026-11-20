package pe.fiadope.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.fiadope.api.domain.Bitacora;

import java.time.LocalDateTime;

public interface BitacoraRepository extends JpaRepository<Bitacora, Integer> {

    @Query(value = """
            select b.* from bitacora b
            where (cast(:tienda as integer) is null or b.id_tienda = cast(:tienda as integer))
              and (cast(:usuario as text) is null or b.usuario_login ilike concat('%', cast(:usuario as text), '%'))
              and (cast(:accion as text) is null or b.accion = cast(:accion as text))
              and (cast(:ip as text) is null or host(b.ip) like concat('%', cast(:ip as text), '%'))
              and (cast(:desde as timestamp) is null or b.fecha_hora >= cast(:desde as timestamp))
              and (cast(:hasta as timestamp) is null or b.fecha_hora < cast(:hasta as timestamp))
            order by b.fecha_hora desc, b.id_bitacora desc""",
            countQuery = """
            select count(*) from bitacora b
            where (cast(:tienda as integer) is null or b.id_tienda = cast(:tienda as integer))
              and (cast(:usuario as text) is null or b.usuario_login ilike concat('%', cast(:usuario as text), '%'))
              and (cast(:accion as text) is null or b.accion = cast(:accion as text))
              and (cast(:ip as text) is null or host(b.ip) like concat('%', cast(:ip as text), '%'))
              and (cast(:desde as timestamp) is null or b.fecha_hora >= cast(:desde as timestamp))
              and (cast(:hasta as timestamp) is null or b.fecha_hora < cast(:hasta as timestamp))""",
            nativeQuery = true)
    Page<Bitacora> buscar(@Param("tienda") Integer idTienda, @Param("usuario") String usuario,
                          @Param("accion") String accion, @Param("ip") String ip,
                          @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                          Pageable pageable);

    @Query(value = "select distinct accion from bitacora where (cast(:tienda as integer) is null or id_tienda = cast(:tienda as integer)) order by accion",
            nativeQuery = true)
    java.util.List<String> acciones(@Param("tienda") Integer idTienda);
}

