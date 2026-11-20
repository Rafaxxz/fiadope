package pe.fiadope.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.fiadope.api.dto.Dtos.BitacoraDto;
import pe.fiadope.api.dto.Dtos.PaginaDto;
import pe.fiadope.api.service.BitacoraConsultaService;

import java.time.LocalDate;
import java.util.List;

/** HU12: solo lectura. No existen endpoints para modificar ni borrar registros. */
@Tag(name = "P12 Bitácora")
@RestController
@RequestMapping("/api/bitacora")
@RequiredArgsConstructor
public class BitacoraController {

    private final BitacoraConsultaService servicio;

    @GetMapping
    public PaginaDto<BitacoraDto> buscar(@RequestParam(required = false) String usuario,
                                         @RequestParam(required = false) String accion,
                                         @RequestParam(required = false) String ip,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                         @RequestParam(required = false) Integer idTienda,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "20") int tamano) {
        return servicio.buscar(usuario, accion, ip, desde, hasta, idTienda, pagina, tamano);
    }

    @GetMapping("/acciones")
    public List<String> acciones() {
        return servicio.acciones();
    }
}
