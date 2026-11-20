package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.dto.Dtos.BitacoraDto;
import pe.fiadope.api.dto.Dtos.PaginaDto;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.repository.BitacoraRepository;
import pe.fiadope.api.security.UsuarioActual;
import pe.fiadope.api.util.Validaciones;

import java.time.LocalDate;
import java.util.List;

/** HU12: consulta de la bitácora (solo lectura). El administrador de negocio solo ve su tienda. */
@Service
@RequiredArgsConstructor
public class BitacoraConsultaService {

    private final BitacoraRepository repo;

    private Integer alcance(Integer idTiendaFiltro) {
        var p = UsuarioActual.get();
        return p.rol() == Rol.ADMIN_PLATAFORMA ? idTiendaFiltro : p.idTienda();
    }

    @Transactional(readOnly = true)
    public PaginaDto<BitacoraDto> buscar(String usuario, String accion, String ip, LocalDate desde, LocalDate hasta,
                                         Integer idTienda, int pagina, int tamano) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw ApiException.badRequest("El rango de fechas no es válido");
        }
        var page = repo.buscar(alcance(idTienda), Validaciones.vacioANull(usuario), Validaciones.vacioANull(accion),
                Validaciones.vacioANull(ip), desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamano), 100)));
        return new PaginaDto<>(page.getContent().stream().map(Mapper::bitacora).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<String> acciones() {
        return repo.acciones(alcance(null));
    }
}
